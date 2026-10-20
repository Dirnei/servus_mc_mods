package io.github.dirnei.mc.servus.brewery;

import io.github.dirnei.mc.servus.brewery.gametest.BreweryGameTests;
import io.github.dirnei.mc.servus.brewery.registry.BreweryFluids;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryCreativeTabs;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.brewery.config.ServusBreweryClientConfig;
import io.github.dirnei.mc.servus.brewery.rausch.RauschAttachment;
import io.github.dirnei.mc.servus.brewery.rausch.RauschEffects;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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
        BreweryFluids.register(modEventBus);
        ServusBreweryCreativeTabs.TABS.register(modEventBus);
        ServusBreweryRecipes.RECIPE_TYPES.register(modEventBus);
        ServusBreweryRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ServusBreweryRecipes.syncToClients();
        RauschEffects.EFFECTS.register(modEventBus);
        RauschAttachment.ATTACHMENT_TYPES.register(modEventBus);
        container.registerConfig(ModConfig.Type.CLIENT, ServusBreweryClientConfig.SPEC);
        ServusGameTests.register(modEventBus, ServusBreweryIds.MOD_ID, BreweryGameTests.CLASSES);
        LOGGER.info("Servus: Brewery loaded");
    }
}
