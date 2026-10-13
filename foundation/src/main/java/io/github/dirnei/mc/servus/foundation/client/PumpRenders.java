package io.github.dirnei.mc.servus.foundation.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/** Parts shared by the renderers of the two block tall beam pumps, drawn from the lower half. */
final class PumpRenders {

    private PumpRenders() {
    }

    /** The light of the upper half, or {@code fallback} without a level. */
    static int upperLight(BlockEntity pump, int fallback) {
        return pump.getLevel() == null ? fallback : LevelRenderer.getLightCoords(pump.getLevel(), pump.getBlockPos().above());
    }

    /** Both halves, so the moving parts stay drawn while only the upper block is in view. */
    static AABB bothHalves(BlockEntity pump) {
        return new AABB(pump.getBlockPos()).expandTowards(0, 1, 0);
    }
}
