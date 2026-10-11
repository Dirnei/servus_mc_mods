package io.github.dirnei.mc.servus.foundation.client;

import net.minecraft.util.RandomSource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import java.util.List;
import java.util.ArrayList;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssists;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws a translucent ghost and an arrow where a sneak click with an
 * assisted item would place, using the same walk as the placement.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class PlacementAssistRenderer {

    static final int R = 90;
    static final int G = 200;
    static final int B = 255;
    static final int A = 220;
    private static final int GHOST_ALPHA = 120;
    private static final float SHAFT = 0.07F;
    private static final float HEAD = 0.22F;
    private static final float TAIL = -0.4F;
    private static final float NECK = 0.1F;
    private static final float TIP = 0.45F;

    private PlacementAssistRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Optional<PlacementAssistPreview.Preview> preview = PlacementAssistPreview.current();
        if (preview.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PlacementAssists.Target target = preview.get().target();
        BlockState state = preview.get().state();

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        BlockPos pos = target.pos();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(target.direction().getRotation());
        arrow(buffers.getBuffer(RenderTypes.debugQuads()), poseStack.last());
        poseStack.popPose();
        buffers.endBatch(RenderTypes.debugQuads());

        if (state != null) {
            RenderType ghostType = Sheets.translucentBlockSheet();
            VertexConsumer consumer = buffers.getBuffer(ghostType);
            List<BlockStateModelPart> parts = new ArrayList<>();
            minecraft.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(42), parts);
            QuadInstance instance = new QuadInstance();
            instance.setColor(GHOST_ALPHA << 24 | 0xFFFFFF);
            instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
            instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
            for (BlockStateModelPart part : parts) {
                for (BakedQuad quad : part.getQuads(null)) {
                    consumer.putBakedQuad(poseStack.last(), quad, instance);
                }
                for (Direction direction : Direction.values()) {
                    for (BakedQuad quad : part.getQuads(direction)) {
                        consumer.putBakedQuad(poseStack.last(), quad, instance);
                    }
                }
            }
            buffers.endBatch(ghostType);
        }
        poseStack.popPose();
    }

    /** An arrow along +Y through the block centre, drawn in two crossed planes so it shows from every side. */
    private static void arrow(VertexConsumer consumer, PoseStack.Pose pose) {
        for (boolean xPlane : new boolean[] {true, false}) {
            quad(consumer, pose, xPlane, -SHAFT, TAIL, SHAFT, TAIL, SHAFT, NECK, -SHAFT, NECK);
            quad(consumer, pose, xPlane, -HEAD, NECK, HEAD, NECK, 0.0F, TIP, 0.0F, TIP);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, boolean xPlane,
                             float a, float ay, float b, float by, float c, float cy, float d, float dy) {
        vertex(consumer, pose, xPlane, a, ay);
        vertex(consumer, pose, xPlane, b, by);
        vertex(consumer, pose, xPlane, c, cy);
        vertex(consumer, pose, xPlane, d, dy);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, boolean xPlane, float side, float y) {
        consumer.addVertex(pose, xPlane ? side : 0.0F, y, xPlane ? 0.0F : side).setColor(R, G, B, A);
    }
}
