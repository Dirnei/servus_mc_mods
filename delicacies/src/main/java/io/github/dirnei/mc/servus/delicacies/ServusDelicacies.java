package io.github.dirnei.mc.servus.delicacies;

import io.github.dirnei.mc.servus.delicacies.gametest.DelicaciesGameTests;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesCreativeTabs;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entrypoint for Servus: Delicacies. */
@Mod(ServusDelicaciesIds.MOD_ID)
public final class ServusDelicacies {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusDelicacies.class);

    public ServusDelicacies(IEventBus modEventBus, ModContainer container) {
        NeoForgeMod.enableMilkFluid();
        ServusDelicaciesBlocks.BLOCKS.register(modEventBus);
        ServusDelicaciesBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusDelicaciesRecipes.RECIPE_TYPES.register(modEventBus);
        ServusDelicaciesRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ServusDelicaciesRecipes.syncToClients();
        ServusDelicaciesItems.ITEMS.register(modEventBus);
        ServusDelicaciesCreativeTabs.TABS.register(modEventBus);
        ServusGameTests.register(modEventBus, ServusDelicaciesIds.MOD_ID, DelicaciesGameTests.CLASSES);
        LOGGER.info("Servus: Delicacies loaded");
    }
}
