package io.github.dirnei.mc.servus.brewery;

import io.github.dirnei.mc.servus.brewery.gametest.BreweryGameTests;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryCreativeTabs;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entrypoint for Servus: Brewery. */
@Mod(ServusBreweryIds.MOD_ID)
public final class ServusBrewery {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusBrewery.class);

    public ServusBrewery(IEventBus modEventBus, ModContainer container) {
        ServusBreweryItems.ITEMS.register(modEventBus);
        ServusBreweryCreativeTabs.TABS.register(modEventBus);
        ServusGameTests.register(modEventBus, ServusBreweryIds.MOD_ID, BreweryGameTests.CLASSES);
        LOGGER.info("Servus: Brewery loaded");
    }
}
