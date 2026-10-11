package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctItemHandler;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctStatsManager;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.routing.RoundRobinPlan;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Item Pipe network of a set of connected utility ducts. Holds no items.
 * Routes inserted stacks to inventories on output sides of the same channel,
 * highest priority first and round robin within a priority, honouring side
 * filters, and pulls from inventories on pull in sides every few ticks onto
 * the pulling side's channel.
 */
public final class ItemChannelNetwork extends ServusNetwork<UtilityDuctBlockEntity, DuctEvent> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "item_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, ItemChannelNetwork> TYPE =
            new NetworkType<>(ID, (from, fromNode, side, to, toNode) -> UtilityDuctBlockEntity.sideOpen(fromNode, side), ItemChannelNetwork::new);

    private record Endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, SideMode mode, int priority,
                            int channel, BlockCapabilityCache<IItemHandler, Direction> cache) {

        boolean allows(ItemStack stack) {
            return duct.filterAllows(side, stack);
        }
    }

    private final Map<NodePos, List<Endpoint>> endpoints = new LinkedHashMap<>();
    private final RoundRobinPlan<Endpoint> plan = new RoundRobinPlan<>();
    private boolean planDirty = true;
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
            SideMode mode = duct.mode(Line.ITEM, direction);
            if (mode == SideMode.DISABLED) {
                continue;
            }
            BlockPos target = blockPos.relative(direction);
            list.add(new Endpoint(duct, direction, target, mode, duct.priority(direction), duct.channel(Line.ITEM, direction),
                    BlockCapabilityCache.create(Capabilities.Item.BLOCK, level, target, direction.getOpposite())));
        }
        endpoints.put(pos, list);
        planDirty = true;
    }

    @Override
    protected void onNodeRemoved(NodePos pos, UtilityDuctBlockEntity duct) {
        endpoints.remove(pos);
        planDirty = true;
    }

    @Override
    protected void onAbsorb(ServusNetwork<UtilityDuctBlockEntity, DuctEvent> other) {
        if (other instanceof ItemChannelNetwork that) {
            endpoints.putAll(that.endpoints);
            if (level == null) {
                level = that.level;
            }
            planDirty = true;
        }
    }

    @Override
    protected void onDisposed() {
        endpoints.clear();
        plan.clear();
    }

    /** The routing plan over all output endpoints, grouped by channel and priority. */
    private RoundRobinPlan<Endpoint> plan() {
        if (planDirty) {
            List<RoundRobinPlan.Target<Endpoint>> targets = new ArrayList<>();
            for (List<Endpoint> list : endpoints.values()) {
                for (Endpoint endpoint : list) {
                    if (endpoint.mode().allowsExtract()) {
                        targets.add(new RoundRobinPlan.Target<>(endpoint.channel(), endpoint.priority(), endpoint));
                    }
                }
            }
            plan.rebuild(targets);
            planDirty = false;
        }
        return plan;
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

    /** One pull from a pull in side: the first slot whose items pass the filter and the channel accepts. */
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
            ItemStack left = route(available, source.target(), source.channel(), true);
            int accepted = available.getCount() - left.getCount();
            if (accepted <= 0) {
                continue;
            }
            ItemStack pulled = handler.extractItem(slot, accepted, false);
            ItemStack rejected = route(pulled, source.target(), source.channel(), false);
            if (!rejected.isEmpty()) {
                handler.insertItem(slot, rejected, false);
            }
            return;
        }
    }

    /**
     * Routes a stack to accepting inventories on output sides of the channel,
     * highest priority group first, skipping the inventory at {@code origin},
     * other ducts and sides whose filter rejects the stack.
     *
     * @return what could not be routed
     */
    public ItemStack route(ItemStack stack, BlockPos origin, int channel, boolean simulate) {
        RoundRobinPlan<Endpoint> routing = plan();
        if (stack.isEmpty() || budget <= 0 || !routing.hasTargets(channel)) {
            return stack;
        }
        ItemStack remaining = stack.copyWithCount(Math.min(stack.getCount(), budget));
        ItemStack notOffered = stack.copyWithCount(stack.getCount() - remaining.getCount());
        ItemStack probe = remaining.copy();
        List<RoundRobinPlan.Offer<Endpoint>> offers = routing.plan(channel, remaining.getCount(),
                endpoint -> capacity(endpoint, origin, probe));
        for (RoundRobinPlan.Offer<Endpoint> offer : offers) {
            if (remaining.isEmpty()) {
                break;
            }
            int amount = Math.min(offer.amount(), remaining.getCount());
            if (simulate) {
                remaining.shrink(amount);
                continue;
            }
            IItemHandler handler = offer.target().cache().getCapability();
            if (handler == null) {
                continue;
            }
            ItemStack rejected = insertAnySlot(handler, remaining.copyWithCount(amount), false);
            int accepted = amount - rejected.getCount();
            if (accepted > 0) {
                budget -= accepted;
                routing.delivered(channel, offer.target());
                remaining.shrink(accepted);
            }
        }
        if (notOffered.isEmpty()) {
            return remaining;
        }
        notOffered.grow(remaining.getCount());
        return notOffered;
    }

    /** How much of the stack the endpoint's inventory would take right now, zero for the origin, ducts and filtered items. */
    private static int capacity(Endpoint endpoint, BlockPos origin, ItemStack stack) {
        if (endpoint.target().equals(origin) || !endpoint.allows(stack)) {
            return 0;
        }
        IItemHandler handler = endpoint.cache().getCapability();
        if (handler == null || handler instanceof DuctItemHandler) {
            return 0;
        }
        ItemStack left = insertAnySlot(handler, stack, true);
        return stack.getCount() - left.getCount();
    }

    private static ItemStack insertAnySlot(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }
}
