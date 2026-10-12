package io.github.dirnei.mc.servus.storage.packing;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.data.packet.RateBudget;
import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.foundation.production.TransferGate;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.DeliveryTarget;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseEvent;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.storage.menu.PackingStationMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import io.github.dirnei.mc.servus.storage.order.ProductionLedger;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.WirelessCost;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;

/**
 * The warehouse's door to the data line and its only throughput limit. Items
 * that arrive over the line land in the inherited intake buffer (To storage)
 * and are put away like at an Inbound Station; the outgoing buffer (To
 * network) receives what shuttles pick from the racks for shipments and sends
 * it on, one delivery at a time. It also carries the wireless line: a wireless
 * shipment leaves the To network buffer for a Packing Station of another site,
 * moved every tick and paid with FE by the receiving site's controller, with or
 * without a data line. As a {@link TransferGate} it converts at most
 * {@code items_per_second} per direction, the two directions independent, shared
 * by data line deliveries and wireless transfers.
 */
public class PackingStationBlockEntity extends InboundStationBlockEntity implements DeliveryTarget, DataLink, TransferGate {

    /**
     * Items on their way to one data member, or over the wireless line to a
     * Packing Station of another site. Its progress is a transfer in the
     * {@link ProductionLedger}, so whoever waits for it can book it by id.
     */
    public static final class Shipment {
        private final BlockPos target;
        private final ItemKey key;
        private final ProductionLedger.Transfer transfer;
        private final boolean wireless;
        private final String site;

        Shipment(BlockPos target, ItemKey key, ProductionLedger.Transfer transfer, boolean wireless, String site) {
            this.target = target;
            this.key = key;
            this.transfer = transfer;
            this.wireless = wireless;
            this.site = site == null ? "" : site;
        }

        /** Whether the shipment goes over the wireless line to a Packing Station of another site. */
        public boolean wireless() {
            return wireless;
        }

        /** The registry key of the receiving site, empty for a data line shipment. */
        public String site() {
            return site;
        }

        /** The ledger id of this shipment. */
        public long id() {
            return transfer.id();
        }

        public BlockPos target() {
            return target;
        }

        public ItemKey key() {
            return key;
        }

        public int planned() {
            return transfer.asked();
        }

        public int delivered() {
            return transfer.delivered();
        }

        public boolean ended() {
            return transfer.ended();
        }

        int missing() {
            return transfer.missing();
        }
    }

    /** A saved shipment, resolved against the ledger on the first tick after loading. */
    private record SavedShipment(long id, BlockPos target, ItemKey key, boolean wireless, String site) {
    }

    /** Ticks after the last wireless item moved during which the station counts as sending. */
    private static final int SENDING_TICKS = 20;

    private final ItemSlots outgoing = new ItemSlots(StorageStatsManager.INSTANCE.inboundBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            network().ifPresent(network -> network.post(new WarehouseEvent.OutputChanged(worldPosition)));
        }
    };

    private final List<Shipment> shipments = new ArrayList<>();
    private final List<SavedShipment> unresolved = new ArrayList<>();
    private Delivery running;
    private ServerLevel shippingLevel;
    private Shipment runningFor;
    private int sendTimer;
    private final RateBudget toStorage = new RateBudget(StorageStatsManager.INSTANCE.packingItemsPerSecond());
    private final RateBudget toNetwork = new RateBudget(StorageStatsManager.INSTANCE.packingItemsPerSecond());
    private final UpgradeSlots upgrades = new UpgradeSlots(StorageStatsManager.INSTANCE.packingUpgradeSlots(), this::setChanged);
    // The wireless transfer moving now, for the status line and the request rows.
    private long wirelessId = -1;
    private Component wirelessTo;
    private ItemKey wirelessKey;
    private int sentAgo = SENDING_TICKS + 1;
    /** Why the oldest waiting wireless transfer does not move, null while nothing waits. */
    private Component wirelessWaiting;
    private boolean wirelessWaitingAlert;

    public PackingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.PACKING_STATION.get(), pos, state);
    }

    @Override
    public ItemSlots deliveries() {
        return outgoing;
    }

    public ItemSlots outgoing() {
        return outgoing;
    }

    // Transfer gate

    @Override
    public int allowance(Flow flow) {
        return budget(flow).allowance();
    }

    @Override
    public void consume(Flow flow, int items) {
        budget(flow).consume(items);
        (flow == Flow.IN ? usedIn : usedOut).add(items);
    }

    /** Items per tick over the last second, for the Production Terminal. */
    private static final class Window {
        private final int[] ticks = new int[RateBudget.TICKS_PER_SECOND];
        private int slot;

        void tick() {
            slot = (slot + 1) % ticks.length;
            ticks[slot] = 0;
        }

        void add(int items) {
            ticks[slot] += Math.max(0, items);
        }

        int sum() {
            int total = 0;
            for (int value : ticks) {
                total += value;
            }
            return total;
        }
    }

    private final Window usedIn = new Window();
    private final Window usedOut = new Window();

    private RateBudget budget(Flow flow) {
        return flow == Flow.IN ? toStorage : toNetwork;
    }

    /** The speed upgrades; the only slots of the GUI a player can change. */
    public UpgradeSlots upgrades() {
        return upgrades;
    }

    /** Items per second converted in each direction: the base rate from data times the upgrades. */
    public double rate() {
        return StorageStatsManager.INSTANCE.packingItemsPerSecond() * upgrades.multiplier();
    }

    /** Items that entered the To storage buffer over the line, or left the To network buffer, during the last second. */
    public int used(Flow flow) {
        return flow == Flow.IN ? usedIn.sum() : usedOut.sum();
    }

    /** Refills both budgets by one tick, following the rate from data and upgrades. */
    protected void tickBudgets() {
        usedIn.tick();
        usedOut.tick();
        double rate = rate();
        if (toStorage.rate() != rate) {
            toStorage.setRate(rate);
            toNetwork.setRate(rate);
        }
        toStorage.tick();
        toNetwork.tick();
    }

    // Menu

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PackingStationMenu(containerId, playerInventory, this);
    }

    /**
     * The status line of the GUI: the delivery running now, or why the station
     * waits (no rail, offline, no data line, put-away blocked, To storage full,
     * waiting for a shuttle). Server side.
     */
    public PackingStatusPayload status() {
        String prefix = "gui.servus_storage.packing_station.status.";
        Optional<WarehouseNetwork> network = network();
        if (network.isEmpty()) {
            return status(Component.translatable(prefix + "no_rail"), true);
        }
        switch (network.get().status()) {
            case NO_CONTROLLER -> {
                return status(Component.translatable("gui.servus_storage.terminal.no_controller"), true);
            }
            case MULTIPLE_CONTROLLERS -> {
                return status(Component.translatable("gui.servus_storage.terminal.multiple_controllers"), true);
            }
            case ONLINE -> {
            }
        }
        if (!(level instanceof ServerLevel server)) {
            return status(Component.translatable(prefix + "no_data_line"), true);
        }
        boolean dataLine = DataNetworks.networkAt(server, worldPosition).isPresent();
        boolean capable = network.get().controller().map(WarehouseControllerBlockEntity::capable).orElse(false);
        if (!dataLine && !capable && wirelessWaiting == null && !wirelessSending()) {
            return status(Component.translatable(prefix + "no_data_line"), true);
        }
        if (wirelessSending() && wirelessKey != null) {
            return status(Component.translatable(prefix + "sending", wirelessKey.stack(1).getHoverName(), wirelessTo), false);
        }
        if (dataSending() && runningFor != null) {
            return status(Component.translatable(prefix + "sending",
                    runningFor.key.stack(1).getHoverName(), targetName(server, runningFor.target)), false);
        }
        if (blockedReason() != null) {
            String item = blockedStack().getHoverName().getString();
            String reason = blockedReason().name().toLowerCase(java.util.Locale.ROOT);
            String key = reason.equals("no_room") && blockedZone().isEmpty() ? "no_room_general" : reason;
            return status(Component.translatable("gui.servus_storage.station.blocked." + key, item, blockedZone()), true);
        }
        if (wirelessWaiting != null) {
            return status(wirelessWaiting, wirelessWaitingAlert);
        }
        if (toStorageFull()) {
            return status(Component.translatable(prefix + "to_storage_full"), true);
        }
        for (Shipment shipment : shipments) {
            if (!shipment.ended() && shipment.missing() > outgoingCount(shipment.key)) {
                return status(Component.translatable(prefix + "waiting_shuttle",
                        shipment.key.stack(1).getHoverName()), false);
            }
        }
        return status(Component.translatable(prefix + (dataLine ? "idle" : "wireless_only")), false);
    }

    private PackingStatusPayload status(Component line, boolean alert) {
        return new PackingStatusPayload(line, alert, (float) rate());
    }

    private static Component targetName(ServerLevel level, BlockPos target) {
        if (!level.isLoaded(target)) {
            return Component.literal(target.toShortString());
        }
        return level.getBlockState(target).getBlock().getName();
    }

    /** Whether the To storage buffer has no free slot left. */
    private boolean toStorageFull() {
        for (int slot = 0; slot < buffer().getSlots(); slot++) {
            ItemStack stack = buffer().getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    /** Open shipments, oldest first. */
    public List<Shipment> shipments() {
        return List.copyOf(shipments);
    }

    /** Registers a shipment of {@code planned} items; picks arrive in the outgoing buffer afterwards. */
    public Shipment addShipment(BlockPos target, ItemKey key, int planned) {
        Shipment shipment = new Shipment(target.immutable(), key, ledger().open(planned), false, "");
        shipments.add(shipment);
        setChanged();
        return shipment;
    }

    /**
     * Registers a wireless shipment of {@code planned} items to the Packing
     * Station at {@code target} of the site with key {@code site}; picks arrive
     * in the outgoing buffer afterwards and are moved every tick.
     */
    public Shipment addWirelessShipment(BlockPos target, String site, ItemKey key, int planned) {
        Shipment shipment = new Shipment(target.immutable(), key, ledger().open(planned), true, site);
        shipments.add(shipment);
        WirelessIncoming.add(level.dimension(), shipment.target, shipment.id());
        setChanged();
        return shipment;
    }

    private ProductionLedger ledger() {
        return ProductionLedger.get((ServerLevel) level);
    }

    /** A pick for a shipment of this item came up short: expect fewer items. */
    public void shortfall(ItemKey key, int count) {
        for (Shipment shipment : shipments) {
            if (count <= 0) {
                break;
            }
            if (!shipment.ended() && shipment.key.equals(key)) {
                int cut = Math.min(count, shipment.missing());
                ledger().shortfall(shipment.transfer, cut);
                count -= cut;
                endIfComplete(shipment);
            }
        }
        setChanged();
    }

    /** Whether packets leave over the data line or items over the wireless line right now. */
    public boolean sending() {
        return dataSending() || wirelessSending();
    }

    private boolean dataSending() {
        return running != null && !running.ended();
    }

    private boolean wirelessSending() {
        return sentAgo <= SENDING_TICKS;
    }

    /** Whether the items moving right now belong to the shipment with that id. */
    public boolean isSending(long shipmentId) {
        return dataSending() && runningFor != null && runningFor.id() == shipmentId
                || wirelessSending() && wirelessId == shipmentId;
    }

    /** Books items of a shipment that arrived; the shipment ends once nothing is missing. */
    private void credit(Shipment shipment, int count) {
        ledger().delivered(shipment.transfer, count);
        endIfComplete(shipment);
        setChanged();
    }

    /** Ends a shipment that cannot go on; what still waits in the outgoing buffer goes back into storage. */
    private void endShipment(Shipment shipment) {
        ledger().end(shipment.transfer);
        setChanged();
    }

    /** Drops ended shipments; a wireless one no longer counts as incoming at its receiver. */
    private void pruneEnded() {
        for (Iterator<Shipment> it = shipments.iterator(); it.hasNext(); ) {
            Shipment shipment = it.next();
            if (shipment.ended()) {
                forgetIncoming(shipment);
                it.remove();
            }
        }
    }

    private void forgetIncoming(Shipment shipment) {
        if (shipment.wireless && level != null) {
            WirelessIncoming.remove(level.dimension(), shipment.target, shipment.id());
        }
    }

    @Override
    public void setRemoved() {
        shipments.forEach(this::forgetIncoming);
        super.setRemoved();
    }

    private void endIfComplete(Shipment shipment) {
        if (shipment.missing() == 0) {
            ledger().end(shipment.transfer);
        }
    }

    /**
     * Ends a shipment early. Packets already on the line still land; what is
     * still waiting in the outgoing buffer goes back into storage.
     */
    public void cancel(long shipmentId) {
        for (Shipment shipment : shipments) {
            if (shipment.id() == shipmentId && !shipment.ended()) {
                ledger().end(shipment.transfer);
                setChanged();
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PackingStationBlockEntity point) {
        InboundStationBlockEntity.serverTick(level, pos, state, point);
        point.tickBudgets();
        if (level instanceof ServerLevel server) {
            point.shipWireless(server);
            if (++point.sendTimer >= StorageStatsManager.INSTANCE.pushInterval()) {
                point.sendTimer = 0;
                point.ship(server);
            }
        }
    }

    /** Starts the next data line delivery when none runs, and hands items nobody waits for to the intake. */
    private void ship(ServerLevel server) {
        if (!unresolved.isEmpty()) {
            resolve();
        }
        if (dataSending()) {
            return;
        }
        pruneEnded();
        for (Shipment shipment : shipments) {
            if (shipment.wireless) {
                continue;
            }
            int count = Math.min(shipment.missing(), outgoingCount(shipment.key));
            if (count <= 0) {
                continue;
            }
            // The items stay in the To network buffer until the move takes them at the station's rate.
            Optional<Delivery> started = DataPackets.move(server, worldPosition, shipment.target, shipment.key.stack(1), count, new ItemView(outgoing));
            if (started.isEmpty() || started.get().ended() && started.get().state() == Delivery.State.FAILED) {
                // Refused at once: this shipment cannot be served.
                ledger().end(shipment.transfer);
                continue;
            }
            running = started.get();
            runningFor = shipment;
            shippingLevel = server;
            running.onEnd(delivery -> finished(shipment, delivery));
            setActivity(true);
            return;
        }
        returnOrphans();
        if (!sending()) {
            setActivity(false);
        }
    }

    // Wireless

    /** The controller of this station's warehouse while it is online. */
    private Optional<WarehouseControllerBlockEntity> controller() {
        return network().filter(network -> network.status() == WarehouseNetwork.Status.ONLINE).flatMap(WarehouseNetwork::controller);
    }

    /** The name of a site by its registry key, or the key. */
    private static Component siteName(ServerLevel level, String site) {
        return Component.literal(SiteRegistry.of(level).site(site).map(io.github.dirnei.mc.servus.storage.site.SiteRecord::name).orElse(site));
    }

    /** How many of up to {@code max} items like the key the To storage buffer takes now. */
    private int intakeRoom(ItemKey key, int max) {
        if (max <= 0) {
            return 0;
        }
        return max - Transfer.insertItemStacked(intake(), key.stack(max), true).getCount();
    }

    private void waitWireless(Component why, boolean alert) {
        wirelessWaiting = why;
        wirelessWaitingAlert = alert;
    }

    /**
     * Moves items of the oldest wireless shipment that has some waiting in To
     * network to its receiving Packing Station, as far as both stations' rates,
     * the receiver's room and the FE of the receiving controller allow. Both
     * controllers must be wireless capable; a receiver whose site is not loaded
     * makes the shipment wait, a receiver that is gone ends it.
     */
    private void shipWireless(ServerLevel server) {
        sentAgo++;
        wirelessWaiting = null;
        if (!unresolved.isEmpty()) {
            resolve();
        }
        pruneEnded();
        boolean waiting = false;
        for (Shipment shipment : shipments) {
            waiting |= shipment.wireless && shipment.missing() > 0;
        }
        if (!waiting) {
            return;
        }
        String prefix = "gui.servus_storage.packing_station.status.";
        Optional<WarehouseControllerBlockEntity> here = controller();
        if (here.isEmpty() || !here.get().capable()) {
            // Open transfers stop and go on once the controller is capable again; the warehouse side keeps working.
            boolean upgrade = here.map(WarehouseControllerBlockEntity::hasWirelessUpgrade).orElse(false);
            waitWireless(Component.translatable(prefix + (upgrade ? "no_power_here" : "no_wireless_here")), true);
            return;
        }
        for (Shipment shipment : new ArrayList<>(shipments)) {
            if (!shipment.wireless || shipment.ended()) {
                continue;
            }
            int ready = Math.min(shipment.missing(), outgoingCount(shipment.key));
            if (ready <= 0) {
                continue;
            }
            Component name = siteName(server, shipment.site);
            if (!server.isLoaded(shipment.target)) {
                waitWireless(Component.translatable(prefix + "receiver_asleep", name), false);
                continue;
            }
            if (!(server.getBlockEntity(shipment.target) instanceof PackingStationBlockEntity to) || to == this) {
                endShipment(shipment);
                continue;
            }
            Optional<WarehouseControllerBlockEntity> there = to.controller();
            if (there.isEmpty()) {
                waitWireless(Component.translatable(prefix + "receiver_asleep", name), false);
                continue;
            }
            if (here.get().team().isEmpty() || !here.get().team().equals(there.get().team())) {
                endShipment(shipment);
                continue;
            }
            if (!there.get().capable()) {
                waitWireless(Component.translatable(prefix + (there.get().hasWirelessUpgrade() ? "no_power_there" : "no_wireless_there"), name), true);
                continue;
            }
            double perItem = WirelessCost.perItem(Math.sqrt(here.get().getBlockPos().distSqr(there.get().getBlockPos())),
                    StorageStatsManager.INSTANCE.wirelessFePerItem(), StorageStatsManager.INSTANCE.wirelessFePerItemPerBlock());
            int room = to.intakeRoom(shipment.key, ready);
            int paid = WirelessCost.affordable(there.get().energyStored(), perItem, ready);
            int moved = WirelessCost.movable(ready, shipment.missing(), allowance(Flow.OUT), to.allowance(Flow.IN), room, paid);
            if (moved <= 0) {
                if (paid <= 0) {
                    there.get().refuseEnergy();
                    waitWireless(Component.translatable(prefix + "no_power_there", name), true);
                } else if (room <= 0) {
                    waitWireless(Component.translatable(prefix + "receiver_full", name), true);
                }
                return;
            }
            ItemStack taken = takeOutgoing(shipment.key, moved);
            ItemStack left = Transfer.insertItemStacked(to.intake(), taken, false);
            int arrived = taken.getCount() - left.getCount();
            if (!left.isEmpty()) {
                outgoing.insertItemStacked(left, false);
            }
            if (arrived > 0) {
                there.get().spendEnergy(WirelessCost.cost(arrived, perItem));
                consume(Flow.OUT, arrived);
                to.consume(Flow.IN, arrived);
                credit(shipment, arrived);
                wirelessId = shipment.id();
                wirelessTo = name;
                wirelessKey = shipment.key;
                sentAgo = 0;
                setActivity(true);
            }
            return;
        }
    }

    private ItemStack takeOutgoing(ItemKey key, int count) {
        ItemStack result = ItemStack.EMPTY;
        for (int slot = 0; slot < outgoing.getSlots() && result.getCount() < count; slot++) {
            ItemStack stack = outgoing.getStackInSlot(slot);
            if (stack.isEmpty() || !key.matches(stack)) {
                continue;
            }
            ItemStack taken = outgoing.extractItem(slot, count - result.getCount(), false);
            if (result.isEmpty()) {
                result = taken;
            } else {
                result.grow(taken.getCount());
            }
        }
        return result;
    }

    /**
     * Whether a wireless shipment of this station waits for power: this site's
     * controller or the loaded receiving site's controller is not wireless
     * capable. Never loads the receiver's chunk.
     */
    public boolean waitsForPower(long shipmentId) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (Shipment shipment : shipments) {
            if (shipment.id() == shipmentId && shipment.wireless && !shipment.ended()) {
                if (controller().filter(WarehouseControllerBlockEntity::capable).isEmpty()) {
                    return true;
                }
                return server.isLoaded(shipment.target) && server.getBlockEntity(shipment.target) instanceof PackingStationBlockEntity to
                        && to.controller().filter(controller -> !controller.capable()).isPresent();
            }
        }
        return false;
    }

    private void finished(Shipment shipment, Delivery delivery) {
        ProductionLedger ledger = ProductionLedger.get(shippingLevel);
        ledger.delivered(shipment.transfer, delivery.delivered());
        if (delivery.state() == Delivery.State.FAILED || shipment.missing() == 0) {
            ledger.end(shipment.transfer);
        }
        if (runningFor == shipment) {
            running = null;
            runningFor = null;
        }
        setChanged();
    }

    /** Moves items in the outgoing buffer that no open shipment wants into the intake, to be put away. */
    protected void returnOrphans() {
        for (int slot = 0; slot < outgoing.getSlots(); slot++) {
            ItemStack stack = outgoing.getStackInSlot(slot);
            if (stack.isEmpty() || wanted(ItemKey.of(stack))) {
                continue;
            }
            ItemStack left = buffer().insertItemStacked(stack.copy(), false);
            outgoing.setStackInSlot(slot, left);
        }
    }

    private boolean wanted(ItemKey key) {
        for (Shipment shipment : shipments) {
            if (!shipment.ended() && shipment.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the saved shipments in the ledger again and lets each wait only for
     * what the outgoing buffer holds for it, oldest first: picks that were on
     * their way may have been lost with the reload. The rest ends short.
     */
    private void resolve() {
        ProductionLedger ledger = ledger();
        for (SavedShipment saved : unresolved) {
            ProductionLedger.Transfer transfer = ledger.transfer(saved.id());
            if (transfer != null && !transfer.ended()) {
                Shipment shipment = new Shipment(saved.target(), saved.key(), transfer, saved.wireless(), saved.site());
                shipments.add(shipment);
                if (shipment.wireless) {
                    WirelessIncoming.add(level.dimension(), shipment.target, shipment.id());
                }
            }
        }
        unresolved.clear();
        List<ItemKey> keys = new ArrayList<>();
        for (Shipment shipment : shipments) {
            if (!keys.contains(shipment.key)) {
                keys.add(shipment.key);
            }
        }
        for (ItemKey key : keys) {
            List<Shipment> ofKey = shipments.stream().filter(shipment -> shipment.key.equals(key)).toList();
            List<Integer> cuts = ShipmentCap.shortfalls(ofKey.stream().map(Shipment::missing).toList(), outgoingCount(key));
            for (int i = 0; i < ofKey.size(); i++) {
                ledger.shortfall(ofKey.get(i).transfer, cuts.get(i));
                endIfComplete(ofKey.get(i));
            }
        }
        setChanged();
    }

    protected int outgoingCount(ItemKey key) {
        int count = 0;
        for (int slot = 0; slot < outgoing.getSlots(); slot++) {
            ItemStack stack = outgoing.getStackInSlot(slot);
            if (!stack.isEmpty() && ItemKey.of(stack).equals(key)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    protected void setActivity(boolean working) {
        if (level == null || level.isClientSide() || !isEmpty()) {
            return;
        }
        if (working) {
            clearBlockedWorking();
        } else {
            clearBlocked();
        }
    }

    /** The orange lamp while packets leave, even when the intake is empty. */
    private void clearBlockedWorking() {
        BlockState state = getBlockState();
        if (state.hasProperty(StorageNodeBlocks.ACTIVITY)) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ACTIVITY,
                    StorageNodeBlocks.Activity.WORKING), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        upgrades.takeAll().forEach(stack -> Block.popResource(level, pos, stack));
        for (int i = 0; i < outgoing.getSlots(); i++) {
            ItemStack stack = outgoing.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                outgoing.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // Persistence

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Outgoing", outgoing.serializeNBT(registries));
        tag.put("Upgrades", upgrades.serializeNBT(registries));
        ListTag list = new ListTag();
        for (Shipment shipment : shipments) {
            if (!shipment.ended()) {
                list.add(savedShipment(new SavedShipment(shipment.id(), shipment.target, shipment.key, shipment.wireless, shipment.site), registries));
            }
        }
        for (SavedShipment saved : unresolved) {
            list.add(savedShipment(saved, registries));
        }
        tag.put("Shipments", list);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        if (tag.contains("Outgoing")) {
            ItemSlots saved = new ItemSlots();
            saved.deserializeNBT(registries, tag.getCompoundOrEmpty("Outgoing"));
            for (int i = 0; i < outgoing.getSlots(); i++) {
                outgoing.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        if (tag.contains("Upgrades")) {
            ItemSlots saved = new ItemSlots();
            saved.deserializeNBT(registries, tag.getCompoundOrEmpty("Upgrades"));
            for (int i = 0; i < upgrades.getSlots(); i++) {
                upgrades.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        shipments.clear();
        unresolved.clear();
        for (Tag raw : tag.getListOrEmpty("Shipments")) {
            CompoundTag entry = (CompoundTag) raw;
            Optional<BlockPos> target = ValueIo.readPos(entry, "Target");
            ItemStack item = ValueIo.parseStack(registries, entry.getCompoundOrEmpty("Item"));
            if (target.isPresent() && !item.isEmpty() && entry.contains("Id")) {
                unresolved.add(new SavedShipment(entry.getLongOr("Id", 0L), target.get(), ItemKey.of(item),
                        entry.getBooleanOr("Wireless", false), entry.getStringOr("Site", "")));
            }
        }
    }

    private static CompoundTag savedShipment(SavedShipment saved, HolderLookup.Provider registries) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("Id", saved.id());
        entry.put("Target", ValueIo.posTag(saved.target()));
        entry.put("Item", ValueIo.saveStack(registries, saved.key().stack(1)));
        if (saved.wireless()) {
            entry.putBoolean("Wireless", true);
            entry.putString("Site", saved.site());
        }
        return entry;
    }
}
