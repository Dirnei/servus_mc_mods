package io.github.dirnei.mc.servus.logistics;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus: Logistics.
 */
@Mod(ServusLogisticsIds.MOD_ID)
public final class ServusLogistics {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusLogistics.class);

    public ServusLogistics(IEventBus modEventBus) {
        LOGGER.info("Servus: Logistics loaded");
    }
}
