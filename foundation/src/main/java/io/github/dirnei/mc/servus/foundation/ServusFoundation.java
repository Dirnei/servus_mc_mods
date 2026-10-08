package io.github.dirnei.mc.servus.foundation;

import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.ServusLogisticsContent;
import io.github.dirnei.mc.servus.foundation.network.NetworkTypes;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationBlockEntities;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationBlocks;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus: Foundation.
 */
@Mod(ServusFoundationIds.MOD_ID)
public final class ServusFoundation {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusFoundation.class);

    public ServusFoundation(IEventBus modEventBus, ModContainer container) {
        ServusFoundationBlocks.BLOCKS.register(modEventBus);
        ServusFoundationItems.ITEMS.register(modEventBus);
        ServusFoundationBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusFoundationCreativeTabs.TABS.register(modEventBus);
        NetworkTypes.register(DataNetwork.TYPE);
        ServusLogisticsContent.register(modEventBus);
        container.registerConfig(ModConfig.Type.COMMON, ServusFoundationConfig.SPEC);
        LOGGER.info("Servus: Foundation loaded");
    }
}
