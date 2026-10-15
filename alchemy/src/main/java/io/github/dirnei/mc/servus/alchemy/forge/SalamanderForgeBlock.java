package io.github.dirnei.mc.servus.alchemy.forge;

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

/** The Salamander Forge: a furnace whose fire never goes out, so it is lit from the moment it is placed. */
public class SalamanderForgeBlock extends MachineBlock {

    public static final MapCodec<SalamanderForgeBlock> CODEC = simpleCodec(SalamanderForgeBlock::new);

    public SalamanderForgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusAlchemyBlockEntities.SALAMANDER_FORGE.get();
    }

    /** Flames and smoke from the mouth and now and then the furnace's crackle, like a lit furnace. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        HearthFire.animate(state, level, pos, random, 5, 5, 0.4);
    }
}
