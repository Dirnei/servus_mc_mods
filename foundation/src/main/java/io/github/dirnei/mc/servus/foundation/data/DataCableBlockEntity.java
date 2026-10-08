package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A Data Cable as a node of its data network. Holds nothing. */
public class DataCableBlockEntity extends NetworkBlockEntity<DataCableBlockEntity> {

    public DataCableBlockEntity(BlockPos pos, BlockState state) {
        super(ServusFoundationBlockEntities.DATA_CABLE.get(), pos, state);
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
