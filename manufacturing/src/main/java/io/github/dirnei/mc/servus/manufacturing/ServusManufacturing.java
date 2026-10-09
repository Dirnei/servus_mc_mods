package io.github.dirnei.mc.servus.manufacturing;

import io.github.dirnei.mc.servus.manufacturing.machine.ManufacturingMachines;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus: Manufacturing.
 */
@Mod(ServusManufacturingIds.MOD_ID)
public final class ServusManufacturing {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusManufacturing.class);

    public ServusManufacturing(IEventBus modEventBus, ModContainer container) {
        ManufacturingMachines.registerDefaults();
        ServusManufacturingBlocks.BLOCKS.register(modEventBus);
        ServusManufacturingItems.ITEMS.register(modEventBus);
        ServusManufacturingBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusManufacturingMenus.MENUS.register(modEventBus);
        ServusManufacturingRecipes.RECIPE_TYPES.register(modEventBus);
        ServusManufacturingRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        LOGGER.info("Servus: Manufacturing loaded");
    }
}
