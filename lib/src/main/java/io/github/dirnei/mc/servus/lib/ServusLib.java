package io.github.dirnei.mc.servus.lib;

import io.github.dirnei.mc.servus.lib.config.ServusLibConfig;
import io.github.dirnei.mc.servus.lib.registry.ServusLibCreativeTabs;
import io.github.dirnei.mc.servus.lib.registry.ServusLibItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus Lib.
 */
@Mod(ServusLibIds.MOD_ID)
public final class ServusLib {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusLib.class);

    public ServusLib(IEventBus modEventBus, ModContainer container) {
        ServusLibItems.ITEMS.register(modEventBus);
        ServusLibCreativeTabs.TABS.register(modEventBus);
        container.registerConfig(ModConfig.Type.COMMON, ServusLibConfig.SPEC);
        LOGGER.info("Servus Lib loaded");
    }
}
