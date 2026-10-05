package io.github.dirnei.mc.servus.logistics.network;

import io.github.dirnei.mc.servus.lib.graph.ConnectionRule;
import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.Channel;
import io.github.dirnei.mc.servus.logistics.duct.DuctItemHandler;
import io.github.dirnei.mc.servus.logistics.duct.DuctStatsManager;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
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
 * The item channel of a set of connected utility ducts. Holds no items.
 * Routes inserted stacks to inventories on output sides, highest priority
 * first and round robin within a priority, honouring side filters, and pulls
 * from inventories on input sides every few ticks.
 */
public final class ItemChannelNetwork extends ServusNetwork<UtilityDuctBlockEntity, DuctEvent> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "item_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, ItemChannelNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), ItemChannelNetwork::new);

    private record Endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, SideMode mode, int priority,
                            BlockCapabilityCache<IItemHandler, Direction> cache) {

        boolean allows(ItemStack stack) {
            return duct.filterAllows(side, stack);
        }
    }

    private final Map<NodePos, List<Endpoint>> endpoints = new LinkedHashMap<>();
    /** Output endpoints grouped by priority, highest first. */
    private final List<List<Endpoint>> groups = new ArrayList<>();
    private final Map<Integer, Integer> cursors = new HashMap<>();
    private boolean groupsDirty = true;
    private ServerLevel level;
    private int budget;
    private int pullTimer;

    public ItemChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
        super(id, owner);
    }

    @Override
    protected void handle(DuctEvent event) {
        if (event instanceof DuctEvent.ConfigChanged changed && level != null
                && level.getBlockEntity(Conversions.toBlockPos(changed.pos())) instanceof UtilityDuctBlockEntity duct) {
            rebuild(changed.pos(), duct);
        }
    }

    @Override
    protected void onNodeAdded(NodePos pos, UtilityDuctBlockEntity duct) {
        if (!(duct.getLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        rebuild(pos, duct);
    }

    private void rebuild(NodePos pos, UtilityDuctBlockEntity duct) {
        BlockPos blockPos = Conversions.toBlockPos(pos);
        List<Endpoint> list = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            SideMode mode = duct.mode(Channel.ITEM, direction);
            if (mode == SideMode.DISABLED) {
                continue;
            }
            BlockPos target = blockPos.relative(direction);
            list.add(new Endpoint(duct, direction, target, mode, duct.priority(direction), BlockCapabilityCache.create(
                    Capabilities.ItemHandler.BLOCK, level, target, direction.getOpposite())));
        }
        endpoints.put(pos, list);
        groupsDirty = true;
    }

    @Override
    protected void onNodeRemoved(NodePos pos, UtilityDuctBlockEntity duct) {
        endpoints.remove(pos);
        groupsDirty = true;
    }

    @Override
    protected void onAbsorb(ServusNetwork<UtilityDuctBlockEntity, DuctEvent> other) {
        if (other instanceof ItemChannelNetwork that) {
            endpoints.putAll(that.endpoints);
            if (level == null) {
                level = that.level;
            }
            groupsDirty = true;
        }
    }

    @Override
    protected void onDisposed() {
        endpoints.clear();
        groups.clear();
        cursors.clear();
    }

    private List<List<Endpoint>> groups() {
        if (groupsDirty) {
            Map<Integer, List<Endpoint>> byPriority = new HashMap<>();
            for (List<Endpoint> list : endpoints.values()) {
                for (Endpoint endpoint : list) {
                    if (endpoint.mode().allowsExtract()) {
                        byPriority.computeIfAbsent(endpoint.priority(), p -> new ArrayList<>()).add(endpoint);
                    }
                }
            }
            groups.clear();
            byPriority.keySet().stream().sorted((a, b) -> Integer.compare(b, a)).forEach(p -> groups.add(byPriority.get(p)));
            groupsDirty = false;
        }
        return groups;
    }

    @Override
    protected void onTick() {
        DuctStatsManager stats = DuctStatsManager.INSTANCE;
        budget = stats.itemsPerTick();
        if (level == null || endpoints.isEmpty()) {
            return;
        }
        if (++pullTimer < stats.pullInterval()) {
            return;
        }
        pullTimer = 0;
        int perPull = stats.itemsPerPull();
        for (List<Endpoint> list : new ArrayList<>(endpoints.values())) {
            for (Endpoint source : list) {
                if (source.mode() == SideMode.INPUT) {
                    pull(source, perPull);
                }
            }
        }
    }

    /** One pull from an input side: the first slot whose items pass the filter and the network accepts. */
    private void pull(Endpoint source, int perPull) {
        IItemHandler handler = source.cache().getCapability();
        if (handler == null || handler instanceof DuctItemHandler) {
            return;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack available = handler.extractItem(slot, perPull, true);
            if (available.isEmpty() || !source.allows(available)) {
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
                handler.insertItem(slot, rejected, false);
            }
            return;
        }
    }

    /**
     * Routes a stack to accepting inventories on output sides, highest priority
     * group first, skipping the inventory at {@code origin}, other ducts and
     * sides whose filter rejects the stack.
     *
     * @return what could not be routed
     */
    public ItemStack route(ItemStack stack, BlockPos origin, boolean simulate) {
        List<List<Endpoint>> all = groups();
        if (all.isEmpty() || budget <= 0) {
            return stack;
        }
        ItemStack remaining = stack.copyWithCount(Math.min(stack.getCount(), budget));
        ItemStack notOffered = stack.copyWithCount(stack.getCount() - remaining.getCount());
        for (List<Endpoint> group : all) {
            if (remaining.isEmpty()) {
                break;
            }
            int priority = group.get(0).priority();
            int cursor = cursors.getOrDefault(priority, 0);
            int size = group.size();
            for (int i = 0; i < size && !remaining.isEmpty(); i++) {
                Endpoint endpoint = group.get((cursor + i) % size);
                if (endpoint.target().equals(origin) || !endpoint.allows(remaining)) {
                    continue;
                }
                IItemHandler handler = endpoint.cache().getCapability();
                if (handler == null || handler instanceof DuctItemHandler) {
                    continue;
                }
                ItemStack left = insertAnySlot(handler, remaining, true);
                int accepted = remaining.getCount() - left.getCount();
                if (accepted <= 0) {
                    continue;
                }
                if (!simulate) {
                    ItemStack rejected = insertAnySlot(handler, remaining.copyWithCount(accepted), false);
                    accepted -= rejected.getCount();
                    budget -= accepted;
                    cursors.put(priority, (cursor + i + 1) % size);
                }
                remaining.shrink(accepted);
            }
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
