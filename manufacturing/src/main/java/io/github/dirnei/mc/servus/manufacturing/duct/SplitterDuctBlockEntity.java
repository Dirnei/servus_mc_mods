package io.github.dirnei.mc.servus.manufacturing.duct;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Marker node: the network pulls from this duct's neighbours. */
public class SplitterDuctBlockEntity extends ItemDuctBlockEntity {

    public SplitterDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.SPLITTER_DUCT.get(), pos, state);
    }
}
