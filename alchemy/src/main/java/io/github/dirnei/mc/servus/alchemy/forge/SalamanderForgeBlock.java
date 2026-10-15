package io.github.dirnei.mc.servus.alchemy.forge;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction front = state.getValue(FACING);
        double across = random.nextDouble() * 0.5 - 0.25;
        double out = 0.4;
        double dx = front.getAxis() == Direction.Axis.X ? front.getStepX() * out : across;
        double dz = front.getAxis() == Direction.Axis.Z ? front.getStepZ() * out : across;
        double dy = (5 + random.nextDouble() * 5) / 16.0;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
    }
}
