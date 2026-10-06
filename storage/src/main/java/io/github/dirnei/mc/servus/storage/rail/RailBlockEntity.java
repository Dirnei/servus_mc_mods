package io.github.dirnei.mc.servus.storage.rail;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Connector node: joins the warehouse graph but holds no stock. */
public class RailBlockEntity extends StorageNodeBlockEntity {

    public RailBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.RAIL.get(), pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.RAIL;
    }
}
