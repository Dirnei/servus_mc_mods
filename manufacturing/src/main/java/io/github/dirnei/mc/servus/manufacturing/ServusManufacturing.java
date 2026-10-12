package io.github.dirnei.mc.servus.manufacturing;

import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
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
        PrincipleFluids.register(modEventBus);
        ElementalSight.register(modEventBus);
        ServusManufacturingItems.ITEMS.register(modEventBus);
        ServusManufacturingBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusManufacturingMenus.MENUS.register(modEventBus);
        ServusManufacturingRecipes.RECIPE_TYPES.register(modEventBus);
        ServusManufacturingRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests.register(modEventBus, ServusManufacturingIds.MOD_ID, java.util.List.of(
                io.github.dirnei.mc.servus.manufacturing.gametest.AlchemyGameTests.class,
                io.github.dirnei.mc.servus.manufacturing.gametest.CalcinerGameTests.class,
                io.github.dirnei.mc.servus.manufacturing.gametest.PurifierGameTests.class,
                io.github.dirnei.mc.servus.manufacturing.gametest.CoagulatorGameTests.class));
        // Screens, JEI and the Jade plugin read the alchemy recipes on the client.
        io.github.dirnei.mc.servus.foundation.production.ServusRecipes.syncToClients(ServusManufacturingRecipes.CALCINATION);
        io.github.dirnei.mc.servus.foundation.production.ServusRecipes.syncToClients(ServusManufacturingRecipes.COAGULATION);
        io.github.dirnei.mc.servus.foundation.production.ServusRecipes.syncToClients(ServusManufacturingRecipes.ATHANOR);
        io.github.dirnei.mc.servus.foundation.production.ServusRecipes.syncToClients(ServusManufacturingRecipes.PURIFICATION);
        LOGGER.info("Servus: Manufacturing loaded");
    }
}
