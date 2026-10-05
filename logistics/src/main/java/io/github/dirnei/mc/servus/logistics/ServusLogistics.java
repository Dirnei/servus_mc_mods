package io.github.dirnei.mc.servus.logistics;

import io.github.dirnei.mc.servus.lib.network.NetworkTypes;
import io.github.dirnei.mc.servus.logistics.network.EnergyChannelNetwork;
import io.github.dirnei.mc.servus.logistics.network.ItemChannelNetwork;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsMenus;
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
        NetworkTypes.register(ItemChannelNetwork.TYPE);
        NetworkTypes.register(EnergyChannelNetwork.TYPE);
        LOGGER.info("Servus: Logistics loaded");
    }
}
