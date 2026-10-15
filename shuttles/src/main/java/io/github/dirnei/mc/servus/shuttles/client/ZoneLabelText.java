package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;

/** Draws a zone name as a small centred line of text in the renderer's current face space. */
final class ZoneLabelText {

    private static final int COLOUR = 0xFFF3C0;

    private ZoneLabelText() {
    }

    /**
     * @param x     centre of the text, in the current space
     * @param y     baseline height in the current space
     * @param z     depth in the current space, just outside the face
     * @param scale world units per text pixel
     */
    static void draw(String text, PoseStack poseStack, SubmitNodeCollector collector, Font font,
                     double x, double y, double z, float scale, int light) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(scale, -scale, scale);
        RenderSubmit.text(poseStack, collector, text, -font.width(text) / 2.0F, 0.0F, COLOUR, false, light);
        poseStack.popPose();
    }
}
