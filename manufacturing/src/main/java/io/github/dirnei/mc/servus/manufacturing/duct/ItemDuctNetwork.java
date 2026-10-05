package io.github.dirnei.mc.servus.manufacturing.duct;

import io.github.dirnei.mc.servus.lib.graph.ConnectionRule;
import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStatsManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * One connected set of item ducts. Holds no items. Routes stacks inserted
 * into any duct to the next inventory on the network that accepts them, round
 * robin, within a per tick budget. Filter ducts restrict what their neighbours
 * receive; splitter ducts pull from their neighbours every few ticks.
 */
public final class ItemDuctNetwork extends ServusNetwork<ItemDuctBlockEntity, Object> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "item_duct");
    public static final ResourceLocation SPLITTER_ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "splitter_duct");
    private static final MachineStats DEFAULTS = MachineStats.duct(16);
    private static final MachineStats SPLITTER_DEFAULTS = MachineStats.duct(8);
    private static final int PULL_INTERVAL = 8;

    public static final NetworkType<ItemDuctBlockEntity, Object, ItemDuctNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), ItemDuctNetwork::new);

    /** A neighbour of one duct: which duct owns it, where it is, and its cached handler. */
    private record Endpoint(NodePos duct, BlockPos target, BlockCapabilityCache<IItemHandler, Direction> cache) {
    }

    private final Map<NodePos, List<Endpoint>> endpoints = new LinkedHashMap<>();
    private final Set<NodePos> splitters = new LinkedHashSet<>();
    private final List<Endpoint> flat = new ArrayList<>();
    private ServerLevel level;
    private int cursor;
    private int budget;
    private int pullTimer;
    private boolean flatDirty = true;

    public ItemDuctNetwork(int id, NetworkSet<ItemDuctBlockEntity, Object, ?> owner) {
        super(id, owner);
    }

    @Override
    protected void handle(Object event) {
    }

    @Override
    protected void onNodeAdded(NodePos pos, ItemDuctBlockEntity duct) {
        if (!(duct.getLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        BlockPos blockPos = Conversions.toBlockPos(pos);
        List<Endpoint> list = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            BlockPos target = blockPos.relative(direction);
            list.add(new Endpoint(pos, target, BlockCapabilityCache.create(
                    Capabilities.ItemHandler.BLOCK, server, target, direction.getOpposite())));
        }
        endpoints.put(pos, list);
        if (duct instanceof SplitterDuctBlockEntity) {
            splitters.add(pos);
        }
        flatDirty = true;
    }

    @Override
    protected void onNodeRemoved(NodePos pos, ItemDuctBlockEntity duct) {
        endpoints.remove(pos);
        splitters.remove(pos);
        flatDirty = true;
    }

    @Override
    protected void onAbsorb(ServusNetwork<ItemDuctBlockEntity, Object> other) {
        if (other instanceof ItemDuctNetwork ducts) {
            endpoints.putAll(ducts.endpoints);
            splitters.addAll(ducts.splitters);
            if (level == null) {
                level = ducts.level;
            }
            flatDirty = true;
        }
    }

    @Override
    protected void onDisposed() {
        endpoints.clear();
        splitters.clear();
        flat.clear();
    }

    @Override
    protected void onTick() {
        budget = MachineStatsManager.INSTANCE.get(ID, DEFAULTS).itemsPerTick();
        if (splitters.isEmpty() || level == null) {
            return;
        }
        if (++pullTimer < PULL_INTERVAL) {
            return;
        }
        pullTimer = 0;
        int perPull = MachineStatsManager.INSTANCE.get(SPLITTER_ID, SPLITTER_DEFAULTS).itemsPerTick();
        for (NodePos splitter : new ArrayList<>(splitters)) {
            pull(splitter, perPull);
        }
    }

    /** One pull for a splitter: the first neighbour slot whose items the network accepts. */
    private void pull(NodePos splitter, int perPull) {
        List<Endpoint> own = endpoints.get(splitter);
        if (own == null) {
            return;
        }
        for (Endpoint source : own) {
            IItemHandler handler = source.cache().getCapability();
            if (handler == null || handler instanceof ItemDuctHandler) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack available = handler.extractItem(slot, perPull, true);
                if (available.isEmpty()) {
                    continue;
                }
                ItemStack left = route(available, source.target(), true);
                int accepted = available.getCount() - left.getCount();
                if (accepted <= 0) {
                    continue;
                }
                ItemStack pulled = handler.extractItem(slot, accepted, false);
                ItemStack rejected = route(pulled, source.target(), false);
                if (!rejected.isEmpty()) {
                    // Should not happen right after a successful simulation; give it back.
                    handler.insertItem(slot, rejected, false);
                }
                return;
            }
        }
    }

    private List<Endpoint> flat() {
        if (flatDirty) {
            flat.clear();
            endpoints.values().forEach(flat::addAll);
            flatDirty = false;
        }
        return flat;
    }

    private boolean filterAllows(Endpoint endpoint, ItemStack stack) {
        if (level == null) {
            return true;
        }
        return !(level.getBlockEntity(Conversions.toBlockPos(endpoint.duct())) instanceof FilterDuctBlockEntity filter)
                || filter.allows(stack);
    }

    /**
     * Routes a stack to the next accepting inventory, skipping the inventory
     * at {@code origin} (the one that handed the stack over), other ducts and
     * neighbours of filter ducts that reject the stack.
     *
     * @return what could not be routed
     */
    public ItemStack route(ItemStack stack, BlockPos origin, boolean simulate) {
        List<Endpoint> all = flat();
        if (all.isEmpty() || budget <= 0) {
            return stack;
        }
        ItemStack remaining = stack.copyWithCount(Math.min(stack.getCount(), budget));
        ItemStack notOffered = stack.copyWithCount(stack.getCount() - remaining.getCount());
        int size = all.size();
        for (int i = 0; i < size && !remaining.isEmpty(); i++) {
            Endpoint endpoint = all.get((cursor + i) % size);
            if (endpoint.target().equals(origin) || !filterAllows(endpoint, remaining)) {
                continue;
            }
            IItemHandler handler = endpoint.cache().getCapability();
            if (handler == null || handler instanceof ItemDuctHandler) {
                continue;
            }
            ItemStack left = insertAnySlot(handler, remaining, true);
            int accepted = remaining.getCount() - left.getCount();
            if (accepted <= 0) {
                continue;
            }
            if (!simulate) {
                ItemStack offered = remaining.copyWithCount(accepted);
                ItemStack rejected = insertAnySlot(handler, offered, false);
                accepted -= rejected.getCount();
                budget -= accepted;
                cursor = (cursor + i + 1) % size;
            }
            remaining.shrink(accepted);
        }
        if (notOffered.isEmpty()) {
            return remaining;
        }
        notOffered.grow(remaining.getCount());
        return notOffered;
    }

    private static ItemStack insertAnySlot(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }
}
