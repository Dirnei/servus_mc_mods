package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The lower half of a Rubedo Obelisk; it holds nothing and only gives the obelisk its renderer. */
public class RubedoObeliskBlockEntity extends BlockEntity {

    public RubedoObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.RUBEDO_OBELISK.get(), pos, state);
    }
}
