package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CrystalCluster;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CrystalCluster.Spike;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector3f;

/**
 * Draws what a ritual grows in a Principle Vat: the glowing liquid in the vat and the cluster of crystal prisms out of
 * it, at any stage of their growth. Shared by the Athanor while its ritual runs and by the grown vat afterwards, so the
 * cluster looks the same before and after the turn. Poses start at the vat's block corner.
 */
public final class CrystalDrawing {

    private static final Identifier CRYSTAL = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "block/ritual_crystal");
    private static final Identifier LIQUID = Identifier.withDefaultNamespace("block/water_still");
    /** Full light: the crystal and the liquid glow. */
    public static final int GLOW = LightCoordsUtil.pack(15, 15);
    private static final int SIDES = 6;
    /** The liquid's floor and walls in pixels; it keeps a twentieth of a pixel off the vat's floor and walls. */
    private static final float FLOOR = 1.05F;
    private static final float WALL_MIN = 3.05F;
    private static final float WALL_MAX = 12.95F;
    private static final int LIQUID_ALPHA = 0xE0;

    private CrystalDrawing() {
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    /** The surface of the liquid in the vat, {@code fill} of the way up from its floor to {@link CrystalCluster#SURFACE}. */
    public static void liquid(PoseStack poseStack, SubmitBuffers buffers, float fill, int color) {
        if (fill <= 0) {
            return;
        }
        TextureAtlasSprite sprite = sprite(LIQUID);
        float y = (FLOOR + fill * (CrystalCluster.SURFACE * 16 - FLOOR)) / 16;
        float min = WALL_MIN / 16;
        float max = WALL_MAX / 16;
        int argb = LIQUID_ALPHA << 24 | color & 0xFFFFFF;
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        float u0 = sprite.getU(WALL_MIN / 16);
        float u1 = sprite.getU(WALL_MAX / 16);
        float v0 = sprite.getV(WALL_MIN / 16);
        float v1 = sprite.getV(WALL_MAX / 16);
        vertex(consumer, pose, min, y, min, u0, v0, argb, 0, 1, 0);
        vertex(consumer, pose, min, y, max, u0, v1, argb, 0, 1, 0);
        vertex(consumer, pose, max, y, max, u1, v1, argb, 0, 1, 0);
        vertex(consumer, pose, max, y, min, u1, v0, argb, 0, 1, 0);
    }

    /**
     * The cluster grown {@code growth} of the way, 0 to 1, tinted {@code body}. Each vat turns its cluster by its position,
     * so neighbouring clusters differ.
     */
    public static void cluster(PoseStack poseStack, SubmitBuffers buffers, BlockPos vat, float growth, int body) {
        if (growth <= 0) {
            return;
        }
        TextureAtlasSprite sprite = sprite(CRYSTAL);
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        int argb = 0xFF000000 | body & 0xFFFFFF;
        float seed = Math.floorMod(vat.getX() * 7 + vat.getZ() * 13, SIDES);
        for (Spike spike : CrystalCluster.SPIKES) {
            float scale = CrystalCluster.scale(spike, growth);
            if (scale <= 0) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.5F, CrystalCluster.ROOT, 0.5F);
            poseStack.mulPose(Axis.YP.rotation(spike.turn() + seed));
            poseStack.mulPose(Axis.XP.rotation(spike.tilt()));
            poseStack.scale(scale, scale, scale);
            prism(consumer, poseStack.last(), spike, sprite, argb);
            poseStack.popPose();
        }
    }

    /**
     * One six sided prism standing on the pose's origin along +y: the body widening a little toward the root, closed at
     * the root, and a pointed tip on it. The body's top and the tip's base share their ring of corners and have no faces.
     */
    private static void prism(VertexConsumer consumer, PoseStack.Pose pose, Spike spike, TextureAtlasSprite sprite, int argb) {
        float body = spike.height() * CrystalCluster.BODY;
        float root = spike.radius() * CrystalCluster.ROOT_WIDENING;
        float top = spike.radius();
        float u0 = sprite.getU(0);
        float u1 = sprite.getU(1);
        float vTip = sprite.getV(0);
        float vShoulder = sprite.getV(1 - CrystalCluster.BODY);
        float vRoot = sprite.getV(1);
        for (int k = 0; k < SIDES; k++) {
            double a0 = Math.PI * 2 * k / SIDES;
            double a1 = Math.PI * 2 * (k + 1) / SIDES;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            Vector3f side = normal(c0 * root, 0, s0 * root, c0 * top, body, s0 * top, c1 * top, body, s1 * top);
            vertex(consumer, pose, c0 * root, 0, s0 * root, u0, vRoot, argb, side.x, side.y, side.z);
            vertex(consumer, pose, c0 * top, body, s0 * top, u0, vShoulder, argb, side.x, side.y, side.z);
            vertex(consumer, pose, c1 * top, body, s1 * top, u1, vShoulder, argb, side.x, side.y, side.z);
            vertex(consumer, pose, c1 * root, 0, s1 * root, u1, vRoot, argb, side.x, side.y, side.z);

            Vector3f tip = normal(c0 * top, body, s0 * top, 0, spike.height(), 0, c1 * top, body, s1 * top);
            float uMid = (u0 + u1) / 2;
            vertex(consumer, pose, c0 * top, body, s0 * top, u0, vShoulder, argb, tip.x, tip.y, tip.z);
            vertex(consumer, pose, 0, spike.height(), 0, uMid, vTip, argb, tip.x, tip.y, tip.z);
            vertex(consumer, pose, 0, spike.height(), 0, uMid, vTip, argb, tip.x, tip.y, tip.z);
            vertex(consumer, pose, c1 * top, body, s1 * top, u1, vShoulder, argb, tip.x, tip.y, tip.z);
        }
        for (int k = 0; k < SIDES; k += 2) {
            double a0 = Math.PI * 2 * k / SIDES;
            double a1 = Math.PI * 2 * (k + 1) / SIDES;
            double a2 = Math.PI * 2 * (k + 2) / SIDES;
            float uMid = (u0 + u1) / 2;
            vertex(consumer, pose, 0, 0, 0, uMid, vRoot, argb, 0, -1, 0);
            vertex(consumer, pose, (float) Math.cos(a0) * root, 0, (float) Math.sin(a0) * root, u0, vShoulder, argb, 0, -1, 0);
            vertex(consumer, pose, (float) Math.cos(a1) * root, 0, (float) Math.sin(a1) * root, uMid, vShoulder, argb, 0, -1, 0);
            vertex(consumer, pose, (float) Math.cos(a2) * root, 0, (float) Math.sin(a2) * root, u1, vShoulder, argb, 0, -1, 0);
        }
    }

    /** The outward normal of the face through three corners given counter clockwise seen from outside. */
    private static Vector3f normal(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz) {
        Vector3f ab = new Vector3f(bx - ax, by - ay, bz - az);
        Vector3f ac = new Vector3f(cx - ax, cy - ay, cz - az);
        return ab.cross(ac).normalize();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb,
                               float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(GLOW)
                .setNormal(pose, nx, ny, nz);
    }
}
