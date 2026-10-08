package io.github.dirnei.mc.servus.storage;

import io.github.dirnei.mc.servus.foundation.network.NetworkTypes;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus: Storage.
 */
@Mod(ServusStorageIds.MOD_ID)
public final class ServusStorage {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusStorage.class);

    public ServusStorage(IEventBus modEventBus, ModContainer container) {
        ServusStorageBlocks.BLOCKS.register(modEventBus);
        ServusStorageItems.ITEMS.register(modEventBus);
        ServusStorageBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ServusStorageMenus.MENUS.register(modEventBus);
        ServusStorageComponents.COMPONENTS.register(modEventBus);
        ServusStorageRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        NetworkTypes.register(WarehouseNetwork.TYPE);
        for (var block : java.util.List.of(ServusStorageBlocks.CRAFTING_CPU, ServusStorageBlocks.ACCESS_POINT,
                ServusStorageBlocks.INBOUND_STATION, ServusStorageBlocks.OUTBOUND_STATION, ServusStorageBlocks.COUNTER_TERMINAL,
                ServusStorageBlocks.PRODUCTION_TERMINAL, ServusStorageBlocks.PATTERN_STORAGE, ServusStorageBlocks.CRAFTING_STATION,
                ServusStorageBlocks.WAREHOUSE_CONTROLLER, ServusStorageBlocks.SHUTTLE_GARAGE)) {
            Wood.register(block.getId());
        }
        LOGGER.info("Servus: Storage loaded");
    }
}
