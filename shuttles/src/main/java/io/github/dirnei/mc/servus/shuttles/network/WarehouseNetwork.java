package io.github.dirnei.mc.servus.shuttles.network;

import com.google.common.base.Suppliers;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlock;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.naming.MachineLabels;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.packet.Grant;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.field.FieldDue;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeads;
import io.github.dirnei.mc.servus.shuttles.field.FieldStatus;
import io.github.dirnei.mc.servus.shuttles.field.SeedPlan;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.field.SupplyPlan;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidPutAwayPlan;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidRestockPlan;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidRouting;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidRole;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidStoreRef;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.index.WarehouseIndex;
import io.github.dirnei.mc.servus.shuttles.lock.LockHolder;
import io.github.dirnei.mc.servus.shuttles.index.ReservationTally;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotFill;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.lock.TopUp;
import io.github.dirnei.mc.servus.shuttles.order.CapRoom;
import io.github.dirnei.mc.servus.shuttles.order.ExportPlan;
import io.github.dirnei.mc.servus.shuttles.order.StockRule;
import io.github.dirnei.mc.servus.shuttles.order.StockRules;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.IngressRanking;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationChoice;
import io.github.dirnei.mc.servus.shuttles.packing.WirelessIncoming;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackSectionHandler;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.shuttle.CounterData;
import io.github.dirnei.mc.servus.shuttles.shuttle.FertilizeJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.FleetSaves;
import io.github.dirnei.mc.servus.shuttles.shuttle.FieldJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.FluidJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.SupplyJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.LiftCar;
import io.github.dirnei.mc.servus.shuttles.shuttle.PickJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.PutAwayJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.RailCosts;
import io.github.dirnei.mc.servus.shuttles.shuttle.RailPaths;
import io.github.dirnei.mc.servus.shuttles.shuttle.RequestPlan;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shafts;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shafts.Shaft;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.shuttles.site.SiteChunks;
import io.github.dirnei.mc.servus.shuttles.site.SiteRegistry;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.StationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.storage.StorageFill;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayRank;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * One connected set of storage blocks. Keeps an index of everything its own
 * racks hold, refreshed in batches; inventories touching a member are never
 * indexed. Every pick and put-away runs through racks and shuttles.
 */
public final class WarehouseNetwork extends ServusNetwork<StorageNodeBlockEntity, WarehouseEvent> implements ShuttleFleet.Host {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "warehouse");

    public static final NetworkType<StorageNodeBlockEntity, WarehouseEvent, WarehouseNetwork> TYPE =
            new NetworkType<>(ID, (from, fromNode, side, to, toNode) ->
                    WarehouseConnectionRule.accepts(fromNode.kind(), side, toNode.kind())
                            && railSideAccepted(fromNode, side, toNode)
                            && fromNode.linksTowards(side, toNode), WarehouseNetwork::new);

    public enum Status { ONLINE, NO_CONTROLLER, MULTIPLE_CONTROLLERS }

    private static final int BLOCKED_RETRY_TICKS = 20;
    /** Machines numbered together get numbers by height, then x, then z. */
    private static final Comparator<BlockPos> NUMBER_ORDER =
            Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ);
    /** Ticks between two sweeps for station claims whose job is gone. */
    private static final int CLAIM_SWEEP_TICKS = 100;

    /** A load that did not fit into its destination's delivery buffer yet. */
    private record HeldLoad(PickJob job, ItemStack stack) {
    }

    /** One locked slot of a block: which of its lockable inventories, which slot and the job holding it. */
    private record LockKey(BlockPos pos, int inventory, int slot, int jobId) {
    }

    /** Indexed members, read through {@link StorageNodeBlockEntity#stock()}. */
    private final Set<BlockPos> sources = new LinkedHashSet<>();
    /** Members that hold no stock, such as rails. Never sources. */
    private final Set<BlockPos> connectors = new HashSet<>();
    /** Elevator segments, a subset of the connectors; grouped into shafts on the tick after a change. */
    private final Set<NodePos> elevators = new HashSet<>();
    private Map<NodePos, Shaft> shaftFloors = Map.of();
    private boolean shaftsDirty;
    private final WarehouseZones zones = new WarehouseZones();
    /** Set when a zone or room changed; stations with waiting stacks retry on the next tick. */
    private boolean zonesDirty;
    /** Inbound, outbound and Packing Stations. */
    private final Set<BlockPos> stations = new HashSet<>();
    private final Set<BlockPos> garages = new HashSet<>();
    /** Stations whose buffer changed; drained on the next tick. */
    private final Set<BlockPos> pendingStations = new LinkedHashSet<>();
    /** Stations holding a stack that found no room; retried on zone and room changes and on a slow timer. */
    private final Set<BlockPos> blockedStations = new LinkedHashSet<>();
    /** Members that are named machines: stations, garages and elevator segments. */
    private final Set<BlockPos> namedMembers = new HashSet<>();
    /** Named machines that joined or whose warehouse changed; numbered while online once they are loaded. */
    private final Set<BlockPos> machinesToNumber = new HashSet<>();
    /** The data line whose members were last queued for numbers, and its members revision then. */
    private DataNetwork numberedLine;
    private int numberedLineRevision;
    private int blockedRetry;
    /** Fluid Gantries, by their master's position. */
    private final Set<BlockPos> gantries = new LinkedHashSet<>();
    /** Gantries whose tanks changed; read again on the next tick. */
    private final Set<BlockPos> pendingGantries = new LinkedHashSet<>();
    /** Fluid Rack bays of this warehouse. */
    private final Set<BlockPos> fluidRacks = new LinkedHashSet<>();
    /** Fluid stations, by their lower half; never part of the fluid stock. */
    private final Set<BlockPos> fluidStations = new LinkedHashSet<>();
    /** Fluid Rack bays whose section changed; their section is read again on the next tick. */
    private final Set<BlockPos> pendingRacks = new LinkedHashSet<>();
    /** The fluid stock: every tank of a Storage gantry and every Fluid Rack section. */
    private final WarehouseIndex<FluidStoreRef, FluidKey> fluidIndex = new WarehouseIndex<>();
    /** What every gantry's tanks hold, Storage or not, as last read. */
    private final Map<BlockPos, List<TankView>> tankViews = new HashMap<>();
    /** What every Fluid Rack section holds as last read, by its port bay. */
    private final Map<BlockPos, TankView> rackViews = new HashMap<>();
    /** Held loads per destination. */
    private final Map<BlockPos, List<HeldLoad>> held = new HashMap<>();
    private final WarehouseIndex<BlockPos, ItemKey> index = new WarehouseIndex<>();
    /** Slots in use and all slots of each rack bay, as its last stock poll read them. */
    private final Map<BlockPos, RackFill> rackFill = new HashMap<>();
    /** Items picks have locked in the racks, per rack. */
    private final ReservationTally<BlockPos, ItemKey> reservedOut = new ReservationTally<>();
    private final ArrayDeque<BlockPos> dirtyQueue = new ArrayDeque<>();
    private final Set<BlockPos> dirtySet = new HashSet<>();
    private final List<BlockPos> pollOrder = new ArrayList<>();
    private boolean pollOrderStale = true;
    private int cursor;
    private boolean lampsDirty;
    private ServerLevel level;
    private int controllers;
    /** The controller's position while exactly one is in the network. */
    private BlockPos controllerPos;
    private final StockRuleKeeper rules = new StockRuleKeeper(this);
    private int outboundTimer;
    private int fieldTimer;
    private int fluidTimer;
    /** The min load of tanker runs when a test set one for this warehouse, else -1 for the stats. */
    private int tankerMinLoadForTests = -1;
    private final ShuttleFleet fleet = new ShuttleFleet(this);
    private final OrphanSweep<LockKey> lockSweep = new OrphanSweep<>();
    private int claimTimer;
    /** Racks that left for good; picks planned from them are planned again on the next tick. */
    private final Set<BlockPos> brokenSources = new LinkedHashSet<>();
    /** The order in which sources joined, rebuilt after membership changes. */
    private Map<BlockPos, Integer> joinOrder;
    private boolean spanDirty = true;
    private int snapshotTimer;
    private long snapshotVersion = -1;
    /** Set when a Packing Station joins or leaves, or the controller's Wireless Upgrade or power changes. */
    private boolean wirelessDirty = true;

    public WarehouseNetwork(int id, NetworkSet<StorageNodeBlockEntity, WarehouseEvent, ?> owner) {
        super(id, owner);
    }

    /** A stock node may limit which of its sides take a rail; the rail side is asked from the stock node's view. */
    private static boolean railSideAccepted(StorageNodeBlockEntity from, Side side, StorageNodeBlockEntity to) {
        boolean fromRail = from.kind() == WarehouseConnectionRule.NodeKind.RAIL;
        boolean toRail = to.kind() == WarehouseConnectionRule.NodeKind.RAIL;
        if (fromRail == toRail) {
            return true;
        }
        if (toRail) {
            return !from.docksRail() || from.acceptsRail(side);
        }
        return !to.docksRail() || to.acceptsRail(side.opposite());
    }

    @Override
    protected void onNodeAdded(NodePos pos, StorageNodeBlockEntity node) {
        if (!(node.getLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        BlockPos blockPos = Conversions.toBlockPos(pos);
        if (node.holdsStock()) {
            if (node.stock() != null) {
                addSource(blockPos);
            }
            if (node instanceof HeavyRackBlockEntity rack) {
                reservedOut.set(blockPos, rack.items().outLocked());
            }
            zones.set(blockPos, node.zoneDefinition());
            zonesDirty = true;
        } else if (node.kind() == WarehouseConnectionRule.NodeKind.STATION) {
            stations.add(blockPos);
            pendingStations.add(blockPos);
        } else if (node instanceof ShuttleGarageBlockEntity garage) {
            garages.add(blockPos);
            fleet.adoptGarage(blockPos, garage.slots(), garage.isActive());
        } else if (node.kind() == WarehouseConnectionRule.NodeKind.GANTRY) {
            gantries.add(blockPos);
            pendingGantries.add(blockPos);
        } else if (node.kind() == WarehouseConnectionRule.NodeKind.FLUID_RACK) {
            fluidRacks.add(blockPos);
            pendingRacks.add(blockPos);
        } else if (node.kind() == WarehouseConnectionRule.NodeKind.FLUID_STATION) {
            fluidStations.add(blockPos);
        } else {
            connectors.add(blockPos);
            if (node.kind() == WarehouseConnectionRule.NodeKind.ELEVATOR) {
                elevators.add(pos);
                shaftsDirty = true;
            }
        }
        if (node instanceof NamedMachine) {
            namedMembers.add(blockPos);
            machinesToNumber.add(blockPos);
        }
        if (node instanceof WarehouseControllerBlockEntity) {
            controllers++;
            controllerPos = blockPos;
            renumberAll();
        }
        membershipChanged(node);
        restoreState(node, blockPos);
    }

    @Override
    protected void onNodeRemoved(NodePos pos, StorageNodeBlockEntity node) {
        BlockPos blockPos = Conversions.toBlockPos(pos);
        boolean forGood = node.leavingForGood();
        if (!forGood) {
            node.stashWarehouseState(saveState(node));
        }
        connectors.remove(blockPos);
        if (elevators.remove(pos)) {
            shaftsDirty = true;
        }
        stations.remove(blockPos);
        pendingStations.remove(blockPos);
        blockedStations.remove(blockPos);
        namedMembers.remove(blockPos);
        machinesToNumber.remove(blockPos);
        if (node instanceof ElevatorBlockEntity) {
            machinesToNumber.addAll(namedMembers);
        }
        zones.remove(blockPos);
        dropSource(blockPos);
        if (forGood && node instanceof HeavyRackBlockEntity) {
            brokenSources.add(blockPos);
        }
        if (garages.remove(blockPos)) {
            fleet.releaseGarage(blockPos, forGood);
        }
        if (gantries.remove(blockPos)) {
            pendingGantries.remove(blockPos);
            dropTanks(blockPos);
        }
        if (fluidRacks.remove(blockPos)) {
            pendingRacks.remove(blockPos);
            dropRack(blockPos);
        }
        fluidStations.remove(blockPos);
        List<HeldLoad> heldHere = held.remove(blockPos);
        if (forGood && heldHere != null && level != null) {
            for (HeldLoad load : heldHere) {
                Block.popResource(level, blockPos, load.stack());
            }
        }
        if (node instanceof WarehouseControllerBlockEntity) {
            if (!forGood) {
                fleet.clearQueue();
            }
            controllers--;
            if (blockPos.equals(controllerPos)) {
                controllerPos = null;
            }
            renumberAll();
        }
        membershipChanged(node);
    }

    private void membershipChanged(StorageNodeBlockEntity node) {
        lampsDirty = true;
        spanDirty = true;
        wirelessDirty |= node instanceof PackingStationBlockEntity || node instanceof WarehouseControllerBlockEntity;
        fleet.refreshHomes();
    }

    @Override
    protected void onAbsorb(ServusNetwork<StorageNodeBlockEntity, WarehouseEvent> other) {
        if (!(other instanceof WarehouseNetwork that)) {
            return;
        }
        if (level == null) {
            level = that.level;
        }
        controllers += that.controllers;
        if (controllerPos == null) {
            controllerPos = that.controllerPos;
        }
        connectors.addAll(that.connectors);
        elevators.addAll(that.elevators);
        shaftsDirty = true;
        stations.addAll(that.stations);
        pendingStations.addAll(that.stations);
        namedMembers.addAll(that.namedMembers);
        renumberAll();
        garages.addAll(that.garages);
        gantries.addAll(that.gantries);
        pendingGantries.addAll(that.gantries);
        fluidRacks.addAll(that.fluidRacks);
        pendingRacks.addAll(that.fluidRacks);
        fluidStations.addAll(that.fluidStations);
        that.held.forEach((pos, loads) -> held.computeIfAbsent(pos, p -> new ArrayList<>()).addAll(loads));
        zones.absorb(that.zones);
        that.sources.forEach(pos -> {
            sources.add(pos);
            index.update(pos, that.index.snapshot(pos));
        });
        rackFill.putAll(that.rackFill);
        reservedOut.absorb(that.reservedOut);
        pollOrderStale = true;
        joinOrder = null;
        lampsDirty = true;
        spanDirty = true;
        wirelessDirty = true;
        fleet.absorb(that.fleet);
    }

    @Override
    protected void onDisposed() {
        sources.clear();
        reservedOut.clear();
        rackFill.clear();
        connectors.clear();
        elevators.clear();
        shaftFloors = Map.of();
        stations.clear();
        garages.clear();
        gantries.clear();
        pendingGantries.clear();
        tankViews.clear();
        fluidRacks.clear();
        pendingRacks.clear();
        rackViews.clear();
        fluidStations.clear();
        held.clear();
        pendingStations.clear();
        blockedStations.clear();
        namedMembers.clear();
        machinesToNumber.clear();
        numberedLine = null;
        zones.clear();
        dirtyQueue.clear();
        dirtySet.clear();
        pollOrder.clear();
        controllers = 0;
    }

    /**
     * What this network keeps for the node, as the node saves it: a garage's
     * shuttles that are out, a controller's job queue, the loads held for a
     * destination. Empty when there is nothing.
     */
    public CompoundTag saveState(StorageNodeBlockEntity node) {
        CompoundTag tag = new CompoundTag();
        if (node.getLevel() == null) {
            return tag;
        }
        HolderLookup.Provider registries = node.getLevel().registryAccess();
        BlockPos pos = node.getBlockPos();
        if (node instanceof ShuttleGarageBlockEntity) {
            CompoundTag garage = fleet.saveGarage(pos, registries);
            if (!garage.isEmpty()) {
                tag.put("Garage", garage);
            }
        }
        if (node instanceof WarehouseControllerBlockEntity && fleet.queued() > 0) {
            tag.put("Queue", fleet.saveQueue(registries));
        }
        List<HeldLoad> loads = held.getOrDefault(pos, List.of());
        if (!loads.isEmpty()) {
            ListTag list = new ListTag();
            for (HeldLoad load : loads) {
                CompoundTag entry = new CompoundTag();
                entry.put("Job", FleetSaves.saveJob(load.job(), registries));
                entry.put("Stack", ValueIo.saveStack(registries, load.stack()));
                list.add(entry);
            }
            tag.put("Held", list);
        }
        return tag;
    }

    /** Takes back what the node kept while it was in no network. */
    private void restoreState(StorageNodeBlockEntity node, BlockPos pos) {
        CompoundTag tag = node.takeWarehouseState();
        if (tag.isEmpty() || node.getLevel() == null) {
            return;
        }
        HolderLookup.Provider registries = node.getLevel().registryAccess();
        if (tag.contains("Garage") && garages.contains(pos)) {
            fleet.restoreGarage(pos, tag.getCompoundOrEmpty("Garage"), registries);
        }
        if (tag.contains("Queue")) {
            fleet.restoreQueue(tag.getListOrEmpty("Queue"), registries);
        }
        ListTag loads = tag.getListOrEmpty("Held");
        for (int i = 0; i < loads.size(); i++) {
            CompoundTag entry = loads.getCompoundOrEmpty(i);
            ItemStack stack = ValueIo.parseStack(registries, entry.getCompoundOrEmpty("Stack"));
            Optional<ShuttleJob> job = FleetSaves.loadJob(entry.getCompoundOrEmpty("Job"), registries);
            if (!stack.isEmpty() && job.isPresent() && job.get() instanceof PickJob pick) {
                held.computeIfAbsent(pos, p -> new ArrayList<>()).add(new HeldLoad(pick, stack));
            }
        }
    }

    @Override
    public void stateChanged(BlockPos pos) {
        if (level != null) {
            level.blockEntityChanged(pos);
        }
    }

    @Override
    public void queueChanged() {
        if (controllerPos != null) {
            stateChanged(controllerPos);
        }
    }

    /** A garage's shuttles or pause state changed. */
    public void garageChanged(ShuttleGarageBlockEntity garage) {
        fleet.adoptGarage(garage.getBlockPos(), garage.slots(), garage.isActive());
    }

    /** Brings every shuttle of the garage inside; cargo drops at the garage and unloaded jobs are queued again. */
    public void recallGarage(BlockPos pos) {
        fleet.recall(pos);
    }

    /** Shuttles of one garage that are out on a job. */
    public int garageOut(BlockPos pos) {
        return fleet.outOf(pos);
    }

    /** Slots of one garage whose shuttle is out, one bit per slot. */
    public int garageOutSlots(BlockPos pos) {
        return fleet.outSlots(pos);
    }

    /** Jobs waiting for a shuttle. */
    public int queuedJobs() {
        return fleet.queued();
    }

    /** Jobs shuttles carry right now, every load of a Cargo Hold counted. */
    public int ridingJobs() {
        return fleet.riding();
    }

    /** One garage's line for its right click. */
    public Component garageSummary(BlockPos pos) {
        return Component.literal("Garage: " + fleet.garageSummary(pos));
    }

    /** The lift car of the shaft whose lowest segment is at that position, or null. */
    public LiftCar lift(BlockPos bottom) {
        return fleet.lift(bottom);
    }

    /** The shuttles of one garage as clients draw them. */
    public List<ShuttleView> shuttleViews(BlockPos garage) {
        return fleet.views(garage);
    }

    /** The Data Potato's report of the shuttles on the rail or elevator segment at pos. */
    public List<Component> shuttleReport(BlockPos pos) {
        return fleet.report(pos);
    }

    /** Sends an idle shuttle of the garage along the rails at those positions without a job. For GameTests. */
    public boolean sendShuttleAlong(BlockPos garage, int slot, List<BlockPos> path) {
        return fleet.sendAlong(garage, slot, path.stream().map(Conversions::toNodePos).toList());
    }

    /** The cheapest way between two rails as the warehouse would plan it now, traffic included; empty without one. For GameTests. */
    public List<BlockPos> plannedWay(BlockPos from, BlockPos to) {
        return fleet.costsFrom(List.of(Conversions.toNodePos(from))).pathTo(Conversions.toNodePos(to)).stream()
                .map(Conversions::toBlockPos).toList();
    }

    /** Whether the source is a Heavy Duty Rack, which only shuttles serve. */
    @Override
    public boolean isRack(BlockPos pos) {
        return loaded(pos) instanceof HeavyRackBlockEntity;
    }

    /** The zoned blocks that take this key, by exact item and by tag. */
    private PutAwayPlan.Tiers<BlockPos> tiers(ItemKey key) {
        return zones.tiers(key, sources);
    }

    /** The target lists in order: exact matches, tag matches, then general storage; holders first within each. */
    private List<List<BlockPos>> targetTiers(ItemKey key, PutAwayPlan.Tiers<BlockPos> tiers) {
        List<BlockPos> holders = new ArrayList<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            if (sources.contains(pos)) {
                holders.add(pos);
            }
        }
        return zones.targets(tiers, sources, holders);
    }

    /**
     * The racks a stack may be put away into, best first by the put-away rule:
     * zone tier, racks holding the item (a partial stack first, then the
     * cheapest), then the others by free slot bucket and cost, join order last.
     * Only unlocked room counts. With {@code costs} a rack must be reachable
     * from its starts and is ranked by its cost; without, every rack costs the same.
     */
    private List<BlockPos> rankTargets(ItemStack stack, PutAwayPlan.Tiers<BlockPos> tiers, RailCosts.Search<NodePos> costs,
                                       Predicate<BlockPos> allowed) {
        ItemKey key = ItemKey.of(stack);
        int max = stack.getMaxStackSize();
        List<List<BlockPos>> tierLists = targetTiers(key, tiers);
        List<PutAwayRank.Bay<BlockPos>> bays = new ArrayList<>();
        for (int tier = 0; tier < tierLists.size(); tier++) {
            for (BlockPos pos : tierLists.get(tier)) {
                LockedSlots slots = rackSlots(pos);
                if (slots == null || !allowed.test(pos)) {
                    continue;
                }
                List<SlotFill.Slot<ItemKey>> states = slots.states();
                int room = SlotFill.room(states, key, max);
                if (room <= 0) {
                    continue;
                }
                double cost = costs == null ? 0 : costs.costToAny(dockRails(Conversions.toNodePos(pos)));
                if (Double.isInfinite(cost)) {
                    continue;
                }
                bays.add(new PutAwayRank.Bay<>(pos, tier, SlotFill.holds(states, key), SlotFill.partial(states, key, max),
                        SlotFill.freeSlots(states), slots.getSlots(), room, cost, joinOrder(pos)));
            }
        }
        List<BlockPos> ranked = new ArrayList<>();
        for (PutAwayRank.Bay<BlockPos> bay : PutAwayRank.order(bays, ShuttlesStatsManager.INSTANCE.freeSlotBucket())) {
            ranked.add(bay.id());
        }
        return ranked;
    }

    /** The position of a source in the order sources joined. */
    private int joinOrder(BlockPos pos) {
        if (joinOrder == null) {
            joinOrder = new HashMap<>();
            for (BlockPos source : sources) {
                joinOrder.put(source, joinOrder.size());
            }
        }
        return joinOrder.getOrDefault(pos, Integer.MAX_VALUE);
    }

    /** The lockable slots of a rack of this warehouse whose chunk is loaded, else null. */
    private LockedSlots rackSlots(BlockPos pos) {
        return sources.contains(pos) && loaded(pos) instanceof HeavyRackBlockEntity rack ? rack.items() : null;
    }

    /** Slots in use and all slots of one rack bay. */
    private record RackFill(int used, int total) {
    }

    /**
     * How full the warehouse is: rack slots in use per zone as the stock poll
     * last read them, and the fluid in its Storage gantries' tanks and Fluid Rack sections.
     */
    public StorageFill.Report storageFill() {
        List<StorageFill.Bay> bays = new ArrayList<>(rackFill.size());
        rackFill.forEach((pos, fill) -> {
            ZoneDefinition zone = zones.zoneOf(pos);
            bays.add(zone == null ? new StorageFill.Bay(StorageFill.NO_ZONE, "", fill.used(), fill.total())
                    : new StorageFill.Bay(zone.key(), zone.name(), fill.used(), fill.total()));
        });
        List<StorageFill.Tank> tanks = new ArrayList<>();
        for (TankView view : fluidStores()) {
            tanks.add(new StorageFill.Tank(view.fluid().getAmount(), view.capacity()));
        }
        return StorageFill.of(bays, tanks);
    }

    /** The Storage gantries of this warehouse, as their tanks were last read. */
    public List<BlockPos> storageGantries() {
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos pos : gantries()) {
            List<TankView> views = tankViews(pos);
            if (!views.isEmpty() && views.get(0).mode() == GantryMode.STORAGE) {
                list.add(pos);
            }
        }
        return list;
    }

    /** Stock blocks per zone key, sorted by key. */
    public Map<String, Integer> zoneBlockCounts() {
        return zones.blockCounts();
    }

    /** One zone as its blocks carry it: the union of their filters. */
    public record ZoneSummary(String key, String display, int blocks, List<String> items, List<String> tags) {
    }

    /** Every zone of the warehouse, sorted by key. */
    public List<ZoneSummary> zoneSummaries() {
        return zones.summaries();
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public Set<NodePos> travelNeighbours(NodePos node) {
        Set<NodePos> result = new HashSet<>();
        for (NodePos neighbour : owner().graph().neighbors(node)) {
            if (isTravelNode(neighbour)) {
                result.add(neighbour);
            }
        }
        return result;
    }

    @Override
    public boolean isTravelNode(NodePos node) {
        return connectors.contains(Conversions.toBlockPos(node)) && members().contains(node);
    }

    /**
     * A rack is reached only through the rails in front of the bays of its
     * section, a Fluid Rack only through the rail in front of its section's
     * port, a fluid station only through the rail in front of its lower
     * half, a Field Head through the farm rail beside it, any other block
     * through every travel node touching it.
     */
    @Override
    public Set<NodePos> dockRails(NodePos storage) {
        Set<NodePos> result = new HashSet<>();
        BlockPos pos = Conversions.toBlockPos(storage);
        if (level == null || !level.isLoaded(pos)) {
            return result;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HeavyRackBlock) {
            for (BlockPos bay : HeavyRackBlock.section(level, pos)) {
                NodePos front = Conversions.toNodePos(bay.relative(level.getBlockState(bay).getValue(HeavyRackBlock.FACING)));
                if (isTravelNode(front)) {
                    result.add(front);
                }
            }
            return result;
        }
        if (state.getBlock() instanceof FluidRackBlock) {
            BlockPos port = FluidRackBlock.port(level, pos);
            if (port != null) {
                NodePos front = Conversions.toNodePos(port.relative(level.getBlockState(port).getValue(FluidRackBlock.FACING)));
                if (isTravelNode(front)) {
                    result.add(front);
                }
            }
            return result;
        }
        if (loaded(pos) instanceof FieldHeadBlockEntity head) {
            BlockPos rail = head.headRail();
            if (rail != null && isTravelNode(Conversions.toNodePos(rail))) {
                result.add(Conversions.toNodePos(rail));
            }
            return result;
        }
        if (loaded(pos) instanceof FluidStationBlockEntity station) {
            NodePos rail = Conversions.toNodePos(station.railPos());
            if (isTravelNode(rail)) {
                result.add(rail);
            }
            return result;
        }
        if (loaded(pos) instanceof FluidGantryBlockEntity gantry) {
            NodePos rail = Conversions.toNodePos(pos.relative(gantry.railSide()));
            if (isTravelNode(rail)) {
                result.add(rail);
            }
            return result;
        }
        for (Side side : Side.ALL) {
            NodePos beside = storage.relative(side);
            if (isTravelNode(beside)) {
                result.add(beside);
            }
        }
        return result;
    }

    /** Positions of storage blocks holding the key right now. */
    public List<BlockPos> sourcesHolding(ItemKey key) {
        return index.sourcesHolding(key);
    }

    /** Takes up to amount of the key out of exactly that source, from slots no job locked. */
    public ItemStack extractFrom(BlockPos pos, ItemKey key, int amount) {
        ItemView handler = handlerOf(pos);
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, key.stack(1).getMaxStackSize());
        for (int slot = 0; slot < handler.getSlots() && wanted > 0; slot++) {
            if (!key.matches(handler.getStackInSlot(slot))) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, wanted, false);
            result = combine(result, taken);
            wanted -= taken.getCount();
        }
        if (!result.isEmpty()) {
            markDirty(pos);
        }
        return result;
    }

    /** Adds what was taken to the result so far; the first non empty stack becomes the result. */
    private static ItemStack combine(ItemStack result, ItemStack taken) {
        if (taken.isEmpty()) {
            return result;
        }
        if (result.isEmpty()) {
            return taken;
        }
        result.grow(taken.getCount());
        return result;
    }

    @Override
    public double liftSpeed() {
        return ShuttlesStatsManager.INSTANCE.liftSpeed();
    }

    @Override
    public Shaft shaftOf(NodePos node) {
        return shaftFloors.get(node);
    }

    /** Whether some garage shuttle can drive to a rail beside the position. */
    public boolean canReach(BlockPos pos) {
        return fleet.canReach(pos);
    }

    /** Delivers items into an outbound station's buffer, planned and refused like a request. */
    public String feedStation(OutboundStationBlockEntity station, ItemKey key, int amount) {
        return planAndStart(station.getBlockPos(), key, amount, 0).refusal();
    }

    /** The outcome of a shipping request: a refusal key, or the shipment on its Packing Station. */
    public record Shipping(String refusal, Optional<PackingStationBlockEntity.Shipment> shipment, BlockPos packingStation) {
        public boolean shipped() {
            return refusal.isEmpty();
        }
    }

    private static Shipping refused(String refusal) {
        return new Shipping(refusal, Optional.empty(), null);
    }

    /**
     * Ships items to a data member through the Packing Station with the shortest
     * shuttle trip from the racks holding them.
     *
     * @return refusal keys: {@code offline}, {@code no_packing_station}, {@code out_of_stock},
     *         {@code needs_shuttle} or {@code no_rail}
     */
    public Shipping ship(BlockPos target, ItemKey key, int amount) {
        return ship(target, key, amount, 0);
    }

    /** Like {@link #ship(BlockPos, ItemKey, int)}, with pick jobs tagged by {@code requestId} so they can be dropped. */
    public Shipping ship(BlockPos target, ItemKey key, int amount, int requestId) {
        if (status() != Status.ONLINE || level == null) {
            return refused("offline");
        }
        List<PackingStationBlockEntity> candidates = new ArrayList<>();
        for (BlockPos pos : stations) {
            if (loaded(pos) instanceof PackingStationBlockEntity point
                    && DataNetworks.networkAt(level, pos).filter(network -> network.reaches(target)).isPresent()) {
                candidates.add(point);
            }
        }
        return shipThrough(candidates, target, null, null, key, amount, requestId, "no_packing_station");
    }

    /**
     * Sends items over the wireless line to a Packing Station of another site:
     * picked into this warehouse's Packing Station with the shortest shuttle
     * trip, then moved by that station. The refusals call this warehouse
     * "there", as the asking site shows them.
     *
     * @return refusal keys: {@code offline}, {@code no_wireless_there} (no Wireless Upgrade), {@code no_power_there},
     *         {@code no_packing_there}, {@code out_of_stock}, {@code needs_shuttle} or {@code no_rail}
     */
    public Shipping shipWireless(BlockPos receiver, String receiverSite, ItemKey key, int amount, int requestId) {
        return shipWireless(receiver, receiverSite, key, amount, requestId, null);
    }

    /**
     * Like {@link #shipWireless(BlockPos, String, ItemKey, int, int)} for a
     * request at the Storage Terminal at {@code terminal}: the items land in the
     * receiver's To network buffer and go on to that terminal. Without a
     * terminal they are put away at the receiving site.
     */
    public Shipping shipWireless(BlockPos receiver, String receiverSite, ItemKey key, int amount, int requestId, BlockPos terminal) {
        if (status() != Status.ONLINE || level == null) {
            return refused("offline");
        }
        Optional<WarehouseControllerBlockEntity> controller = controller();
        if (controller.isEmpty()) {
            return refused("offline");
        }
        if (!controller.get().capable()) {
            return refused(controller.get().hasWirelessUpgrade() ? "no_power_there" : "no_wireless_there");
        }
        List<PackingStationBlockEntity> candidates = new ArrayList<>();
        for (BlockPos pos : packingStations()) {
            if (!pos.equals(receiver) && loaded(pos) instanceof PackingStationBlockEntity point) {
                candidates.add(point);
            }
        }
        return shipThrough(candidates, receiver, receiverSite, terminal, key, amount, requestId, "no_packing_there");
    }

    /**
     * Plans the picks into the candidate with the shortest shuttle trip and
     * opens its shipment to the target: a wireless one when {@code site}, the
     * receiving site's key, is given, else one over the data line.
     */
    private Shipping shipThrough(List<PackingStationBlockEntity> candidates, BlockPos target, String site, BlockPos terminal, ItemKey key,
                                 int amount, int requestId, String none) {
        candidates.sort(Comparator.comparing(point -> point.getBlockPos().asLong()));
        List<BlockPos> racks = sourcesHolding(key).stream().filter(this::isRack).toList();
        Optional<PackingStationBlockEntity> chosen = PackingStationChoice.choose(candidates, point -> trip(racks, point.getBlockPos()));
        if (chosen.isEmpty()) {
            return refused(none);
        }
        BlockPos destination = chosen.get().getBlockPos();
        boolean reachable = fleet.canReach(destination);
        RequestPlan.Plan<BlockPos> plan = plan(key, amount, destination, reachable);
        if (plan.jobs().isEmpty()) {
            return new Shipping(refusal(plan, reachable), Optional.empty(), destination);
        }
        PackingStationBlockEntity.Shipment shipment = site == null ? chosen.get().addShipment(target, key, plan.planned())
                : chosen.get().addWirelessShipment(target, site, key, plan.planned(), terminal);
        startJobs(plan, requestId, key, destination);
        return new Shipping("", Optional.of(shipment), destination);
    }

    /**
     * Where items going into storage over the data line go: the Packing
     * Station that granted room and its grant, or the refusal with the station
     * that was asked first, none when no station was asked.
     */
    public record Ingress(BlockPos station, Grant grant) {

        static Ingress refused(String why) {
            return new Ingress(null, Grant.refused(why));
        }

        public boolean granted() {
            return grant.granted();
        }

        /** Why no station took the items, a lang key suffix; empty when one did. */
        public String refusal() {
            return grant.refusal();
        }
    }

    /**
     * Offers {@code amount} items going into storage from the data member at
     * {@code sender} to this warehouse's Packing Stations on its line, cheapest
     * shuttle trip from the station to the rack the put-away rule picks first,
     * and returns the first grant.
     *
     * @return refusal keys: {@code offline}, {@code no_storage}, {@code no_packing_station},
     *         {@code to_storage_full} or another station refusal
     */
    public Ingress offerIngress(BlockPos sender, ItemKey key, int amount) {
        if (status() != Status.ONLINE || level == null) {
            return Ingress.refused(PackingStationBlockEntity.REFUSE_OFFLINE);
        }
        Optional<BlockPos> rack = putAwayTarget(key.stack(1), pos -> true);
        if (rack.isEmpty()) {
            return Ingress.refused(PackingStationBlockEntity.REFUSE_NO_STORAGE);
        }
        List<IngressRanking.Candidate<PackingStationBlockEntity>> candidates = new ArrayList<>();
        for (BlockPos pos : packingStations()) {
            if (loaded(pos) instanceof PackingStationBlockEntity point
                    && DataNetworks.networkAt(level, pos).filter(network -> network.reaches(sender)).isPresent()) {
                candidates.add(new IngressRanking.Candidate<>(point, trip(List.of(rack.get()), pos), pos.asLong()));
            }
        }
        if (candidates.isEmpty()) {
            return Ingress.refused("no_packing_station");
        }
        Ingress first = null;
        for (PackingStationBlockEntity point : IngressRanking.order(candidates)) {
            Ingress ingress = new Ingress(point.getBlockPos(), point.offer(key.stack(1), amount));
            if (ingress.granted()) {
                return ingress;
            }
            first = first == null ? ingress : first;
        }
        return first;
    }

    /** The travel cost from the cheapest rack to the Packing Station, 0 without racks, unreachable without a way. */
    private int trip(List<BlockPos> racks, BlockPos packingStation) {
        if (racks.isEmpty()) {
            return 0;
        }
        RailCosts.Search<NodePos> costs = fleet.costsFrom(dockRails(Conversions.toNodePos(packingStation)));
        double best = Double.POSITIVE_INFINITY;
        for (BlockPos rack : racks) {
            best = Math.min(best, costs.costToAny(dockRails(Conversions.toNodePos(rack))));
        }
        return Double.isInfinite(best) ? PackingStationChoice.UNREACHABLE : (int) Math.round(best);
    }

    /** How many of an item the sources hold that no job reserved; a rack whose chunk is not loaded counts as last indexed. */
    @Override
    public long available(ItemKey key) {
        long sum = 0;
        for (Map.Entry<BlockPos, Integer> entry : stockAt(key).entrySet()) {
            LockedSlots slots = rackSlots(entry.getKey());
            sum += slots != null ? slots.available(key) : entry.getValue();
        }
        return sum;
    }

    /** How many of an item the sources hold, cheap enough for a GUI that refreshes every second. */
    public long stockCount(ItemKey key) {
        long sum = 0;
        for (Map.Entry<BlockPos, Integer> entry : stockAt(key).entrySet()) {
            sum += entry.getValue();
        }
        return sum;
    }

    /** The positive counts of the key per source, in index order. */
    private Map<BlockPos, Integer> stockAt(ItemKey key) {
        Map<BlockPos, Integer> stock = new LinkedHashMap<>();
        for (BlockPos pos : index.sourcesHolding(key)) {
            Integer count = index.snapshot(pos).get(key);
            if (count != null && count > 0 && sources.contains(pos)) {
                stock.put(pos, count);
            }
        }
        return stock;
    }

    /** The unlocked stock of the key per rack, with the travel cost between each rack and the destination. */
    private List<RequestPlan.Stock<BlockPos>> stockOf(ItemKey key, BlockPos destination) {
        RailCosts.Search<NodePos> costs = fleet.costsFrom(dockRails(Conversions.toNodePos(destination)));
        List<RequestPlan.Stock<BlockPos>> stock = new ArrayList<>();
        for (BlockPos pos : stockAt(key).keySet()) {
            LockedSlots slots = rackSlots(pos);
            int count = slots == null ? 0 : slots.available(key);
            if (count > 0) {
                double cost = costs.costToAny(dockRails(Conversions.toNodePos(pos)));
                stock.add(new RequestPlan.Stock<>(pos, count, !Double.isInfinite(cost), cost));
            }
        }
        return stock;
    }

    /**
     * How many open wireless transfers are on their way to the Packing Station
     * at that position, as the sending stations reported them.
     */
    public int incomingWireless(BlockPos station) {
        return level == null ? 0 : WirelessIncoming.count(level.dimension(), station);
    }

    /** The Packing Stations of this warehouse. */
    public List<BlockPos> packingStations() {
        List<BlockPos> points = new ArrayList<>();
        if (level == null) {
            return points;
        }
        for (BlockPos pos : stations) {
            if (loaded(pos) instanceof PackingStationBlockEntity) {
                points.add(pos);
            }
        }
        points.sort(Comparator.comparingLong(BlockPos::asLong));
        return points;
    }

    /** The controller of an online warehouse, when its chunk is loaded. */
    public Optional<WarehouseControllerBlockEntity> controller() {
        if (status() != Status.ONLINE || controllerPos == null) {
            return Optional.empty();
        }
        return loaded(controllerPos) instanceof WarehouseControllerBlockEntity controller ? Optional.of(controller) : Optional.empty();
    }

    /**
     * Draws FE from the controller's buffer for one of the warehouse's
     * consumers; refused while the warehouse is offline or its controller is
     * not loaded. A free draw is always paid.
     */
    public boolean drawPower(long amount) {
        if (amount <= 0) {
            return true;
        }
        return controller().map(controller -> controller.drawEnergy(amount)).orElse(false);
    }

    /** Whether the warehouse is in the no power state; an offline one or one without a loaded controller has none. */
    public boolean noPower() {
        return controller().map(WarehouseControllerBlockEntity::noPower).orElse(true);
    }

    /** The position of the single controller, empty unless online. */
    public Optional<BlockPos> controllerPos() {
        return status() == Status.ONLINE ? Optional.ofNullable(controllerPos) : Optional.empty();
    }

    /** Drops the queued pick jobs of a request; loads already taken keep going. */
    public void dropQueuedFor(int requestId) {
        fleet.removeQueued(requestId);
    }

    /** Tops every outbound station's buffer up to its keep amounts and records what could not be filled. */
    private void tickOutbound() {
        if (status() != Status.ONLINE) {
            return;
        }
        releaseGoneExports();
        for (BlockPos pos : new ArrayList<>(stations)) {
            if (loaded(pos) instanceof OutboundStationBlockEntity station) {
                restock(pos, station);
            }
        }
    }

    /**
     * Plans the keep slots of a station, then its exports with the free slots
     * the keep plans left, and sets the station's state and lamp.
     */
    private void restock(BlockPos pos, OutboundStationBlockEntity station) {
        ItemStack shortStack = ItemStack.EMPTY;
        String shortReason = "";
        boolean pending = false;
        int freeSlots = station.buffer().freeSlots();
        for (int slot = 0; slot < station.keep().getSlots(); slot++) {
            ItemStack wanted = station.keep().getStackInSlot(slot);
            if (wanted.isEmpty()) {
                continue;
            }
            ItemKey key = ItemKey.of(wanted);
            int onTheWay = pendingTo(pos, key);
            pending |= onTheWay > 0;
            int have = station.bufferCount(key) + onTheWay;
            if (have >= wanted.getCount()) {
                continue;
            }
            int missing = Math.min(wanted.getCount() - have, station.buffer().freeSlots() * key.stack(1).getMaxStackSize());
            if (missing <= 0) {
                pending = true;
                continue;
            }
            Started started = planAndStart(pos, key, missing, 0);
            pending |= started.planned() > 0;
            freeSlots -= slotsFor(started.planned(), key);
            if (started.planned() < missing && shortStack.isEmpty()) {
                shortStack = key.stack(1);
                shortReason = started.refusal().isEmpty() ? "out_of_stock" : started.refusal();
            }
        }
        Map<Integer, ItemKey> exports = stockRules().map(found -> found.exportsAt(pos)).orElse(Map.of());
        for (ItemKey key : exports.values()) {
            StockRule rule = stockRules().map(found -> found.get(key)).orElse(null);
            if (rule == null || !rule.hasExport()) {
                continue;
            }
            int onTheWay = pendingTo(pos, key);
            long surplus = ExportPlan.surplus(available(key), rule.export(), 0);
            int wanted = ExportPlan.bounded(surplus, Math.max(0, freeSlots), key.stack(1).getMaxStackSize());
            int planned = wanted > 0 ? planAndStart(pos, key, wanted, 0).planned() : 0;
            freeSlots -= slotsFor(planned, key);
            pending |= planned > 0 || onTheWay > 0;
            boolean full = surplus > 0 && wanted == 0;
            if (full && shortStack.isEmpty()) {
                shortStack = key.stack(1);
                shortReason = "export_full";
            }
            rules.exportChecked(key, full ? StockRuleKeeper.STATION_FULL : planned > 0 || onTheWay > 0 ? StockRuleKeeper.EXPORTING : null);
        }
        station.setRestockState(shortStack, shortReason, pending);
    }

    /** How many buffer slots that many items of the key fill. */
    private static int slotsFor(int items, ItemKey key) {
        int stack = Math.max(1, key.stack(1).getMaxStackSize());
        return (items + stack - 1) / stack;
    }

    /** Items of the key queued, riding or held for a destination. */
    private int pendingTo(BlockPos destination, ItemKey key) {
        return fleet.queuedAmount(destination, key) + fleet.ridingAmount(destination, key) + heldAmount(destination, key)
                + putAwaysWaiting(key, job -> job.target().equals(destination)) + fleet.putAwayCargoTo(destination, key);
    }

    private int heldAmount(BlockPos destination, ItemKey key) {
        int count = 0;
        for (HeldLoad load : held.getOrDefault(destination, List.of())) {
            if (load.job().key().equals(key)) {
                count += load.stack().getCount();
            }
        }
        return count;
    }

    /** The controller's stock rules, present while a controller is in the network and loaded. */
    public Optional<StockRules> stockRules() {
        return controllerPos != null && loaded(controllerPos) instanceof WarehouseControllerBlockEntity controller
                ? Optional.of(controller.rules()) : Optional.empty();
    }

    /**
     * Sets the Make pair of the item's rule and keeps its cap.
     *
     * @return {@link io.github.dirnei.mc.servus.shuttles.order.RuleThresholds#OK} or the reason the rule was refused
     */
    public String setStockRule(ItemKey key, int min, int max) {
        return rules.setMake(key, min, max);
    }

    /**
     * Replaces the item's rule with the Make pair (null for none) and the cap (0 for none).
     *
     * @return {@link io.github.dirnei.mc.servus.shuttles.order.RuleThresholds#OK} or the reason the rule was refused
     */
    public String setStockRule(ItemKey key, StockRule.Make make, int cap) {
        return rules.set(key, make, cap, -1);
    }

    /**
     * Replaces the item's rule with the Make pair (null for none), the cap (0 for none) and the export (-1 for none).
     *
     * @return {@link io.github.dirnei.mc.servus.shuttles.order.RuleThresholds#OK} or the reason the rule was refused
     */
    public String setStockRule(ItemKey key, StockRule.Make make, int cap, int export) {
        return rules.set(key, make, cap, export);
    }

    /** One export a station holds: the item, the rule's threshold and the export state. */
    public record ExportSlot(ItemKey key, int threshold, String state) {
    }

    /** An export of this warehouse with its threshold and the station holding it, empty while none does. */
    public record ExportOffer(ItemKey key, int threshold, Optional<BlockPos> holder) {
    }

    /** The exports the station at the position holds, by keep slot. */
    public Map<Integer, ExportSlot> exportSlotsAt(BlockPos station) {
        Map<Integer, ExportSlot> slots = new TreeMap<>();
        stockRules().ifPresent(found -> found.exportsAt(station).forEach((slot, key) -> {
            StockRule rule = found.get(key);
            if (rule != null) {
                slots.put(slot, new ExportSlot(key, rule.export(), rules.exportState(key)));
            }
        }));
        return slots;
    }

    /** Whether a keep slot of the station at the position holds an export. */
    public boolean holdsExport(BlockPos station, int slot) {
        return stockRules().map(found -> found.exportsAt(station).containsKey(slot)).orElse(false);
    }

    /** Every export of the warehouse, for the claim picker. */
    public List<ExportOffer> exportOffers() {
        List<ExportOffer> offers = new ArrayList<>();
        stockRules().ifPresent(found -> {
            for (StockRule rule : found.all()) {
                if (rule.hasExport()) {
                    offers.add(new ExportOffer(rule.key(), rule.export(), found.exportHolder(rule.key()).map(claim -> claim.station())));
                }
            }
        });
        return offers;
    }

    /**
     * Gives the item's export to a keep slot of an Outbound Station of this
     * warehouse whose slot holds neither a keep amount nor an export.
     *
     * @return whether the export was claimed
     */
    public boolean claimExport(ItemKey key, BlockPos station, int slot) {
        if (status() != Status.ONLINE || !stations.contains(station) || !(loaded(station) instanceof OutboundStationBlockEntity outbound)
                || slot < 0 || slot >= outbound.keep().getSlots() || !outbound.keep().getStackInSlot(slot).isEmpty()) {
            return false;
        }
        return stockRules().map(found -> found.claimExport(key, station, slot)).orElse(false);
    }

    /** Frees a keep slot of a station from its export; true when it held one. */
    public boolean releaseExport(BlockPos station, int slot) {
        return stockRules().flatMap(found -> found.releaseExport(station, slot)).isPresent();
    }

    /**
     * Frees the exports of stations that left the warehouse: broken,
     * dismantled or cut off by a removed rail. A station whose chunk is not
     * loaded, or that has not joined a network yet, keeps its exports.
     */
    private void releaseGoneExports() {
        stockRules().ifPresent(found -> {
            Set<BlockPos> holders = new LinkedHashSet<>();
            found.exportClaims().values().forEach(claim -> holders.add(claim.station()));
            for (BlockPos pos : holders) {
                boolean member = stations.contains(pos) || !level.isLoaded(pos);
                boolean joining = level.getBlockEntity(pos) instanceof OutboundStationBlockEntity station && station.network().isEmpty();
                if (!member && !joining) {
                    found.releaseStation(pos);
                }
            }
        });
    }

    /** Sets the cap of the item's rule and keeps its Make pair; returns OK or the reason. */
    public String setStockCap(ItemKey key, int cap) {
        return rules.setCap(key, cap);
    }

    /** Clears the cap of the item's rule, removing a rule left without a Make pair; false when it had no cap. */
    public boolean clearStockCap(ItemKey key) {
        return rules.clearCap(key);
    }

    /**
     * How many of {@code offered} items of the key an Inbound Station may take
     * under the cap of the item's rule: all without a capped rule or while
     * offline. Held counts the indexed stock, every Inbound buffer, the
     * put-aways loaded on shuttles and what open CPU orders still make for
     * storage.
     */
    public int capRoom(ItemKey key, int offered) {
        if (status() != Status.ONLINE) {
            return offered;
        }
        StockRule rule = stockRules().map(found -> found.get(key)).orElse(null);
        if (rule == null || !rule.hasCap()) {
            return offered;
        }
        long held = index.count(key) + fleet.putAwayCargo(key, this::exportStop)
                + CpuOrders.network(level, this).map(line -> CpuOrders.incomingFor(level, line, key)).orElse(0);
        for (BlockPos pos : stations) {
            if (loaded(pos) instanceof InboundStationBlockEntity inbound) {
                held += inbound.held(key) - putAwaysWaiting(key, job -> job.station().equals(pos) && exportStop(job.target()));
            }
        }
        return CapRoom.room(rule.cap(), held, offered);
    }

    /** The label of the station at the position, or a plain "Station" when it is not loaded. */
    public Component stationLabel(BlockPos pos) {
        return MachineLabels.at(level, pos, Component.translatable("station.servus_shuttles.label.unloaded"));
    }

    public boolean removeStockRule(ItemKey key) {
        return rules.remove(key);
    }

    public List<RuleRow> ruleRows() {
        return rules.rows();
    }

    /** Creates a work order by hand; returns the refusal key or an empty string. */
    public String order(ItemKey key, int amount) {
        if (status() != Status.ONLINE || level == null) {
            return "offline";
        }
        Optional<DataNetwork> line = CpuOrders.network(level, this);
        return line.map(network -> CpuOrders.create(level, network, key, amount, 0).refusal()).orElse("no_cpu");
    }

    /** What a plan and start did: how many are on their way, or why nothing happened. */
    private record Started(String refusal, int planned) {
    }

    private Started planAndStart(BlockPos destination, ItemKey key, int amount, int requestId) {
        if (status() != Status.ONLINE) {
            return new Started("offline", 0);
        }
        boolean reachable = fleet.canReach(destination);
        RequestPlan.Plan<BlockPos> plan = plan(key, amount, destination, reachable);
        if (plan.jobs().isEmpty()) {
            return new Started(refusal(plan, reachable), 0);
        }
        startJobs(plan, requestId, key, destination);
        return new Started("", plan.planned());
    }

    private RequestPlan.Plan<BlockPos> plan(ItemKey key, int amount, BlockPos destination, boolean destinationReachable) {
        return RequestPlan.plan(amount, key.stack(1).getMaxStackSize(), destinationReachable, stockOf(key, destination));
    }

    /** Why a plan without jobs failed: {@code no_rail}, {@code needs_shuttle} or {@code out_of_stock}. */
    private String refusal(RequestPlan.Plan<BlockPos> plan, boolean destinationReachable) {
        if (!plan.needsShuttle()) {
            return "out_of_stock";
        }
        return fleet.hasShuttles() && !destinationReachable ? "no_rail" : "needs_shuttle";
    }

    /** The next job id of the world; also the lock id of a Packing Station's grant. */
    public int nextJobId() {
        return CounterData.get(level.getServer(), CounterData.Kind.JOB_IDS).take();
    }

    /**
     * Queues one shuttle pick per planned job: it locks the slots it takes
     * from at once, and its destination slot now when one is free, else
     * before a shuttle is sent for it.
     */
    private void startJobs(RequestPlan.Plan<BlockPos> plan, int requestId, ItemKey key, BlockPos destination) {
        for (RequestPlan.Job<BlockPos> job : plan.jobs()) {
            LockedSlots slots = rackSlots(job.source());
            int id = nextJobId();
            int locked = slots == null ? 0 : slots.lockOut(id, key, job.count());
            if (locked <= 0) {
                continue;
            }
            PickJob pick = new PickJob(id, requestId, key, locked, job.source(), destination);
            readyToSend(pick);
            fleet.requestPick(pick);
        }
    }

    @Override
    public boolean readyToSend(PickJob job) {
        if (level == null || !level.isLoaded(job.terminal())) {
            return false;
        }
        if (!(loaded(job.terminal()) instanceof DeliveryTarget target)) {
            return true;
        }
        LockedSlots slots = target.deliveries();
        if (slots.holds(job.id())) {
            return true;
        }
        if (target instanceof PackingStationBlockEntity station && !station.admits(job.key(), job.amount())) {
            return false;
        }
        int locked = slots.lockIn(job.id(), job.key(), job.amount(), true);
        if (locked >= job.amount()) {
            return true;
        }
        if (locked > 0) {
            slots.release(job.id());
        }
        return false;
    }

    @Override
    public boolean destinationGone(PickJob job) {
        return level != null && level.isLoaded(job.terminal()) && !(loaded(job.terminal()) instanceof DeliveryTarget);
    }

    @Override
    public ItemStack loadPutAway(PutAwayJob job) {
        return loaded(job.station()) instanceof PutAwaySource source ? source.putAwayBuffer().extractAs(job.id()) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack loadPick(PickJob job) {
        LockedSlots slots = rackSlots(job.source());
        if (slots == null) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = slots.extractAs(job.id());
        if (!taken.isEmpty()) {
            markDirty(job.source());
        }
        return taken;
    }

    @Override
    public ItemStack unloadInto(BlockPos target, int jobId, ItemStack cargo) {
        LockedSlots slots = putAwaySlots(target);
        if (slots == null || cargo.isEmpty()) {
            return cargo;
        }
        ItemStack left = slots.insertAs(jobId, cargo);
        if (left.getCount() != cargo.getCount()) {
            markDirty(target);
        }
        return left;
    }

    @Override
    public List<BlockPos> storeTargets(int jobId, ItemStack cargo, Set<NodePos> from, Function<NodePos, Set<NodePos>> ways) {
        List<BlockPos> stops = new ArrayList<>();
        if (cargo.isEmpty()) {
            return stops;
        }
        ItemKey key = ItemKey.of(cargo);
        RailCosts.Search<NodePos> costs = fleet.costsFrom(from, ways);
        int left = cargo.getCount();
        for (BlockPos pos : rankTargets(cargo, tiers(key), costs, candidate -> true)) {
            if (left <= 0) {
                break;
            }
            int locked = rackSlots(pos).lockIn(jobId, key, left, false);
            if (locked > 0) {
                stops.add(pos);
                left -= locked;
            }
        }
        return stops;
    }

    @Override
    public Map<BlockPos, Integer> lockStock(int jobId, ItemKey key, int amount, Set<NodePos> from, Function<NodePos, Set<NodePos>> ways) {
        Map<BlockPos, Integer> locked = new LinkedHashMap<>();
        if (amount <= 0 || from.isEmpty()) {
            return locked;
        }
        RailCosts.Search<NodePos> costs = fleet.costsFrom(from, ways);
        List<RequestPlan.Stock<BlockPos>> stock = new ArrayList<>();
        for (BlockPos pos : stockAt(key).keySet()) {
            LockedSlots slots = rackSlots(pos);
            int count = slots == null ? 0 : slots.available(key);
            if (count > 0) {
                double cost = costs.costToAny(dockRails(Conversions.toNodePos(pos)));
                stock.add(new RequestPlan.Stock<>(pos, count, !Double.isInfinite(cost), cost));
            }
        }
        for (RequestPlan.Job<BlockPos> job : RequestPlan.plan(amount, Integer.MAX_VALUE, true, stock).jobs()) {
            int got = rackSlots(job.source()).lockOut(jobId, key, job.count());
            if (got > 0) {
                locked.merge(job.source(), got, Integer::sum);
            }
        }
        return locked;
    }

    @Override
    public List<ItemStack> takeLocked(BlockPos source, int jobId) {
        LockedSlots slots = rackSlots(source);
        if (slots == null) {
            return List.of();
        }
        List<ItemStack> taken = slots.extractAllAs(jobId);
        if (!taken.isEmpty()) {
            markDirty(source);
        }
        return taken;
    }

    @Override
    public List<ItemStack> unloadAllInto(BlockPos target, int jobId, List<ItemStack> stacks) {
        LockedSlots slots = rackSlots(target);
        if (slots == null || stacks.isEmpty()) {
            return stacks;
        }
        List<ItemStack> left = slots.insertAllAs(jobId, stacks);
        markDirty(target);
        return left;
    }

    @Override
    public boolean anyRoomFor(ItemStack cargo) {
        return !rankTargets(cargo, tiers(ItemKey.of(cargo)), null, candidate -> true).isEmpty();
    }

    @Override
    public void releaseLocks(int jobId, Collection<BlockPos> at) {
        boolean any = false;
        for (BlockPos pos : at) {
            if (loaded(pos) instanceof LockHolder holder) {
                for (LockedSlots slots : holder.lockedSlots()) {
                    any |= slots.release(jobId);
                }
            }
        }
        if (any) {
            fleet.roomChanged();
        }
    }

    @Override
    public void strandedChanged(BlockPos garage, boolean stranded) {
        if (loaded(garage) instanceof ShuttleGarageBlockEntity entity) {
            entity.setStranded(stranded);
        }
    }

    /** The shuttles of a garage parked with cargo that has nowhere to go. */
    public List<ShuttleFleet.Stranded> stranded(BlockPos garage) {
        return fleet.stranded(garage);
    }

    /** The item of the shuttle on that job, or empty while the job waits for one. */
    public Optional<ItemStack> shuttleOnJob(int jobId) {
        return fleet.shuttleOnJob(jobId);
    }

    /** Plans the picks of racks that left for good again from other racks; what cannot be planned counts as missing. */
    private void replanBrokenSources() {
        List<BlockPos> broken = List.copyOf(brokenSources);
        brokenSources.clear();
        for (BlockPos source : broken) {
            for (PickJob job : fleet.dropPicksFrom(source)) {
                releaseLocks(job.id(), List.of(job.terminal()));
                Started started = planAndStart(job.terminal(), job.key(), job.amount(), job.requestId());
                if (started.planned() < job.amount()) {
                    pickLoaded(job, started.planned());
                }
            }
        }
    }

    @Override
    public void pickLoaded(PickJob job, int loaded) {
        if (loaded < job.amount() && loaded(job.terminal()) instanceof PackingStationBlockEntity point) {
            point.shortfall(job.key(), job.amount() - loaded);
        }
    }

    @Override
    public void pickDropped(PickJob job, int count) {
        if (loaded(job.terminal()) instanceof PackingStationBlockEntity point) {
            point.shortfall(job.key(), count);
        }
    }

    /** Puts a pick's cargo into the destination slot locked for it, the rest into free slots; returns what did not fit. */
    @Override
    public ItemStack deliverTo(PickJob job, ItemStack stack) {
        if (stack.isEmpty() || level == null || !level.isLoaded(job.terminal())
                || !(loaded(job.terminal()) instanceof DeliveryTarget target)) {
            return stack;
        }
        ItemStack left = target.deliveries().insertAs(job.id(), stack);
        return target.deliveries().insertItem(left, false);
    }

    @Override
    public void deliverInstant(PickJob job, ItemStack stack) {
        ItemStack left = deliverTo(job, stack);
        if (!left.isEmpty()) {
            held.computeIfAbsent(job.terminal(), pos -> new ArrayList<>()).add(new HeldLoad(job, left));
            stateChanged(job.terminal());
        }
    }

    /** Retries the held loads of a destination after its delivery buffer changed. */
    private void retryHeld(BlockPos terminalPos) {
        List<HeldLoad> loads = held.get(terminalPos);
        if (loads == null) {
            return;
        }
        List<HeldLoad> remaining = new ArrayList<>();
        for (HeldLoad load : loads) {
            ItemStack left = deliverTo(load.job(), load.stack());
            if (!left.isEmpty()) {
                remaining.add(new HeldLoad(load.job(), left));
            }
        }
        if (remaining.isEmpty()) {
            held.remove(terminalPos);
        } else {
            held.put(terminalPos, remaining);
        }
        stateChanged(terminalPos);
    }

    private void addSource(BlockPos pos) {
        if (sources.add(pos)) {
            pollOrderStale = true;
            joinOrder = null;
        }
        markDirty(pos);
    }

    private void dropSource(BlockPos pos) {
        reservedOut.remove(pos);
        rackFill.remove(pos);
        if (sources.remove(pos)) {
            index.remove(pos);
            dirtySet.remove(pos);
            pollOrderStale = true;
            joinOrder = null;
        }
    }

    /** The slots of a source, or null when it is no source, its chunk is not loaded or it holds no stock. */
    private ItemView handlerOf(BlockPos pos) {
        return sources.contains(pos) && loaded(pos) instanceof StorageNodeBlockEntity node ? node.stock() : null;
    }

    /**
     * The block entity at a position whose chunk is loaded, else null. Never
     * loads a chunk: a load from a membership hook while chunks unload keeps
     * the server from stopping.
     */
    private BlockEntity loaded(BlockPos pos) {
        return level != null && level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
    }

    @Override
    protected void handle(WarehouseEvent event) {
        if (event instanceof WarehouseEvent.Dirty dirty) {
            if (sources.contains(dirty.pos())) {
                markDirty(dirty.pos());
            }
        } else if (event instanceof WarehouseEvent.OutputChanged output) {
            retryHeld(output.pos());
        } else if (event instanceof WarehouseEvent.Intake intake) {
            if (stations.contains(intake.pos())) {
                pendingStations.add(intake.pos());
            } else if (loaded(intake.pos()) instanceof FieldHeadBlockEntity) {
                pendingStations.add(intake.pos());
                // A field shuttle parked with cargo for a full To storage buffer tries again.
                fleet.roomChanged();
            }
        } else if (event instanceof WarehouseEvent.CapRefused refused) {
            rules.refused(refused.key(), refused.station());
        } else if (event instanceof WarehouseEvent.FluidChanged fluid) {
            if (gantries.contains(fluid.pos())) {
                pendingGantries.add(fluid.pos());
            }
            if (fluidRacks.contains(fluid.pos())) {
                pendingRacks.add(fluid.pos());
            }
        } else if (event instanceof WarehouseEvent.LiftUpgradesChanged lift) {
            if (elevators.contains(Conversions.toNodePos(lift.pos()))) {
                shaftsDirty = true;
            }
        } else if (event instanceof WarehouseEvent.ZoneChanged zoned) {
            if (loaded(zoned.pos()) instanceof StorageNodeBlockEntity node && node.holdsStock()) {
                zones.set(zoned.pos(), node.zoneDefinition());
                zonesDirty = true;
            }
        }
    }

    /** What a fluid store is: a gantry tank or a Fluid Rack section. */
    public enum StoreKind { GANTRY, RACK }

    /**
     * One fluid store as last read: a gantry tank with its gantry's mode and
     * its lock (empty for Any), or a Fluid Rack section, which counts as
     * Storage, with its zone and the fluids the zone lists; what it holds
     * and its capacity.
     */
    public record TankView(FluidStoreRef ref, StoreKind kind, GantryMode mode, Optional<Fluid> lock, String zone, Set<Fluid> zoneFluids,
                           FluidStack fluid, int capacity) {

        static TankView gantryTank(FluidStoreRef ref, GantryMode mode, @Nullable Fluid lock, FluidStack fluid, int capacity) {
            return new TankView(ref, StoreKind.GANTRY, mode, Optional.ofNullable(lock), "", Set.of(), fluid, capacity);
        }

        static TankView rack(FluidStoreRef ref, String zone, Set<Fluid> zoneFluids, FluidStack fluid, int capacity) {
            return new TankView(ref, StoreKind.RACK, GantryMode.STORAGE, Optional.empty(), zone, Set.copyOf(zoneFluids), fluid, capacity);
        }

        /** Whether the store prefers this fluid: a tank locked to it, a section zoned for it. */
        public boolean preferred(Fluid fluid) {
            return kind == StoreKind.RACK ? zoneFluids.contains(fluid) : lock.filter(fluid::equals).isPresent();
        }

        /** Whether the store's lock or zone lets this fluid in, whatever it holds. */
        public boolean allows(Fluid fluid) {
            return kind == StoreKind.RACK ? zoneFluids.isEmpty() || zoneFluids.contains(fluid) : lock.map(fluid::equals).orElse(true);
        }
    }

    /** Reads the stores that changed: every tank of a pending gantry and every pending Fluid Rack section. */
    private void readFluidStores() {
        for (BlockPos pos : List.copyOf(pendingGantries)) {
            pendingGantries.remove(pos);
            if (!(loaded(pos) instanceof FluidGantryBlockEntity gantry)) {
                continue;
            }
            List<TankView> views = new ArrayList<>(2);
            for (int tank = 0; tank < 2; tank++) {
                FluidStoreRef ref = new FluidStoreRef(pos, tank);
                FluidStack fluid = gantry.tank(tank).getFluid().copy();
                views.add(TankView.gantryTank(ref, gantry.mode(), gantry.lock(tank), fluid, gantry.tank(tank).getCapacity()));
                fluidIndex.update(ref, gantry.mode() == GantryMode.STORAGE && !fluid.isEmpty()
                        ? Map.of(FluidKey.of(fluid), fluid.getAmount()) : Map.of());
            }
            tankViews.put(pos, List.copyOf(views));
        }
        for (BlockPos pos : List.copyOf(pendingRacks)) {
            pendingRacks.remove(pos);
            readRackSection(pos);
        }
    }

    /**
     * Reads the section of the Fluid Rack bay at {@code pos}: its port bay
     * becomes the section's store; every other bay of it, and the bay itself
     * when it left the section, stops being one.
     */
    private void readRackSection(BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) {
            return;
        }
        List<BlockPos> section = FluidRackBlock.section(level, pos);
        BlockPos port = FluidRackBlock.port(level, pos);
        if (!section.contains(pos)) {
            dropRack(pos);
        }
        for (BlockPos bay : section) {
            if (!bay.equals(port)) {
                dropRack(bay);
            }
        }
        if (port == null || !fluidRacks.contains(port) || !(loaded(port) instanceof FluidRackBlockEntity rack)) {
            return;
        }
        FluidRackSectionHandler.Content content = rack.sectionHandler().content();
        FluidStoreRef ref = FluidStoreRef.rack(port);
        FluidStack fluid = content.fluid().copy();
        rackViews.put(port, TankView.rack(ref, rack.zone(), rack.zoneFluids(), fluid, content.capacity()));
        fluidIndex.update(ref, fluid.isEmpty() ? Map.of() : Map.of(FluidKey.of(fluid), fluid.getAmount()));
    }

    private void dropTanks(BlockPos gantry) {
        fluidIndex.remove(new FluidStoreRef(gantry, 0));
        fluidIndex.remove(new FluidStoreRef(gantry, 1));
        tankViews.remove(gantry);
    }

    private void dropRack(BlockPos bay) {
        if (rackViews.remove(bay) != null) {
            fluidIndex.remove(FluidStoreRef.rack(bay));
        }
    }

    /** The fluid stock: the tanks of the Storage gantries and the Fluid Rack sections, per fluid. */
    public WarehouseIndex<FluidStoreRef, FluidKey> fluidIndex() {
        return fluidIndex;
    }

    /** The Fluid Rack sections of this warehouse as last read, by their port bay in a stable order. */
    public List<TankView> rackViews() {
        List<TankView> list = new ArrayList<>(rackViews.values());
        list.sort(Comparator.comparingLong(view -> view.ref().pos().asLong()));
        return list;
    }

    /** Every store that holds fluid stock: the tanks of the Storage gantries, then the Fluid Rack sections. */
    public List<TankView> fluidStores() {
        List<TankView> stores = new ArrayList<>();
        for (BlockPos gantry : storageGantries()) {
            stores.addAll(tankViews(gantry));
        }
        stores.addAll(rackViews());
        return stores;
    }

    /** The Fluid Gantries of this warehouse, by their master's position, in a stable order. */
    public List<BlockPos> gantries() {
        List<BlockPos> list = new ArrayList<>(gantries);
        list.sort(Comparator.comparingLong(BlockPos::asLong));
        return list;
    }

    /** The two tanks of a gantry as last read, empty before the first read. */
    public List<TankView> tankViews(BlockPos gantry) {
        return tankViews.getOrDefault(gantry, List.of());
    }

    private void markDirty(BlockPos pos) {
        if (dirtySet.add(pos)) {
            dirtyQueue.add(pos);
        }
    }

    @Override
    protected void onTick() {
        if (level == null) {
            return;
        }
        if (lampsDirty) {
            pushLamps();
        }
        if (controllers == 1) {
            tickController();
        }
        if (shaftsDirty) {
            shaftsDirty = false;
            List<Shaft> shafts = Shafts.group(elevators);
            shaftFloors = Shafts.floorsOf(shafts);
            fleet.refreshShafts(shafts);
        }
        fleet.tick();
        if (++claimTimer >= CLAIM_SWEEP_TICKS) {
            claimTimer = 0;
            sweepLocks();
        }
        if (!brokenSources.isEmpty() && status() == Status.ONLINE) {
            replanBrokenSources();
        }
        if (zonesDirty) {
            fleet.roomChanged();
        }
        tickStations();
        if (!machinesToNumber.isEmpty()) {
            numberMachines();
        }
        if (!pendingGantries.isEmpty() || !pendingRacks.isEmpty()) {
            readFluidStores();
        }
        if (++outboundTimer >= ShuttlesStatsManager.INSTANCE.outboundCheckInterval()) {
            outboundTimer = 0;
            tickOutbound();
        }
        rules.tick(level);
        if (++fieldTimer >= ShuttlesStatsManager.INSTANCE.fieldCheckInterval()) {
            fieldTimer = 0;
            queueFieldAndLineMachines();
            tickFields();
        }
        if (++fluidTimer >= ShuttlesStatsManager.INSTANCE.fluidCheckInterval()) {
            fluidTimer = 0;
            tickFluids();
        }
        if (!sources.isEmpty()) {
            pollSources();
        }
    }

    /** The span, site record, power and stock snapshot of the single controller. */
    private void tickController() {
        if (spanDirty && controllerPos != null) {
            updateSpan();
        }
        if (wirelessDirty) {
            controller().ifPresent(controller -> {
                wirelessDirty = false;
                SiteRegistry.of(level).updateWireless(controller, controller.hasWirelessUpgrade(),
                        controller.poweredForWireless(), !packingStations().isEmpty());
            });
        }
        controller().ifPresent(WarehouseControllerBlockEntity::tickPower);
        if (++snapshotTimer >= ShuttlesStatsManager.INSTANCE.siteSnapshotInterval()) {
            snapshotTimer = 0;
            snapshotStock();
            controller().ifPresent(controller -> SiteRegistry.of(level).updateEnergy(controller));
        }
    }

    /** Polls dirty sources first, then walks the rest round robin, within the per tick budget. */
    private void pollSources() {
        int budget = ShuttlesStatsManager.INSTANCE.sourcesPerTick();
        while (budget > 0 && !dirtyQueue.isEmpty()) {
            BlockPos pos = dirtyQueue.poll();
            dirtySet.remove(pos);
            if (sources.contains(pos)) {
                poll(pos);
                budget--;
            }
        }
        if (pollOrderStale) {
            pollOrder.clear();
            pollOrder.addAll(sources);
            pollOrderStale = false;
        }
        for (int i = 0; i < budget && !pollOrder.isEmpty(); i++) {
            cursor = (cursor + 1) % pollOrder.size();
            BlockPos pos = pollOrder.get(cursor);
            if (sources.contains(pos)) {
                poll(pos);
            }
        }
    }

    /** Queues every named member and, at the next check, every named machine of the data line: the warehouse changed. */
    private void renumberAll() {
        machinesToNumber.addAll(namedMembers);
        numberedLine = null;
    }

    /**
     * Gives every loaded named machine waiting for a number one from the controller,
     * in position order so machines numbered together get stable numbers. Unloaded
     * machines wait; nothing happens while the warehouse is offline.
     */
    private void numberMachines() {
        Optional<WarehouseControllerBlockEntity> found = controller();
        if (found.isEmpty()) {
            return;
        }
        WarehouseControllerBlockEntity controller = found.get();
        List<BlockPos> waiting = new ArrayList<>(machinesToNumber);
        waiting.sort(NUMBER_ORDER);
        for (BlockPos pos : waiting) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            machinesToNumber.remove(pos);
            if (level.getBlockEntity(pos) instanceof NamedMachine machine && machine.needsNumber(controller.warehouseId())) {
                machine.assignNumber(controller.warehouseId(), controller.takeNumber(machine.machineKind()));
            }
        }
    }

    /**
     * Queues the named machines that belong to this warehouse without being on
     * its rails: the Field Heads it serves that need a number from it, and the
     * named machines of its data line once the line or its members changed.
     */
    private void queueFieldAndLineMachines() {
        Optional<WarehouseControllerBlockEntity> controller = controller();
        if (controller.isEmpty()) {
            return;
        }
        for (FieldHeadBlockEntity head : servedHeads()) {
            if (head.needsNumber(controller.get().warehouseId())) {
                machinesToNumber.add(head.getBlockPos());
            }
        }
        Optional<DataNetwork> line = CpuOrders.network(level, this);
        if (line.isEmpty() || line.get() == numberedLine && line.get().membersRevision() == numberedLineRevision) {
            return;
        }
        numberedLine = line.get();
        numberedLineRevision = numberedLine.membersRevision();
        for (DataMember member : numberedLine.dataMembers()) {
            BlockPos pos = member.pos();
            if (!level.isLoaded(pos)) {
                continue;
            }
            CpuBlockEntity cpu = CpuBlock.master(level, pos);
            if (cpu != null) {
                machinesToNumber.add(cpu.getBlockPos());
                continue;
            }
            MachineLabels.machineAt(level, pos).map(machine -> machine.nameSlot().pos())
                    .filter(this::numberedThroughLine)
                    .ifPresent(machinesToNumber::add);
        }
    }

    /**
     * Whether this warehouse numbers the named machine there through its data line: anything
     * but a rail member of another warehouse with a controller, so a Packing Station on two
     * warehouses' lines keeps the number of the one whose rails it joins.
     */
    private boolean numberedThroughLine(BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof StorageNodeBlockEntity node)) {
            return true;
        }
        return node.network().filter(other -> other != this && other.controllerPos().isPresent()).isEmpty();
    }

    private void tickStations() {
        if (stations.isEmpty() && pendingStations.isEmpty() && blockedStations.isEmpty()) {
            return;
        }
        if (zonesDirty) {
            zonesDirty = false;
            pendingStations.addAll(blockedStations);
        }
        if (!blockedStations.isEmpty() && ++blockedRetry >= BLOCKED_RETRY_TICKS) {
            blockedRetry = 0;
            pendingStations.addAll(blockedStations);
        }
        if (pendingStations.isEmpty() || status() != Status.ONLINE) {
            return;
        }
        for (BlockPos pos : new ArrayList<>(pendingStations)) {
            pendingStations.remove(pos);
            if (loaded(pos) instanceof PutAwaySource source) {
                drain(pos, source);
            } else {
                blockedStations.remove(pos);
            }
        }
    }

    /**
     * Releases locks whose job the fleet has not known at two sweeps in a
     * row. Sweeps only while online and while every chunk of the site is
     * loaded, so a job in a garage that is not loaded yet keeps its locks.
     */
    private void sweepLocks() {
        if (status() != Status.ONLINE || !siteLoaded()) {
            return;
        }
        List<LockKey> locks = new ArrayList<>();
        for (BlockPos pos : lockHolders()) {
            if (loaded(pos) instanceof LockHolder holder) {
                List<LockedSlots> inventories = holder.lockedSlots();
                for (int inventory = 0; inventory < inventories.size(); inventory++) {
                    for (LockedSlots.Locked locked : inventories.get(inventory).locked()) {
                        locks.add(new LockKey(pos, inventory, locked.slot(), locked.lock().jobId()));
                    }
                }
            }
        }
        for (LockKey orphan : lockSweep.sweep(locks, lock -> fleet.knows(lock.jobId()) || grantLock(lock))) {
            if (loaded(orphan.pos()) instanceof LockHolder holder && orphan.inventory() < holder.lockedSlots().size()) {
                LockedSlots slots = holder.lockedSlots().get(orphan.inventory());
                if (slots.locksOf(orphan.slot()).stream().anyMatch(lock -> lock.jobId() == orphan.jobId())) {
                    slots.release(orphan.slot(), orphan.jobId());
                    if (holder instanceof PutAwaySource) {
                        pendingStations.add(orphan.pos());
                    }
                    fleet.roomChanged();
                }
            }
        }
    }

    /** Whether the lock belongs to an open grant of an Inbound or Packing Station, which its station sweeps itself. */
    private boolean grantLock(LockKey lock) {
        return loaded(lock.pos()) instanceof InboundStationBlockEntity station && station.holdsGrant(lock.jobId());
    }

    /** Stations, sources and served Field Heads, the blocks whose slots take locks. */
    private List<BlockPos> lockHolders() {
        List<BlockPos> holders = new ArrayList<>(stations);
        holders.addAll(sources);
        for (FieldHeadBlockEntity head : servedHeads()) {
            holders.add(head.getBlockPos());
        }
        return holders;
    }

    /** Whether every chunk of the controller's site span is loaded; false without a loaded controller. */
    private boolean siteLoaded() {
        Optional<WarehouseControllerBlockEntity> controller = controller();
        if (controller.isEmpty()) {
            return false;
        }
        for (long key : controller.get().span()) {
            if (!level.getChunkSource().hasChunk(SiteChunks.x(key), SiteChunks.z(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Plans up to {@code put_away_per_tick} unlocked stacks of the station's
     * buffer: each goes to the best rack by the put-away rule as seen from the
     * station, locking its room there and its station slot for the amount
     * that rack takes. In a top up buffer the items a slot holds beyond its
     * OUT locks are planned the same way; slots a grant reserves wait.
     */
    private void drain(BlockPos pos, PutAwaySource station) {
        int budget = ShuttlesStatsManager.INSTANCE.putAwayPerTick();
        ItemKey blockedKey = null;
        String blockedZone = null;
        boolean blockedByRack = false;
        boolean more = false;
        boolean stationReached = fleet.canReach(pos);
        Supplier<RailCosts.Search<NodePos>> costs = stationReached
                ? Suppliers.memoize(() -> fleet.costsFrom(dockRails(Conversions.toNodePos(pos)))) : () -> null;
        LockedSlots buffer = station.putAwayBuffer();
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack stack = unplanned(buffer, slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (budget <= 0) {
                more = true;
                break;
            }
            ItemKey key = ItemKey.of(stack);
            int direct = exportDirect(pos, station, slot, stack, costs);
            int rest = stack.getCount() - direct;
            if (rest <= 0 || joinOpenPutAway(pos, station, slot, key, rest, sources::contains) > 0) {
                budget--;
                continue;
            }
            PutAwayPlan.Tiers<BlockPos> tiers = tiers(key);
            List<BlockPos> ranked = stationReached ? rankTargets(stack, tiers, costs.get(), candidate -> true) : List.of();
            if (ranked.isEmpty()) {
                if (blockedKey == null) {
                    blockedKey = key;
                    blockedZone = zones.matchedZone(tiers);
                    blockedByRack = !rankTargets(stack, tiers, null, candidate -> true).isEmpty();
                }
                continue;
            }
            budget--;
            BlockPos target = ranked.get(0);
            int id = nextJobId();
            int amount = rackSlots(target).lockIn(id, key, rest, false);
            buffer.lock(slot, new SlotLock(SlotLock.Kind.OUT, key, amount, id));
            fleet.requestPutAway(new PutAwayJob(id, pos, slot, key, amount, target));
        }
        if (more) {
            pendingStations.add(pos);
        }
        if (blockedKey != null) {
            blockedStations.add(pos);
            PutAwayPlan.Reason reason = blockedByRack ? PutAwayPlan.Reason.NO_SHUTTLE
                    : hasStorage(blockedKey) ? PutAwayPlan.Reason.NO_ROOM : PutAwayPlan.Reason.NO_STORAGE;
            station.setBlocked(reason, blockedKey, blockedZone);
        } else {
            blockedStations.remove(pos);
            station.clearBlocked();
        }
    }

    /**
     * The items of a buffer slot no put-away has planned yet: the whole stack
     * of an unlocked slot, in a top up buffer what a slot holding only OUT
     * locks has beyond them; empty for other locked slots.
     */
    private static ItemStack unplanned(LockedSlots buffer, int slot) {
        ItemStack stack = buffer.getStackInSlot(slot);
        if (stack.isEmpty() || !buffer.isLocked(slot)) {
            return stack;
        }
        if (!buffer.topsUp() || buffer.hasIn(slot)) {
            return ItemStack.EMPTY;
        }
        int surplus = SlotFill.unreserved(stack.getCount(), buffer.reserved(slot, SlotLock.Kind.OUT, 0));
        return surplus <= 0 ? ItemStack.EMPTY : stack.copyWithCount(surplus);
    }

    /**
     * Lets {@code count} unplanned items of a buffer slot join a put-away of
     * the same item from this station to a target that matches that no
     * shuttle has loaded yet, the job already on that slot first, up to a
     * stack per job and as far as its target has room, so a buffer filling
     * item by item makes one trip, not many. A job on the slot grows its OUT
     * lock there. Returns how many joined.
     */
    private int joinOpenPutAway(BlockPos pos, PutAwaySource station, int slot, ItemKey key, int count, Predicate<BlockPos> targets) {
        LockedSlots buffer = station.putAwayBuffer();
        List<PutAwayJob> open = new ArrayList<>(fleet.openPutAways(pos, key, targets));
        open.sort(Comparator.comparing(job -> !buffer.holdsOut(slot, job.id())));
        for (PutAwayJob job : open) {
            LockedSlots target = putAwaySlots(job.target());
            int room = TopUp.join(buffer.lockedAmount(job.id(), SlotLock.Kind.OUT), count, key.stack(1).getMaxStackSize());
            if (target == null || room <= 0) {
                continue;
            }
            int joined = target.extendIn(job.id(), key, room);
            if (joined <= 0) {
                continue;
            }
            if (buffer.holdsOut(slot, job.id())) {
                buffer.growOut(slot, job.id(), joined);
            } else {
                buffer.lock(slot, new SlotLock(SlotLock.Kind.OUT, key, joined, job.id()));
            }
            return joined;
        }
        return 0;
    }

    /**
     * Sends the part of a buffer stack that the stock would hold above its
     * rule's export threshold once stored straight to the Outbound Station
     * holding the export, as far as that station has free slots and a shuttle
     * can drive there from the source. Locks the source slot and the room at
     * the station and returns how many items go there; the rest is stored.
     */
    private int exportDirect(BlockPos pos, PutAwaySource source, int slot, ItemStack stack, Supplier<RailCosts.Search<NodePos>> costs) {
        ItemKey key = ItemKey.of(stack);
        StockRule rule = stockRules().map(found -> found.get(key)).orElse(null);
        if (rule == null || !rule.hasExport()) {
            return 0;
        }
        BlockPos holder = stockRules().flatMap(found -> found.exportHolder(key)).map(claim -> claim.station()).orElse(null);
        if (holder == null || !exportStop(holder) || !(loaded(holder) instanceof OutboundStationBlockEntity outbound) || costs.get() == null
                || Double.isInfinite(costs.get().costToAny(dockRails(Conversions.toNodePos(holder))))) {
            return 0;
        }
        long stock = available(key) + fleet.putAwayCargo(key, this::exportStop) + putAwaysWaiting(key, job -> !exportStop(job.target()));
        int wanted = ExportPlan.direct(stock, rule.export(), stack.getCount());
        if (wanted <= 0) {
            return 0;
        }
        int sent = joinOpenPutAway(pos, source, slot, key, wanted, holder::equals);
        if (sent < wanted) {
            int id = nextJobId();
            int locked = outbound.deliveries().lockIn(id, key, wanted - sent, true);
            if (locked > 0) {
                source.putAwayBuffer().lock(slot, new SlotLock(SlotLock.Kind.OUT, key, locked, id));
                fleet.requestPutAway(new PutAwayJob(id, pos, slot, key, locked, holder));
                sent += locked;
            }
        }
        return sent;
    }

    /** Whether a put-away stop takes its cargo out of storage: a station of this warehouse rather than a rack. */
    private boolean exportStop(BlockPos pos) {
        return stations.contains(pos) && !sources.contains(pos);
    }

    /**
     * Items of the key on put-aways not loaded yet whose job matches, each
     * counted by the OUT locks it holds at its source, since joining items
     * grows those locks and not the job.
     */
    private int putAwaysWaiting(ItemKey key, Predicate<PutAwayJob> which) {
        return fleet.putAwaysWaiting(key, which, job -> loaded(job.station()) instanceof PutAwaySource source
                ? source.putAwayBuffer().lockedAmount(job.id(), SlotLock.Kind.OUT) : job.amount());
    }

    /** The slots a put-away unloads into: a rack's, or the buffer of an Outbound Station that takes an export straight from the source. */
    private LockedSlots putAwaySlots(BlockPos target) {
        LockedSlots rack = rackSlots(target);
        if (rack != null) {
            return rack;
        }
        return exportStop(target) && loaded(target) instanceof OutboundStationBlockEntity outbound ? outbound.deliveries() : null;
    }

    /** Whether any source the key could go to has an inventory at all; the controller alone has none. */
    private boolean hasStorage(ItemKey key) {
        for (List<BlockPos> tier : targetTiers(key, tiers(key))) {
            for (BlockPos pos : tier) {
                if (handlerOf(pos) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The rack the put-away rule ranks first for the stack among those allowed, ignoring travel; empty when nothing allowed has room. */
    public Optional<BlockPos> putAwayTarget(ItemStack stack, Predicate<BlockPos> allowed) {
        return rankTargets(stack, tiers(ItemKey.of(stack)), null, allowed).stream().findFirst();
    }

    /**
     * Queues a supply run for each Field Head on this warehouse's rails whose
     * buffer runs low, and a field run for each that has work, when a
     * matching shuttle can take it. Each head reads its produce stock first.
     * Polls the loaded heads only.
     */
    private void tickFields() {
        if (status() != Status.ONLINE) {
            return;
        }
        boolean harvesters = fleet.hasFieldShuttles(JobNeeds.HARVESTER);
        boolean lumbers = fleet.hasFieldShuttles(JobNeeds.LUMBER);
        boolean spreaders = fleet.hasFieldShuttles(JobNeeds.SPREADER);
        boolean suppliers = fleet.hasShuttles();
        for (FieldHeadBlockEntity head : servedHeads()) {
            head.readProduceStock(this);
            BlockPos pos = head.getBlockPos();
            if (suppliers && !fleet.hasSupplyJob(pos) && supplyDue(head)) {
                fleet.requestSupply(new SupplyJob(nextJobId(), pos));
            }
            if (head.toStorageHoldsItems()) {
                pendingStations.add(pos);
            }
            boolean trees = head.kind().trees();
            if ((trees ? lumbers : harvesters) && !fleet.hasFieldJob(pos) && head.toStorageHasFreeSlot()
                    && fleet.fieldReachable(pos, head.jobNeeds())) {
                boolean seeds = head.supplyCount(SupplyBuffer.Part.SEEDS) >= (head.twoByTwo() ? SeedPlan.SQUARE : 1);
                boolean due = trees ? FieldDue.dueTrees(head.harvestCalled(), head.plantMode(), head.plantableCount(), seeds)
                        : FieldDue.due(head.harvestCalled(), head.plantMode(), head.hydrated(), head.tillableCount(), head.plantableCount(), seeds);
                if (due) {
                    fleet.requestField(new FieldJob(nextJobId(), pos, head.kind()));
                }
            }
            if (spreaders && head.fertilizing() && !fleet.hasFertilizeJob(pos) && fleet.fieldReachable(pos, JobNeeds.SPREADER)) {
                long now = level.getGameTime();
                if (head.fertilizeDue(now)) {
                    fleet.requestFertilize(new FertilizeJob(nextJobId(), pos));
                    head.fertilizeCalled(now);
                }
            }
        }
    }

    /** The loaded Field Heads that serve a field on this warehouse's rails. */
    private List<FieldHeadBlockEntity> servedHeads() {
        List<FieldHeadBlockEntity> heads = new ArrayList<>();
        for (FieldHeadBlockEntity head : FieldHeads.heads(level)) {
            if (head.status() == FieldStatus.SERVING && !head.rails().isEmpty()
                    && members().contains(Conversions.toNodePos(head.rails().iterator().next()))) {
                heads.add(head);
            }
        }
        return heads;
    }

    /** Whether a part of the head's buffer is low while this warehouse holds more of its item than the head keeps in stock. */
    private boolean supplyDue(FieldHeadBlockEntity head) {
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            Item item = head.supplyItem(part);
            if (item == null) {
                continue;
            }
            if (SupplyPlan.due(new SupplyPlan.Part(head.supplyCount(part), head.supplyLevel(part),
                    available(ItemKey.of(new ItemStack(item))), head.supplyReserve(part)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * One tank tankers empty on put-away runs, an Import gantry tank or a
     * Fluid Inbound tank, with what it holds and its capacity.
     */
    private record FluidSource(FluidStoreRef ref, FluidStack fluid, int capacity) {
    }

    /** The put-away sources: the tanks of the Import gantries as last read, then the Fluid Inbound tanks as they are now. */
    private List<FluidSource> putAwaySources() {
        List<FluidSource> sources = new ArrayList<>();
        for (BlockPos pos : gantries()) {
            for (TankView view : tankViews(pos)) {
                if (view.mode() == GantryMode.IMPORT) {
                    sources.add(new FluidSource(view.ref(), view.fluid(), view.capacity()));
                }
            }
        }
        for (BlockPos pos : fluidStations()) {
            if (loaded(pos) instanceof FluidStationBlockEntity station && station.role() == FluidRole.IMPORT) {
                for (int tank = 0; tank < station.tankCount(); tank++) {
                    FluidSlot slot = station.tank(tank);
                    sources.add(new FluidSource(new FluidStoreRef(pos, tank), slot.getFluid().copy(), slot.getCapacity()));
                }
            }
        }
        return sources;
    }

    /**
     * Queues a tanker run for each put-away source holding fluid that has
     * no run yet, holds the min load or is full, and has a fluid store with
     * room for it, when a Tanker shuttle reaches both. Then restocks kept
     * Export tanks by {@link #restockFluids}. Last it sets the lamps: red
     * for a source whose fluid has no room or no Tanker and for an Export
     * gantry whose kept fluid no fluid store holds; orange while a run for it
     * is queued or under way, and on a Fluid Inbound also while a tank holds
     * fluid; else green.
     */
    private void tickFluids() {
        if (status() != Status.ONLINE || gantries.isEmpty() && fluidStations.isEmpty()) {
            return;
        }
        int capacity = fleet.tankerCapacity();
        Map<BlockPos, Boolean> reach = new HashMap<>();
        Map<BlockPos, Fluid> noRooms = new HashMap<>();
        Set<BlockPos> noTankers = new HashSet<>();
        Set<BlockPos> holding = new HashSet<>();
        for (FluidSource source : putAwaySources()) {
            BlockPos pos = source.ref().pos();
            if (source.fluid().isEmpty()) {
                continue;
            }
            holding.add(pos);
            if (fleet.hasFluidRun(source.ref())) {
                continue;
            }
            if (capacity <= 0 || !reach.computeIfAbsent(pos, fleet::tankerReaches)) {
                noTankers.add(pos);
                continue;
            }
            FluidKey key = FluidKey.of(source.fluid());
            List<FluidRouting.Target<FluidStoreRef>> targets = routeFluid(key, pos, reach);
            if (targets.isEmpty()) {
                noRooms.putIfAbsent(pos, source.fluid().getFluid());
                continue;
            }
            int held = source.fluid().getAmount();
            int amount = FluidPutAwayPlan.amount(held - fleet.fluidQueuedFrom(source.ref()), held >= source.capacity(), tankerMinLoad(),
                    capacity, targets.getFirst().room());
            if (amount > 0) {
                FluidStoreRef target = targets.getFirst().id();
                fleet.requestFluid(new FluidJob(nextJobId(), pos, source.ref().tank(), target.pos(), target.tank(), key, amount,
                        FluidJob.Purpose.PUT_AWAY));
            }
        }
        noTankers.removeAll(noRooms.keySet());
        List<BlockPos> all = gantries();
        Map<BlockPos, Fluid> missing = restockFluids(all, capacity, reach);
        for (BlockPos pos : all) {
            if (loaded(pos) instanceof FluidGantryBlockEntity gantry) {
                boolean blocked = noRooms.containsKey(pos) || noTankers.contains(pos) || missing.containsKey(pos);
                gantry.setFluidActivity(new FluidActivity(lamp(blocked, fleet.hasFluidRunAt(pos)), noRooms.get(pos), noTankers.contains(pos),
                        missing.get(pos)));
            }
        }
        for (BlockPos pos : fluidStations()) {
            if (loaded(pos) instanceof FluidStationBlockEntity station) {
                boolean blocked = noRooms.containsKey(pos) || noTankers.contains(pos);
                station.setFluidActivity(new FluidActivity(lamp(blocked, holding.contains(pos) || fleet.hasFluidRunAt(pos)), noRooms.get(pos),
                        noTankers.contains(pos), null));
            }
        }
    }

    private static StorageNodeBlocks.Activity lamp(boolean blocked, boolean working) {
        return blocked ? StorageNodeBlocks.Activity.BLOCKED : working ? StorageNodeBlocks.Activity.WORKING : StorageNodeBlocks.Activity.IDLE;
    }

    /** The fluid stations of this warehouse, by their lower half, in a stable order. */
    public List<BlockPos> fluidStations() {
        List<BlockPos> list = new ArrayList<>(fluidStations);
        list.sort(Comparator.comparingLong(BlockPos::asLong));
        return list;
    }

    /** A fluid store a restock run could take fluid from, with that fluid. */
    private record RestockSource(FluidStoreRef ref, FluidKey key) {
    }

    /**
     * Queues a restock run for each locked Export tank below its keep level
     * with no run on its way, from the Storage tank a Tanker reaches with the
     * most free fluid. Returns, per Export gantry, the first kept fluid that
     * no reachable Storage tank holds.
     */
    private Map<BlockPos, Fluid> restockFluids(List<BlockPos> all, int capacity, Map<BlockPos, Boolean> reach) {
        Map<BlockPos, Fluid> missing = new HashMap<>();
        for (BlockPos pos : all) {
            if (!(loaded(pos) instanceof FluidGantryBlockEntity gantry) || gantry.mode() != GantryMode.EXPORT) {
                continue;
            }
            for (TankView view : tankViews(pos)) {
                int keep = gantry.keptLevel(view.ref().tank());
                Fluid lock = view.lock().orElse(null);
                if (keep <= 0 || lock == null || !view.fluid().isEmpty() && view.fluid().getFluid() != lock || fleet.hasFluidRunTo(view.ref())) {
                    continue;
                }
                int lacking = FluidRestockPlan.missing(keep, view.fluid().getAmount(), fleet.fluidIncomingTo(view.ref()));
                if (lacking <= 0 || capacity <= 0 || !reach.computeIfAbsent(pos, fleet::tankerReaches)) {
                    continue;
                }
                FluidKey wanted = view.fluid().isEmpty() ? null : FluidKey.of(view.fluid());
                List<FluidRestockPlan.Source<RestockSource>> sources = restockSources(pos, lock, wanted, reach);
                if (sources.isEmpty()) {
                    missing.putIfAbsent(pos, lock);
                    continue;
                }
                List<FluidRestockPlan.Source<RestockSource>> ranked = FluidRestockPlan.sources(sources);
                if (ranked.isEmpty()) {
                    continue;
                }
                FluidRestockPlan.Source<RestockSource> source = ranked.getFirst();
                int amount = FluidRestockPlan.amount(lacking, tankerMinLoad(), capacity, source.free());
                if (amount > 0) {
                    FluidStoreRef from = source.id().ref();
                    fleet.requestFluid(new FluidJob(nextJobId(), from.pos(), from.tank(), pos, view.ref().tank(), source.id().key(), amount,
                            FluidJob.Purpose.RESTOCK));
                }
            }
        }
        return missing;
    }

    /**
     * The fluid stores a Tanker reaches by rail from the Export gantry at
     * {@code to} that hold the fluid, with the same key as the kept tank's
     * content when it holds any; their free fluid leaves out what runs already take.
     */
    private List<FluidRestockPlan.Source<RestockSource>> restockSources(BlockPos to, Fluid fluid, @Nullable FluidKey wanted,
                                                                        Map<BlockPos, Boolean> reach) {
        Set<NodePos> start = dockRails(Conversions.toNodePos(to));
        Map<BlockPos, Integer> distances = new HashMap<>();
        List<FluidRestockPlan.Source<RestockSource>> sources = new ArrayList<>();
        for (TankView view : fluidStores()) {
            FluidStack held = view.fluid();
            if (held.isEmpty() || held.getFluid() != fluid || wanted != null && !FluidKey.of(held).equals(wanted)) {
                continue;
            }
            BlockPos pos = view.ref().pos();
            if (!reach.computeIfAbsent(pos, fleet::tankerReaches)) {
                continue;
            }
            int distance = distances.computeIfAbsent(pos, store -> railDistance(start, dockRails(Conversions.toNodePos(store))));
            if (distance < 0) {
                continue;
            }
            sources.add(new FluidRestockPlan.Source<>(new RestockSource(view.ref(), FluidKey.of(held)),
                    held.getAmount() - fleet.fluidQueuedFrom(view.ref()), distance));
        }
        return sources;
    }

    /**
     * The fluid stores with room for the fluid in the order a tanker from the
     * store at {@code from} tries them; {@code reach} caches which stores a
     * Tanker shuttle reaches.
     */
    private List<FluidRouting.Target<FluidStoreRef>> routeFluid(FluidKey key, BlockPos from, Map<BlockPos, Boolean> reach) {
        Set<NodePos> start = dockRails(Conversions.toNodePos(from));
        Fluid fluid = key.stack(1).getFluid();
        Map<BlockPos, Integer> distances = new HashMap<>();
        List<FluidRouting.Candidate<FluidStoreRef>> candidates = new ArrayList<>();
        for (TankView view : fluidStores()) {
            BlockPos pos = view.ref().pos();
            if (!reach.computeIfAbsent(pos, fleet::tankerReaches)) {
                continue;
            }
            int distance = distances.computeIfAbsent(pos, store -> railDistance(start, dockRails(Conversions.toNodePos(store))));
            if (distance < 0) {
                continue;
            }
            boolean empty = view.fluid().isEmpty();
            FluidKey content = empty ? null : FluidKey.of(view.fluid());
            FluidKey incoming = fleet.fluidIncomingKey(view.ref()).orElse(null);
            candidates.add(FluidRouting.Candidate.of(view.ref(), key, view.preferred(fluid), view.allows(fluid), content,
                    view.fluid().getAmount(), view.capacity(), fleet.fluidIncomingTo(view.ref()), incoming, distance));
        }
        return FluidRouting.targets(candidates);
    }

    /** Rail steps of the shortest way from any start to any goal, lifts allowed; -1 without a way. */
    private int railDistance(Set<NodePos> start, Set<NodePos> goal) {
        int best = -1;
        for (NodePos node : start) {
            List<NodePos> path = RailPaths.find(node, goal::contains, this::travelNeighbours);
            if (!path.isEmpty() && (best < 0 || path.size() - 1 < best)) {
                best = path.size() - 1;
            }
        }
        return best;
    }

    private int tankerMinLoad() {
        return tankerMinLoadForTests >= 0 ? tankerMinLoadForTests : ShuttlesStatsManager.INSTANCE.tankerMinLoad();
    }

    /** Sets the min load of this warehouse's tanker runs, for tests that must not change the stats of others. */
    /** Items held for a destination until its delivery buffer has room. */
    public int heldCount(BlockPos destination) {
        int count = 0;
        for (HeldLoad load : held.getOrDefault(destination, List.of())) {
            count += load.stack().getCount();
        }
        return count;
    }

    /** Holds a stack for a destination as an instant pick that did not fit would; for tests. */
    public void holdForTests(BlockPos destination, ItemStack stack) {
        PickJob job = new PickJob(nextJobId(), 0, ItemKey.of(stack), stack.getCount(), destination, destination);
        held.computeIfAbsent(destination, pos -> new ArrayList<>()).add(new HeldLoad(job, stack.copy()));
        stateChanged(destination);
    }

    public void setTankerMinLoadForTests(int minLoad) {
        tankerMinLoadForTests = minLoad;
    }

    @Override
    public Optional<FluidRole> fluidStoreRole(FluidStoreRef store) {
        if (store.isRack()) {
            return fluidRacks.contains(store.pos()) && loaded(store.pos()) instanceof FluidRackBlockEntity rack && rack.isPort()
                    ? Optional.of(FluidRole.STORAGE) : Optional.empty();
        }
        FluidStationBlockEntity station = station(store);
        if (station != null) {
            return Optional.of(station.role());
        }
        return gantries.contains(store.pos()) && loaded(store.pos()) instanceof FluidGantryBlockEntity entity
                ? Optional.of(FluidRole.of(entity.mode())) : Optional.empty();
    }

    @Override
    public boolean fluidStoreDocks(FluidStoreRef store) {
        return store.isRack() || fluidStations.contains(store.pos());
    }

    /** The fluid station of this warehouse a store names, when it is loaded and has that tank; else null. */
    private @Nullable FluidStationBlockEntity station(FluidStoreRef store) {
        return fluidStations.contains(store.pos()) && loaded(store.pos()) instanceof FluidStationBlockEntity station
                && store.tank() >= 0 && store.tank() < station.tankCount() ? station : null;
    }

    @Override
    public FluidStack drainFluidStore(FluidStoreRef store, FluidKey key, int max) {
        if (store.isRack()) {
            if (!(loaded(store.pos()) instanceof FluidRackBlockEntity rack)) {
                return FluidStack.EMPTY;
            }
            pendingRacks.add(store.pos());
            return rack.sectionHandler().drain(key.stack(max), false);
        }
        FluidStationBlockEntity station = station(store);
        if (station != null) {
            FluidStack held = station.tank(store.tank()).getFluid();
            return held.isEmpty() || !FluidKey.of(held).equals(key) ? FluidStack.EMPTY : station.tank(store.tank()).drain(max, false);
        }
        if (!(loaded(store.pos()) instanceof FluidGantryBlockEntity entity)) {
            return FluidStack.EMPTY;
        }
        FluidStack held = entity.tank(store.tank()).getFluid();
        if (held.isEmpty() || !FluidKey.of(held).equals(key)) {
            return FluidStack.EMPTY;
        }
        pendingGantries.add(store.pos());
        return entity.tank(store.tank()).drain(max, false);
    }

    @Override
    public FluidStack fillFluidStore(FluidStoreRef store, FluidStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        int filled;
        if (store.isRack()) {
            if (!(loaded(store.pos()) instanceof FluidRackBlockEntity rack)) {
                return stack;
            }
            filled = rack.sectionHandler().fill(stack, false);
            pendingRacks.add(store.pos());
        } else if (station(store) != null) {
            filled = station(store).tank(store.tank()).fill(stack, false);
        } else {
            if (!(loaded(store.pos()) instanceof FluidGantryBlockEntity entity)) {
                return stack;
            }
            filled = entity.tank(store.tank()).fill(stack, false);
            pendingGantries.add(store.pos());
        }
        return filled >= stack.getAmount() ? FluidStack.EMPTY : stack.copyWithAmount(stack.getAmount() - filled);
    }

    @Override
    public List<FluidStoreRef> fluidTargets(FluidKey key, BlockPos from) {
        List<FluidStoreRef> refs = new ArrayList<>();
        for (FluidRouting.Target<FluidStoreRef> target : routeFluid(key, from, new HashMap<>())) {
            refs.add(target.id());
        }
        return refs;
    }

    @Override
    public void gantryPumping(BlockPos gantry, int tank, boolean intoGantry, FluidStack fluid, boolean on) {
        if (loaded(gantry) instanceof FluidGantryBlockEntity entity) {
            entity.setPumping(tank, intoGantry, fluid, on);
        }
    }

    /** Whether a supply run for that head is queued or under way. */
    public boolean hasSupplyJob(BlockPos head) {
        return fleet.hasSupplyJob(head);
    }

    /** Whether a shuttle without a tool can drive to the farm rail beside that head, lifts allowed. */
    public boolean supplyReachable(BlockPos head) {
        return fleet.supplyReachable(head);
    }

    /** Whether some garage of this warehouse holds a shuttle for field jobs with these needs. */
    public boolean hasFieldShuttles(JobNeeds needs) {
        return fleet.hasFieldShuttles(needs);
    }

    /** Whether a shuttle with these needs can drive to that head's field, without a lift when it is too wide for one. */
    public boolean fieldReachable(BlockPos head, JobNeeds needs) {
        return fleet.fieldReachable(head, needs);
    }

    /** Whether a fertilize run for that head is queued or under way. */
    public boolean hasFertilizeJob(BlockPos head) {
        return fleet.hasFertilizeJob(head);
    }

    /** The item of the Spreader dosing that head's field right now, or empty. */
    public Optional<ItemStack> spreadWorker(BlockPos head) {
        return fleet.spreadWorker(head);
    }

    /** Whether a field run for that head is queued or under way. */
    public boolean hasFieldJob(BlockPos head) {
        return fleet.hasFieldJob(head);
    }

    /** The item of the shuttle working that head's field right now, or empty. */
    public Optional<ItemStack> fieldWorker(BlockPos head) {
        return fleet.fieldWorker(head);
    }

    /** Inserts into exactly that source; returns what did not fit. */
    @Override
    public ItemStack insertAt(BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemView handler = handlerOf(pos);
        if (handler == null) {
            return stack;
        }
        ItemStack left = handler.insertItem(stack, false);
        if (left.getCount() != stack.getCount()) {
            markDirty(pos);
        }
        return left;
    }

    /**
     * Hands the controller the chunks its site spans: every member of this
     * network, merged with the known chunks that are not loaded. Looks at no block.
     */
    private void updateSpan() {
        if (!(loaded(controllerPos) instanceof WarehouseControllerBlockEntity controller)) {
            return;
        }
        spanDirty = false;
        Set<Long> current = new LinkedHashSet<>();
        for (NodePos member : members()) {
            current.add(SiteChunks.ofBlock(member.x(), member.z()));
        }
        controller.setSpan(SiteChunks.merge(controller.span(), current,
                key -> level.getChunkSource().hasChunk(SiteChunks.x(key), SiteChunks.z(key))));
    }

    /**
     * Reports the index as the site's last known stock, only while every chunk
     * of the site is loaded, so a site that unloads piece by piece keeps the
     * stock it had as a whole.
     */
    private void snapshotStock() {
        if (index.version() == snapshotVersion || !siteLoaded()) {
            return;
        }
        controller().ifPresent(controller -> {
            snapshotVersion = index.version();
            SiteRegistry.of(level).updateStock(controller, index.entries(), level.getGameTime());
        });
    }

    /** The controller's Wireless Upgrade or powered state changed: the site record learns it next tick. */
    public void wirelessChanged() {
        wirelessDirty = true;
    }

    /**
     * Whether the site's own work runs: shuttles out or jobs waiting, inbound
     * buffers to drain, deliveries held. A woken site stays loaded while this is true.
     */
    public boolean siteBusy() {
        return fleet.busy() > 0 || fleet.queued() > 0 || !pendingStations.isEmpty()
                || held.values().stream().anyMatch(loads -> !loads.isEmpty());
    }

    /** Writes the online state into every loaded stock holding member whose lamp is wrong. */
    private void pushLamps() {
        lampsDirty = false;
        boolean online = status() == Status.ONLINE;
        for (NodePos member : members()) {
            BlockPos pos = Conversions.toBlockPos(member);
            if (connectors.contains(pos) || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(StorageNodeBlocks.ONLINE) && state.getValue(StorageNodeBlocks.ONLINE) != online) {
                level.setBlock(pos, state.setValue(StorageNodeBlocks.ONLINE, online), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private void poll(BlockPos pos) {
        ItemView handler = handlerOf(pos);
        long before = total(index.snapshot(pos));
        if (handler == null) {
            index.remove(pos);
            rackFill.remove(pos);
            return;
        }
        if (loaded(pos) instanceof HeavyRackBlockEntity rack) {
            rackFill.put(pos, new RackFill(SlotFill.inUse(rack.items().states()), rack.items().getSlots()));
        }
        Map<ItemKey, Integer> snapshot = new HashMap<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                snapshot.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
            }
        }
        index.update(pos, snapshot);
        if (total(snapshot) < before) {
            zonesDirty = true;
        }
    }

    private static long total(Map<ItemKey, Integer> snapshot) {
        long sum = 0;
        for (int count : snapshot.values()) {
            sum += count;
        }
        return sum;
    }

    public Status status() {
        if (controllers == 1) {
            return Status.ONLINE;
        }
        return controllers == 0 ? Status.NO_CONTROLLER : Status.MULTIPLE_CONTROLLERS;
    }

    public WarehouseIndex<BlockPos, ItemKey> index() {
        return index;
    }

    /** A rack's locks changed: its OUT locked items are read again. */
    public void rackLocksChanged(BlockPos rack, LockedSlots slots) {
        if (sources.contains(rack)) {
            reservedOut.set(rack, slots.outLocked());
        }
    }

    /** Items of every key that picks have locked in the racks. */
    public Map<ItemKey, Long> reservedOut() {
        return reservedOut.totals();
    }

    /** Moves whenever {@link #reservedOut()} changes. */
    public long reservationVersion() {
        return reservedOut.version();
    }

    public int sourceCount() {
        return sources.size();
    }

    public Component summary() {
        String state = switch (status()) {
            case ONLINE -> "online";
            case NO_CONTROLLER -> "offline, no controller";
            case MULTIPLE_CONTROLLERS -> "offline, more than one controller";
        };
        return Component.literal("Warehouse #" + id() + ": " + state + ", " + size() + " nodes, " + connectors.size() + " connectors, " + sources.size()
                + " sources, " + stations.size() + " stations, " + garages.size() + " garages, " + gantries.size() + " gantries, " + index.typeCount() + " item types, " + index.totalItems() + " items, " + fleet.summary()
                + (status() == Status.ONLINE && noPower() ? ", no power" : ""));
    }

    /**
     * Takes up to {@code amount} (at most one stack) of the key straight out of
     * the racks, for the op command {@code /servus_warehouse take} only.
     */
    public ItemStack takeDirect(ItemKey key, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, key.stack(1).getMaxStackSize());
        for (BlockPos pos : List.copyOf(index.sourcesHolding(key))) {
            if (wanted <= 0) {
                break;
            }
            ItemStack taken = extractFrom(pos, key, wanted);
            result = combine(result, taken);
            wanted -= taken.getCount();
        }
        return result;
    }
}
