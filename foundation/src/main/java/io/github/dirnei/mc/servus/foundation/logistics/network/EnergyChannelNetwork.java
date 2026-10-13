package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.energy.EnergyDistributor;
import io.github.dirnei.mc.servus.foundation.energy.EnergySink;
import io.github.dirnei.mc.servus.foundation.energy.EnergySource;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctStatsManager;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.core.NetworkSet;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.EnergyView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    record Endpoint(SideMode mode, int channel, BlockCapabilityCache<EnergyHandler, Direction> cache) {
    }

    public EnergyChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
        super(id, owner, Line.ENERGY, new HashMap<>());
    }

    @Override
    protected Endpoint endpoint(UtilityDuctBlockEntity duct, Direction side, BlockPos target, SideMode mode, int channel) {
        return new Endpoint(mode, channel, BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, target, side.getOpposite()));
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
                EnergyView storage = EnergyView.of(endpoint.cache().getCapability());
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
