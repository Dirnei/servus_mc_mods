package io.github.dirnei.mc.servus.delicacies;

import io.github.dirnei.mc.servus.delicacies.gametest.DelicaciesGameTests;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesCreativeTabs;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entrypoint for Servus: Delicacies. */
@Mod(ServusDelicaciesIds.MOD_ID)
public final class ServusDelicacies {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusDelicacies.class);

    public ServusDelicacies(IEventBus modEventBus, ModContainer container) {
        ServusDelicaciesBlocks.BLOCKS.register(modEventBus);
        ServusDelicaciesItems.ITEMS.register(modEventBus);
        ServusDelicaciesCreativeTabs.TABS.register(modEventBus);
        ServusGameTests.register(modEventBus, ServusDelicaciesIds.MOD_ID, DelicaciesGameTests.CLASSES);
        LOGGER.info("Servus: Delicacies loaded");
    }
}
