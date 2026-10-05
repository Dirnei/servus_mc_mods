package io.github.dirnei.mc.servus.manufacturing.cable;

import io.github.dirnei.mc.servus.lib.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A cable node. All logic lives in {@link PowerCableNetwork}. */
public class PowerCableBlockEntity extends NetworkBlockEntity<PowerCableBlockEntity> {

    public PowerCableBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.POWER_CABLE.get(), pos, state);
    }

    @Override
    public NetworkType<PowerCableBlockEntity, ?, ?> networkType() {
        return PowerCableNetwork.TYPE;
    }

    @Override
    public PowerCableBlockEntity networkNode() {
        return this;
    }
}
