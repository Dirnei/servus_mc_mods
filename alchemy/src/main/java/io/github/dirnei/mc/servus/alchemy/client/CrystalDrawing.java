package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower.Box;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower.Bud;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower.Face;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CitrineTower.Shard;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.CrystalRamp;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockGetter;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws what a ritual grows in a Principle Vat: the glowing liquid in the vat and the citrine tower with its buds out of it,
 * at any stage of their growth. Shared by the Athanor while its ritual runs and by the grown vat afterwards, so the tower
 * looks the same before and after the turn. Poses start at the vat's block corner.
 */
public final class CrystalDrawing {

    private static final Identifier CRYSTAL = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "block/ritual_crystal");
    private static final Identifier LIQUID = Identifier.withDefaultNamespace("block/water_still");
    /** Full light: the crystal and the liquid glow. */
    public static final int GLOW = LightCoordsUtil.pack(15, 15);
    /** The surface of the full vat in pixels above its floor, below its rim at 12. */
    private static final float SURFACE = 10;
    /** The liquid's floor and walls in pixels; it keeps a twentieth of a pixel off the vat's floor and walls. */
    private static final float FLOOR = 1.05F;
    private static final float WALL_MIN = 3.05F;
    private static final float WALL_MAX = 12.95F;
    private static final int LIQUID_ALPHA = 0xE0;
    /** The light the crystal is shaded by, from above and a little behind the left. */
    private static final Vector3f LIGHT = new Vector3f(-0.5F, 0.9F, -0.35F).normalize();
    private static final Vector3f DOWN = new Vector3f(0, -1, 0);

    private CrystalDrawing() {
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    /** The surface of the liquid in the vat, {@code fill} of the way up from its floor to the full vat's surface. */
    public static void liquid(PoseStack poseStack, SubmitBuffers buffers, float fill, int color) {
        if (fill <= 0) {
            return;
        }
        TextureAtlasSprite sprite = sprite(LIQUID);
        float y = (FLOOR + fill * (SURFACE - FLOOR)) / 16;
        float min = WALL_MIN / 16;
        float max = WALL_MAX / 16;
        int argb = LIQUID_ALPHA << 24 | color & 0xFFFFFF;
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        float u0 = sprite.getU(WALL_MIN / 16);
        float u1 = sprite.getU(WALL_MAX / 16);
        float v0 = sprite.getV(WALL_MIN / 16);
        float v1 = sprite.getV(WALL_MAX / 16);
        vertex(consumer, pose, min, y, min, u0, v0, argb);
        vertex(consumer, pose, min, y, max, u0, v1, argb);
        vertex(consumer, pose, max, y, max, u1, v1, argb);
        vertex(consumer, pose, max, y, min, u1, v0, argb);
    }

    /**
     * The tower grown {@code growth} of the way, 0 to 1, shaded along {@code ramp}. Buds grow only on the sides where the block
     * beside the vat is air, and each vat grows its own tower by its position.
     */
    public static void tower(PoseStack poseStack, SubmitBuffers buffers, BlockGetter level, BlockPos vat, float growth, CrystalRamp ramp) {
        if (growth <= 0) {
            return;
        }
        CitrineTower.Shape shape = CitrineTower.grown(CitrineTower.seed(vat.getX(), vat.getZ()), growth,
                (face, height) -> level.getBlockState(vat.offset(face.dx(), height, face.dz())).isAir());
        TextureAtlasSprite sprite = sprite(CRYSTAL);
        Cells cells = new Cells(buffers.getBuffer(Sheets.cutoutBlockSheet()), sprite.getU(0.5F), sprite.getV(0.5F), ramp);
        poseStack.pushPose();
        poseStack.scale(1 / 16F, 1 / 16F, 1 / 16F);
        int segments = shape.tower().size();
        for (int i = 0; i < segments; i++) {
            Box box = shape.tower().get(i);
            boolean tip = i >= CitrineTower.SEGMENTS;
            cells.box(poseStack.last(), new Quaternionf(), box, (face, column, row, columns) -> tip ? 0.92F
                    : face == Face.UP ? 0.74F : facet(column, row + (int) box.y(), columns), 0.72F);
        }
        for (Bud bud : shape.buds()) {
            bud(poseStack, cells, bud);
        }
        poseStack.popPose();
    }

    /** A bud: its shards turned from the face's frame by its lean toward up and its spin about its own axis. */
    private static void bud(PoseStack poseStack, Cells cells, Bud bud) {
        Quaternionf frame = frame(bud.face()).rotateX(-bud.lean()).rotateY(bud.spin());
        for (Shard shard : bud.shards()) {
            Quaternionf rotation = new Quaternionf(frame);
            poseStack.pushPose();
            poseStack.translate(bud.x(), bud.y(), bud.z());
            poseStack.mulPose(frame);
            poseStack.translate(shard.x(), 0, shard.z());
            Quaternionf tilt = new Quaternionf().rotateZ(shard.tiltZ()).rotateX(shard.tiltX());
            poseStack.mulPose(tilt);
            rotation.mul(tilt);
            int width = shard.width();
            Box body = new Box(-width / 2F, 0, -width / 2F, width, shard.body(), width);
            cells.box(poseStack.last(), rotation, body, (face, column, row, columns) -> shardTone(face, column, columns), 0.8F);
            if (shard.tipWidth() > 0) {
                int tip = shard.tipWidth();
                cells.box(poseStack.last(), rotation, new Box(-tip / 2F, shard.body(), -tip / 2F, tip, shard.tipHeight(), tip),
                        (face, column, row, columns) -> 1, 0.8F);
            }
            poseStack.popPose();
        }
    }

    /**
     * The turn from a bud's own frame, where it grows along +y, to the world: up for a bud on a step; for a bud on a side its
     * axis points out of the side and its +z points down, so a lean about x tips it upward on every side.
     */
    private static Quaternionf frame(Face face) {
        if (face == Face.UP) {
            return new Quaternionf();
        }
        Vector3f out = new Vector3f(face.dx(), 0, face.dz());
        Vector3f across = out.cross(DOWN, new Vector3f());
        return new Quaternionf().setFromNormalized(new Matrix3f(across, out, DOWN));
    }

    /** Lighter and darker bands of facets running up the tower's sides, with dark edges. */
    private static float facet(int column, int row, int columns) {
        if (column == 0 || column == columns - 1) {
            return 0.5F;
        }
        if ((column * 7 + row * 13) % 29 == 0) {
            return 0.98F;
        }
        int band = Math.floorMod((column + row / 5) / 3, 3);
        return new float[] {0.62F, 0.72F, 0.8F}[band];
    }

    /** A shard's sides are dark at the edges and bright down the middle; its top is bright. */
    private static float shardTone(Face face, int column, int columns) {
        if (face == Face.UP) {
            return 1;
        }
        if (columns >= 3 && (column == 0 || column == columns - 1)) {
            return 0.55F;
        }
        return columns >= 3 && column == columns / 2 ? 1 : 0.8F;
    }

    @FunctionalInterface
    private interface Tone {
        float of(Face face, int column, int row, int columns);
    }

    /** Draws boxes as one quad per texel, each coloured along the ramp by its tone and how much its side faces the light. */
    private record Cells(VertexConsumer consumer, float u, float v, CrystalRamp ramp) {

        /**
         * The box's top and four sides; its bottom always rests on something. {@code rotation} turns the box's sides into the
         * world, for the light; {@code ambient} is the share of brightness a side gets without light.
         */
        void box(PoseStack.Pose pose, Quaternionf rotation, Box box, Tone tone, float ambient) {
            float x0 = box.x();
            float y0 = box.y();
            float z0 = box.z();
            float x1 = x0 + box.width();
            float y1 = y0 + box.height();
            float z1 = z0 + box.depth();
            side(pose, rotation, tone, ambient, Face.UP, x0, y1, z1, 1, 0, 0, 0, 0, -1, box.width(), box.depth());
            side(pose, rotation, tone, ambient, Face.NORTH, x1, y0, z0, -1, 0, 0, 0, 1, 0, box.width(), box.height());
            side(pose, rotation, tone, ambient, Face.SOUTH, x0, y0, z1, 1, 0, 0, 0, 1, 0, box.width(), box.height());
            side(pose, rotation, tone, ambient, Face.WEST, x0, y0, z0, 0, 0, 1, 0, 1, 0, box.depth(), box.height());
            side(pose, rotation, tone, ambient, Face.EAST, x1, y0, z1, 0, 0, -1, 0, 1, 0, box.depth(), box.height());
        }

        /**
         * One side from corner {@code o} along {@code a} (columns, left to right seen from outside) and {@code b} (rows,
         * upward), split into texels.
         */
        private void side(PoseStack.Pose pose, Quaternionf rotation, Tone tone, float ambient, Face face, float ox, float oy, float oz,
                          float ax, float ay, float az, float bx, float by, float bz, float lengthA, float lengthB) {
            int columns = Math.max(1, Math.round(lengthA));
            int rows = Math.max(1, Math.round(lengthB));
            float stepA = lengthA / columns;
            float stepB = lengthB / rows;
            Vector3f normal = rotation.transform(new Vector3f(face == Face.UP ? 0 : face.dx(), face == Face.UP ? 1 : 0, face.dz()));
            float lit = ambient + 0.35F * Math.max(0, normal.dot(LIGHT));
            for (int column = 0; column < columns; column++) {
                for (int row = 0; row < rows; row++) {
                    int argb = 0xFF000000 | ramp.at(tone.of(face, column, row, columns) * lit);
                    float ca = column * stepA;
                    float cb = row * stepB;
                    float px = ox + ax * ca + bx * cb;
                    float py = oy + ay * ca + by * cb;
                    float pz = oz + az * ca + bz * cb;
                    float dax = ax * stepA;
                    float day = ay * stepA;
                    float daz = az * stepA;
                    float dbx = bx * stepB;
                    float dby = by * stepB;
                    float dbz = bz * stepB;
                    vertex(consumer, pose, px, py, pz, u, v, argb);
                    vertex(consumer, pose, px + dax, py + day, pz + daz, u, v, argb);
                    vertex(consumer, pose, px + dax + dbx, py + day + dby, pz + daz + dbz, u, v, argb);
                    vertex(consumer, pose, px + dbx, py + dby, pz + dbz, u, v, argb);
                }
            }
        }
    }

    /**
     * Every vertex points its normal straight up in the world: the entity shader then gives it full light, and the shading
     * comes from the ramp alone instead of darkening the colours toward brown.
     */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
        consumer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(GLOW)
                .setNormal(0, 1, 0);
    }
}
