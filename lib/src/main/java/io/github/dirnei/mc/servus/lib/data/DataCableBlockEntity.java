package io.github.dirnei.mc.servus.lib.data;

import io.github.dirnei.mc.servus.lib.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.registry.ServusLibBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A Data Cable as a node of its data network. Holds nothing. */
public class DataCableBlockEntity extends NetworkBlockEntity<DataCableBlockEntity> {

    public DataCableBlockEntity(BlockPos pos, BlockState state) {
        super(ServusLibBlockEntities.DATA_CABLE.get(), pos, state);
    }

    @Override
    public NetworkType<DataCableBlockEntity, ?, ?> networkType() {
        return DataNetwork.TYPE;
    }

    @Override
    public DataCableBlockEntity networkNode() {
        return this;
    }
}
