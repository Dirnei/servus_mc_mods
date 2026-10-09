package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.DoubleCrateBlock;
import io.github.dirnei.mc.servus.storage.crate.QuadCrateBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlock;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusStorageIds.MOD_ID);

    public static final DeferredBlock<CrateBlock> CRATE = BLOCKS.registerBlock("crate", CrateBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<DoubleCrateBlock> DOUBLE_CRATE = BLOCKS.registerBlock("double_crate", DoubleCrateBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<QuadCrateBlock> QUAD_CRATE = BLOCKS.registerBlock("quad_crate", QuadCrateBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<WarehouseControllerBlock> WAREHOUSE_CONTROLLER =
            BLOCKS.registerBlock("warehouse_controller", WarehouseControllerBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<StorageTerminalBlock> STORAGE_TERMINAL =
            BLOCKS.registerBlock("storage_terminal", StorageTerminalBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD));

    public static final DeferredBlock<ProductionTerminalBlock> PRODUCTION_TERMINAL =
            BLOCKS.registerBlock("production_terminal", ProductionTerminalBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<PatternStorageBlock> PATTERN_STORAGE =
            BLOCKS.registerBlock("pattern_storage", PatternStorageBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<RailBlock> RAIL = BLOCKS.registerBlock("rail", RailBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<ElevatorBlock> ELEVATOR = BLOCKS.registerBlock("elevator", ElevatorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<HeavyRackBlock> HEAVY_RACK = BLOCKS.registerBlock("heavy_rack", HeavyRackBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<InboundStationBlock> INBOUND_STATION =
            BLOCKS.registerBlock("inbound_station", InboundStationBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<PackingStationBlock> PACKING_STATION =
            BLOCKS.registerBlock("packing_station", PackingStationBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<ShuttleGarageBlock> SHUTTLE_GARAGE =
            BLOCKS.registerBlock("shuttle_garage", ShuttleGarageBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<OutboundStationBlock> OUTBOUND_STATION =
            BLOCKS.registerBlock("outbound_station", OutboundStationBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    public static final DeferredBlock<CraftingStationBlock> CRAFTING_STATION =
            BLOCKS.registerBlock("crafting_station", CraftingStationBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD));

    public static final DeferredBlock<CpuBlock> CRAFTING_CPU =
            BLOCKS.registerBlock("crafting_cpu", CpuBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    private ServusStorageBlocks() {
    }
}
