package io.github.dirnei.mc.servus.storage;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus: Storage.
 */
@Mod(ServusStorageIds.MOD_ID)
public final class ServusStorage {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusStorage.class);

    public ServusStorage(IEventBus modEventBus) {
        LOGGER.info("Servus: Storage loaded");
    }
}
