package io.github.dirnei.mc.servus.lib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus Lib.
 */
@Mod(ServusLibIds.MOD_ID)
public final class ServusLib {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusLib.class);

    public ServusLib(IEventBus modEventBus) {
        LOGGER.info("Servus Lib loaded");
    }
}
