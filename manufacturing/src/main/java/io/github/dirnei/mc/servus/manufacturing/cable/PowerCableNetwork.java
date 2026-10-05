package io.github.dirnei.mc.servus.manufacturing.cable;

import io.github.dirnei.mc.servus.lib.energy.EnergyDistributor;
import io.github.dirnei.mc.servus.lib.energy.EnergySink;
import io.github.dirnei.mc.servus.lib.energy.EnergySource;
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
 * One connected set of cables. Keeps capability caches for the neighbours of
 * every cable and moves energy from sources to sinks each tick.
 */
public final class PowerCableNetwork extends ServusNetwork<PowerCableBlockEntity, Object> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "power_cable");
    private static final MachineStats DEFAULTS = MachineStats.cable(256);

    public static final NetworkType<PowerCableBlockEntity, Object, PowerCableNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), PowerCableNetwork::new);

    private final Map<NodePos, List<BlockCapabilityCache<IEnergyStorage, Direction>>> endpoints = new HashMap<>();
    private ServerLevel level;

    public PowerCableNetwork(int id, NetworkSet<PowerCableBlockEntity, Object, ?> owner) {
        super(id, owner);
    }

    @Override
    protected void handle(Object event) {
    }

    @Override
    protected void onNodeAdded(NodePos pos, PowerCableBlockEntity cable) {
        if (!(cable.getLevel() instanceof ServerLevel server)) {
            return;
        }
        level = server;
        BlockPos blockPos = Conversions.toBlockPos(pos);
        List<BlockCapabilityCache<IEnergyStorage, Direction>> caches = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            caches.add(BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, server,
                    blockPos.relative(direction), direction.getOpposite()));
        }
        endpoints.put(pos, caches);
    }

    @Override
    protected void onNodeRemoved(NodePos pos, PowerCableBlockEntity cable) {
        endpoints.remove(pos);
    }

    @Override
    protected void onAbsorb(ServusNetwork<PowerCableBlockEntity, Object> other) {
        if (other instanceof PowerCableNetwork cables) {
            endpoints.putAll(cables.endpoints);
            if (level == null) {
                level = cables.level;
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
        List<EnergySource> sources = new ArrayList<>();
        List<EnergySink> sinks = new ArrayList<>();
        for (List<BlockCapabilityCache<IEnergyStorage, Direction>> caches : endpoints.values()) {
            for (BlockCapabilityCache<IEnergyStorage, Direction> cache : caches) {
                IEnergyStorage storage = cache.getCapability();
                if (storage == null) {
                    continue;
                }
                if (storage.canExtract()) {
                    sources.add(storage::extractEnergy);
                } else if (storage.canReceive()) {
                    sinks.add(storage::receiveEnergy);
                }
            }
        }
        if (sources.isEmpty() || sinks.isEmpty()) {
            return;
        }
        int rate = MachineStatsManager.INSTANCE.get(ID, DEFAULTS).energyPerTick();
        EnergyDistributor.distribute(sources, sinks, rate);
    }
}
