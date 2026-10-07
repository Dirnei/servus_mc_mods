package io.github.dirnei.mc.servus.logistics.network;

import io.github.dirnei.mc.servus.lib.energy.EnergyDistributor;
import io.github.dirnei.mc.servus.lib.energy.EnergySink;
import io.github.dirnei.mc.servus.lib.energy.EnergySource;
import io.github.dirnei.mc.servus.lib.graph.ConnectionRule;
import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.logistics.duct.DuctStatsManager;
import io.github.dirnei.mc.servus.logistics.duct.Line;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The Power Cable network of a set of connected utility ducts. Every tick it
 * moves energy, per channel, from storages on input sides to storages on
 * output sides of the same channel, water filling, within the configured
 * rate. Carries no buffer.
 */
public final class EnergyChannelNetwork extends ServusNetwork<UtilityDuctBlockEntity, DuctEvent> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "energy_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, EnergyChannelNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), EnergyChannelNetwork::new);

    private record Endpoint(SideMode mode, int channel, BlockCapabilityCache<IEnergyStorage, Direction> cache) {
    }

    private final Map<NodePos, List<Endpoint>> endpoints = new HashMap<>();
    private ServerLevel level;

    public EnergyChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
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
            SideMode mode = duct.mode(Line.ENERGY, direction);
            if (mode == SideMode.DISABLED) {
                continue;
            }
            list.add(new Endpoint(mode, duct.channel(Line.ENERGY, direction), BlockCapabilityCache.create(
                    Capabilities.EnergyStorage.BLOCK, level, blockPos.relative(direction), direction.getOpposite())));
        }
        endpoints.put(pos, list);
    }

    @Override
    protected void onNodeRemoved(NodePos pos, UtilityDuctBlockEntity duct) {
        endpoints.remove(pos);
    }

    @Override
    protected void onAbsorb(ServusNetwork<UtilityDuctBlockEntity, DuctEvent> other) {
        if (other instanceof EnergyChannelNetwork that) {
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
    @SuppressWarnings("unchecked")
    protected void onTick() {
        if (level == null || endpoints.isEmpty()) {
            return;
        }
        List<EnergySource>[] sources = new List[DuctChannels.COUNT];
        List<EnergySink>[] sinks = new List[DuctChannels.COUNT];
        for (List<Endpoint> list : endpoints.values()) {
            for (Endpoint endpoint : list) {
                IEnergyStorage storage = endpoint.cache().getCapability();
                if (storage == null) {
                    continue;
                }
                int channel = DuctChannels.clamp(endpoint.channel());
                boolean mayPull = endpoint.mode().allowsInsert();
                boolean mayPush = endpoint.mode().allowsExtract();
                if (mayPull && storage.canExtract()) {
                    if (sources[channel] == null) {
                        sources[channel] = new ArrayList<>();
                    }
                    sources[channel].add(storage::extractEnergy);
                } else if (mayPush && storage.canReceive()) {
                    if (sinks[channel] == null) {
                        sinks[channel] = new ArrayList<>();
                    }
                    sinks[channel].add(storage::receiveEnergy);
                }
            }
        }
        int rate = DuctStatsManager.INSTANCE.energyPerTick();
        for (int channel = 0; channel < DuctChannels.COUNT; channel++) {
            if (sources[channel] != null && sinks[channel] != null) {
                EnergyDistributor.distribute(sources[channel], sinks[channel], rate);
            }
        }
    }
}
