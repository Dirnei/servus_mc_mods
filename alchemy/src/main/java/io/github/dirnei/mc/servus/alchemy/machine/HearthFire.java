package io.github.dirnei.mc.servus.alchemy.machine;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The look of an eternal fire in a machine's front: flames and smoke at its mouth and now and then a furnace's crackle. */
public final class HearthFire {

    private HearthFire() {
    }

    /**
     * Client particles for one animate tick of a fire whose mouth faces the block's {@link MachineBlock#FACING}.
     *
     * @param lowest  lowest height of the flames in pixels
     * @param range   how many pixels above {@code lowest} they may start
     * @param out     how far in front of the block's centre they start, in blocks
     */
    public static void animate(BlockState state, Level level, BlockPos pos, RandomSource random, double lowest, double range, double out) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction front = state.getValue(MachineBlock.FACING);
        double across = random.nextDouble() * 0.5 - 0.25;
        double dx = front.getAxis() == Direction.Axis.X ? front.getStepX() * out : across;
        double dz = front.getAxis() == Direction.Axis.Z ? front.getStepZ() * out : across;
        double dy = (lowest + random.nextDouble() * range) / 16.0;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
    }
}
