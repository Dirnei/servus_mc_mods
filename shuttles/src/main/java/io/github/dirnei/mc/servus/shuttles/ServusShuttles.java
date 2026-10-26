package io.github.dirnei.mc.servus.shuttles;

import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import io.github.dirnei.mc.servus.foundation.network.NetworkTypes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.shuttles.gametest.ShuttlesGameTests;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMachineKinds;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesRecipes;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesSounds;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entrypoint for Servus: Shuttles. */
@Mod(ServusShuttlesIds.MOD_ID)
public final class ServusShuttles {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusShuttles.class);

    public ServusShuttles(IEventBus modEventBus, ModContainer container) {
        ServusShuttlesBlocks.BLOCKS.register(modEventBus);
        ServusShuttlesItems.ITEMS.register(modEventBus);
        ServusShuttlesBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusShuttlesMenus.MENUS.register(modEventBus);
        ServusShuttlesComponents.COMPONENTS.register(modEventBus);
        ServusShuttlesRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ServusShuttlesSounds.SOUNDS.register(modEventBus);
        NetworkTypes.register(WarehouseNetwork.TYPE);
        ServusShuttlesMachineKinds.register();
        for (var block : List.of(ServusShuttlesBlocks.CRAFTING_CPU, ServusShuttlesBlocks.CPU_BUFFER, ServusShuttlesBlocks.PACKING_STATION,
                ServusShuttlesBlocks.INBOUND_STATION, ServusShuttlesBlocks.OUTBOUND_STATION, ServusShuttlesBlocks.STORAGE_TERMINAL,
                ServusShuttlesBlocks.PRODUCTION_TERMINAL, ServusShuttlesBlocks.PATTERN_STORAGE, ServusShuttlesBlocks.CRAFTING_STATION,
                ServusShuttlesBlocks.WAREHOUSE_CONTROLLER, ServusShuttlesBlocks.SHUTTLE_GARAGE, ServusShuttlesBlocks.FIELD_HEAD,
                ServusShuttlesBlocks.RAIL, ServusShuttlesBlocks.FARM_RAIL, ServusShuttlesBlocks.SHUTTLE_WORKSHOP, ServusShuttlesBlocks.ELEVATOR)) {
            Material.register(block.getId(), MaterialKind.WOOD);
        }
        // The pattern editor matches crafting grids on the client too.
        ServusRecipes.syncToClients(() -> RecipeType.CRAFTING);
        ServusGameTests.register(modEventBus, ServusShuttlesIds.MOD_ID, ShuttlesGameTests.CLASSES);
        LOGGER.info("Servus: Shuttles loaded");
    }
}
