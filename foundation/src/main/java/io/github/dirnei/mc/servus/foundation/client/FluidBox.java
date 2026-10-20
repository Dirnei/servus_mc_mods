package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * A box of fluid for block entity renderers, in block units within one block
 * (0 to 1 on every axis): each face is a quad with the fluid's still sprite
 * cut to the face, so a box smaller than a block shows the matching part of
 * the texture.
 */
public record FluidBox(float x0, float y0, float z0, float x1, float y1, float z1) {

    /** Draws the faces whose bit ({@code 1 << direction.ordinal()}) is set in {@code faces}. */
    public void draw(VertexConsumer consumer, PoseStack.Pose pose, int faces, TextureAtlasSprite sprite, int color, int light) {
        for (Direction face : Direction.values()) {
            if ((faces & (1 << face.ordinal())) != 0) {
                quad(consumer, pose, face, sprite, color, light);
            }
        }
    }

    /** One face of the box, counter clockwise seen from outside. */
    public void quad(VertexConsumer consumer, PoseStack.Pose pose, Direction face, TextureAtlasSprite sprite, int color, int light) {
        switch (face) {
            case UP -> {
                float u0 = sprite.getU(x0), u1 = sprite.getU(x1), v0 = sprite.getV(z0), v1 = sprite.getV(z1);
                vertex(consumer, pose, x0, y1, z0, u0, v0, color, light, face);
                vertex(consumer, pose, x0, y1, z1, u0, v1, color, light, face);
                vertex(consumer, pose, x1, y1, z1, u1, v1, color, light, face);
                vertex(consumer, pose, x1, y1, z0, u1, v0, color, light, face);
            }
            case DOWN -> {
                float u0 = sprite.getU(x0), u1 = sprite.getU(x1), v0 = sprite.getV(z0), v1 = sprite.getV(z1);
                vertex(consumer, pose, x0, y0, z0, u0, v0, color, light, face);
                vertex(consumer, pose, x1, y0, z0, u1, v0, color, light, face);
                vertex(consumer, pose, x1, y0, z1, u1, v1, color, light, face);
                vertex(consumer, pose, x0, y0, z1, u0, v1, color, light, face);
            }
            case NORTH -> {
                float u0 = sprite.getU(x0), u1 = sprite.getU(x1), v0 = sprite.getV(1 - y1), v1 = sprite.getV(1 - y0);
                vertex(consumer, pose, x1, y1, z0, u1, v0, color, light, face);
                vertex(consumer, pose, x1, y0, z0, u1, v1, color, light, face);
                vertex(consumer, pose, x0, y0, z0, u0, v1, color, light, face);
                vertex(consumer, pose, x0, y1, z0, u0, v0, color, light, face);
            }
            case SOUTH -> {
                float u0 = sprite.getU(x0), u1 = sprite.getU(x1), v0 = sprite.getV(1 - y1), v1 = sprite.getV(1 - y0);
                vertex(consumer, pose, x0, y1, z1, u0, v0, color, light, face);
                vertex(consumer, pose, x0, y0, z1, u0, v1, color, light, face);
                vertex(consumer, pose, x1, y0, z1, u1, v1, color, light, face);
                vertex(consumer, pose, x1, y1, z1, u1, v0, color, light, face);
            }
            case WEST -> {
                float u0 = sprite.getU(z0), u1 = sprite.getU(z1), v0 = sprite.getV(1 - y1), v1 = sprite.getV(1 - y0);
                vertex(consumer, pose, x0, y1, z0, u0, v0, color, light, face);
                vertex(consumer, pose, x0, y0, z0, u0, v1, color, light, face);
                vertex(consumer, pose, x0, y0, z1, u1, v1, color, light, face);
                vertex(consumer, pose, x0, y1, z1, u1, v0, color, light, face);
            }
            case EAST -> {
                float u0 = sprite.getU(z0), u1 = sprite.getU(z1), v0 = sprite.getV(1 - y1), v1 = sprite.getV(1 - y0);
                vertex(consumer, pose, x1, y1, z1, u1, v0, color, light, face);
                vertex(consumer, pose, x1, y0, z1, u1, v1, color, light, face);
                vertex(consumer, pose, x1, y0, z0, u0, v1, color, light, face);
                vertex(consumer, pose, x1, y1, z0, u0, v0, color, light, face);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int color, int light, Direction face) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
    }
}
