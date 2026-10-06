package io.github.dirnei.mc.servus.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the stored item and its count on the crate's front face. */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity> {

    private static final float ITEM_SCALE = 0.5F;
    private static final float TEXT_SCALE = 0.012F;
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
        ItemStack stack = crate.type();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(0.0, 0.0, -0.5 - OFFSET);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0.0, 0.1, 0.0);
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, 0.02F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, packedOverlay,
                poseStack, bufferSource, crate.getLevel(), 0);
        poseStack.popPose();

        Font font = Minecraft.getInstance().font;
        String text = Integer.toString(crate.count());
        poseStack.pushPose();
        poseStack.translate(0.0, -0.25, -0.001);
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFFFF, false, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, light);
        poseStack.popPose();

        poseStack.popPose();
    }
}
