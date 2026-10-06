package io.github.dirnei.mc.servus.storage.rail;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Connector node: joins the warehouse graph but holds no stock. */
public class ElevatorBlockEntity extends StorageNodeBlockEntity {

    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.ELEVATOR.get(), pos, state);
    }

    @Override
    public boolean holdsStock() {
        return false;
    }
}
