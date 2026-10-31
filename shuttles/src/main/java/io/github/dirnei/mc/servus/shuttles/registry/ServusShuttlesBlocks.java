package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.field.FarmRailBlock;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidOutboundBlock;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlock;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlock;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlock;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlock;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBufferBlock;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlock;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlock;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleWorkshopBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import io.github.dirnei.mc.servus.shuttles.production.ProductionTerminalBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusShuttlesBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusShuttlesIds.MOD_ID);

    public static final DeferredBlock<WarehouseControllerBlock> WAREHOUSE_CONTROLLER =
            BLOCKS.registerBlock("warehouse_controller", WarehouseControllerBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<StorageTerminalBlock> STORAGE_TERMINAL =
            BLOCKS.registerBlock("storage_terminal", StorageTerminalBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD));

    public static final DeferredBlock<ProductionTerminalBlock> PRODUCTION_TERMINAL =
            BLOCKS.registerBlock("production_terminal", ProductionTerminalBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<PatternStorageBlock> PATTERN_STORAGE =
            BLOCKS.registerBlock("pattern_storage", PatternStorageBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<RailBlock> RAIL = BLOCKS.registerBlock("rail", RailBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<FarmRailBlock> FARM_RAIL = BLOCKS.registerBlock("farm_rail", FarmRailBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<FieldHeadBlock> FIELD_HEAD = BLOCKS.registerBlock("field_head", FieldHeadBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<ElevatorBlock> ELEVATOR = BLOCKS.registerBlock("elevator", ElevatorBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<HeavyRackBlock> HEAVY_RACK = BLOCKS.registerBlock("heavy_rack", HeavyRackBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<FluidRackBlock> FLUID_RACK = BLOCKS.registerBlock("fluid_rack", FluidRackBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<InboundStationBlock> INBOUND_STATION =
            BLOCKS.registerBlock("inbound_station", InboundStationBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<PackingStationBlock> PACKING_STATION =
            BLOCKS.registerBlock("packing_station", PackingStationBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<ShuttleGarageBlock> SHUTTLE_GARAGE =
            BLOCKS.registerBlock("shuttle_garage", ShuttleGarageBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion());

    public static final DeferredBlock<OutboundStationBlock> OUTBOUND_STATION =
            BLOCKS.registerBlock("outbound_station", OutboundStationBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<CraftingStationBlock> CRAFTING_STATION =
            BLOCKS.registerBlock("crafting_station", CraftingStationBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD));

    public static final DeferredBlock<CpuBlock> CRAFTING_CPU =
            BLOCKS.registerBlock("crafting_cpu", CpuBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<CpuBufferBlock> CPU_BUFFER =
            BLOCKS.registerBlock("crafting_cpu_buffer", CpuBufferBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<ShuttleWorkshopBlock> SHUTTLE_WORKSHOP =
            BLOCKS.registerBlock("shuttle_workshop", ShuttleWorkshopBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD));

    public static final DeferredBlock<FluidInboundBlock> FLUID_INBOUND =
            BLOCKS.registerBlock("fluid_inbound", FluidInboundBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .noLootTable());

    public static final DeferredBlock<FluidOutboundBlock> FLUID_OUTBOUND =
            BLOCKS.registerBlock("fluid_outbound", FluidOutboundBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .noLootTable());

    public static final DeferredBlock<ParcelLockerBlock> PARCEL_LOCKER =
            BLOCKS.registerBlock("parcel_locker", ParcelLockerBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_YELLOW)
                            .strength(3.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .noLootTable());

    private ServusShuttlesBlocks() {
    }
}
