package io.github.dirnei.mc.servus.storage.client;

import net.minecraft.client.renderer.SubmitNodeCollector;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldHeadLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.RecordedGeometry;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Draws what the Field Head shows beyond its block model: the water of its
 * buffer in the glass window of every side, up to the synced level, with the
 * water's still texture and the biome's water tint; and the field's crop item
 * lying flat on the plate on top, upright for a player in front of the head.
 */
public final class FieldHeadRenderer extends LiveBlockEntityRenderer<FieldHeadBlockEntity> {

    /** Gap between the drawings and the block faces. */
    private static final float OFFSET = 0.002F;
    /** The glass window in face coordinates (0 to 1, y up): texels 2 to 14 across, 9 to 14 up. */
    private static final float WINDOW_X0 = 2 / 16F;
    private static final float WINDOW_X1 = 14 / 16F;
    private static final float WINDOW_Y0 = 9 / 16F;
    private static final float WINDOW_Y1 = 14 / 16F;
    /** The crop icon covers the 8 texels inside the plate's rim. */
    private static final float ICON_SCALE = 0.5F;

    public FieldHeadRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(FieldHeadBlockEntity head, float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                          int packedLight) {
        int packedOverlay = OverlayTexture.NO_OVERLAY;
        Level level = head.getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = head.getBlockPos();
        int water = head.waterLevel();
        if (water > 0) {
            renderWater(level, pos, water, poseStack, collector);
        }
        ItemStack crop = head.crop();
        if (!crop.isEmpty() && !head.harvestOnly()) {
            renderCrop(head, level, pos, crop, poseStack, collector, packedOverlay);
        }
    }

    private static void renderWater(Level level, BlockPos pos, int water, PoseStack poseStack, SubmitNodeCollector collector) {
        FluidStack waterStack = new FluidStack(Fluids.WATER, 1000);
        TextureAtlasSprite sprite = FluidSprites.still(waterStack);
        int color = level instanceof net.minecraft.client.renderer.block.BlockAndTintGetter tinted
                ? FluidSprites.tint(tinted, pos, waterStack) : FluidSprites.tint(waterStack);
        float top = WINDOW_Y0 + (WINDOW_Y1 - WINDOW_Y0) * water / FieldHeadLook.WATER_LEVELS;
        RecordedGeometry consumer = new RecordedGeometry();
        for (Direction face : Direction.Plane.HORIZONTAL) {
            int light = LevelRenderer.getLightCoords(level, pos.relative(face));
            poseStack.pushPose();
            facePose(poseStack, face);
            // x runs to the viewer's right from -0.5 to 0.5, y up, the viewer on +z.
            float x0 = WINDOW_X0 - 0.5F;
            float x1 = WINDOW_X1 - 0.5F;
            float y0 = WINDOW_Y0 - 0.5F;
            float y1 = top - 0.5F;
            float u0 = sprite.getU(WINDOW_X0);
            float u1 = sprite.getU(WINDOW_X1);
            float v0 = sprite.getV(1 - top);
            float v1 = sprite.getV(1 - WINDOW_Y0);
            PoseStack.Pose pose = poseStack.last();
            vertex(consumer, pose, x0, y0, u0, v1, color, light);
            vertex(consumer, pose, x1, y0, u1, v1, color, light);
            vertex(consumer, pose, x1, y1, u1, v0, color, light);
            vertex(consumer, pose, x0, y1, u0, v0, color, light);
            poseStack.popPose();
        }
        consumer.submit(collector, Sheets.translucentBlockSheet());
    }

    /** Moves the origin to the centre of that face, a hair outside it, turned so the viewer looks along -z. */
    private static void facePose(PoseStack poseStack, Direction face) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - face.toYRot()));
        poseStack.translate(0.0, 0.0, -0.5 - OFFSET);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int color, int light) {
        consumer.addVertex(pose, x, y, 0)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 0, 1);
    }

    private static void renderCrop(FieldHeadBlockEntity head, Level level, BlockPos pos, ItemStack crop, PoseStack poseStack,
                                   SubmitNodeCollector collector, int packedOverlay) {
        Direction facing = head.getBlockState().getValue(FieldHeadBlock.FACING);
        int light = LevelRenderer.getLightCoords(level, pos.above());
        poseStack.pushPose();
        poseStack.translate(0.5, 1.0 + OFFSET * 2, 0.5);
        // The item's up points away from a player standing in front of the head.
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(ICON_SCALE, ICON_SCALE, 0.02F);
        RenderSubmit.item(poseStack, collector, crop, ItemDisplayContext.FIXED, level, (int) pos.asLong(), light);
        poseStack.popPose();
    }
}
