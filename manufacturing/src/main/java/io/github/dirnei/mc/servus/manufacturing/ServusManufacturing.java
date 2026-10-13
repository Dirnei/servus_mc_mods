package io.github.dirnei.mc.servus.manufacturing;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.manufacturing.gametest.AlchemyGameTests;
import io.github.dirnei.mc.servus.manufacturing.gametest.CalcinerGameTests;
import io.github.dirnei.mc.servus.manufacturing.gametest.CoagulatorGameTests;
import io.github.dirnei.mc.servus.manufacturing.gametest.PurifierGameTests;
import io.github.dirnei.mc.servus.manufacturing.machine.ManufacturingMachines;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entrypoint for Servus: Manufacturing. */
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
        ServusGameTests.register(modEventBus, ServusManufacturingIds.MOD_ID, List.of(
                AlchemyGameTests.class, CalcinerGameTests.class, PurifierGameTests.class, CoagulatorGameTests.class));
        ServusRecipes.syncToClients(ServusManufacturingRecipes.CALCINATION);
        ServusRecipes.syncToClients(ServusManufacturingRecipes.COAGULATION);
        ServusRecipes.syncToClients(ServusManufacturingRecipes.ATHANOR);
        ServusRecipes.syncToClients(ServusManufacturingRecipes.PURIFICATION);
        LOGGER.info("Servus: Manufacturing loaded");
    }
}
