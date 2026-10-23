package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualTimeline;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RubedoObeliskBlock;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import org.joml.Vector3f;

/**
 * Draws the Rubedo Obelisk: a four sided tapered dark red column on the plinth with a pyramid tip, bands of light running
 * up it, a shard turning above the tip, and the glowing cracks of its Obsidian. Shared by the obelisk's renderer and by the
 * Athanor while the ritual raises it, so it looks the same before and after the turn. Poses start at the lower half's
 * block corner; sizes are in pixels.
 */
public final class ObeliskDrawing {

    private static final Identifier SHELL = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "block/rubedo_obelisk");
    private static final Identifier VEINS = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "block/rubedo_veins");
    private static final Identifier SHARD = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "block/ritual_crystal");
    private static final Identifier BAND = Identifier.withDefaultNamespace("block/white_concrete");

    private static final float BASE = RubedoObeliskBlock.PLINTH;
    private static final float SHOULDER = 27;
    private static final float APEX = 32;
    private static final float BASE_HALF = 4.5F;
    private static final float SHOULDER_HALF = 3;
    /** The shard's centre and size; its lowest point stays a pixel above the apex while it bobs. */
    private static final float SHARD_Y = 36;
    private static final float SHARD_RADIUS = 2.2F;
    private static final float SHARD_BOB = 1;
    private static final int SHARD_COLOR = 0xFF5030;
    private static final int BANDS = 3;
    private static final float BAND_HEIGHT = 0.8F;
    /** How far the bands and the cracks lie off the faces they cover, so they never share a plane with them. */
    private static final float LIFT = 0.1F;
    /** The side each turn of {@link #turnToSide} faces. */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};

    private ObeliskDrawing() {
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    /**
     * The obelisk at rest at game time {@code time}: its shell, its bands and its shard, and the cracks of its plinth on the
     * {@code open} sides.
     */
    public static void idle(PoseStack poseStack, SubmitBuffers buffers, float time, float phase, boolean[] open) {
        shell(poseStack, buffers, pulse(time, phase));
        bands(poseStack, buffers, time, phase);
        shard(poseStack, buffers, time, phase);
        cracks(poseStack, buffers, 0.7F + 0.3F * (float) Math.sin(time * 0.2F + phase), RubedoObeliskBlock.PLINTH, 1, open);
    }

    /**
     * Which sides of the block at {@code pos} show, in the order the cracks are drawn (north, west, south, east): those
     * whose neighbour does not hide them.
     */
    public static boolean[] openSides(BlockGetter level, BlockPos pos) {
        boolean[] open = new boolean[4];
        for (int side = 0; side < 4; side++) {
            open[side] = !level.getBlockState(pos.relative(SIDES[side])).isSolidRender();
        }
        return open;
    }

    /** A stable turn of the animations for the obelisk at {@code pos}, so neighbours do not move in step. */
    public static float phase(BlockPos pos) {
        return Math.floorMod(pos.getX() * 31 + pos.getZ() * 17, 64) / 64F * (float) (Math.PI * 2);
    }

    /**
     * The obelisk rising out of its Obsidian, {@code rise} of the way, 0 to 1, with an overshoot. It grows from the plinth's
     * top inside the Obsidian block, so it pushes out through the block's top; the shard pops free late in the rise.
     */
    public static void rising(PoseStack poseStack, SubmitBuffers buffers, float rise, float time, float phase) {
        if (rise <= 0) {
            return;
        }
        float tall = Math.max(0.0001F, RitualTimeline.easeBack(rise));
        float wide = Math.min(1, 0.3F + 0.7F * tall);
        poseStack.pushPose();
        poseStack.translate(0.5F, BASE / 16, 0.5F);
        poseStack.scale(wide, tall, wide);
        poseStack.translate(-0.5F, -BASE / 16, -0.5F);
        shell(poseStack, buffers, 1);
        if (rise > 0.6F) {
            shard(poseStack, buffers, time, phase);
        }
        poseStack.popPose();
    }

    /** How bright the core glows through the shell, 0.75 to 1, pulsing slowly. */
    private static float pulse(float time, float phase) {
        return 0.875F + 0.125F * (float) Math.sin(time * 0.11F + phase);
    }

    private static float halfWidth(float y) {
        return BASE_HALF + (SHOULDER_HALF - BASE_HALF) * (y - BASE) / (SHOULDER - BASE);
    }

    /** The four tapered sides and the four faces of the tip. It has no bottom: it stands on the plinth. */
    private static void shell(PoseStack poseStack, SubmitBuffers buffers, float brightness) {
        TextureAtlasSprite sprite = sprite(SHELL);
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        int level = Math.round(255 * brightness);
        int argb = 0xFF000000 | level << 16 | level << 8 | level;
        float u0 = sprite.getU(0);
        float u1 = sprite.getU(1);
        float vTop = sprite.getV(0);
        float vBottom = sprite.getV(1);
        float vApex = sprite.getV(0);
        float vShoulder = sprite.getV(0.3F);
        float uMid = (u0 + u1) / 2;
        for (int side = 0; side < 4; side++) {
            poseStack.pushPose();
            turnToSide(poseStack, side);
            PoseStack.Pose face = poseStack.last();
            float b = BASE_HALF / 16;
            float s = SHOULDER_HALF / 16;
            float y0 = BASE / 16;
            float y1 = SHOULDER / 16;
            float apex = APEX / 16;
            // The side facing -z after the turn, counter clockwise seen from outside.
            Vector3f body = normal(b, y0, -b, -b, y0, -b, -s, y1, -s);
            vertex(consumer, face, b, y0, -b, u0, vBottom, argb, body);
            vertex(consumer, face, -b, y0, -b, u1, vBottom, argb, body);
            vertex(consumer, face, -s, y1, -s, u1, vTop, argb, body);
            vertex(consumer, face, s, y1, -s, u0, vTop, argb, body);
            Vector3f tip = normal(s, y1, -s, -s, y1, -s, 0, apex, 0);
            vertex(consumer, face, s, y1, -s, u0, vShoulder, argb, tip);
            vertex(consumer, face, -s, y1, -s, u1, vShoulder, argb, tip);
            vertex(consumer, face, 0, apex, 0, uMid, vApex, argb, tip);
            vertex(consumer, face, 0, apex, 0, uMid, vApex, argb, tip);
            poseStack.popPose();
        }
    }

    /** Turns the pose about the obelisk's axis so side {@code side} faces -z, with the origin on the axis. */
    private static void turnToSide(PoseStack poseStack, int side) {
        poseStack.translate(0.5F, 0, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90 * side));
    }

    /** Light bands running up the sides one after another, fading in and out, a tenth of a pixel off the shell. */
    private static void bands(PoseStack poseStack, SubmitBuffers buffers, float time, float phase) {
        TextureAtlasSprite sprite = sprite(BAND);
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentBlockSheet());
        float u0 = sprite.getU(0.25F);
        float u1 = sprite.getU(0.75F);
        float v0 = sprite.getV(0.25F);
        float v1 = sprite.getV(0.75F);
        for (int k = 0; k < BANDS; k++) {
            float f = ((time * 0.025F + (float) k / BANDS + phase / (float) (Math.PI * 2)) % 1 + 1) % 1;
            float alpha = (float) Math.sin(f * Math.PI) * 0.9F;
            if (alpha <= 0.02F) {
                continue;
            }
            int argb = Math.round(alpha * 255) << 24 | RubedoObeliskBlock.HOT;
            float lower = BASE + 0.5F + f * (SHOULDER - BASE - 1.5F);
            float upper = lower + BAND_HEIGHT;
            float bl = (halfWidth(lower) + LIFT) / 16;
            float bu = (halfWidth(upper) + LIFT) / 16;
            float y0 = lower / 16;
            float y1 = upper / 16;
            for (int side = 0; side < 4; side++) {
                poseStack.pushPose();
                turnToSide(poseStack, side);
                PoseStack.Pose face = poseStack.last();
                Vector3f normal = normal(bl, y0, -bl, -bl, y0, -bl, -bu, y1, -bu);
                vertex(consumer, face, bl, y0, -bl, u0, v1, argb, normal);
                vertex(consumer, face, -bl, y0, -bl, u1, v1, argb, normal);
                vertex(consumer, face, -bu, y1, -bu, u1, v0, argb, normal);
                vertex(consumer, face, bu, y1, -bu, u0, v0, argb, normal);
                poseStack.popPose();
            }
        }
    }

    /** The shard, an eight sided double pyramid, floating and turning above the tip. */
    private static void shard(PoseStack poseStack, SubmitBuffers buffers, float time, float phase) {
        TextureAtlasSprite sprite = sprite(SHARD);
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        int argb = 0xFF000000 | SHARD_COLOR;
        float r = SHARD_RADIUS / 16;
        float u0 = sprite.getU(0);
        float u1 = sprite.getU(1);
        float uMid = (u0 + u1) / 2;
        float v0 = sprite.getV(0);
        float v1 = sprite.getV(1);
        poseStack.pushPose();
        poseStack.translate(0.5F, (SHARD_Y + SHARD_BOB * (float) Math.sin(time * 0.09F + phase)) / 16, 0.5F);
        poseStack.mulPose(Axis.YP.rotation(time * 0.06F + phase));
        for (int side = 0; side < 4; side++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90 * side));
            PoseStack.Pose face = poseStack.last();
            for (int end = -1; end <= 1; end += 2) {
                float tipY = end * r;
                // Upper faces run from the +x corner to the -z corner, lower ones the other way, so both face outward.
                float ax = end > 0 ? r : 0;
                float az = end > 0 ? 0 : -r;
                float bx = end > 0 ? 0 : r;
                float bz = end > 0 ? -r : 0;
                Vector3f normal = normal(ax, 0, az, bx, 0, bz, 0, tipY, 0);
                vertex(consumer, face, ax, 0, az, u0, v1, argb, normal);
                vertex(consumer, face, bx, 0, bz, u1, v1, argb, normal);
                vertex(consumer, face, 0, tipY, 0, uMid, v0, argb, normal);
                vertex(consumer, face, 0, tipY, 0, uMid, v0, argb, normal);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /**
     * The glowing cracks on the {@code sides} that show and the top of a block {@code height} pixels tall, a tenth of a
     * pixel off its faces, at {@code strength} of their glow, 0 to 1, run {@code open} of the way down from the top edge.
     */
    public static void cracks(PoseStack poseStack, SubmitBuffers buffers, float strength, float height, float open, boolean[] sides) {
        if (strength <= 0 || open <= 0) {
            return;
        }
        TextureAtlasSprite sprite = sprite(VEINS);
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentBlockSheet());
        int argb = Math.round(RitualTimeline.clamp01(strength) * 255) << 24 | 0xFFFFFF;
        float half = (8 + LIFT) / 16;
        float top = height / 16;
        float bottom = top - top * RitualTimeline.clamp01(open);
        float u0 = sprite.getU(0);
        float u1 = sprite.getU(1);
        float v0 = sprite.getV(0);
        float vOpen = sprite.getV(RitualTimeline.clamp01(open) * height / 16);
        for (int side = 0; side < 4; side++) {
            if (!sides[side]) {
                continue;
            }
            poseStack.pushPose();
            turnToSide(poseStack, side);
            PoseStack.Pose face = poseStack.last();
            Vector3f normal = new Vector3f(0, 0, -1);
            vertex(consumer, face, half, bottom, -half, u0, vOpen, argb, normal);
            vertex(consumer, face, -half, bottom, -half, u1, vOpen, argb, normal);
            vertex(consumer, face, -half, top, -half, u1, v0, argb, normal);
            vertex(consumer, face, half, top, -half, u0, v0, argb, normal);
            poseStack.popPose();
        }
        float lid = top + LIFT / 16;
        float inner = half * RitualTimeline.clamp01(open);
        float uIn0 = sprite.getU(0.5F - 0.5F * RitualTimeline.clamp01(open));
        float uIn1 = sprite.getU(0.5F + 0.5F * RitualTimeline.clamp01(open));
        float vIn0 = sprite.getV(0.5F - 0.5F * RitualTimeline.clamp01(open));
        float vIn1 = sprite.getV(0.5F + 0.5F * RitualTimeline.clamp01(open));
        PoseStack.Pose pose = poseStack.last();
        Vector3f up = new Vector3f(0, 1, 0);
        vertex(consumer, pose, 0.5F - inner, lid, 0.5F - inner, uIn0, vIn0, argb, up);
        vertex(consumer, pose, 0.5F - inner, lid, 0.5F + inner, uIn0, vIn1, argb, up);
        vertex(consumer, pose, 0.5F + inner, lid, 0.5F + inner, uIn1, vIn1, argb, up);
        vertex(consumer, pose, 0.5F + inner, lid, 0.5F - inner, uIn1, vIn0, argb, up);
    }

    /** The outward normal of the face through three corners given counter clockwise seen from outside. */
    private static Vector3f normal(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz) {
        Vector3f ab = new Vector3f(bx - ax, by - ay, bz - az);
        Vector3f ac = new Vector3f(cx - ax, cy - ay, cz - az);
        return ab.cross(ac).normalize();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb,
                               Vector3f normal) {
        consumer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(CrystalDrawing.GLOW)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }
}
