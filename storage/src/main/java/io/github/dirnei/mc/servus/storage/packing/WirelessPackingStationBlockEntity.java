package io.github.dirnei.mc.servus.storage.packing;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.foundation.production.TransferGate;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.site.WirelessCost;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Packing Station whose far side is a wireless line instead of the data
 * line: the warehouse side (rails, picks into To network, put-away from To
 * storage) is the Packing Station's, but its shipments go to another Wireless
 * Packing Station of the same team in the same dimension, moved here every
 * tick at both stations' rates and paid with FE by the receiving station.
 */
public class WirelessPackingStationBlockEntity extends PackingStationBlockEntity {

    /** Ticks after the last moved item during which the station counts as sending. */
    private static final int SENDING_TICKS = 20;

    /** The FE buffer: takes FE from any side, never gives any out. */
    private final class StationEnergy extends SimpleEnergyHandler {

        StationEnergy(int capacity) {
            super(capacity, capacity, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }

        int getEnergyStored() {
            return energy;
        }

        int getMaxEnergyStored() {
            return capacity;
        }

        void spend(long amount) {
            energy = (int) Math.max(0, energy - amount);
            setChanged();
        }

        void setStored(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }
    }

    private UUID team;
    private final StationEnergy energy = new StationEnergy(StorageStatsManager.INSTANCE.wirelessEnergyCapacity());
    private long sendingId = -1;
    private BlockPos sendingTo;
    private ItemKey sendingKey;
    private int sentAgo = SENDING_TICKS + 1;
    /** Why the oldest waiting transfer does not move, null while nothing waits for the receiver. */
    private Component waiting;

    public WirelessPackingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.WIRELESS_PACKING_STATION.get(), pos, state);
    }

    // Team and energy

    /** The team of the player who placed the station; its wireless line is that team's. */
    public Optional<UUID> team() {
        return Optional.ofNullable(team);
    }

    public void setTeam(UUID team) {
        this.team = team;
        setChanged();
        network().ifPresent(WarehouseNetwork::wirelessChanged);
    }

    public EnergyHandler energy() {
        return energy;
    }

    /** For GameTests and the status line: FE stored. */
    public int energyStored() {
        return energy.getEnergyStored();
    }

    /** For GameTests: sets the FE stored. */
    public void setEnergy(int amount) {
        energy.setStored(amount);
        setChanged();
    }

    @Override
    public double rate() {
        return StorageStatsManager.INSTANCE.wirelessItemsPerSecond() * upgrades().multiplier();
    }

    // Sending

    public static void serverTick(Level level, BlockPos pos, BlockState state, WirelessPackingStationBlockEntity point) {
        InboundStationBlockEntity.serverTick(level, pos, state, point);
        point.tickBudgets();
        if (level instanceof ServerLevel server) {
            point.ship(server);
        }
    }

    @Override
    public boolean sending() {
        return sentAgo <= SENDING_TICKS;
    }

    @Override
    public boolean isSending(long shipmentId) {
        return sending() && sendingId == shipmentId;
    }

    /** The receiving station at a position: loaded, wireless and of this station's team. Empty when it is not loaded. */
    private Optional<WirelessPackingStationBlockEntity> receiver(ServerLevel server, BlockPos target) {
        if (!server.isLoaded(target)) {
            return Optional.empty();
        }
        return server.getBlockEntity(target) instanceof WirelessPackingStationBlockEntity to && team != null
                && to.team().filter(team::equals).isPresent() && to != this ? Optional.of(to) : Optional.empty();
    }

    /** FE every item costs between this station and another. */
    public double costPerItem(BlockPos other) {
        return WirelessCost.perItem(Math.sqrt(worldPosition.distSqr(other)), StorageStatsManager.INSTANCE.wirelessFePerItem(),
                StorageStatsManager.INSTANCE.wirelessFePerItemPerBlock());
    }

    /** How many of up to {@code max} items like the key the To storage buffer takes now. */
    int intakeRoom(ItemKey key, int max) {
        if (max <= 0) {
            return 0;
        }
        return max - Transfer.insertItemStacked(intake(), key.stack(max), true).getCount();
    }

    /**
     * Moves items of the oldest shipment that has some waiting in To network
     * to its receiver, as far as both rates, the receiver's room and its FE
     * allow. A receiver that is loaded but no longer a station of the team ends
     * the shipment; one that is not loaded makes it wait.
     */
    @Override
    protected void ship(ServerLevel server) {
        sentAgo++;
        waiting = null;
        for (Shipment shipment : new ArrayList<>(liveShipments())) {
            int ready = Math.min(missingOf(shipment), outgoingCount(shipment.key()));
            if (ready <= 0) {
                continue;
            }
            if (!server.isLoaded(shipment.target())) {
                waiting = Component.translatable("gui.servus_storage.wireless.status.receiver_asleep");
                continue;
            }
            Optional<WirelessPackingStationBlockEntity> found = receiver(server, shipment.target());
            if (found.isEmpty()) {
                endShipment(shipment);
                continue;
            }
            WirelessPackingStationBlockEntity to = found.get();
            double perItem = costPerItem(to.getBlockPos());
            int room = to.intakeRoom(shipment.key(), ready);
            int paid = WirelessCost.affordable(to.energy.getEnergyStored(), perItem, ready);
            int moved = WirelessCost.movable(ready, missingOf(shipment), allowance(TransferGate.Flow.OUT),
                    to.allowance(TransferGate.Flow.IN), room, paid);
            if (moved <= 0) {
                if (paid <= 0) {
                    waiting = Component.translatable("gui.servus_storage.wireless.status.needs_power",
                            to.siteName(), (int) Math.ceil(perItem));
                    to.waiting = Component.translatable("gui.servus_storage.wireless.status.needs_power_here", (int) Math.ceil(perItem));
                } else if (room <= 0) {
                    waiting = Component.translatable("gui.servus_storage.wireless.status.receiver_full", to.siteName());
                }
                return;
            }
            ItemStack taken = takeOutgoing(shipment.key(), moved);
            ItemStack left = Transfer.insertItemStacked(to.intake(), taken, false);
            int arrived = taken.getCount() - left.getCount();
            if (!left.isEmpty()) {
                outgoing().insertItemStacked(left, false);
            }
            if (arrived > 0) {
                to.energy.spend(WirelessCost.cost(arrived, perItem));
                consume(TransferGate.Flow.OUT, arrived);
                to.consume(TransferGate.Flow.IN, arrived);
                credit(shipment, arrived);
                sendingId = shipment.id();
                sendingTo = to.getBlockPos();
                sendingKey = shipment.key();
                sentAgo = 0;
                setActivity(true);
            }
            return;
        }
        returnOrphans();
        if (!sending()) {
            setActivity(false);
        }
    }

    private ItemStack takeOutgoing(ItemKey key, int count) {
        ItemStack result = ItemStack.EMPTY;
        for (int slot = 0; slot < outgoing().getSlots() && result.getCount() < count; slot++) {
            ItemStack stack = outgoing().getStackInSlot(slot);
            if (stack.isEmpty() || !key.matches(stack)) {
                continue;
            }
            ItemStack taken = outgoing().extractItem(slot, count - result.getCount(), false);
            if (result.isEmpty()) {
                result = taken;
            } else {
                result.grow(taken.getCount());
            }
        }
        return result;
    }

    /** The name of the site this station belongs to, or its position. */
    public Component siteName() {
        return network().flatMap(WarehouseNetwork::controller).map(controller -> Component.literal(controller.displayName()))
                .orElse(Component.literal(worldPosition.toShortString()));
    }

    // Status

    @Override
    public PackingStatusPayload status() {
        String prefix = "gui.servus_storage.packing_station.status.";
        Optional<WarehouseNetwork> network = network();
        if (network.isEmpty()) {
            return line(Component.translatable(prefix + "no_rail"), true);
        }
        switch (network.get().status()) {
            case NO_CONTROLLER -> {
                return line(Component.translatable("gui.servus_storage.terminal.no_controller"), true);
            }
            case MULTIPLE_CONTROLLERS -> {
                return line(Component.translatable("gui.servus_storage.terminal.multiple_controllers"), true);
            }
            case ONLINE -> {
            }
        }
        if (team == null) {
            return line(Component.translatable("gui.servus_storage.wireless.status.no_team"), true);
        }
        if (sending() && sendingKey != null && level instanceof ServerLevel server) {
            Component target = receiver(server, sendingTo).map(WirelessPackingStationBlockEntity::siteName)
                    .orElse(Component.literal(sendingTo.toShortString()));
            return line(Component.translatable("gui.servus_storage.wireless.status.sending", sendingKey.stack(1).getHoverName(), target), false);
        }
        if (waiting != null) {
            return line(waiting, true);
        }
        if (blockedReason() != null) {
            String item = blockedStack().getHoverName().getString();
            String reason = blockedReason().name().toLowerCase(java.util.Locale.ROOT);
            String key = reason.equals("no_room") && blockedZone().isEmpty() ? "no_room_general" : reason;
            return line(Component.translatable("gui.servus_storage.station.blocked." + key, item, blockedZone()), true);
        }
        for (Shipment shipment : shipments()) {
            if (!shipment.ended() && missingOf(shipment) > outgoingCount(shipment.key())) {
                return line(Component.translatable(prefix + "waiting_shuttle", shipment.key().stack(1).getHoverName()), false);
            }
        }
        return line(Component.translatable("gui.servus_storage.wireless.status.idle", energy.getEnergyStored(), energy.getMaxEnergyStored()), false);
    }

    private PackingStatusPayload line(Component text, boolean alert) {
        return new PackingStatusPayload(text, alert, (float) rate());
    }

    // Persistence

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        if (team != null) {
            tag.store("Team", UUIDUtil.CODEC, team);
        }
        output.putInt("Energy", energy.getEnergyStored());
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        team = tag.read("Team", UUIDUtil.CODEC).isPresent() ? tag.read("Team", UUIDUtil.CODEC).orElseThrow() : null;
        energy.setStored(input.getIntOr("Energy", 0));
    }
}
