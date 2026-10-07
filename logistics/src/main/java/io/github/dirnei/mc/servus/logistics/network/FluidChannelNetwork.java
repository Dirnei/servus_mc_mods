package io.github.dirnei.mc.servus.logistics.network;

import io.github.dirnei.mc.servus.lib.graph.ConnectionRule;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.core.NetworkSet;
import io.github.dirnei.mc.servus.lib.network.core.ServusNetwork;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import net.minecraft.resources.ResourceLocation;

/** The Fluid Pipe network of a set of connected utility ducts. */
public final class FluidChannelNetwork extends ServusNetwork<UtilityDuctBlockEntity, DuctEvent> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "fluid_channel");

    public static final NetworkType<UtilityDuctBlockEntity, DuctEvent, FluidChannelNetwork> TYPE =
            new NetworkType<>(ID, ConnectionRule.always(), FluidChannelNetwork::new);

    public FluidChannelNetwork(int id, NetworkSet<UtilityDuctBlockEntity, DuctEvent, ?> owner) {
        super(id, owner);
    }

    @Override
    protected void handle(DuctEvent event) {
    }
}
