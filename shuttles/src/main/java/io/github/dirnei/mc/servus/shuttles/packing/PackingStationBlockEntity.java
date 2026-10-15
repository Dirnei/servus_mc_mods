package io.github.dirnei.mc.servus.shuttles.packing;

import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.data.packet.RateBudget;
import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.foundation.production.TransferGate;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.PackingStationMenu;
import io.github.dirnei.mc.servus.shuttles.network.DeliveryTarget;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseEvent;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.order.ProductionLedger;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.site.SiteRecord;
import io.github.dirnei.mc.servus.shuttles.site.SiteRegistry;
import io.github.dirnei.mc.servus.shuttles.site.WirelessCost;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The warehouse's door to the data line and its only throughput limit. Items
 * that arrive over the line land in the inherited intake buffer (To storage)
 * and are put away like at an Inbound Station; the outgoing buffer (To
 * network) receives what shuttles pick from the racks for shipments and sends
 * it on, one delivery at a time. It also carries the wireless line: a wireless
 * shipment leaves the To network buffer for a Packing Station of another site,
 * moved every tick and paid with FE by the receiving site's controller, with or
 * without a data line. A transfer requested at a Storage Terminal lands in the
 * receiver's To network buffer and goes on to that terminal as a relay, a
 * data line shipment fed by the transfer. As a {@link TransferGate} it converts at most
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
        private final BlockPos terminal;
        private final long feed;

        Shipment(BlockPos target, ItemKey key, ProductionLedger.Transfer transfer, boolean wireless, String site, BlockPos terminal, long feed) {
            this.target = target;
            this.key = key;
            this.transfer = transfer;
            this.wireless = wireless;
            this.site = site == null ? "" : site;
            this.terminal = terminal;
            this.feed = feed;
        }

        /**
         * The Storage Terminal a wireless shipment was requested at: its items
         * land in the receiver's To network buffer and go on to that terminal.
         * Empty for operator and test shipments, which are put away.
         */
        public Optional<BlockPos> terminal() {
            return Optional.ofNullable(terminal);
        }

        /** Whether this data line shipment passes on the items of a wireless transfer instead of shuttle picks. */
        public boolean relay() {
            return feed >= 0;
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
    private record SavedShipment(long id, BlockPos target, ItemKey key, boolean wireless, String site, BlockPos terminal, long feed) {
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

    /** Ticks after the last wireless item moved during which the station counts as sending. */
    private static final int SENDING_TICKS = 20;
    private static final String STATUS = "gui.servus_shuttles.packing_station.status.";

    private final ItemSlots outgoing = new ItemSlots(ShuttlesStatsManager.INSTANCE.inboundBufferSlots()) {
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
    private final RateBudget toStorage = new RateBudget(ShuttlesStatsManager.INSTANCE.packingItemsPerSecond());
    private final RateBudget toNetwork = new RateBudget(ShuttlesStatsManager.INSTANCE.packingItemsPerSecond());
    private final Window usedIn = new Window();
    private final Window usedOut = new Window();
    private final UpgradeSlots upgrades = new UpgradeSlots(ShuttlesStatsManager.INSTANCE.packingUpgradeSlots(), this::setChanged);
    /** The ledger id of the wireless transfer moving now, for the status line and the request rows. */
    private long wirelessId = -1;
    private Component wirelessTo;
    private ItemKey wirelessKey;
    private int sentAgo = SENDING_TICKS + 1;
    /** Why the oldest waiting wireless transfer does not move, null while nothing waits. */
    private Component wirelessWaiting;
    private boolean wirelessWaitingAlert;

    public PackingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.PACKING_STATION.get(), pos, state);
    }

    @Override
    public ItemSlots deliveries() {
        return outgoing;
    }

    public ItemSlots outgoing() {
        return outgoing;
    }

    @Override
    public int allowance(Flow flow) {
        return budget(flow).allowance();
    }

    @Override
    public void consume(Flow flow, int items) {
        budget(flow).consume(items);
        (flow == Flow.IN ? usedIn : usedOut).add(items);
    }

    private RateBudget budget(Flow flow) {
        return flow == Flow.IN ? toStorage : toNetwork;
    }

    /** The speed upgrades; the only slots of the GUI a player can change. */
    public UpgradeSlots upgrades() {
        return upgrades;
    }

    /** Items per second converted in each direction: the base rate from data times the upgrades. */
    public double rate() {
        return ShuttlesStatsManager.INSTANCE.packingItemsPerSecond() * upgrades.multiplier();
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
        Optional<WarehouseNetwork> network = network();
        if (network.isEmpty()) {
            return status(Component.translatable(STATUS + "no_rail"), true);
        }
        switch (network.get().status()) {
            case NO_CONTROLLER -> {
                return status(Component.translatable("gui.servus_shuttles.terminal.no_controller"), true);
            }
            case MULTIPLE_CONTROLLERS -> {
                return status(Component.translatable("gui.servus_shuttles.terminal.multiple_controllers"), true);
            }
            case ONLINE -> {
            }
        }
        if (!(level instanceof ServerLevel server)) {
            return status(Component.translatable(STATUS + "no_data_line"), true);
        }
        boolean dataLine = DataNetworks.networkAt(server, worldPosition).isPresent();
        boolean capable = network.get().controller().map(WarehouseControllerBlockEntity::capable).orElse(false);
        if (!dataLine && !capable && wirelessWaiting == null && !wirelessSending()) {
            return status(Component.translatable(STATUS + "no_data_line"), true);
        }
        if (wirelessSending() && wirelessKey != null) {
            return status(Component.translatable(STATUS + "sending", wirelessKey.stack(1).getHoverName(), wirelessTo), false);
        }
        if (dataSending() && runningFor != null) {
            return status(Component.translatable(STATUS + "sending",
                    runningFor.key.stack(1).getHoverName(), targetName(server, runningFor.target)), false);
        }
        if (blockedReason() != null) {
            String item = blockedStack().getHoverName().getString();
            String reason = blockedReason().name().toLowerCase(Locale.ROOT);
            String key = reason.equals("no_room") && blockedZone().isEmpty() ? "no_room_general" : reason;
            return status(Component.translatable("gui.servus_shuttles.station.blocked." + key, item, blockedZone()), true);
        }
        if (wirelessWaiting != null) {
            return status(wirelessWaiting, wirelessWaitingAlert);
        }
        if (toStorageFull()) {
            return status(Component.translatable(STATUS + "to_storage_full"), true);
        }
        for (Shipment shipment : shipments) {
            if (!shipment.ended() && !shipment.relay() && shipment.missing() > outgoingCount(shipment.key)) {
                return status(Component.translatable(STATUS + "waiting_shuttle",
                        shipment.key.stack(1).getHoverName()), false);
            }
        }
        return status(Component.translatable(STATUS + (dataLine ? "idle" : "wireless_only")), false);
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
        Shipment shipment = new Shipment(target.immutable(), key, ledger().open(planned), false, "", null, -1);
        shipments.add(shipment);
        setChanged();
        return shipment;
    }

    /**
     * Registers a data line shipment of {@code planned} items to the Storage
     * Terminal at {@code terminal} that passes on the wireless transfer with
     * ledger id {@code feed}: its items arrive in the outgoing buffer from the
     * other site, not from shuttles, and it waits for no more than that
     * transfer brings.
     */
    public Shipment addRelay(BlockPos terminal, ItemKey key, int planned, long feed) {
        Shipment shipment = new Shipment(terminal.immutable(), key, ledger().open(planned), false, "", null, feed);
        shipments.add(shipment);
        setChanged();
        return shipment;
    }

    /** Whether an open data line shipment of this station passes on the wireless transfer with that ledger id. */
    private boolean relays(long feed) {
        for (Shipment shipment : shipments) {
            if (shipment.feed == feed && !shipment.ended()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers a wireless shipment of {@code planned} items to the Packing
     * Station at {@code target} of the site with key {@code site}; picks arrive
     * in the outgoing buffer afterwards and are moved every tick. With a
     * {@code terminal} the items land in the receiver's To network buffer for
     * that terminal, without one in its To storage buffer.
     */
    public Shipment addWirelessShipment(BlockPos target, String site, ItemKey key, int planned, BlockPos terminal) {
        Shipment shipment = new Shipment(target.immutable(), key, ledger().open(planned), true, site,
                terminal == null ? null : terminal.immutable(), -1);
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
            if (++point.sendTimer >= ShuttlesStatsManager.INSTANCE.pushInterval()) {
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
        capRelays();
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
            showSending(true);
            return;
        }
        returnOrphans();
        if (!sending()) {
            showSending(false);
        }
    }

    /**
     * Lets each relay wait for no more than its wireless transfer brings: what
     * that transfer still asks while it runs, what it delivered once it ended.
     * A transfer the ledger forgot leaves the relay what waits in the buffer.
     */
    private void capRelays() {
        ProductionLedger ledger = ledger();
        for (Shipment shipment : shipments) {
            if (!shipment.relay() || shipment.ended()) {
                continue;
            }
            ProductionLedger.Transfer feed = ledger.transfer(shipment.feed);
            int cut = feed == null
                    ? ShipmentCap.relayCut(shipment.planned(), shipment.delivered() + outgoingCount(shipment.key), 0, false)
                    : ShipmentCap.relayCut(shipment.planned(), feed.asked(), feed.delivered(), feed.ended());
            if (cut > 0) {
                ledger.shortfall(shipment.transfer, cut);
                endIfComplete(shipment);
                setChanged();
            }
        }
    }

    /** The controller of this station's warehouse while it is online. */
    private Optional<WarehouseControllerBlockEntity> controller() {
        return network().filter(network -> network.status() == WarehouseNetwork.Status.ONLINE).flatMap(WarehouseNetwork::controller);
    }

    /** The name of a site by its registry key, or the key. */
    private static Component siteName(ServerLevel level, String site) {
        return Component.literal(SiteRegistry.of(level).site(site).map(SiteRecord::name).orElse(site));
    }

    /** How many of up to {@code max} items like the key the To storage buffer takes now. */
    private int intakeRoom(ItemKey key, int max) {
        if (max <= 0) {
            return 0;
        }
        return max - Transfer.insertItemStacked(intake(), key.stack(max), true).getCount();
    }

    /** How many of up to {@code max} items like the key the To network buffer takes now. */
    private int outgoingRoom(ItemKey key, int max) {
        if (max <= 0) {
            return 0;
        }
        return max - outgoing.insertItemStacked(key.stack(max), true).getCount();
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
        Optional<WarehouseControllerBlockEntity> here = controller();
        if (here.isEmpty() || !here.get().capable()) {
            // Transfers wait until this controller is capable again; the warehouse side keeps working.
            boolean upgrade = here.map(WarehouseControllerBlockEntity::hasWirelessUpgrade).orElse(false);
            waitWireless(Component.translatable(STATUS + (upgrade ? "no_power_here" : "no_wireless_here")), true);
            return;
        }
        for (Shipment shipment : new ArrayList<>(shipments)) {
            if (shipment.wireless && !shipment.ended() && shipWireless(server, shipment, here.get())) {
                return;
            }
        }
    }

    /**
     * Moves what is ready of one wireless shipment to its receiver; true when
     * this tick's wireless move is used up, false to try the next shipment.
     */
    private boolean shipWireless(ServerLevel server, Shipment shipment, WarehouseControllerBlockEntity here) {
        int ready = Math.min(shipment.missing(), outgoingCount(shipment.key));
        if (ready <= 0) {
            return false;
        }
        Component name = siteName(server, shipment.site);
        if (!server.isLoaded(shipment.target)) {
            waitWireless(Component.translatable(STATUS + "receiver_asleep", name), false);
            return false;
        }
        if (!(server.getBlockEntity(shipment.target) instanceof PackingStationBlockEntity to) || to == this) {
            endShipment(shipment);
            return false;
        }
        Optional<WarehouseControllerBlockEntity> receiver = to.controller();
        if (receiver.isEmpty()) {
            waitWireless(Component.translatable(STATUS + "receiver_asleep", name), false);
            return false;
        }
        WarehouseControllerBlockEntity there = receiver.get();
        if (here.team().isEmpty() || !here.team().equals(there.team())) {
            endShipment(shipment);
            return false;
        }
        if (!there.capable()) {
            waitWireless(Component.translatable(STATUS + (there.hasWirelessUpgrade() ? "no_power_there" : "no_wireless_there"), name), true);
            return false;
        }
        double perItem = WirelessCost.perItem(Math.sqrt(here.getBlockPos().distSqr(there.getBlockPos())),
                ShuttlesStatsManager.INSTANCE.wirelessFePerItem(), ShuttlesStatsManager.INSTANCE.wirelessFePerItemPerBlock());
        // A transfer for a terminal goes on from the receiver's To network buffer, unless its relay there ended.
        boolean relayed = shipment.terminal != null && to.relays(shipment.id());
        int room = relayed ? to.outgoingRoom(shipment.key, ready) : to.intakeRoom(shipment.key, ready);
        int paid = WirelessCost.affordable(there.energyStored(), perItem, ready);
        int moved = WirelessCost.movable(ready, shipment.missing(), allowance(Flow.OUT), to.allowance(Flow.IN), room, paid);
        if (moved <= 0) {
            if (paid <= 0) {
                there.refuseEnergy();
                waitWireless(Component.translatable(STATUS + "no_power_there", name), true);
            } else if (room <= 0) {
                waitWireless(Component.translatable(STATUS + "receiver_full", name), true);
            }
            return true;
        }
        ItemStack taken = takeOutgoing(shipment.key, moved);
        ItemStack left = relayed ? to.outgoing.insertItemStacked(taken, false) : Transfer.insertItemStacked(to.intake(), taken, false);
        int arrived = taken.getCount() - left.getCount();
        if (!left.isEmpty()) {
            outgoing.insertItemStacked(left, false);
        }
        if (arrived > 0) {
            there.spendEnergy(WirelessCost.cost(arrived, perItem));
            consume(Flow.OUT, arrived);
            to.consume(Flow.IN, arrived);
            credit(shipment, arrived);
            wirelessId = shipment.id();
            wirelessTo = name;
            wirelessKey = shipment.key;
            sentAgo = 0;
            showSending(true);
        }
        return true;
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
                Shipment shipment = new Shipment(saved.target(), saved.key(), transfer, saved.wireless(), saved.site(), saved.terminal(), saved.feed());
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
            // Relays wait for their wireless transfer, not for lost picks; they keep what already arrived for them.
            int reserved = 0;
            for (Shipment shipment : shipments) {
                if (shipment.key.equals(key) && shipment.relay()) {
                    ProductionLedger.Transfer feed = ledger.transfer(shipment.feed);
                    reserved += feed == null ? shipment.missing()
                            : Math.min(shipment.missing(), Math.max(0, feed.delivered() - shipment.delivered()));
                }
            }
            List<Shipment> ofKey = shipments.stream().filter(shipment -> shipment.key.equals(key) && !shipment.relay()).toList();
            List<Integer> cuts = ShipmentCap.shortfalls(ofKey.stream().map(Shipment::missing).toList(), outgoingCount(key) - reserved);
            for (int i = 0; i < ofKey.size(); i++) {
                ledger.shortfall(ofKey.get(i).transfer, cuts.get(i));
                endIfComplete(ofKey.get(i));
            }
        }
        setChanged();
    }

    protected int outgoingCount(ItemKey key) {
        return countOf(outgoing, key);
    }

    /** Sets the lamp while the intake is empty: orange while packets or items leave, else green. */
    protected void showSending(boolean working) {
        if (level == null || level.isClientSide() || !isEmpty()) {
            return;
        }
        if (working) {
            setActivity(StorageNodeBlocks.Activity.WORKING);
        } else {
            clearBlocked();
        }
    }

    /** Drops the To storage buffer, the upgrades and the To network buffer. */
    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        upgrades.takeAll().forEach(stack -> Block.popResource(level, pos, stack));
        outgoing.dropAll(level, pos);
    }

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
                list.add(savedShipment(new SavedShipment(shipment.id(), shipment.target, shipment.key, shipment.wireless, shipment.site,
                        shipment.terminal, shipment.feed), registries));
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
            outgoing.fillFrom(registries, tag.getCompoundOrEmpty("Outgoing"));
        }
        if (tag.contains("Upgrades")) {
            upgrades.fillFrom(registries, tag.getCompoundOrEmpty("Upgrades"));
        }
        shipments.clear();
        unresolved.clear();
        for (Tag raw : tag.getListOrEmpty("Shipments")) {
            CompoundTag entry = (CompoundTag) raw;
            Optional<BlockPos> target = ValueIo.readPos(entry, "Target");
            ItemStack item = ValueIo.parseStack(registries, entry.getCompoundOrEmpty("Item"));
            if (target.isPresent() && !item.isEmpty() && entry.contains("Id")) {
                unresolved.add(new SavedShipment(entry.getLongOr("Id", 0L), target.get(), ItemKey.of(item),
                        entry.getBooleanOr("Wireless", false), entry.getStringOr("Site", ""),
                        ValueIo.readPos(entry, "Terminal").orElse(null), entry.getLongOr("Feed", -1L)));
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
        if (saved.terminal() != null) {
            entry.put("Terminal", ValueIo.posTag(saved.terminal()));
        }
        if (saved.feed() >= 0) {
            entry.putLong("Feed", saved.feed());
        }
        return entry;
    }
}
