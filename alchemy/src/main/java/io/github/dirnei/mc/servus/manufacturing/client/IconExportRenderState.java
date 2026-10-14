package io.github.dirnei.mc.servus.manufacturing.client;

import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Dev only: the items {@link IconExportRenderer} draws as icons, submitted once as a picture in picture so the
 * drawing happens inside the GUI render pass. Its own GUI area is a single pixel at the origin.
 */
public record IconExportRenderState(List<Icon> icons, int x0, int y0, int x1, int y1, float scale,
                                    @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
        implements PictureInPictureRenderState {

    /** One item to export: its id and its GUI model. */
    public record Icon(Identifier id, ItemStackRenderState item) {
    }

    public static IconExportRenderState of(List<Icon> icons) {
        return new IconExportRenderState(List.copyOf(icons), 0, 0, 1, 1, 1.0F, null,
                PictureInPictureRenderState.getBounds(0, 0, 1, 1, null));
    }
}
