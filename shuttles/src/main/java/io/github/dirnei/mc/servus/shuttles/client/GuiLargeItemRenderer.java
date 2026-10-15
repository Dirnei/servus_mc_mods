package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Renders a {@link GuiLargeItemRenderState} into its own texture, the way
 * vanilla's oversized item renderer does: GUI lighting, the item centred and
 * flipped into GUI space.
 */
public final class GuiLargeItemRenderer extends PictureInPictureRenderer<GuiLargeItemRenderState> {

    private static final int FULL_BRIGHT = 15728880;

    public GuiLargeItemRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<GuiLargeItemRenderState> getRenderStateClass() {
        return GuiLargeItemRenderState.class;
    }

    @Override
    protected void renderToTexture(GuiLargeItemRenderState state, PoseStack poseStack) {
        poseStack.scale(1.0F, -1.0F, -1.0F);
        Minecraft.getInstance().gameRenderer.getLighting()
                .setupFor(state.item().usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
        FeatureRenderDispatcher dispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        state.item().submit(poseStack, dispatcher.getSubmitNodeStorage(), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        dispatcher.renderAllFeatures();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "servus_large_item";
    }
}
