package io.github.dirnei.mc.servus.storage;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTests;
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
        for (var block : java.util.List.of(ServusStorageBlocks.CRAFTING_CPU, ServusStorageBlocks.CPU_BUFFER, ServusStorageBlocks.PACKING_STATION,
                ServusStorageBlocks.INBOUND_STATION, ServusStorageBlocks.OUTBOUND_STATION, ServusStorageBlocks.STORAGE_TERMINAL,
                ServusStorageBlocks.PRODUCTION_TERMINAL, ServusStorageBlocks.PATTERN_STORAGE, ServusStorageBlocks.CRAFTING_STATION,
                ServusStorageBlocks.WAREHOUSE_CONTROLLER, ServusStorageBlocks.SHUTTLE_GARAGE, ServusStorageBlocks.FIELD_HEAD,
                ServusStorageBlocks.RAIL, ServusStorageBlocks.FARM_RAIL)) {
            Wood.register(block.getId());
        }
        // The pattern editor matches crafting grids on the client too.
        io.github.dirnei.mc.servus.foundation.production.ServusRecipes.syncToClients(() -> net.minecraft.world.item.crafting.RecipeType.CRAFTING);
        ServusGameTests.register(modEventBus, ServusStorageIds.MOD_ID, java.util.List.of(
                io.github.dirnei.mc.servus.storage.gametest.AdapterOrderGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.AdjacentDataGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.BlockStyleGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.ChainHandoffGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.ControllerSiteGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.ControllerWirelessGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.CraftingCpuGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.CraftingGridGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.DataBandwidthGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.DataCableGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.DataPacketGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.FieldGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.FieldLookGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.HarvesterGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.PackingStationGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.PacketInletGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.PatternGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.PatternResultGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.PersistGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.PipeAccessGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.ProductionTerminalGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.RailCorrosionGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.RackDockGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.RailLookGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.MiniWarehouseGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.ServusStorageGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.ShuttleModuleGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.SiteWakeGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.SpreaderGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.StationHandsGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.StationRailGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.StorageTerminalRowsGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.TerminalCableGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.TerminalCraftGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.TerminalSpeedGameTests.class,
                io.github.dirnei.mc.servus.storage.gametest.WarehousePowerGameTests.class));
        LOGGER.info("Servus: Storage loaded");
    }
}
