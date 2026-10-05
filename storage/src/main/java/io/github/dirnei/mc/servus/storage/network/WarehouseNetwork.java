package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.lib.graph.ConnectionRule;
import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.index.WarehouseIndex;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * One connected set of storage blocks plus the inventories touching them.
 * Keeps an index of everything they hold, refreshed in batches, and offers
 * extract and insert over all of it.
 */
public final class WarehouseNetwork extends ServusNetwork<StorageNodeBlockEntity, WarehouseEvent> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "warehouse");

    public static final NetworkType<StorageNodeBlockEntity, WarehouseEvent, WarehouseNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), WarehouseNetwork::new);

    public enum Status { ONLINE, NO_CONTROLLER, MULTIPLE_CONTROLLERS }

    private record Source(BlockPos pos, boolean node, BlockCapabilityCache<IItemHandler, Direction> cache) {
    }

    private final Map<BlockPos, Source> sources = new LinkedHashMap<>();
    private final WarehouseIndex<BlockPos, ItemKey> index = new WarehouseIndex<>();
    private final ArrayDeque<BlockPos> dirtyQueue = new ArrayDeque<>();
    private final Set<BlockPos> dirtySet = new HashSet<>();
    private final List<BlockPos> pollOrder = new ArrayList<>();
    private boolean pollOrderStale = true;
    private int cursor;
    private ServerLevel level;
    private int controllers;

    public WarehouseNetwork(int id, NetworkSet<StorageNodeBlockEntity, WarehouseEvent, ?> owner) {
        super(id, owner);
    }

    // Membership

    @Override
    protected void onNodeAdded(NodePos pos, StorageNodeBlockEntity node) {
        if (!(node.getLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        BlockPos blockPos = Conversions.toBlockPos(pos);
        addSource(blockPos, true);
        addCandidatesAround(blockPos);
        if (node instanceof WarehouseControllerBlockEntity) {
            controllers++;
        }
    }

    @Override
    protected void onNodeRemoved(NodePos pos, StorageNodeBlockEntity node) {
        BlockPos blockPos = Conversions.toBlockPos(pos);
        dropSource(blockPos);
        if (node instanceof WarehouseControllerBlockEntity) {
            controllers--;
        }
        rebuildCandidates();
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
        that.sources.forEach((pos, source) -> {
            if (!sources.containsKey(pos) || source.node()) {
                sources.put(pos, source);
                index.update(pos, that.index.snapshot(pos));
            }
        });
        pollOrderStale = true;
    }

    @Override
    protected void onDisposed() {
        sources.clear();
        dirtyQueue.clear();
        dirtySet.clear();
        pollOrder.clear();
        controllers = 0;
    }

    private void addSource(BlockPos pos, boolean node) {
        Source existing = sources.get(pos);
        if (existing != null && existing.node() == node) {
            markDirty(pos);
            return;
        }
        sources.put(pos, new Source(pos, node, BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, pos, null)));
        pollOrderStale = true;
        markDirty(pos);
    }

    private void dropSource(BlockPos pos) {
        if (sources.remove(pos) != null) {
            index.remove(pos);
            dirtySet.remove(pos);
            pollOrderStale = true;
        }
    }

    private void addCandidatesAround(BlockPos nodePos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = nodePos.relative(direction);
            if (!sources.containsKey(neighbour)) {
                addSource(neighbour, false);
            } else {
                markDirty(neighbour);
            }
        }
    }

    /** Candidates are the neighbours of the remaining members that are not members. */
    private void rebuildCandidates() {
        Set<BlockPos> wanted = new LinkedHashSet<>();
        Set<BlockPos> memberPositions = new HashSet<>();
        for (NodePos member : members()) {
            memberPositions.add(Conversions.toBlockPos(member));
        }
        for (BlockPos member : memberPositions) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = member.relative(direction);
                if (!memberPositions.contains(neighbour)) {
                    wanted.add(neighbour);
                }
            }
        }
        for (BlockPos pos : new ArrayList<>(sources.keySet())) {
            Source source = sources.get(pos);
            if (!source.node() && !wanted.contains(pos)) {
                dropSource(pos);
            }
        }
        for (BlockPos pos : wanted) {
            if (!sources.containsKey(pos)) {
                addSource(pos, false);
            }
        }
    }

    // Events and polling

    @Override
    protected void handle(WarehouseEvent event) {
        if (event instanceof WarehouseEvent.Dirty dirty) {
            if (sources.containsKey(dirty.pos())) {
                markDirty(dirty.pos());
            }
        } else if (event instanceof WarehouseEvent.NeighborsChanged changed) {
            if (sources.containsKey(changed.pos())) {
                addCandidatesAround(changed.pos());
            }
        }
    }

    private void markDirty(BlockPos pos) {
        if (dirtySet.add(pos)) {
            dirtyQueue.add(pos);
        }
    }

    @Override
    protected void onTick() {
        if (level == null || sources.isEmpty()) {
            return;
        }
        int budget = StorageStatsManager.INSTANCE.sourcesPerTick();
        while (budget > 0 && !dirtyQueue.isEmpty()) {
            BlockPos pos = dirtyQueue.poll();
            dirtySet.remove(pos);
            if (sources.containsKey(pos)) {
                poll(pos);
                budget--;
            }
        }
        if (pollOrderStale) {
            pollOrder.clear();
            pollOrder.addAll(sources.keySet());
            pollOrderStale = false;
        }
        for (int i = 0; i < budget && !pollOrder.isEmpty(); i++) {
            cursor = (cursor + 1) % pollOrder.size();
            BlockPos pos = pollOrder.get(cursor);
            if (sources.containsKey(pos)) {
                poll(pos);
            }
        }
    }

    private void poll(BlockPos pos) {
        Source source = sources.get(pos);
        IItemHandler handler = source.cache().getCapability();
        if (handler == null) {
            if (source.node()) {
                index.remove(pos);
            } else {
                dropSource(pos);
            }
            return;
        }
        Map<ItemKey, Integer> snapshot = new HashMap<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                snapshot.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
            }
        }
        index.update(pos, snapshot);
    }

    // Queries

    public Status status() {
        if (controllers == 1) {
            return Status.ONLINE;
        }
        return controllers == 0 ? Status.NO_CONTROLLER : Status.MULTIPLE_CONTROLLERS;
    }

    public WarehouseIndex<BlockPos, ItemKey> index() {
        return index;
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
        return Component.literal("Warehouse #" + id() + ": " + state + ", " + size() + " nodes, " + sources.size()
                + " sources, " + index.typeCount() + " item types, " + index.totalItems() + " items");
    }

    // Operations

    /** Pulls up to {@code amount} (at most one stack) of the key out of the warehouse. */
    public ItemStack extract(ItemKey key, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, key.stack(1).getMaxStackSize());
        for (BlockPos pos : index.sourcesHolding(key)) {
            Source source = sources.get(pos);
            IItemHandler handler = source == null ? null : source.cache().getCapability();
            if (handler == null) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots() && wanted > 0; slot++) {
                if (!key.matches(handler.getStackInSlot(slot))) {
                    continue;
                }
                ItemStack taken = handler.extractItem(slot, wanted, false);
                if (taken.isEmpty()) {
                    continue;
                }
                if (result.isEmpty()) {
                    result = taken;
                } else {
                    result.grow(taken.getCount());
                }
                wanted -= taken.getCount();
            }
            markDirty(pos);
            if (wanted <= 0) {
                break;
            }
        }
        return result;
    }

    /**
     * Puts a stack into the warehouse: sources already holding it, then empty
     * crates, then other storage nodes, then foreign inventories in use.
     *
     * @return what did not fit
     */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemKey key = ItemKey.of(stack);
        ItemStack remaining = stack.copy();
        remaining = insertInto(index.sourcesHolding(key), remaining);
        if (remaining.isEmpty()) {
            return remaining;
        }
        List<BlockPos> emptyCrates = new ArrayList<>();
        List<BlockPos> otherNodes = new ArrayList<>();
        List<BlockPos> foreignInUse = new ArrayList<>();
        for (Source source : sources.values()) {
            if (source.node()) {
                if (level.getBlockEntity(source.pos()) instanceof CrateBlockEntity crate && crate.isEmpty()) {
                    emptyCrates.add(source.pos());
                } else {
                    otherNodes.add(source.pos());
                }
            } else if (!index.isEmptySource(source.pos())) {
                foreignInUse.add(source.pos());
            }
        }
        remaining = insertInto(emptyCrates, remaining);
        remaining = insertInto(otherNodes, remaining);
        remaining = insertInto(foreignInUse, remaining);
        return remaining;
    }

    private ItemStack insertInto(List<BlockPos> positions, ItemStack remaining) {
        for (BlockPos pos : positions) {
            if (remaining.isEmpty()) {
                break;
            }
            Source source = sources.get(pos);
            IItemHandler handler = source == null ? null : source.cache().getCapability();
            if (handler == null) {
                continue;
            }
            int before = remaining.getCount();
            remaining = ItemHandlerHelper.insertItem(handler, remaining, false);
            if (remaining.getCount() != before) {
                markDirty(pos);
            }
        }
        return remaining;
    }
}
