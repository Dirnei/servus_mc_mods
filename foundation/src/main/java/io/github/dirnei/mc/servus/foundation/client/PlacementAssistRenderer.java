package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssists;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.model.data.ModelData;

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
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Optional<PlacementAssistPreview.Preview> preview = PlacementAssistPreview.current();
        if (preview.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PlacementAssists.Target target = preview.get().target();
        BlockState state = preview.get().state();

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        BlockPos pos = target.pos();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(target.direction().getRotation());
        arrow(buffers.getBuffer(RenderType.debugQuads()), poseStack.last());
        poseStack.popPose();
        buffers.endBatch(RenderType.debugQuads());

        if (state != null) {
            RenderType ghostType = Sheets.translucentCullBlockSheet();
            minecraft.getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                    new AlphaConsumer(buffers.getBuffer(ghostType)), state,
                    minecraft.getBlockRenderer().getBlockModel(state), 1.0F, 1.0F, 1.0F,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, ghostType);
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

    /** Passes vertices through with a fixed alpha, so a block model draws as a ghost. */
    private record AlphaConsumer(VertexConsumer delegate) implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, GHOST_ALPHA);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
