package io.github.dirnei.mc.servus.foundation.logistics;

import io.github.dirnei.mc.servus.foundation.logistics.network.EnergyChannelNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.network.ItemChannelNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsComponents;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsMenus;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsRecipes;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankNetwork;
import io.github.dirnei.mc.servus.foundation.network.NetworkTypes;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The duct, tank and facade content of Servus: Foundation. Registered from
 * the Foundation entrypoint.
 */
public final class ServusLogisticsContent {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusLogisticsContent.class);

    private ServusLogisticsContent() {
    }

    public static void register(IEventBus modEventBus) {
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
        LOGGER.info("Servus: Foundation logistics content registered");
    }
}
