package io.github.dirnei.mc.servus.foundation.machine.loader;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The Chunk Loader, shaped like an ender anchor; it is lit while it holds its chunk. */
public class ChunkLoaderBlock extends MachineBlock {

    public static final MapCodec<ChunkLoaderBlock> CODEC = simpleCodec(ChunkLoaderBlock::new);

    /** One portal particle in this many client ticks on average while active. */
    private static final int PARTICLE_CHANCE = 3;

    public ChunkLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.CHUNK_LOADER_ENTITY.get();
    }

    /** A few faint portal particles drift around the block while it holds its chunk, like around an ender chest. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(PARTICLE_CHANCE) != 0) {
            return;
        }
        int sideX = random.nextInt(2) * 2 - 1;
        int sideZ = random.nextInt(2) * 2 - 1;
        double x = pos.getX() + 0.5 + 0.25 * sideX;
        double y = pos.getY() + random.nextFloat();
        double z = pos.getZ() + 0.5 + 0.25 * sideZ;
        level.addParticle(ParticleTypes.PORTAL, x, y, z,
                random.nextFloat() * sideX, (random.nextFloat() - 0.5) * 0.125, random.nextFloat() * sideZ);
    }
}
