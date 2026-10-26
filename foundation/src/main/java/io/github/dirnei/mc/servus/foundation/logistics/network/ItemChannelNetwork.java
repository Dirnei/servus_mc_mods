package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctItemHandler;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctStatsManager;
import io.github.dirnei.mc.servus.foundation.logistics.duct.KeepAmounts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.routing.RoundRobinPlan;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The Item Pipe network of a set of connected utility ducts. Holds no items.
 * Routes inserted stacks to inventories on output sides of the same channel,
 * highest priority first and round robin within a priority, honouring side
 * filters and fill up to amounts, and pulls from inventories on sides with Pull
 * every few ticks onto the pulling side's channel, leaving keep amounts behind.
 */
public final class ItemChannelNetwork extends LineNetwork<ItemChannelNetwork.Endpoint> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "item_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, ItemChannelNetwork> TYPE =
            new NetworkType<>(ID, RULE, ItemChannelNetwork::new);

    record Endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, DuctMode mode, int priority,
                            int channel, BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> cache) {

        boolean allows(ItemStack stack) {
            return duct.filterAllows(side, stack);
        }

        boolean active() {
            return duct.isActive(Line.ITEM, side);
        }

        int amountFor(ItemStack stack) {
            return duct.itemAmountFor(side, stack);
        }
    }

    private final RoundRobinPlan<Endpoint> plan = new RoundRobinPlan<>();
    private boolean planDirty = true;
    private int budget;
    private int pullTimer;

    public ItemChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
        super(id, owner, Line.ITEM, new LinkedHashMap<>());
    }

    @Override
    protected Endpoint endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, DuctMode mode, int channel) {
        return new Endpoint(duct, side, target, mode, duct.priority(side), channel,
                BlockCapabilityCache.create(Capabilities.Item.BLOCK, level, target, side.getOpposite()));
    }

    @Override
    protected void endpointsChanged() {
        planDirty = true;
    }

    @Override
    protected void onDisposed() {
        super.onDisposed();
        plan.clear();
    }

    /** The routing plan over all output endpoints, grouped by channel and priority. */
    private RoundRobinPlan<Endpoint> plan() {
        if (planDirty) {
            List<RoundRobinPlan.Target<Endpoint>> targets = new ArrayList<>();
            for (List<Endpoint> list : endpoints.values()) {
                for (Endpoint endpoint : list) {
                    if (endpoint.mode().out()) {
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
                if (source.mode().pull() && source.active()) {
                    pull(source, perPull);
                }
            }
        }
    }

    /**
     * One pull from a side with Pull: the first slot whose items pass the filter and the channel accepts,
     * never taking an item below its keep amount.
     */
    private void pull(Endpoint source, int perPull) {
        ItemView handler = ItemView.of(source.cache().getCapability());
        if (handler == null || handler.handler() instanceof DuctItemHandler) {
            return;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack available = handler.extractItem(slot, perPull, true);
            if (available.isEmpty() || !source.allows(available)) {
                continue;
            }
            int keep = source.amountFor(available);
            if (keep > 0) {
                int allowed = KeepAmounts.giveAllowance(heldCount(handler, available), keep, available.getCount());
                if (allowed <= 0) {
                    continue;
                }
                available = available.copyWithCount(allowed);
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
            ItemView handler = ItemView.of(offer.target().cache().getCapability());
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

    /**
     * How much of the stack the endpoint's inventory would take right now, no more than its fill up to amount
     * allows; zero for the origin, ducts, inactive sides and filtered items.
     */
    private static int capacity(Endpoint endpoint, BlockPos origin, ItemStack stack) {
        if (endpoint.target().equals(origin) || !endpoint.active() || !endpoint.allows(stack)) {
            return 0;
        }
        ItemView handler = ItemView.of(endpoint.cache().getCapability());
        if (handler == null || handler.handler() instanceof DuctItemHandler) {
            return 0;
        }
        ItemStack left = insertAnySlot(handler, stack, true);
        int room = stack.getCount() - left.getCount();
        int upTo = endpoint.amountFor(stack);
        return upTo > 0 && room > 0 ? KeepAmounts.receiveAllowance(heldCount(handler, stack), upTo, room) : room;
    }

    /** How many of the stack's item the inventory holds over all its slots. */
    private static long heldCount(ItemView handler, ItemStack stack) {
        long held = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (ItemStack.isSameItem(inSlot, stack)) {
                held += inSlot.getCount();
            }
        }
        return held;
    }

    private static ItemStack insertAnySlot(ItemView handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }
}
