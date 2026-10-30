package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The handheld's effects in the world: a red scan line from the hand to the
 * aimed bay while use is held, a fading green or red box over a scanned
 * shelf, and a yellow tint with an outline, seen through blocks, over the
 * shelves a FIND lit up while a handheld is in hand. Each effect covers a
 * whole shelf as one box.
 *
 * <p>No two faces share a plane where they overlap: a flash stands
 * {@link #FLASH_OFFSET} off the shelf's faces, the tint {@link #TINT_OUT} and
 * the outline bands {@link #GLOW_OUT}, and every further shelf of the same
 * effect one {@link #STEP} further. Bands along Y and Z stop one band width
 * short of each corner, where the X bands end, so bands only touch there.
 * Tint and outline have no depth test, so they cannot fight with blocks.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class HandheldRenderer {

    private static final double PX = 1 / 16D;
    private static final double FLASH_OFFSET = 0.05 * PX;
    private static final double STEP = 0.005 * PX;
    private static final int FLASH_ALPHA = 110;
    private static final int GREEN = 0x50FF78;
    private static final int RED = 0xFF3C3C;
    private static final int GLOW = 0xFFE14D;
    private static final int TINT_ALPHA = 46;
    /** Half the width of a glow edge band, in blocks. */
    private static final float GLOW_BAND = 0.025F;
    private static final double GLOW_OUT = 0.02;
    private static final double TINT_OUT = GLOW_OUT - 0.05 * PX;
    /** Half the width of the scan line, in blocks. */
    private static final float LASER_HALF = 0.006F;
    private static final int LASER = 0xFF2D2D;
    private static final int LASER_ALPHA = 190;
    private static final int[][] FACES = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};

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
        List<ClientHandheld.Flash> flashes = ClientHandheld.flashes(now);
        boolean holding = !ClientHandheld.held(player).isEmpty();
        List<AABB> glowing = holding ? ClientHandheld.glowing(now) : List.of();
        BlockHitResult laserHit = holding ? laserTarget(minecraft, player) : null;
        if (flashes.isEmpty() && glowing.isEmpty() && laserHit == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (!flashes.isEmpty() || laserHit != null) {
            VertexConsumer quads = buffers.getBuffer(RenderTypes.debugQuads());
            for (int i = 0; i < flashes.size(); i++) {
                ClientHandheld.Flash flash = flashes.get(i);
                float age = (now - flash.start() + partial) / ClientHandheld.FLASH_TICKS;
                int alpha = (int) (FLASH_ALPHA * Math.max(0, 1 - age));
                box(quads, poseStack, flash.box().inflate(FLASH_OFFSET + i * STEP), camera, flash.ok() ? GREEN : RED, alpha);
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
            for (int i = 0; i < glowing.size(); i++) {
                AABB shelf = glowing.get(i);
                box(glow, poseStack, shelf.inflate(TINT_OUT + i * STEP), camera, GLOW, TINT_ALPHA);
                outline(glow, poseStack, shelf.inflate(GLOW_OUT + i * STEP), camera, alpha);
            }
            buffers.endBatch(seeThrough);
        }
    }

    /** Where the scan line points: use is held and the player aims at a bay within the scan range. */
    private static BlockHitResult laserTarget(Minecraft minecraft, LocalPlayer player) {
        if (!minecraft.options.keyUse.isDown() || ClientHandheld.aimedBay() == null) {
            return null;
        }
        return HandheldItem.aim(player);
    }

    /** The six faces of the box, seen from outside. */
    private static void box(VertexConsumer consumer, PoseStack poseStack, AABB box, Vec3 camera, int rgb, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        float x0 = (float) (box.minX - camera.x);
        float y0 = (float) (box.minY - camera.y);
        float z0 = (float) (box.minZ - camera.z);
        float x1 = (float) (box.maxX - camera.x);
        float y1 = (float) (box.maxY - camera.y);
        float z1 = (float) (box.maxZ - camera.z);
        float[][] corners = {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0},
                {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
        for (int[] face : FACES) {
            for (int corner : face) {
                consumer.addVertex(pose, corners[corner][0], corners[corner][1], corners[corner][2])
                        .setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, alpha)
                        .setLight(LightCoordsUtil.FULL_BRIGHT);
            }
        }
    }

    /**
     * The twelve edges of the box as crossed bands. Bands along X run the full
     * edge; bands along Y and Z stop one band width short of each corner.
     */
    private static void outline(VertexConsumer consumer, PoseStack poseStack, AABB box, Vec3 camera, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        float x0 = (float) (box.minX - camera.x);
        float y0 = (float) (box.minY - camera.y);
        float z0 = (float) (box.minZ - camera.z);
        float x1 = (float) (box.maxX - camera.x);
        float y1 = (float) (box.maxY - camera.y);
        float z1 = (float) (box.maxZ - camera.z);
        for (float a : new float[] {y0, y1}) {
            for (float b : new float[] {z0, z1}) {
                edge(consumer, pose, new Vector3f(x0, a, b), new Vector3f(x1, a, b), alpha);
            }
        }
        for (float a : new float[] {x0, x1}) {
            for (float b : new float[] {z0, z1}) {
                edge(consumer, pose, new Vector3f(a, y0 + GLOW_BAND, b), new Vector3f(a, y1 - GLOW_BAND, b), alpha);
            }
            for (float b : new float[] {y0, y1}) {
                edge(consumer, pose, new Vector3f(a, b, z0 + GLOW_BAND), new Vector3f(a, b, z1 - GLOW_BAND), alpha);
            }
        }
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
