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

/** Stands the rack's sample on its two shelf boards inside the frame, two items per level, no counts. */
public class HeavyRackRenderer implements BlockEntityRenderer<HeavyRackBlockEntity> {

    private static final float ITEM_SCALE = 0.33F;
    private static final float HALF = ITEM_SCALE / 2.0F;
    private static final double[] COLUMNS = {-0.22, 0.22};
    private static final double[] SHELF_TOPS = {1.0 / 16.0, 9.0 / 16.0};
    private static final double BACK = -0.05;
    private static final float YAW = 20.0F;
    private static final double ZONE_Y = 0.93;
    private static final float ZONE_SCALE = 0.008F;

    public HeavyRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HeavyRackBlockEntity rack, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        List<ItemStack> sample = rack.displayed();
        if (rack.getLevel() == null || (sample.isEmpty() && rack.zone().isEmpty())) {
            return;
        }
        Direction facing = rack.getBlockState().getValue(HeavyRackBlock.FACING);
        int light = LevelRenderer.getLightColor(rack.getLevel(), rack.getBlockPos());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Now x runs to the aisle viewer's right, y up and plus z towards the aisle.

        if (!rack.zone().isEmpty()) {
            ZoneLabelText.draw(rack.zone(), poseStack, bufferSource, Minecraft.getInstance().font, 0.0, ZONE_Y, 0.5 + 0.004, ZONE_SCALE, light);
        }

        int shown = Math.min(sample.size(), COLUMNS.length * SHELF_TOPS.length);
        for (int i = 0; i < shown; i++) {
            int column = i % COLUMNS.length;
            double cy = SHELF_TOPS[i / COLUMNS.length] + HALF;
            poseStack.pushPose();
            poseStack.translate(COLUMNS[column], cy, BACK);
            poseStack.mulPose(Axis.YP.rotationDegrees(column == 0 ? YAW : -YAW));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(sample.get(i), ItemDisplayContext.FIXED, light, packedOverlay,
                    poseStack, bufferSource, rack.getLevel(), i);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
