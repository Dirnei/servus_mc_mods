package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.energy.EnergyDistributor;
import io.github.dirnei.mc.servus.foundation.energy.EnergySink;
import io.github.dirnei.mc.servus.foundation.energy.EnergySource;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctStatsManager;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.transfer.EnergyView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * The Power Cable network of a set of connected utility ducts. Every tick it
 * moves energy, per channel, from storages on input sides to storages on
 * output sides of the same channel, water filling, within the configured
 * rate. Carries no buffer.
 */
public final class EnergyChannelNetwork extends LineNetwork<EnergyChannelNetwork.Endpoint> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "energy_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, EnergyChannelNetwork> TYPE =
            new NetworkType<>(ID, RULE, EnergyChannelNetwork::new);

    record Endpoint(UtilityDuctBlockEntity duct, Direction side, DuctMode mode, int channel,
                    BlockCapabilityCache<EnergyHandler, Direction> cache) {

        boolean active() {
            return duct.isActive(Line.ENERGY, side);
        }
    }

    public EnergyChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
        super(id, owner, Line.ENERGY, new HashMap<>());
    }

    @Override
    protected Endpoint endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, DuctMode mode, int channel) {
        return new Endpoint(duct, side, mode, channel, BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, target, side.getOpposite()));
    }

    @Override
    protected void onTick() {
        if (level == null || endpoints.isEmpty()) {
            return;
        }
        Map<Integer, List<EnergySource>> sources = new HashMap<>();
        Map<Integer, List<EnergySink>> sinks = new HashMap<>();
        for (List<Endpoint> list : endpoints.values()) {
            for (Endpoint endpoint : list) {
                if (!endpoint.active()) {
                    continue;
                }
                EnergyView storage = EnergyView.of(endpoint.cache().getCapability());
                if (storage == null) {
                    continue;
                }
                int channel = endpoint.channel();
                boolean mayPull = endpoint.mode().in();
                boolean mayPush = endpoint.mode().out();
                if (mayPull && storage.canExtract()) {
                    sources.computeIfAbsent(channel, c -> new ArrayList<>()).add(storage::extractEnergy);
                } else if (mayPush && storage.canReceive()) {
                    sinks.computeIfAbsent(channel, c -> new ArrayList<>()).add(storage::receiveEnergy);
                }
            }
        }
        int rate = DuctStatsManager.INSTANCE.energyPerTick();
        sources.forEach((channel, channelSources) -> {
            List<EnergySink> channelSinks = sinks.get(channel);
            if (channelSinks != null) {
                EnergyDistributor.distribute(channelSources, channelSinks, rate);
            }
        });
    }
}
