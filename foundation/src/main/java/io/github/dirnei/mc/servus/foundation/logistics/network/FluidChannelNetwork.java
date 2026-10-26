package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.fluid.FluidRouting;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctFluidHandler;
import io.github.dirnei.mc.servus.foundation.logistics.duct.FluidFilter;
import io.github.dirnei.mc.servus.foundation.logistics.duct.KeepAmounts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * The Fluid Pipe network of a set of connected utility ducts. Every tick it
 * moves fluid, per channel, from source blocks to sink blocks of the same
 * channel with {@link FluidRouting}. The pipe has no rate of its own and no
 * buffer: as much moves as the sources give and the sinks take. Sides' fluid
 * filters decide which fluids leave and enter a block, and sinks are served
 * by fluid priority, highest first.
 *
 * <p>Roles per tick: a side with only In makes its block a source, one with only Out
 * a sink. The block of a side with In and Out is a sink when it accepts one of
 * the fluids offered on its channel, and otherwise a source when it holds
 * fluid. Once such a block has received fluid it stays a sink
 * until that duct's configuration changes, so two tanks on In and Out sides
 * never trade fluid back and forth.
 */
public final class FluidChannelNetwork extends LineNetwork<FluidChannelNetwork.Endpoint> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "fluid_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, FluidChannelNetwork> TYPE =
            new NetworkType<>(ID, RULE, FluidChannelNetwork::new);

    /** A fluid with its components, compared without the amount. */
    public static final class FluidKey {

        private final FluidStack stack;

        private FluidKey(FluidStack stack) {
            this.stack = stack.copyWithAmount(1);
        }

        public static FluidKey of(FluidStack stack) {
            return new FluidKey(stack);
        }

        public FluidStack stack(int amount) {
            return stack.copyWithAmount(amount);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof FluidKey that && FluidStack.isSameFluidSameComponents(stack, that.stack);
        }

        @Override
        public int hashCode() {
            return FluidStack.hashFluidAndComponents(stack);
        }
    }

    /** One duct side with its neighbour's fluid handler. */
    static final class Endpoint {
        final UtilityDuctBlockEntity duct;
        final Direction side;
        final BlockPos target;
        final DuctMode mode;
        final int channel;
        final BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> cache;
        /** A side with In and Out whose block has received fluid acts only as a sink from then on. */
        boolean receiving;

        Endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, DuctMode mode, int channel,
                 BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> cache) {
            this.duct = duct;
            this.side = side;
            this.target = target;
            this.mode = mode;
            this.channel = channel;
            this.cache = cache;
        }

        boolean active() {
            return duct.isActive(Line.FLUID, side);
        }

        boolean allows(FluidKey key) {
            return duct.fluidFilterAllows(side, key.stack(1).getFluid());
        }

        int priority() {
            return duct.fluidPriority(side);
        }

        int amountFor(Fluid fluid) {
            return duct.fluidAmountFor(side, fluid);
        }

        boolean inAndOut() {
            return mode.in() && mode.out();
        }

        /** The neighbour's handler, null when there is none or it is another duct. */
        FluidView handler() {
            FluidView handler = FluidView.of(cache.getCapability());
            return handler == null || handler.handler() instanceof DuctFluidHandler ? null : handler;
        }
    }

    /** An endpoint as a routing source. */
    private record Source(FluidView handler, FluidRouting.Offer<FluidKey> offer) implements FluidRouting.Source<FluidKey> {

        @Override
        public int drain(FluidKey key, int amount) {
            return handler.drain(key.stack(amount), false).getAmount();
        }
    }

    /** An endpoint as a routing sink, taking only what its fluid filter passes and up to its fill up to amount. */
    private record Sink(FluidView handler, Endpoint endpoint) implements FluidRouting.Sink<FluidKey> {

        @Override
        public int priority() {
            return endpoint.priority();
        }

        @Override
        public int fill(FluidKey key, int amount, boolean simulate) {
            if (!endpoint.allows(key)) {
                return 0;
            }
            Fluid fluid = key.stack(1).getFluid();
            int upTo = endpoint.amountFor(fluid);
            if (upTo > 0) {
                amount = KeepAmounts.receiveAllowance(heldAmount(handler, fluid), upTo, amount);
                if (amount <= 0) {
                    return 0;
                }
            }
            int filled = handler.fill(key.stack(amount), simulate);
            if (!simulate && filled > 0 && endpoint.inAndOut()) {
                endpoint.receiving = true;
            }
            return filled;
        }
    }

    public FluidChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
        super(id, owner, Line.FLUID, new HashMap<>());
    }

    @Override
    protected Endpoint endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, DuctMode mode, int channel) {
        return new Endpoint(duct, side, target, mode, channel, BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, target, side.getOpposite()));
    }

    @Override
    protected void onTick() {
        if (level == null || endpoints.isEmpty()) {
            return;
        }
        Map<Integer, List<Endpoint>> perChannel = new HashMap<>();
        for (List<Endpoint> list : endpoints.values()) {
            for (Endpoint endpoint : list) {
                perChannel.computeIfAbsent(endpoint.channel, c -> new ArrayList<>()).add(endpoint);
            }
        }
        for (List<Endpoint> channel : perChannel.values()) {
            if (channel.size() >= 2) {
                tickChannel(channel);
            }
        }
    }

    /** Assigns the roles of one channel's endpoints and routes. */
    private static void tickChannel(List<Endpoint> channel) {
        List<Source> sources = new ArrayList<>();
        List<Sink> sinks = new ArrayList<>();
        List<Endpoint> undecided = new ArrayList<>();
        Map<Endpoint, Source> offers = new HashMap<>();
        Set<FluidKey> offered = new LinkedHashSet<>();
        for (Endpoint endpoint : channel) {
            FluidView handler = endpoint.active() ? endpoint.handler() : null;
            if (handler == null) {
                continue;
            }
            boolean mayGive = endpoint.mode.in() && !endpoint.receiving;
            if (mayGive) {
                FluidStack available = keepBack(endpoint, handler, offerOf(handler, endpoint.duct.fluidFilter(endpoint.side)));
                if (!available.isEmpty()) {
                    FluidKey key = FluidKey.of(available);
                    offers.put(endpoint, new Source(handler, new FluidRouting.Offer<>(key, available.getAmount())));
                    offered.add(key);
                }
            }
            if (!endpoint.mode.out()) {
                Source source = offers.get(endpoint);
                if (source != null) {
                    sources.add(source);
                }
            } else if (!endpoint.mode.in()) {
                sinks.add(new Sink(handler, endpoint));
            } else {
                undecided.add(endpoint);
            }
        }
        if (offered.isEmpty()) {
            return;
        }
        for (Endpoint endpoint : undecided) {
            FluidView handler = endpoint.handler();
            if (accepts(endpoint, handler, offered)) {
                sinks.add(new Sink(handler, endpoint));
            } else if (offers.containsKey(endpoint)) {
                sources.add(offers.get(endpoint));
            }
        }
        if (!sources.isEmpty() && !sinks.isEmpty()) {
            FluidRouting.route(sources, sinks);
        }
    }

    /**
     * What a source block would give through a side with that fluid filter, without draining: with an empty
     * filter whatever the block gives first, otherwise the first listed fluid the block holds.
     */
    public static FluidStack offerOf(FluidView handler, FluidFilter filter) {
        if (filter.isEmpty()) {
            return handler.drain(Integer.MAX_VALUE, true);
        }
        for (String id : filter.entries()) {
            Identifier parsed = Identifier.tryParse(id);
            Fluid fluid = parsed == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(parsed);
            if (fluid == Fluids.EMPTY) {
                continue;
            }
            FluidStack available = handler.drain(new FluidStack(fluid, Integer.MAX_VALUE), true);
            if (!available.isEmpty()) {
                return available;
            }
        }
        return FluidStack.EMPTY;
    }

    /** The offer less what the side's keep amount for that fluid leaves in the block. */
    private static FluidStack keepBack(Endpoint endpoint, FluidView handler, FluidStack available) {
        if (available.isEmpty()) {
            return available;
        }
        int keep = endpoint.amountFor(available.getFluid());
        if (keep <= 0) {
            return available;
        }
        int allowed = KeepAmounts.giveAllowance(heldAmount(handler, available.getFluid()), keep, available.getAmount());
        return allowed <= 0 ? FluidStack.EMPTY : available.copyWithAmount(allowed);
    }

    /** How much of the fluid the handler holds over all its tanks. */
    private static long heldAmount(FluidView handler, Fluid fluid) {
        ResourceHandler<FluidResource> tanks = handler.handler();
        long held = 0;
        for (int index = 0; index < tanks.size(); index++) {
            if (tanks.getResource(index).getFluid() == fluid) {
                held += tanks.getAmountAsLong(index);
            }
        }
        return held;
    }

    private static boolean accepts(Endpoint endpoint, FluidView handler, Set<FluidKey> keys) {
        Sink sink = new Sink(handler, endpoint);
        for (FluidKey key : keys) {
            if (sink.fill(key, Integer.MAX_VALUE, true) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fluid filled into a duct side by its neighbour: passed straight on to
     * the blocks on output sides of the channel whose filter passes it and that
     * accept it, skipping the block at {@code origin}, by fluid priority and
     * shared fairly within one. Keeps nothing.
     *
     * @return the amount accepted (or that would be accepted)
     */
    public int fill(FluidStack resource, BlockPos origin, int channel, boolean simulate) {
        if (resource.isEmpty() || level == null) {
            return 0;
        }
        FluidKey key = FluidKey.of(resource);
        List<Sink> sinks = new ArrayList<>();
        long room = 0;
        for (List<Endpoint> list : endpoints.values()) {
            for (Endpoint endpoint : list) {
                if (endpoint.channel != channel || !endpoint.mode.out() || endpoint.target.equals(origin) || !endpoint.active()
                        || !endpoint.allows(key)) {
                    continue;
                }
                FluidView handler = endpoint.handler();
                if (handler == null) {
                    continue;
                }
                Sink sink = new Sink(handler, endpoint);
                int accepted = sink.fill(key, resource.getAmount(), true);
                if (accepted > 0) {
                    sinks.add(sink);
                    room += accepted;
                }
            }
        }
        if (sinks.isEmpty()) {
            return 0;
        }
        if (simulate) {
            return (int) Math.min(resource.getAmount(), room);
        }
        int[] left = {resource.getAmount()};
        FluidRouting.Source<FluidKey> pushed = new FluidRouting.Source<>() {
            @Override
            public FluidRouting.Offer<FluidKey> offer() {
                return new FluidRouting.Offer<>(key, left[0]);
            }

            @Override
            public int drain(FluidKey drained, int amount) {
                int taken = Math.min(amount, left[0]);
                left[0] -= taken;
                return taken;
            }
        };
        return FluidRouting.route(List.of(pushed), sinks);
    }
}
