package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.fluid.FluidRouting;
import io.github.dirnei.mc.servus.foundation.graph.ConnectionRule;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctFluidHandler;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * The Fluid Pipe network of a set of connected utility ducts. Every tick it
 * moves fluid, per channel, from source blocks to sink blocks of the same
 * channel with {@link FluidRouting}. The pipe has no rate of its own and no
 * buffer: as much moves as the sources give and the sinks take.
 *
 * <p>Roles per tick: a pull in side's block is a source, a push out side's
 * block a sink. An in and out side's block is a sink when it accepts one of
 * the fluids offered on its channel, and otherwise a source when it holds
 * fluid. Once an in and out side's block has received fluid it stays a sink
 * until that duct's configuration changes, so two tanks on in and out sides
 * never trade fluid back and forth.
 */
public final class FluidChannelNetwork extends ServusNetwork<UtilityDuctBlockEntity, DuctEvent> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "fluid_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, FluidChannelNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), FluidChannelNetwork::new);

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
    private static final class Endpoint {
        final BlockPos target;
        final SideMode mode;
        final int channel;
        final BlockCapabilityCache<IFluidHandler, Direction> cache;
        /** An in and out side whose block has received fluid acts only as a sink from then on. */
        boolean receiving;

        Endpoint(BlockPos target, SideMode mode, int channel, BlockCapabilityCache<IFluidHandler, Direction> cache) {
            this.target = target;
            this.mode = mode;
            this.channel = DuctChannels.clamp(channel);
            this.cache = cache;
        }

        boolean inAndOut() {
            return mode.allowsInsert() && mode.allowsExtract();
        }

        /** The neighbour's handler, null when there is none or it is another duct. */
        IFluidHandler handler() {
            IFluidHandler handler = cache.getCapability();
            return handler instanceof DuctFluidHandler ? null : handler;
        }
    }

    /** An endpoint as a routing source. */
    private record Source(IFluidHandler handler, FluidRouting.Offer<FluidKey> offer) implements FluidRouting.Source<FluidKey> {

        @Override
        public int drain(FluidKey key, int amount) {
            return handler.drain(key.stack(amount), FluidAction.EXECUTE).getAmount();
        }
    }

    /** An endpoint as a routing sink. */
    private record Sink(IFluidHandler handler, Endpoint endpoint) implements FluidRouting.Sink<FluidKey> {

        @Override
        public int fill(FluidKey key, int amount, boolean simulate) {
            int filled = handler.fill(key.stack(amount), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
            if (!simulate && filled > 0 && endpoint.inAndOut()) {
                endpoint.receiving = true;
            }
            return filled;
        }
    }

    private final Map<NodePos, List<Endpoint>> endpoints = new HashMap<>();
    private ServerLevel level;

    public FluidChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
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
            SideMode mode = duct.mode(Line.FLUID, direction);
            if (mode == SideMode.DISABLED) {
                continue;
            }
            BlockPos target = blockPos.relative(direction);
            list.add(new Endpoint(target, mode, duct.channel(Line.FLUID, direction), BlockCapabilityCache.create(
                    Capabilities.FluidHandler.BLOCK, level, target, direction.getOpposite())));
        }
        endpoints.put(pos, list);
    }

    @Override
    protected void onNodeRemoved(NodePos pos, UtilityDuctBlockEntity duct) {
        endpoints.remove(pos);
    }

    @Override
    protected void onAbsorb(ServusNetwork<UtilityDuctBlockEntity, DuctEvent> other) {
        if (other instanceof FluidChannelNetwork that) {
            endpoints.putAll(that.endpoints);
            if (level == null) {
                level = that.level;
            }
        }
    }

    @Override
    protected void onDisposed() {
        endpoints.clear();
    }

    @Override
    protected void onTick() {
        if (level == null || endpoints.isEmpty()) {
            return;
        }
        List<List<Endpoint>> perChannel = new ArrayList<>(DuctChannels.COUNT);
        for (int channel = 0; channel < DuctChannels.COUNT; channel++) {
            perChannel.add(new ArrayList<>());
        }
        for (List<Endpoint> list : endpoints.values()) {
            for (Endpoint endpoint : list) {
                perChannel.get(endpoint.channel).add(endpoint);
            }
        }
        for (List<Endpoint> channel : perChannel) {
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
            IFluidHandler handler = endpoint.handler();
            if (handler == null) {
                continue;
            }
            boolean mayGive = endpoint.mode.allowsInsert() && !endpoint.receiving;
            if (mayGive) {
                FluidStack available = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
                if (!available.isEmpty()) {
                    FluidKey key = FluidKey.of(available);
                    offers.put(endpoint, new Source(handler, new FluidRouting.Offer<>(key, available.getAmount())));
                    offered.add(key);
                }
            }
            if (!endpoint.mode.allowsExtract()) {
                Source source = offers.get(endpoint);
                if (source != null) {
                    sources.add(source);
                }
            } else if (!endpoint.mode.allowsInsert()) {
                sinks.add(new Sink(handler, endpoint));
            } else {
                undecided.add(endpoint);
            }
        }
        if (offered.isEmpty()) {
            return;
        }
        for (Endpoint endpoint : undecided) {
            IFluidHandler handler = endpoint.handler();
            if (accepts(handler, offered)) {
                sinks.add(new Sink(handler, endpoint));
            } else if (offers.containsKey(endpoint)) {
                sources.add(offers.get(endpoint));
            }
        }
        if (!sources.isEmpty() && !sinks.isEmpty()) {
            FluidRouting.route(sources, sinks);
        }
    }

    private static boolean accepts(IFluidHandler handler, Set<FluidKey> keys) {
        for (FluidKey key : keys) {
            if (handler.fill(key.stack(Integer.MAX_VALUE), FluidAction.SIMULATE) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fluid filled into a duct side by its neighbour: passed straight on to
     * the blocks on output sides of the channel that accept it, skipping the
     * block at {@code origin}, shared fairly. Keeps nothing.
     *
     * @return the amount accepted (or that would be accepted)
     */
    public int fill(FluidStack resource, BlockPos origin, int channel, FluidAction action) {
        if (resource.isEmpty() || level == null) {
            return 0;
        }
        FluidKey key = FluidKey.of(resource);
        int clamped = DuctChannels.clamp(channel);
        List<Sink> sinks = new ArrayList<>();
        long room = 0;
        for (List<Endpoint> list : endpoints.values()) {
            for (Endpoint endpoint : list) {
                if (endpoint.channel != clamped || !endpoint.mode.allowsExtract() || endpoint.target.equals(origin)) {
                    continue;
                }
                IFluidHandler handler = endpoint.handler();
                if (handler == null) {
                    continue;
                }
                int accepted = handler.fill(key.stack(resource.getAmount()), FluidAction.SIMULATE);
                if (accepted > 0) {
                    sinks.add(new Sink(handler, endpoint));
                    room += accepted;
                }
            }
        }
        if (sinks.isEmpty()) {
            return 0;
        }
        if (action.simulate()) {
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
