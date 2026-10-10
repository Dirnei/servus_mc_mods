package io.github.dirnei.mc.servus.manufacturing.alchemy.structure;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * What a player sees and hears when the Journal forms an alchemy build: a chime at the controller
 * and enchantment glyphs rising from every block of the build. Called only after a successful form
 * from the Journal, never from unforming or test setup. Server thread only.
 */
public final class FormingEffect {

    private static final int PARTICLES_PER_BLOCK = 6;

    private FormingEffect() {
    }

    public static void play(Level level, BlockPos controller, Collection<BlockPos> cells) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        server.playSound(null, controller, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        for (BlockPos cell : cells) {
            server.sendParticles(ParticleTypes.ENCHANT, cell.getX() + 0.5, cell.getY() + 0.8, cell.getZ() + 0.5,
                    PARTICLES_PER_BLOCK, 0.35, 0.4, 0.35, 0.6);
        }
    }
}
