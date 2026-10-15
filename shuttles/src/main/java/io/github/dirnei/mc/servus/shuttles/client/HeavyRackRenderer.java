package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Stands the rack's sample on its two shelf boards inside the frame, two items per level, no counts. */
public class HeavyRackRenderer extends LiveBlockEntityRenderer<HeavyRackBlockEntity> {

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
    protected void render(HeavyRackBlockEntity rack, float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                          int packedLight) {
        List<ItemStack> sample = rack.displayed();
        if (rack.getLevel() == null || (sample.isEmpty() && rack.zone().isEmpty())) {
            return;
        }
        Direction facing = rack.getBlockState().getValue(HeavyRackBlock.FACING);
        int light = LevelRenderer.getLightCoords(rack.getLevel(), rack.getBlockPos());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Now x runs to the aisle viewer's right, y up and plus z towards the aisle.

        if (!rack.zone().isEmpty()) {
            ZoneLabelText.draw(rack.zone(), poseStack, collector, Minecraft.getInstance().font, 0.0, ZONE_Y, 0.5 + 0.004, ZONE_SCALE, light);
        }

        int shown = Math.min(sample.size(), COLUMNS.length * SHELF_TOPS.length);
        for (int i = 0; i < shown; i++) {
            int column = i % COLUMNS.length;
            double cy = SHELF_TOPS[i / COLUMNS.length] + HALF;
            poseStack.pushPose();
            poseStack.translate(COLUMNS[column], cy, BACK);
            poseStack.mulPose(Axis.YP.rotationDegrees(column == 0 ? YAW : -YAW));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            RenderSubmit.item(poseStack, collector, sample.get(i), ItemDisplayContext.FIXED, rack.getLevel(), i, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
