package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.alchemy.coagulator.GrownVatBlockEntity;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatCrystal;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** Draws a grown vat's full liquid and its crystal cluster, which stands up to three blocks tall. */
public class GrownVatRenderer extends BufferedBlockEntityRenderer<GrownVatBlockEntity> {

    public GrownVatRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(GrownVatBlockEntity vat) {
        return new AABB(vat.getBlockPos()).expandTowards(0, 2, 0).inflate(1, 0, 1);
    }

    @Override
    protected void render(GrownVatBlockEntity vat, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        VatCrystal crystal = vat.crystal();
        if (!crystal.grown()) {
            return;
        }
        CrystalDrawing.liquid(poseStack, buffers, 1, crystal.color());
        CrystalDrawing.cluster(poseStack, buffers, vat.getBlockPos(), 1, crystal.body());
    }
}
