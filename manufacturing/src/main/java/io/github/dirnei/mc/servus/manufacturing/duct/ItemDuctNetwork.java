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
import java.util.List;
import java.util.Map;
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
 * robin, within a per tick budget.
 */
public final class ItemDuctNetwork extends ServusNetwork<ItemDuctBlockEntity, Object> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "item_duct");
    private static final MachineStats DEFAULTS = MachineStats.duct(16);

    public static final NetworkType<ItemDuctBlockEntity, Object, ItemDuctNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), ItemDuctNetwork::new);

    /** Endpoint caches keyed by the neighbour position they point at. */
    private record Endpoint(BlockPos target, BlockCapabilityCache<IItemHandler, Direction> cache) {
    }

    private final Map<NodePos, List<Endpoint>> endpoints = new LinkedHashMap<>();
    private final List<Endpoint> flat = new ArrayList<>();
    private int cursor;
    private int budget;
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
        BlockPos blockPos = Conversions.toBlockPos(pos);
        List<Endpoint> list = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            BlockPos target = blockPos.relative(direction);
            list.add(new Endpoint(target, BlockCapabilityCache.create(
                    Capabilities.ItemHandler.BLOCK, server, target, direction.getOpposite())));
        }
        endpoints.put(pos, list);
        flatDirty = true;
    }

    @Override
    protected void onNodeRemoved(NodePos pos, ItemDuctBlockEntity duct) {
        endpoints.remove(pos);
        flatDirty = true;
    }

    @Override
    protected void onAbsorb(ServusNetwork<ItemDuctBlockEntity, Object> other) {
        if (other instanceof ItemDuctNetwork ducts) {
            endpoints.putAll(ducts.endpoints);
            flatDirty = true;
        }
    }

    @Override
    protected void onDisposed() {
        endpoints.clear();
        flat.clear();
    }

    @Override
    protected void onTick() {
        budget = MachineStatsManager.INSTANCE.get(ID, DEFAULTS).itemsPerTick();
    }

    private List<Endpoint> flat() {
        if (flatDirty) {
            flat.clear();
            endpoints.values().forEach(flat::addAll);
            flatDirty = false;
        }
        return flat;
    }

    /**
     * Routes a stack to the next accepting inventory, skipping the inventory
     * at {@code origin} (the one that handed the stack over) and other ducts.
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
            if (endpoint.target().equals(origin)) {
                continue;
            }
            IItemHandler handler = endpoint.cache().getCapability();
            if (handler == null || handler instanceof ItemDuctHandler) {
                continue;
            }
            ItemStack left = handler.insertItem(0, remaining, true);
            int accepted = remaining.getCount() - left.getCount();
            if (accepted <= 0) {
                left = insertAnySlot(handler, remaining, true);
                accepted = remaining.getCount() - left.getCount();
            }
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
