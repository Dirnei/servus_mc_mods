package io.github.dirnei.mc.servus.manufacturing;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus: Manufacturing.
 */
@Mod(ServusManufacturingIds.MOD_ID)
public final class ServusManufacturing {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusManufacturing.class);

    public ServusManufacturing(IEventBus modEventBus) {
        LOGGER.info("Servus: Manufacturing loaded");
    }
}
