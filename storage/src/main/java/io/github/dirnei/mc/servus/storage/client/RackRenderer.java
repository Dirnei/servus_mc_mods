package io.github.dirnei.mc.servus.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.storage.rack.RackBlock;
import io.github.dirnei.mc.servus.storage.rack.RackBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.RackCompartments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws each compartment's stack on the rack's front face. */
public class RackRenderer implements BlockEntityRenderer<RackBlockEntity> {

    private static final float ITEM_SCALE = 0.3F;
    private static final float TEXT_SCALE = 0.008F;
    private static final float OFFSET = 0.004F;

    public RackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RackBlockEntity rack, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (rack.isEmpty() || rack.getLevel() == null) {
            return;
        }
        Direction facing = rack.getBlockState().getValue(RackBlock.FACING);
        int light = LevelRenderer.getLightColor(rack.getLevel(), rack.getBlockPos().relative(facing));
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(0.0, 0.0, -0.5 - OFFSET);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Now x runs to the viewer's right and y up, origin at the face centre.

        for (int i = 0; i < RackCompartments.COUNT; i++) {
            ItemStack stack = rack.items().getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            double cx = RackCompartments.centreFromLeft(i) - 0.5;
            double cy = RackCompartments.centreY(i) - 0.5;

            poseStack.pushPose();
            poseStack.translate(cx, cy, 0.0);
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, 0.02F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, packedOverlay,
                    poseStack, bufferSource, rack.getLevel(), i);
            poseStack.popPose();

            if (stack.getCount() > 1) {
                String text = Integer.toString(stack.getCount());
                poseStack.pushPose();
                poseStack.translate(cx + 0.2, cy - 0.2, -0.001);
                poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
                font.drawInBatch(text, -font.width(text), 0.0F, 0xFFFFFF, false, poseStack.last().pose(),
                        bufferSource, Font.DisplayMode.NORMAL, 0, light);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }
}
