package io.github.dirnei.mc.servus.delicacies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Draws items lying flat inside a kitchen block whose model is built facing north. */
final class FlatItems {

    private FlatItems() {
    }

    /** An item lying flat with its centre at {@code x, y, z} in pixels, {@code scale} of a block. */
    static void lying(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, float x, float y, float z, float scale,
                      Level level, int seed, int light) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(x / 16, y / 16, z / 16);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.scale(scale, scale, scale);
        RenderSubmit.item(poseStack, collector, stack, ItemDisplayContext.FIXED, level, seed, light);
        poseStack.popPose();
    }
}
