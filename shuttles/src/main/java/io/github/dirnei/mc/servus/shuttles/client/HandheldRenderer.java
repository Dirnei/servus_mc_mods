package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The handheld's effects in the world: a red scan line from the hand to the
 * aimed bay while use is held, a fading green or red box on scanned bays, and
 * a yellow outline seen through blocks around the bays a FIND lit up.
 *
 * <p>No two faces share a plane where they overlap: a flash box stands
 * {@link #FLASH_OFFSET} off the bay's faces and every further flashed bay one
 * {@link #STEP} further, so neighbouring bays of a shelf never draw on one
 * plane. The glow is drawn without depth test, so it cannot fight with blocks.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class HandheldRenderer {

    private static final float PX = 1 / 16F;
    private static final float FLASH_OFFSET = 0.05F * PX;
    private static final float STEP = 0.005F * PX;
    private static final int FLASH_ALPHA = 110;
    private static final int GREEN = 0x50FF78;
    private static final int RED = 0xFF3C3C;
    private static final int GLOW = 0xFFE14D;
    /** Half the width of a glow edge band, in blocks. */
    private static final float GLOW_BAND = 0.025F;
    private static final float GLOW_OUT = 0.02F;
    /** Half the width of the scan line, in blocks. */
    private static final float LASER_HALF = 0.006F;
    private static final int LASER = 0xFF2D2D;
    private static final int LASER_ALPHA = 190;

    private HandheldRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        long now = level.getGameTime();
        Map<BlockPos, ClientHandheld.Flash> flashes = ClientHandheld.flashes(now);
        List<BlockPos> glowing = ClientHandheld.glowing(now);
        BlockHitResult laserHit = laserTarget(minecraft, player);
        if (flashes.isEmpty() && glowing.isEmpty() && laserHit == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (!flashes.isEmpty() || laserHit != null) {
            VertexConsumer quads = buffers.getBuffer(RenderTypes.debugQuads());
            int index = 0;
            for (Map.Entry<BlockPos, ClientHandheld.Flash> entry : new ArrayList<>(flashes.entrySet())) {
                float age = (now - entry.getValue().start() + partial) / ClientHandheld.FLASH_TICKS;
                int alpha = (int) (FLASH_ALPHA * Math.max(0, 1 - age));
                float out = FLASH_OFFSET + index++ * STEP;
                box(quads, poseStack, entry.getKey(), camera, -out, 1 + out, entry.getValue().ok() ? GREEN : RED, alpha);
            }
            if (laserHit != null) {
                laser(quads, poseStack, minecraft.gameRenderer.getMainCamera(), camera, laserHit.getLocation());
            }
            buffers.endBatch(RenderTypes.debugQuads());
        }
        if (!glowing.isEmpty()) {
            RenderType seeThrough = RenderTypes.textBackgroundSeeThrough();
            VertexConsumer glow = buffers.getBuffer(seeThrough);
            int alpha = (int) (150 + 80 * Math.sin((now + partial) * 0.3));
            for (BlockPos pos : glowing) {
                outline(glow, poseStack, pos, camera, alpha);
            }
            buffers.endBatch(seeThrough);
        }
    }

    /** The bay the scan line points at: use is held with a handheld and the crosshair is on a bay. */
    private static BlockHitResult laserTarget(Minecraft minecraft, LocalPlayer player) {
        if (!minecraft.options.keyUse.isDown() || ClientHandheld.held(player).isEmpty()
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || ClientHandheld.aimedBay() == null) {
            return null;
        }
        return hit;
    }

    /** A box from {@code min} to {@code max} in block units around the block, all six faces. */
    private static void box(VertexConsumer consumer, PoseStack poseStack, BlockPos pos, Vec3 camera, float min, float max, int rgb,
                            int alpha) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        PoseStack.Pose pose = poseStack.last();
        float[][] corners = {{min, min, min}, {max, min, min}, {max, max, min}, {min, max, min},
                {min, min, max}, {max, min, max}, {max, max, max}, {min, max, max}};
        int[][] faces = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};
        for (int[] face : faces) {
            for (int corner : face) {
                consumer.addVertex(pose, corners[corner][0], corners[corner][1], corners[corner][2])
                        .setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, alpha);
            }
        }
        poseStack.popPose();
    }

    /** The twelve edges of the block as crossed bands, seen through anything. */
    private static void outline(VertexConsumer consumer, PoseStack poseStack, BlockPos pos, Vec3 camera, int alpha) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        PoseStack.Pose pose = poseStack.last();
        float lo = -GLOW_OUT;
        float hi = 1 + GLOW_OUT;
        float[] ends = {lo, hi};
        for (float a : ends) {
            for (float b : ends) {
                edge(consumer, pose, new Vector3f(lo, a, b), new Vector3f(hi, a, b), alpha);
                edge(consumer, pose, new Vector3f(a, lo, b), new Vector3f(a, hi, b), alpha);
                edge(consumer, pose, new Vector3f(a, b, lo), new Vector3f(a, b, hi), alpha);
            }
        }
        poseStack.popPose();
    }

    /** One edge as two crossed bands along it, each drawn from both sides. */
    private static void edge(VertexConsumer consumer, PoseStack.Pose pose, Vector3f from, Vector3f to, int alpha) {
        Vector3f along = new Vector3f(to).sub(from);
        Vector3f first = along.x != 0 ? new Vector3f(0, GLOW_BAND, 0) : new Vector3f(GLOW_BAND, 0, 0);
        Vector3f second = along.z != 0 ? new Vector3f(0, GLOW_BAND, 0) : new Vector3f(0, 0, GLOW_BAND);
        band(consumer, pose, from, to, first, GLOW, alpha);
        band(consumer, pose, from, to, second, GLOW, alpha);
    }

    /** A quad from {@code from} to {@code to}, {@code side} wide each way, front and back. */
    private static void band(VertexConsumer consumer, PoseStack.Pose pose, Vector3fc from, Vector3fc to, Vector3fc side, int rgb, int alpha) {
        Vector3f a = new Vector3f(from).sub(side);
        Vector3f b = new Vector3f(to).sub(side);
        Vector3f c = new Vector3f(to).add(side);
        Vector3f d = new Vector3f(from).add(side);
        Vector3f[][] windings = {{a, b, c, d}, {d, c, b, a}};
        for (Vector3f[] quad : windings) {
            for (Vector3f v : quad) {
                consumer.addVertex(pose, v.x, v.y, v.z).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, alpha)
                        .setLight(LightCoordsUtil.FULL_BRIGHT);
            }
        }
    }

    /** A thin red line from just below and right of the eye to the hit point, turned to face the camera. */
    private static void laser(VertexConsumer consumer, PoseStack poseStack, Camera mainCamera, Vec3 camera, Vec3 hit) {
        Vector3fc look = mainCamera.forwardVector();
        Vector3fc up = mainCamera.upVector();
        Vector3f right = new Vector3f(look).cross(up);
        Vector3f start = new Vector3f(look).mul(0.5F).add(new Vector3f(right).mul(0.28F)).sub(new Vector3f(up).mul(0.22F));
        Vector3f end = new Vector3f((float) (hit.x - camera.x), (float) (hit.y - camera.y), (float) (hit.z - camera.z));
        Vector3f along = new Vector3f(end).sub(start);
        Vector3f side = new Vector3f(along).cross(new Vector3f(end).add(start).mul(0.5F)).normalize().mul(LASER_HALF);
        if (!Float.isFinite(side.x)) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        Vector3f[] quad = {new Vector3f(start).sub(side), new Vector3f(end).sub(side), new Vector3f(end).add(side), new Vector3f(start).add(side)};
        for (Vector3f v : quad) {
            consumer.addVertex(pose, v.x, v.y, v.z).setColor(LASER >> 16 & 0xFF, LASER >> 8 & 0xFF, LASER & 0xFF, LASER_ALPHA);
        }
    }
}
