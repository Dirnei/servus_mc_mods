package io.github.dirnei.mc.servus.foundation.logistics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.data.ClientDuctLoads;
import io.github.dirnei.mc.servus.foundation.item.GogglesItem;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctTracker;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * While the player reveals hidden things: draws the centre of every data duct
 * in the colour of its load over the last second, from idle green to full red,
 * around the centre where it is drawn at its resting pose.
 * Ducts behind facade panels are not drawn; the player sees the panel.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class DuctOverlayRenderer {

    private static final double RANGE_SQ = 32.0 * 32.0;
    private static final int A = 110;
    /** How far in pixels the load box stands off the duct's centre. */
    private static final float LOAD_MARGIN = 0.5F;
    /** Load colours by level: idle, light, half, busy, full. */
    private static final int[] LOAD_COLOURS = {0x3CC85A, 0xA0DC3C, 0xF0DC3C, 0xFF9628, 0xF03228};

    private DuctOverlayRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !GogglesItem.reveals(minecraft.player)) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(ServusRenderTypes.OVERLAY);
        boolean any = false;
        for (UtilityDuctBlockEntity duct : DuctTracker.live(minecraft.level)) {
            BlockPos pos = duct.getBlockPos();
            if (pos.getCenter().distanceToSqr(camera) > RANGE_SQ) {
                continue;
            }
            if (duct.has(Line.DATA)) {
                any = true;
                int colour = LOAD_COLOURS[Math.min(LOAD_COLOURS.length - 1, ClientDuctLoads.level(pos))];
                poseStack.pushPose();
                poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                DuctShapes.Box centre = DuctShapes.center(DuctShapes.centerRadius(duct.lines().size(), duct.connections()), duct.pose(),
                        duct.poseFaces(), DuctShapes.armRadii(duct.armMasks()));
                box(consumer, poseStack.last(), grown(centre.x0(), -1), grown(centre.y0(), -1), grown(centre.z0(), -1),
                        grown(centre.x1(), 1), grown(centre.y1(), 1), grown(centre.z1(), 1),
                        (colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF);
                poseStack.popPose();
            }
        }
        if (any) {
            buffers.endBatch(ServusRenderTypes.OVERLAY);
        }
    }

    /** A box coordinate in pixels moved outward by the load margin, in blocks. */
    private static float grown(float pixels, int outward) {
        return (pixels + outward * LOAD_MARGIN) / 16.0F;
    }

    private static void box(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                            int r, int g, int b) {
        quad(consumer, pose, r, g, b, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(consumer, pose, r, g, b, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(consumer, pose, r, g, b, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(consumer, pose, r, g, b, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        quad(consumer, pose, r, g, b, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(consumer, pose, r, g, b, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int r, int g, int b,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        consumer.addVertex(pose, ax, ay, az).setColor(r, g, b, A);
        consumer.addVertex(pose, bx, by, bz).setColor(r, g, b, A);
        consumer.addVertex(pose, cx, cy, cz).setColor(r, g, b, A);
        consumer.addVertex(pose, dx, dy, dz).setColor(r, g, b, A);
    }
}
