package io.github.dirnei.mc.servus.alchemy.homunculus;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.machine.HearthFire;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The Receptaculum: a Hearth Chest whose fire keeps the homunculus above warm, so it is lit from the moment it is placed. */
public class ReceptaculumBlock extends MachineBlock {

    public static final MapCodec<ReceptaculumBlock> CODEC = simpleCodec(ReceptaculumBlock::new);

    public ReceptaculumBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusAlchemyBlockEntities.RECEPTACULUM.get();
    }

    /** Flames and smoke from the grate of the hearth. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        HearthFire.animate(state, level, pos, random, 1.5, 2.5, 0.5);
    }
}
