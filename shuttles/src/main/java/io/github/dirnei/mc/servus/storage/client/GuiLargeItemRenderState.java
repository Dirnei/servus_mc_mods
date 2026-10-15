package io.github.dirnei.mc.servus.storage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An item drawn large in a GUI, as a picture in picture so the 3D model is
 * rendered at full resolution instead of scaling up the 16 pixel slot icon.
 * {@link GuiLargeItemRenderer} draws it; {@code scale} is GUI pixels per block.
 */
public record GuiLargeItemRenderState(ItemStackRenderState item, int x0, int y0, int x1, int y1, float scale,
                                      @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
        implements PictureInPictureRenderState {

    /** Submits {@code stack} with its GUI transform, centred in the given rectangle, {@code size} GUI pixels per slot (16 is slot size). */
    public static void submit(GuiGraphicsExtractor graphics, ItemStack stack, int x0, int y0, int x1, int y1, float size) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState item = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(item, stack, ItemDisplayContext.GUI, minecraft.level, null, 0);
        ScreenRectangle scissor = graphics.peekScissorStack();
        graphics.submitPictureInPictureRenderState(new GuiLargeItemRenderState(item, x0, y0, x1, y1, size, scissor,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissor)));
    }
}
