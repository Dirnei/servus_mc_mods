package io.github.dirnei.mc.servus.alchemy;

import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.gametest.AlchemyGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.CalcinerGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.CoagulatorGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.DropperGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.ForgeGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.HintGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.HomunculusGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.HollowGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.PrelumGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.PurifierGameTests;
import io.github.dirnei.mc.servus.alchemy.gametest.SalamanderGameTests;
import io.github.dirnei.mc.servus.alchemy.hint.HintSounds;
import io.github.dirnei.mc.servus.alchemy.journal.JournalTasksPage;
import io.github.dirnei.mc.servus.alchemy.machine.AlchemyMachineDefaults;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyComponents;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMenus;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entrypoint for Servus: Alchemy. */
@Mod(ServusAlchemyIds.MOD_ID)
public final class ServusAlchemy {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusAlchemy.class);

    public ServusAlchemy(IEventBus modEventBus, ModContainer container) {
        AlchemyMachineDefaults.registerDefaults();
        JournalTasksPage.register();
        ServusAlchemyBlocks.BLOCKS.register(modEventBus);
        PrincipleFluids.register(modEventBus);
        ElementalSight.register(modEventBus);
        ServusAlchemyEntities.ENTITIES.register(modEventBus);
        ServusAlchemyComponents.COMPONENTS.register(modEventBus);
        ServusAlchemyItems.ITEMS.register(modEventBus);
        ServusAlchemyBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusAlchemyMenus.MENUS.register(modEventBus);
        HintSounds.SOUNDS.register(modEventBus);
        ServusAlchemyRecipes.RECIPE_TYPES.register(modEventBus);
        ServusAlchemyRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ServusGameTests.register(modEventBus, ServusAlchemyIds.MOD_ID, List.of(
                AlchemyGameTests.class, CalcinerGameTests.class, PurifierGameTests.class, CoagulatorGameTests.class, HollowGameTests.class,
                DropperGameTests.class, SalamanderGameTests.class, ForgeGameTests.class,
                HomunculusGameTests.class, PrelumGameTests.class, HintGameTests.class));
        ServusRecipes.syncToClients(ServusAlchemyRecipes.CALCINATION);
        ServusRecipes.syncToClients(ServusAlchemyRecipes.COAGULATION);
        ServusRecipes.syncToClients(ServusAlchemyRecipes.ATHANOR);
        ServusRecipes.syncToClients(ServusAlchemyRecipes.PURIFICATION);
        ServusRecipes.syncToClients(ServusAlchemyRecipes.PRESSING);
        LOGGER.info("Servus: Alchemy loaded");
    }
}
