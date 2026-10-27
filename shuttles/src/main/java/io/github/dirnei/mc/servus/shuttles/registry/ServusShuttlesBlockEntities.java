package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleWorkshopBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import io.github.dirnei.mc.servus.shuttles.production.ProductionTerminalBlockEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusShuttlesBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusShuttlesIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarehouseControllerBlockEntity>> WAREHOUSE_CONTROLLER =
            BLOCK_ENTITIES.register("warehouse_controller", () -> new BlockEntityType<>(WarehouseControllerBlockEntity::new, ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL =
            BLOCK_ENTITIES.register("storage_terminal", () -> new BlockEntityType<>(StorageTerminalBlockEntity::new, ServusShuttlesBlocks.STORAGE_TERMINAL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProductionTerminalBlockEntity>> PRODUCTION_TERMINAL =
            BLOCK_ENTITIES.register("production_terminal", () -> new BlockEntityType<>(ProductionTerminalBlockEntity::new, ServusShuttlesBlocks.PRODUCTION_TERMINAL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternStorageBlockEntity>> PATTERN_STORAGE =
            BLOCK_ENTITIES.register("pattern_storage", () -> new BlockEntityType<>(PatternStorageBlockEntity::new, ServusShuttlesBlocks.PATTERN_STORAGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InboundStationBlockEntity>> INBOUND_STATION =
            BLOCK_ENTITIES.register("inbound_station", () -> new BlockEntityType<>(InboundStationBlockEntity::new, ServusShuttlesBlocks.INBOUND_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackingStationBlockEntity>> PACKING_STATION =
            BLOCK_ENTITIES.register("packing_station", () -> new BlockEntityType<>(PackingStationBlockEntity::new, ServusShuttlesBlocks.PACKING_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuttleGarageBlockEntity>> SHUTTLE_GARAGE =
            BLOCK_ENTITIES.register("shuttle_garage", () -> new BlockEntityType<>(ShuttleGarageBlockEntity::new, ServusShuttlesBlocks.SHUTTLE_GARAGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OutboundStationBlockEntity>> OUTBOUND_STATION =
            BLOCK_ENTITIES.register("outbound_station", () -> new BlockEntityType<>(OutboundStationBlockEntity::new, ServusShuttlesBlocks.OUTBOUND_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingStationBlockEntity>> CRAFTING_STATION =
            BLOCK_ENTITIES.register("crafting_station", () -> new BlockEntityType<>(CraftingStationBlockEntity::new, ServusShuttlesBlocks.CRAFTING_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CpuBlockEntity>> CRAFTING_CPU =
            BLOCK_ENTITIES.register("crafting_cpu", () -> new BlockEntityType<>(CpuBlockEntity::new,
                    ServusShuttlesBlocks.CRAFTING_CPU.get(), ServusShuttlesBlocks.CPU_BUFFER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RailBlockEntity>> RAIL =
            BLOCK_ENTITIES.register("rail", () -> new BlockEntityType<>(RailBlockEntity::new, ServusShuttlesBlocks.RAIL.get(), ServusShuttlesBlocks.FARM_RAIL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElevatorBlockEntity>> ELEVATOR =
            BLOCK_ENTITIES.register("elevator", () -> new BlockEntityType<>(ElevatorBlockEntity::new, ServusShuttlesBlocks.ELEVATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeavyRackBlockEntity>> HEAVY_RACK =
            BLOCK_ENTITIES.register("heavy_rack", () -> new BlockEntityType<>(HeavyRackBlockEntity::new, ServusShuttlesBlocks.HEAVY_RACK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidRackBlockEntity>> FLUID_RACK =
            BLOCK_ENTITIES.register("fluid_rack", () -> new BlockEntityType<>(FluidRackBlockEntity::new, ServusShuttlesBlocks.FLUID_RACK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FieldHeadBlockEntity>> FIELD_HEAD =
            BLOCK_ENTITIES.register("field_head", () -> new BlockEntityType<>(FieldHeadBlockEntity::new, ServusShuttlesBlocks.FIELD_HEAD.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuttleWorkshopBlockEntity>> SHUTTLE_WORKSHOP =
            BLOCK_ENTITIES.register("shuttle_workshop", () -> new BlockEntityType<>(ShuttleWorkshopBlockEntity::new, ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidGantryBlockEntity>> FLUID_GANTRY =
            BLOCK_ENTITIES.register("fluid_gantry", () -> new BlockEntityType<>(FluidGantryBlockEntity::new, ServusShuttlesBlocks.FLUID_GANTRY.get()));

    private ServusShuttlesBlockEntities() {
    }
}
