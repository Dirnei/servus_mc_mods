package io.github.dirnei.mc.servus.alchemy.client.sight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/**
 * Draws on the shell Elemental Sight lays around a Gnome Hollow: a cube a little larger than the block, its faces
 * pushed out by a layer's distance. Face coordinates run from 0 to 1 with u to the right and v down as seen from
 * outside, so a texture lies on every face the way a block model shows it. All layers share the in plane size of
 * the innermost one, so the faces of two neighbouring sides never meet in one plane.
 */
final class HollowShell {

    /** Distance of the rock layer from the block's faces. */
    static final float ROCK_OUT = 0.005F;
    /** Distance of the material layer, over the rock. */
    static final float MATERIAL_OUT = 0.009F;
    /** Distance of the cave layer, over the material. */
    static final float CAVE_OUT = 0.013F;
    /** One texture pixel in face coordinates. */
    static final float PIXEL = 1F / HollowLook.SIZE;
    private static final float SPREAD = 1 + 2 * ROCK_OUT;

    private final PoseStack.Pose pose;

    HollowShell(PoseStack.Pose pose) {
        this.pose = pose;
    }

    /** A point on {@code face} at face coordinates {@code u}, {@code v}, {@code out} from the block's face. */
    static Vector3f at(Direction face, float u, float v, float out) {
        float x;
        float y;
        float z;
        switch (face) {
            case NORTH -> {
                x = 1 - u;
                y = 1 - v;
                z = 0;
            }
            case SOUTH -> {
                x = u;
                y = 1 - v;
                z = 1;
            }
            case WEST -> {
                x = 0;
                y = 1 - v;
                z = u;
            }
            case EAST -> {
                x = 1;
                y = 1 - v;
                z = 1 - u;
            }
            case UP -> {
                x = u;
                y = 1;
                z = v;
            }
            default -> {
                x = u;
                y = 0;
                z = 1 - v;
            }
        }
        Direction.Axis axis = face.getAxis();
        return new Vector3f(place(x, axis == Direction.Axis.X, out), place(y, axis == Direction.Axis.Y, out),
                place(z, axis == Direction.Axis.Z, out));
    }

    /** Pushes a coordinate on the face's axis out by {@code out}, and spreads one in the face's plane to the shell's size. */
    private static float place(float value, boolean onFaceAxis, float out) {
        if (onFaceAxis) {
            return value == 0 ? -out : 1 + out;
        }
        return 0.5F + (value - 0.5F) * SPREAD;
    }

    /** The whole face at {@code out}, showing the whole texture. */
    void face(VertexConsumer consumer, Direction face, float out, int color, int light) {
        quad(consumer, face, 0, 0, 1, 1, out, 0, 0, 1, 1, color, light);
    }

    /** One texture pixel of the face at {@code out}, showing that pixel of the texture. */
    void pixel(VertexConsumer consumer, Direction face, int x, int y, float out, int color, int light) {
        float tu = (x + 0.5F) * PIXEL;
        float tv = (y + 0.5F) * PIXEL;
        quad(consumer, face, x * PIXEL, y * PIXEL, (x + 1) * PIXEL, (y + 1) * PIXEL, out, tu, tv, tu, tv, color, light);
    }

    /**
     * A box standing on one texture pixel of the face, from {@code base} out to {@code base + height}, every side
     * showing that pixel of the texture. Its foot lies on the layer below and is not drawn.
     */
    void box(VertexConsumer consumer, Direction face, int x, int y, float base, float height, int color, int light) {
        float u0 = x * PIXEL;
        float v0 = y * PIXEL;
        float u1 = u0 + PIXEL;
        float v1 = v0 + PIXEL;
        float tu = (x + 0.5F) * PIXEL;
        float tv = (y + 0.5F) * PIXEL;
        float top = base + height;
        quad(consumer, face, u0, v0, u1, v1, top, tu, tv, tu, tv, color, light);
        Vector3f centre = at(face, u0 + PIXEL / 2, v0 + PIXEL / 2, base);
        side(consumer, face, u0, v0, u0, v1, base, top, centre, tu, tv, color, light);
        side(consumer, face, u1, v0, u1, v1, base, top, centre, tu, tv, color, light);
        side(consumer, face, u0, v0, u1, v0, base, top, centre, tu, tv, color, light);
        side(consumer, face, u0, v1, u1, v1, base, top, centre, tu, tv, color, light);
    }

    /** A side of a box along the edge from (ua, va) to (ub, vb), lit as facing away from the box's centre. */
    private void side(VertexConsumer consumer, Direction face, float ua, float va, float ub, float vb, float base, float top,
                      Vector3f centre, float tu, float tv, int color, int light) {
        Vector3f normal = at(face, (ua + ub) / 2, (va + vb) / 2, base).sub(centre).normalize();
        vertex(consumer, at(face, ua, va, top), tu, tv, color, light, normal);
        vertex(consumer, at(face, ua, va, base), tu, tv, color, light, normal);
        vertex(consumer, at(face, ub, vb, base), tu, tv, color, light, normal);
        vertex(consumer, at(face, ub, vb, top), tu, tv, color, light, normal);
    }

    /** A quad on the face from (u0, v0) to (u1, v1), wound counter clockwise as seen from outside. */
    private void quad(VertexConsumer consumer, Direction face, float u0, float v0, float u1, float v1, float out,
                      float tu0, float tv0, float tu1, float tv1, int color, int light) {
        Vector3f normal = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
        vertex(consumer, at(face, u0, v0, out), tu0, tv0, color, light, normal);
        vertex(consumer, at(face, u0, v1, out), tu0, tv1, color, light, normal);
        vertex(consumer, at(face, u1, v1, out), tu1, tv1, color, light, normal);
        vertex(consumer, at(face, u1, v0, out), tu1, tv0, color, light, normal);
    }

    private void vertex(VertexConsumer consumer, Vector3f point, float u, float v, int color, int light, Vector3f normal) {
        consumer.addVertex(pose, point.x, point.y, point.z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }
}
