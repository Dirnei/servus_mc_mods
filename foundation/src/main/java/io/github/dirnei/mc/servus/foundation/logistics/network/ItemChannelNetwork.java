package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.data.packet.Grant;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketEndpoint;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctItemHandler;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctStatsManager;
import io.github.dirnei.mc.servus.foundation.logistics.duct.KeepAmounts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.PipeOffer;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.routing.RoundRobinPlan;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
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
 * Inventories fill partial stacks first. A pull toward a block that answers
 * offers (a packet endpoint) first offers what its source can give, up to a
 * stack, and sends that item there only into the grant it gets.
 */
public final class ItemChannelNetwork extends LineNetwork<ItemChannelNetwork.Endpoint> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "item_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, ItemChannelNetwork> TYPE =
            new NetworkType<>(ID, RULE, ItemChannelNetwork::new);

    record Endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, DuctMode mode, int priority,
                            int channel, BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> cache,
                            BlockCapabilityCache<PacketEndpoint, Direction> packets) {

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

    /** The pulling side of a pull being routed, null for pushed stacks. */
    private record Pull(Endpoint source) {
    }

    /** A grant this pipe holds at one receiving side for one item, with the pulling sides that fed it. */
    private record HeldGrant(Endpoint target, ItemStack like, Grant grant, Set<Endpoint> sources) {

        boolean isFor(Endpoint endpoint, ItemStack stack) {
            return target.equals(endpoint) && ItemStack.isSameItemSameComponents(like, stack);
        }
    }

    private final RoundRobinPlan<Endpoint> plan = new RoundRobinPlan<>();
    private final List<HeldGrant> grants = new ArrayList<>();
    private boolean planDirty = true;
    private int budget;
    private int pullTimer;

    public ItemChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
        super(id, owner, Line.ITEM, new LinkedHashMap<>());
    }

    @Override
    protected Endpoint endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, DuctMode mode, int channel) {
        return new Endpoint(duct, side, target, mode, duct.priority(side), channel,
                BlockCapabilityCache.create(Capabilities.Item.BLOCK, level, target, side.getOpposite()),
                BlockCapabilityCache.create(ServusFoundationCapabilities.PACKET_ENDPOINT, level, target, side.getOpposite()));
    }

    @Override
    protected void endpointsChanged() {
        planDirty = true;
        for (HeldGrant held : new ArrayList<>(grants)) {
            held.sources().removeIf(source -> !present(source));
            if (!present(held.target()) || held.sources().isEmpty()) {
                end(held);
            }
        }
    }

    @Override
    protected void onDisposed() {
        super.onDisposed();
        plan.clear();
        for (HeldGrant held : new ArrayList<>(grants)) {
            end(held);
        }
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
        endSpentGrants();
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
        Pull pull = new Pull(source);
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
            ItemStack left = route(available, source.target(), source.channel(), true, pull);
            int accepted = available.getCount() - left.getCount();
            if (accepted <= 0) {
                continue;
            }
            ItemStack pulled = handler.extractItem(slot, accepted, false);
            ItemStack rejected = route(pulled, source.target(), source.channel(), false, pull);
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
        return route(stack, origin, channel, simulate, null);
    }

    private ItemStack route(ItemStack stack, BlockPos origin, int channel, boolean simulate, Pull pull) {
        RoundRobinPlan<Endpoint> routing = plan();
        if (stack.isEmpty() || budget <= 0 || !routing.hasTargets(channel)) {
            return stack;
        }
        ItemStack remaining = stack.copyWithCount(Math.min(stack.getCount(), budget));
        ItemStack notOffered = stack.copyWithCount(stack.getCount() - remaining.getCount());
        ItemStack probe = remaining.copy();
        List<RoundRobinPlan.Offer<Endpoint>> offers = routing.plan(channel, remaining.getCount(),
                endpoint -> capacity(endpoint, origin, probe, pull));
        for (RoundRobinPlan.Offer<Endpoint> offer : offers) {
            if (remaining.isEmpty()) {
                break;
            }
            int amount = Math.min(offer.amount(), remaining.getCount());
            if (simulate) {
                remaining.shrink(amount);
                continue;
            }
            ItemStack rejected = send(offer.target(), remaining.copyWithCount(amount), pull);
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

    /** Puts the stack into the target: into the pipe's grant there during a pull, else partial stacks first. */
    private ItemStack send(Endpoint target, ItemStack stack, Pull pull) {
        HeldGrant held = pull == null ? null : find(target, stack);
        if (held != null) {
            return held.grant().deliver(stack, level.getGameTime());
        }
        ItemView handler = ItemView.of(target.cache().getCapability());
        return handler == null ? stack : handler.insertItemStacked(stack, false);
    }

    /**
     * How much of the stack the endpoint's inventory would take right now, no more than its fill up to amount
     * allows; zero for the origin, ducts, inactive sides and filtered items. During a pull toward a packet
     * endpoint it is what the pipe's grant there still takes.
     */
    private int capacity(Endpoint endpoint, BlockPos origin, ItemStack stack, Pull pull) {
        if (endpoint.target().equals(origin) || !endpoint.active() || !endpoint.allows(stack)) {
            return 0;
        }
        ItemView handler = ItemView.of(endpoint.cache().getCapability());
        if (handler == null || handler.handler() instanceof DuctItemHandler) {
            return 0;
        }
        int upTo = endpoint.amountFor(stack);
        if (pull != null) {
            HeldGrant held = grantAt(endpoint, handler, stack, upTo, pull);
            if (held != null) {
                return held.grant().left();
            }
        }
        ItemStack left = handler.insertItemStacked(stack, true);
        int room = stack.getCount() - left.getCount();
        return upTo > 0 && room > 0 ? KeepAmounts.receiveAllowance(heldCount(handler, stack), upTo, room) : room;
    }

    /**
     * The open grant at the endpoint for the stack's item, offering what the pulling side gives when there is
     * none or the last one is filled; null when the endpoint answers no offers or refuses.
     */
    private HeldGrant grantAt(Endpoint endpoint, ItemView handler, ItemStack stack, int upTo, Pull pull) {
        HeldGrant held = find(endpoint, stack);
        if (held != null && held.grant().left() > 0) {
            held.sources().add(pull.source());
            return held;
        }
        if (held != null) {
            end(held);
        }
        PacketEndpoint packets = endpoint.packets().getCapability();
        if (packets == null) {
            return null;
        }
        int allowance = upTo > 0 ? KeepAmounts.receiveAllowance(heldCount(handler, stack), upTo, Integer.MAX_VALUE) : PipeOffer.NO_ALLOWANCE;
        int amount = PipeOffer.amount(movable(pull.source(), stack), stack.getMaxStackSize(), allowance);
        if (amount <= 0) {
            return null;
        }
        Grant grant = packets.offer(stack.copyWithCount(1), amount);
        if (!grant.granted()) {
            return null;
        }
        Set<Endpoint> sources = new HashSet<>();
        sources.add(pull.source());
        HeldGrant opened = new HeldGrant(endpoint, stack.copyWithCount(1), grant, sources);
        grants.add(opened);
        return opened;
    }

    private HeldGrant find(Endpoint endpoint, ItemStack stack) {
        for (HeldGrant held : grants) {
            if (held.isFor(endpoint, stack)) {
                return held;
            }
        }
        return null;
    }

    /** How many of the item the pulling side may still give, after its keep amount. */
    private static long movable(Endpoint source, ItemStack stack) {
        ItemView handler = ItemView.of(source.cache().getCapability());
        if (handler == null || handler.handler() instanceof DuctItemHandler || !source.allows(stack)) {
            return 0;
        }
        long held = heldCount(handler, stack);
        int keep = source.amountFor(stack);
        return keep > 0 ? Math.max(0, held - keep) : held;
    }

    /** Ends grants that are filled or closed, whose side left, or whose pulling sides no longer give their item. */
    private void endSpentGrants() {
        for (HeldGrant held : new ArrayList<>(grants)) {
            if (held.grant().left() <= 0 || !present(held.target()) || !fed(held)) {
                end(held);
            }
        }
    }

    private boolean fed(HeldGrant held) {
        for (Endpoint source : held.sources()) {
            if (present(source) && source.mode().pull() && source.active() && movable(source, held.like()) > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean present(Endpoint endpoint) {
        List<Endpoint> list = endpoints.get(Conversions.toNodePos(endpoint.duct().getBlockPos()));
        return list != null && list.contains(endpoint);
    }

    private void end(HeldGrant held) {
        grants.remove(held);
        held.grant().close();
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
}
