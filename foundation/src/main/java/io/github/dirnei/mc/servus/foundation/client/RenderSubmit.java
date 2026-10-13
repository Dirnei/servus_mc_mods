package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Submits items and text from block entity renderers. */
public final class RenderSubmit {

    private RenderSubmit() {
    }

    /** Submits an item at the current pose. */
    public static void item(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, ItemDisplayContext context,
                            @Nullable Level level, int seed, int light) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, context, level, null, seed);
        state.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
    }

    /** Submits text at the current pose; an RGB colour without alpha is drawn opaque. */
    public static void text(PoseStack poseStack, SubmitNodeCollector collector, String text, float x, float y, int colour,
                            boolean shadow, int light) {
        collector.submitText(poseStack, x, y, Component.literal(text).getVisualOrderText(), shadow, Font.DisplayMode.NORMAL,
                light, Argb.opaque(colour), 0, 0);
    }
}
