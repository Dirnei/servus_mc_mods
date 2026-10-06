package io.github.dirnei.mc.servus.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateCompartments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws every filled compartment's item and count on the crate's front face. */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity> {

    private static final float OFFSET = 0.004F;

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrateBlockEntity crate, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (crate.isEmpty() || crate.getLevel() == null) {
            return;
        }
        Direction facing = crate.getBlockState().getValue(CrateBlock.FACING);
        int light = LevelRenderer.getLightColor(crate.getLevel(), crate.getBlockPos().relative(facing));
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
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, packedOverlay,
                    poseStack, bufferSource, crate.getLevel(), i);
            poseStack.popPose();

            String text = Integer.toString(crate.count(i));
            poseStack.pushPose();
            poseStack.translate(cx, cy - textDrop, -0.001);
            poseStack.scale(textScale, -textScale, textScale);
            font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFFFF, false, poseStack.last().pose(),
                    bufferSource, Font.DisplayMode.NORMAL, 0, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
