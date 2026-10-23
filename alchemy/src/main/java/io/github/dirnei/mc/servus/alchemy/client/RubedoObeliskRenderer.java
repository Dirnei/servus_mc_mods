package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RubedoObeliskBlockEntity;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Draws a Rubedo Obelisk from its lower half: the crystal, its light bands, the turning shard and the cracks of its plinth. */
public class RubedoObeliskRenderer extends BufferedBlockEntityRenderer<RubedoObeliskBlockEntity> {

    public RubedoObeliskRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(RubedoObeliskBlockEntity obelisk) {
        return new AABB(obelisk.getBlockPos()).expandTowards(0, 1.5, 0);
    }

    @Override
    protected void render(RubedoObeliskBlockEntity obelisk, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        Level level = obelisk.getLevel();
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        ObeliskDrawing.idle(poseStack, buffers, time, ObeliskDrawing.phase(obelisk.getBlockPos()),
                ObeliskDrawing.openSides(level, obelisk.getBlockPos()));
    }
}
