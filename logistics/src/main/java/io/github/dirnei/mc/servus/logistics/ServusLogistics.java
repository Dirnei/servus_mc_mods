package io.github.dirnei.mc.servus.logistics;

import io.github.dirnei.mc.servus.foundation.network.NetworkTypes;
import io.github.dirnei.mc.servus.logistics.network.EnergyChannelNetwork;
import io.github.dirnei.mc.servus.logistics.network.FluidChannelNetwork;
import io.github.dirnei.mc.servus.logistics.network.ItemChannelNetwork;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsComponents;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsMenus;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsRecipes;
import io.github.dirnei.mc.servus.logistics.tank.TankNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus: Logistics.
 */
@Mod(ServusLogisticsIds.MOD_ID)
public final class ServusLogistics {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusLogistics.class);

    public ServusLogistics(IEventBus modEventBus) {
        ServusLogisticsBlocks.BLOCKS.register(modEventBus);
        ServusLogisticsItems.ITEMS.register(modEventBus);
        ServusLogisticsBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusLogisticsMenus.MENUS.register(modEventBus);
        ServusLogisticsComponents.COMPONENTS.register(modEventBus);
        ServusLogisticsRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        NetworkTypes.register(ItemChannelNetwork.TYPE);
        NetworkTypes.register(EnergyChannelNetwork.TYPE);
        NetworkTypes.register(TankNetwork.TYPE);
        NetworkTypes.register(FluidChannelNetwork.TYPE);
        LOGGER.info("Servus: Logistics loaded");
    }
}
