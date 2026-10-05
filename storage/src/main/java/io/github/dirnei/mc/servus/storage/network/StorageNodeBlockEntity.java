package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.lib.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Base of every block that takes part in a warehouse network. */
public abstract class StorageNodeBlockEntity extends NetworkBlockEntity<StorageNodeBlockEntity> {

    protected StorageNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public NetworkType<StorageNodeBlockEntity, ?, ?> networkType() {
        return WarehouseNetwork.TYPE;
    }

    @Override
    public StorageNodeBlockEntity networkNode() {
        return this;
    }
}
