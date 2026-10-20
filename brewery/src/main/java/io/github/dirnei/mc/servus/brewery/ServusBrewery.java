package io.github.dirnei.mc.servus.brewery;

import io.github.dirnei.mc.servus.brewery.gametest.BreweryGameTests;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryCreativeTabs;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
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
        ServusBreweryBlocks.BLOCKS.register(modEventBus);
        ServusBreweryBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusBreweryItems.ITEMS.register(modEventBus);
        ServusBreweryCreativeTabs.TABS.register(modEventBus);
        ServusBreweryRecipes.RECIPE_TYPES.register(modEventBus);
        ServusBreweryRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ServusRecipes.syncToClients(ServusBreweryRecipes.GERMINATING);
        ServusGameTests.register(modEventBus, ServusBreweryIds.MOD_ID, BreweryGameTests.CLASSES);
        LOGGER.info("Servus: Brewery loaded");
    }
}
