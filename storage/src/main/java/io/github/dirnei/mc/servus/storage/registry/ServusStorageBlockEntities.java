package io.github.dirnei.mc.servus.storage.registry;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.RackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusStorageBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ServusStorageIds.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE =
            BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder
                    .of(CrateBlockEntity::new, ServusStorageBlocks.CRATE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RackBlockEntity>> RACK =
            BLOCK_ENTITIES.register("rack", () -> BlockEntityType.Builder
                    .of(RackBlockEntity::new, ServusStorageBlocks.RACK.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarehouseControllerBlockEntity>> WAREHOUSE_CONTROLLER =
            BLOCK_ENTITIES.register("warehouse_controller", () -> BlockEntityType.Builder
                    .of(WarehouseControllerBlockEntity::new, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CounterTerminalBlockEntity>> COUNTER_TERMINAL =
            BLOCK_ENTITIES.register("counter_terminal", () -> BlockEntityType.Builder
                    .of(CounterTerminalBlockEntity::new, ServusStorageBlocks.COUNTER_TERMINAL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RailBlockEntity>> RAIL =
            BLOCK_ENTITIES.register("rail", () -> BlockEntityType.Builder
                    .of(RailBlockEntity::new, ServusStorageBlocks.RAIL.get())
                    .build(null));

    private ServusStorageBlockEntities() {
    }
}
