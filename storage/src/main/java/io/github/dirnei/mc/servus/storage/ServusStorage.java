package io.github.dirnei.mc.servus.storage;

import io.github.dirnei.mc.servus.lib.network.NetworkTypes;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
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
        NetworkTypes.register(WarehouseNetwork.TYPE);
        LOGGER.info("Servus: Storage loaded");
    }
}
