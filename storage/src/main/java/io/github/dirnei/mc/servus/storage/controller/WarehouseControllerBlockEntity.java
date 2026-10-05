package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Marker node: exactly one per network makes the warehouse online. */
public class WarehouseControllerBlockEntity extends StorageNodeBlockEntity {

    public WarehouseControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(), pos, state);
    }
}
