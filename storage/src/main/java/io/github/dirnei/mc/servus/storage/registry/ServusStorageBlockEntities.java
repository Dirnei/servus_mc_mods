package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlockEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusStorageIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE =
            BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder
                    .of(CrateBlockEntity::new, ServusStorageBlocks.CRATE.get(), ServusStorageBlocks.DOUBLE_CRATE.get(),
                            ServusStorageBlocks.QUAD_CRATE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarehouseControllerBlockEntity>> WAREHOUSE_CONTROLLER =
            BLOCK_ENTITIES.register("warehouse_controller", () -> BlockEntityType.Builder
                    .of(WarehouseControllerBlockEntity::new, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL =
            BLOCK_ENTITIES.register("storage_terminal", () -> BlockEntityType.Builder
                    .of(StorageTerminalBlockEntity::new, ServusStorageBlocks.STORAGE_TERMINAL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProductionTerminalBlockEntity>> PRODUCTION_TERMINAL =
            BLOCK_ENTITIES.register("production_terminal", () -> BlockEntityType.Builder
                    .of(ProductionTerminalBlockEntity::new, ServusStorageBlocks.PRODUCTION_TERMINAL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternStorageBlockEntity>> PATTERN_STORAGE =
            BLOCK_ENTITIES.register("pattern_storage", () -> BlockEntityType.Builder
                    .of(PatternStorageBlockEntity::new, ServusStorageBlocks.PATTERN_STORAGE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InboundStationBlockEntity>> INBOUND_STATION =
            BLOCK_ENTITIES.register("inbound_station", () -> BlockEntityType.Builder
                    .of(InboundStationBlockEntity::new, ServusStorageBlocks.INBOUND_STATION.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackingStationBlockEntity>> PACKING_STATION =
            BLOCK_ENTITIES.register("packing_station", () -> BlockEntityType.Builder
                    .of(PackingStationBlockEntity::new, ServusStorageBlocks.PACKING_STATION.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuttleGarageBlockEntity>> SHUTTLE_GARAGE =
            BLOCK_ENTITIES.register("shuttle_garage", () -> BlockEntityType.Builder
                    .of(ShuttleGarageBlockEntity::new, ServusStorageBlocks.SHUTTLE_GARAGE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OutboundStationBlockEntity>> OUTBOUND_STATION =
            BLOCK_ENTITIES.register("outbound_station", () -> BlockEntityType.Builder
                    .of(OutboundStationBlockEntity::new, ServusStorageBlocks.OUTBOUND_STATION.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingStationBlockEntity>> CRAFTING_STATION =
            BLOCK_ENTITIES.register("crafting_station", () -> BlockEntityType.Builder
                    .of(CraftingStationBlockEntity::new, ServusStorageBlocks.CRAFTING_STATION.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CpuBlockEntity>> CRAFTING_CPU =
            BLOCK_ENTITIES.register("crafting_cpu", () -> BlockEntityType.Builder
                    .of(CpuBlockEntity::new, ServusStorageBlocks.CRAFTING_CPU.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RailBlockEntity>> RAIL =
            BLOCK_ENTITIES.register("rail", () -> BlockEntityType.Builder
                    .of(RailBlockEntity::new, ServusStorageBlocks.RAIL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElevatorBlockEntity>> ELEVATOR =
            BLOCK_ENTITIES.register("elevator", () -> BlockEntityType.Builder
                    .of(ElevatorBlockEntity::new, ServusStorageBlocks.ELEVATOR.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeavyRackBlockEntity>> HEAVY_RACK =
            BLOCK_ENTITIES.register("heavy_rack", () -> BlockEntityType.Builder
                    .of(HeavyRackBlockEntity::new, ServusStorageBlocks.HEAVY_RACK.get())
                    .build(null));

    private ServusStorageBlockEntities() {
    }
}
