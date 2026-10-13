package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

/** Pose steps shared by block entity renderers that draw parts of north built models. */
public final class ModelPoses {

    private ModelPoses() {
    }

    /** Turns the block space about its centre the way a blockstate turns a north built model to {@code facing}. */
    public static void faceFromNorth(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180.0F) % 360.0F)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    /** Runs {@code draw} turned by {@code degrees} about an axis through a point given in pixels. */
    public static void rotateAbout(PoseStack poseStack, float x, float y, float z, Axis axis, float degrees, Runnable draw) {
        poseStack.pushPose();
        poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        poseStack.mulPose(axis.rotationDegrees(degrees));
        poseStack.translate(-x / 16.0F, -y / 16.0F, -z / 16.0F);
        draw.run();
        poseStack.popPose();
    }

    /** Runs {@code draw} moved by an offset in blocks. */
    public static void translated(PoseStack poseStack, float dx, float dy, float dz, Runnable draw) {
        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        draw.run();
        poseStack.popPose();
    }
}
