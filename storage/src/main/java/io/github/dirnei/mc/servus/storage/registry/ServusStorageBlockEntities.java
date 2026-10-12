package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleWorkshopBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlockEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusStorageIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarehouseControllerBlockEntity>> WAREHOUSE_CONTROLLER =
            BLOCK_ENTITIES.register("warehouse_controller", () -> new BlockEntityType<>(WarehouseControllerBlockEntity::new, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL =
            BLOCK_ENTITIES.register("storage_terminal", () -> new BlockEntityType<>(StorageTerminalBlockEntity::new, ServusStorageBlocks.STORAGE_TERMINAL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProductionTerminalBlockEntity>> PRODUCTION_TERMINAL =
            BLOCK_ENTITIES.register("production_terminal", () -> new BlockEntityType<>(ProductionTerminalBlockEntity::new, ServusStorageBlocks.PRODUCTION_TERMINAL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternStorageBlockEntity>> PATTERN_STORAGE =
            BLOCK_ENTITIES.register("pattern_storage", () -> new BlockEntityType<>(PatternStorageBlockEntity::new, ServusStorageBlocks.PATTERN_STORAGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InboundStationBlockEntity>> INBOUND_STATION =
            BLOCK_ENTITIES.register("inbound_station", () -> new BlockEntityType<>(InboundStationBlockEntity::new, ServusStorageBlocks.INBOUND_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.dirnei.mc.servus.storage.packing.WirelessPackingStationBlockEntity>> WIRELESS_PACKING_STATION =
            BLOCK_ENTITIES.register("wireless_packing_station", () -> new BlockEntityType<>(io.github.dirnei.mc.servus.storage.packing.WirelessPackingStationBlockEntity::new, ServusStorageBlocks.WIRELESS_PACKING_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackingStationBlockEntity>> PACKING_STATION =
            BLOCK_ENTITIES.register("packing_station", () -> new BlockEntityType<>(PackingStationBlockEntity::new, ServusStorageBlocks.PACKING_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuttleGarageBlockEntity>> SHUTTLE_GARAGE =
            BLOCK_ENTITIES.register("shuttle_garage", () -> new BlockEntityType<>(ShuttleGarageBlockEntity::new, ServusStorageBlocks.SHUTTLE_GARAGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OutboundStationBlockEntity>> OUTBOUND_STATION =
            BLOCK_ENTITIES.register("outbound_station", () -> new BlockEntityType<>(OutboundStationBlockEntity::new, ServusStorageBlocks.OUTBOUND_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingStationBlockEntity>> CRAFTING_STATION =
            BLOCK_ENTITIES.register("crafting_station", () -> new BlockEntityType<>(CraftingStationBlockEntity::new, ServusStorageBlocks.CRAFTING_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CpuBlockEntity>> CRAFTING_CPU =
            BLOCK_ENTITIES.register("crafting_cpu", () -> new BlockEntityType<>(CpuBlockEntity::new, ServusStorageBlocks.CRAFTING_CPU.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RailBlockEntity>> RAIL =
            BLOCK_ENTITIES.register("rail", () -> new BlockEntityType<>(RailBlockEntity::new, ServusStorageBlocks.RAIL.get(), ServusStorageBlocks.FARM_RAIL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElevatorBlockEntity>> ELEVATOR =
            BLOCK_ENTITIES.register("elevator", () -> new BlockEntityType<>(ElevatorBlockEntity::new, ServusStorageBlocks.ELEVATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeavyRackBlockEntity>> HEAVY_RACK =
            BLOCK_ENTITIES.register("heavy_rack", () -> new BlockEntityType<>(HeavyRackBlockEntity::new, ServusStorageBlocks.HEAVY_RACK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FieldHeadBlockEntity>> FIELD_HEAD =
            BLOCK_ENTITIES.register("field_head", () -> new BlockEntityType<>(FieldHeadBlockEntity::new, ServusStorageBlocks.FIELD_HEAD.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuttleWorkshopBlockEntity>> SHUTTLE_WORKSHOP =
            BLOCK_ENTITIES.register("shuttle_workshop", () -> new BlockEntityType<>(ShuttleWorkshopBlockEntity::new, ServusStorageBlocks.SHUTTLE_WORKSHOP.get()));

    private ServusStorageBlockEntities() {
    }
}
