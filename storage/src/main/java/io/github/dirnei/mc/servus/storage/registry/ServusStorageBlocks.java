package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.DoubleCrateBlock;
import io.github.dirnei.mc.servus.storage.crate.QuadCrateBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlock;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.station.DispatchStationBlock;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    public static final DeferredBlock<CounterTerminalBlock> COUNTER_TERMINAL =
            BLOCKS.registerBlock("counter_terminal", CounterTerminalBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5F)
                            .sound(SoundType.WOOD));

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

    public static final DeferredBlock<DispatchStationBlock> DISPATCH_STATION =
            BLOCKS.registerBlock("dispatch_station", DispatchStationBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
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

    private ServusStorageBlocks() {
    }
}
