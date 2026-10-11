package io.github.dirnei.mc.servus.foundation;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.ServusLogisticsContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.network.NetworkTypes;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationCreativeTabs;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationComponents;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterGameTests;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
import io.github.dirnei.mc.servus.foundation.logistics.gametest.DataCableTierGameTests;
import io.github.dirnei.mc.servus.foundation.logistics.gametest.FluidTankGameTests;
import io.github.dirnei.mc.servus.foundation.logistics.gametest.ServusLogisticsGameTests;
import io.github.dirnei.mc.servus.foundation.logistics.gametest.UpgradeGameTests;
import io.github.dirnei.mc.servus.foundation.machine.MachineBaseGameTests;
import io.github.dirnei.mc.servus.foundation.machine.MachineGameTests;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpGameTests;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkGameTests;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint for Servus: Foundation.
 */
@Mod(ServusFoundationIds.MOD_ID)
public final class ServusFoundation {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusFoundation.class);

    public ServusFoundation(IEventBus modEventBus, ModContainer container) {
        ServusFoundationItems.ITEMS.register(modEventBus);
        ServusFoundationComponents.COMPONENTS.register(modEventBus);
        ServusFoundationCreativeTabs.TABS.register(modEventBus);
        NetworkTypes.register(DataNetwork.TYPE);
        ServusLogisticsContent.register(modEventBus);
        AdapterContent.register(modEventBus);
        MachineContent.register(modEventBus);
        WoodContent.register(modEventBus);
        container.registerConfig(ModConfig.Type.COMMON, ServusFoundationConfig.SPEC);
        ServusGameTests.register(modEventBus, ServusFoundationIds.MOD_ID, List.of(
                AdapterGameTests.class, DataCableTierGameTests.class, FluidTankGameTests.class,
                ServusLogisticsGameTests.class, UpgradeGameTests.class, MachineBaseGameTests.class,
                MachineGameTests.class, PumpGameTests.class, SinkGameTests.class));
        LOGGER.info("Servus: Foundation loaded");
    }
}
