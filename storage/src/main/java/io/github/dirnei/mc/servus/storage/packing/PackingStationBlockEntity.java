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
import java.util.ArrayList;
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
 * it on, one delivery at a time. As a {@link TransferGate} it converts at most
 * {@code items_per_second} per direction, the two directions independent.
 */
public class PackingStationBlockEntity extends InboundStationBlockEntity implements DeliveryTarget, DataLink, TransferGate {

    /**
     * Items on their way to one data member. Its progress is a transfer in the
     * {@link ProductionLedger}, so whoever waits for it can book it by id.
     */
    public static final class Shipment {
        private final BlockPos target;
        private final ItemKey key;
        private final ProductionLedger.Transfer transfer;

        Shipment(BlockPos target, ItemKey key, ProductionLedger.Transfer transfer) {
            this.target = target;
            this.key = key;
            this.transfer = transfer;
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
    private record SavedShipment(long id, BlockPos target, ItemKey key) {
    }

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
    private final LegacyItemHandler[] faces = new LegacyItemHandler[6];
    private final RateBudget toStorage = new RateBudget(StorageStatsManager.INSTANCE.packingItemsPerSecond());
    private final RateBudget toNetwork = new RateBudget(StorageStatsManager.INSTANCE.packingItemsPerSecond());
    private final UpgradeSlots upgrades = new UpgradeSlots(StorageStatsManager.INSTANCE.packingUpgradeSlots(), this::setChanged);

    public PackingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.PACKING_STATION.get(), pos, state);
    }

    /** For the Wireless Packing Station, which keeps this warehouse side with its own type. */
    protected PackingStationBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ItemSlots deliveries() {
        return outgoing;
    }

    public ItemSlots outgoing() {
        return outgoing;
    }

    /**
     * Packets may arrive through any face, the front included. An insert
     * through a face counts for the CPU step running on the block behind that
     * face, so a producer that pushes its outputs in on its own is counted.
     */
    @Override
    public ResourceHandler<ItemResource> handlerFor(Direction side) {
        if (side == null) {
            return intake();
        }
        LegacyItemHandler handler = faces[side.ordinal()];
        if (handler == null) {
            handler = new PushCredit(side);
            faces[side.ordinal()] = handler;
        }
        return handler;
    }

    /** The intake seen through one face; real inserts are credited to the neighbour's running step. */
    private final class PushCredit extends LegacyItemHandler {

        private final Direction side;

        private PushCredit(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return intake().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return intake().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack left = intake().insertItem(slot, stack, simulate);
            int inserted = stack.getCount() - left.getCount();
            if (!simulate && inserted > 0 && level instanceof ServerLevel server) {
                CpuOrders.creditPushed(server, worldPosition, worldPosition.relative(side), ItemKey.of(stack), inserted);
            }
            return left;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return intake().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return intake().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return intake().isItemValid(slot, stack);
        }
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
        if (!(level instanceof ServerLevel server) || DataNetworks.networkAt(server, worldPosition).isEmpty()) {
            return status(Component.translatable(prefix + "no_data_line"), true);
        }
        if (sending() && runningFor != null) {
            return status(Component.translatable(prefix + "sending",
                    runningFor.key.stack(1).getHoverName(), targetName(server, runningFor.target)), false);
        }
        if (blockedReason() != null) {
            String item = blockedStack().getHoverName().getString();
            String reason = blockedReason().name().toLowerCase(java.util.Locale.ROOT);
            String key = reason.equals("no_room") && blockedZone().isEmpty() ? "no_room_general" : reason;
            return status(Component.translatable("gui.servus_storage.station.blocked." + key, item, blockedZone()), true);
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
        return status(Component.translatable(prefix + "idle"), false);
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
        Shipment shipment = new Shipment(target.immutable(), key, ledger().open(planned));
        shipments.add(shipment);
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

    public boolean sending() {
        return running != null && !running.ended();
    }

    /** Whether the packets on the line right now belong to the shipment with that id. */
    public boolean isSending(long shipmentId) {
        return sending() && runningFor != null && runningFor.id() == shipmentId;
    }

    // Hooks for the Wireless Packing Station, which moves the items of its shipments itself

    /** The open shipments, oldest first, after saved ones were found in the ledger again. */
    protected List<Shipment> liveShipments() {
        if (!unresolved.isEmpty()) {
            resolve();
        }
        shipments.removeIf(Shipment::ended);
        return shipments;
    }

    /** How many items a shipment still waits for. */
    protected static int missingOf(Shipment shipment) {
        return shipment.missing();
    }

    /** Books items of a shipment that arrived; the shipment ends once nothing is missing. */
    protected void credit(Shipment shipment, int count) {
        ledger().delivered(shipment.transfer, count);
        endIfComplete(shipment);
        setChanged();
    }

    /** Ends a shipment that cannot go on; what still waits in the outgoing buffer goes back into storage. */
    protected void endShipment(Shipment shipment) {
        ledger().end(shipment.transfer);
        setChanged();
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
        if (level instanceof ServerLevel server && ++point.sendTimer >= StorageStatsManager.INSTANCE.pushInterval()) {
            point.sendTimer = 0;
            point.ship(server);
        }
    }

    /** Starts the next delivery when none runs, and hands items nobody waits for to the intake. */
    protected void ship(ServerLevel server) {
        if (!unresolved.isEmpty()) {
            resolve();
        }
        if (sending()) {
            return;
        }
        shipments.removeIf(Shipment::ended);
        for (Shipment shipment : shipments) {
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
        setActivity(false);
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
                shipments.add(new Shipment(saved.target(), saved.key(), transfer));
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
                list.add(savedShipment(shipment.id(), shipment.target, shipment.key, registries));
            }
        }
        for (SavedShipment saved : unresolved) {
            list.add(savedShipment(saved.id(), saved.target(), saved.key(), registries));
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
                unresolved.add(new SavedShipment(entry.getLongOr("Id", 0L), target.get(), ItemKey.of(item)));
            }
        }
    }

    private static CompoundTag savedShipment(long id, BlockPos target, ItemKey key, HolderLookup.Provider registries) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("Id", id);
        entry.put("Target", ValueIo.posTag(target));
        entry.put("Item", ValueIo.saveStack(registries, key.stack(1)));
        return entry;
    }
}
