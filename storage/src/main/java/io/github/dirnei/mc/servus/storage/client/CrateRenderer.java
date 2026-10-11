package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateCompartments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws every filled compartment's item and count on the crate's front face. */
public class CrateRenderer extends LiveBlockEntityRenderer<CrateBlockEntity> {

    private static final float OFFSET = 0.004F;
    private static final double ZONE_Y = 0.40;
    private static final float ZONE_SCALE = 0.007F;

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(CrateBlockEntity crate, float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                          int packedLight) {
        int packedOverlay = OverlayTexture.NO_OVERLAY;
        if (crate.getLevel() == null || (crate.isEmpty() && crate.zone().isEmpty())) {
            return;
        }
        Direction facing = crate.getBlockState().getValue(CrateBlock.FACING);
        int light = LevelRenderer.getLightCoords(crate.getLevel(), crate.getBlockPos().relative(facing));
        Font font = Minecraft.getInstance().font;
        int compartments = crate.compartments();
        float itemScale = compartments == 1 ? 0.5F : 0.3F;
        float textScale = compartments == 1 ? 0.012F : 0.008F;
        double textDrop = compartments == 1 ? 0.25 : 0.17;
        double itemLift = compartments == 1 ? 0.1 : 0.05;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(0.0, 0.0, -0.5 - OFFSET);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Now x runs to the viewer's right and y up, origin at the face centre.

        if (!crate.zone().isEmpty()) {
            ZoneLabelText.draw(crate.zone(), poseStack, collector, font, 0.0, ZONE_Y, -0.001, ZONE_SCALE, light);
        }

        for (int i = 0; i < compartments; i++) {
            if (crate.isEmpty(i)) {
                continue;
            }
            ItemStack stack = crate.type(i);
            double cx = CrateCompartments.centreFromLeft(compartments, i) - 0.5;
            double cy = CrateCompartments.centreY(compartments, i) - 0.5;

            poseStack.pushPose();
            poseStack.translate(cx, cy + itemLift, 0.0);
            poseStack.scale(itemScale, itemScale, 0.02F);
            RenderSubmit.item(poseStack, collector, stack, ItemDisplayContext.FIXED, crate.getLevel(), i, light);
            poseStack.popPose();

            String text = Integer.toString(crate.count(i));
            poseStack.pushPose();
            poseStack.translate(cx, cy - textDrop, -0.001);
            poseStack.scale(textScale, -textScale, textScale);
            RenderSubmit.text(poseStack, collector, text, -font.width(text) / 2.0F, 0.0F, 0xFFFFFF, false, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
