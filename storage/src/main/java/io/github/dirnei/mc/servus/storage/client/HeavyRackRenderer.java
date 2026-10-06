package io.github.dirnei.mc.servus.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the rack's sample on its two shelf levels, two items per level, no counts. */
public class HeavyRackRenderer implements BlockEntityRenderer<HeavyRackBlockEntity> {

    private static final float ITEM_SCALE = 0.3F;
    private static final float OFFSET = 0.004F;
    private static final double[] COLUMNS = {-0.25, 0.25};
    private static final double[] LEVELS = {0.22, -0.28};

    public HeavyRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HeavyRackBlockEntity rack, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        List<ItemStack> sample = rack.displayed();
        if (sample.isEmpty() || rack.getLevel() == null) {
            return;
        }
        Direction facing = rack.getBlockState().getValue(HeavyRackBlock.FACING);
        int light = LevelRenderer.getLightColor(rack.getLevel(), rack.getBlockPos().relative(facing));

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(0.0, 0.0, -0.5 - OFFSET);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        int shown = Math.min(sample.size(), COLUMNS.length * LEVELS.length);
        for (int i = 0; i < shown; i++) {
            double cx = COLUMNS[i % COLUMNS.length];
            double cy = LEVELS[i / COLUMNS.length];
            poseStack.pushPose();
            poseStack.translate(cx, cy, 0.0);
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, 0.02F);
            Minecraft.getInstance().getItemRenderer().renderStatic(sample.get(i), ItemDisplayContext.FIXED, light, packedOverlay,
                    poseStack, bufferSource, rack.getLevel(), i);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
