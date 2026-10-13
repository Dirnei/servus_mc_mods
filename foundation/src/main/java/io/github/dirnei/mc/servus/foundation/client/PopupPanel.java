package io.github.dirnei.mc.servus.foundation.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The look shared by popups drawn over a GUI: the area behind dimmed, a framed light panel with a drop shadow. */
public final class PopupPanel {

    private static final int DIM = 0x90000000;

    private PopupPanel() {
    }

    /** Dims the GUI area {@code dimX, dimY, dimWidth, dimHeight}, then draws the panel; absolute screen coordinates. */
    public static void draw(GuiGraphicsExtractor graphics, int dimX, int dimY, int dimWidth, int dimHeight, int x, int y, int width, int height) {
        graphics.fill(dimX, dimY, dimX + dimWidth, dimY + dimHeight, DIM);
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, DIM);
        graphics.fill(x, y, x + width, y + height, 0xFF1E1E1E);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFC6C6C6);
    }
}
