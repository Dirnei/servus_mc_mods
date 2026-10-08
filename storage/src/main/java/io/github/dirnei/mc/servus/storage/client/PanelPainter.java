package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import net.minecraft.client.gui.GuiGraphics;

/** Paints vanilla style panels, insets and slot frames straight from layout rectangles. */
final class PanelPainter {

    private static final int FACE = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADE = 0xFF555555;
    private static final int OUTLINE = 0xFF000000;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;

    private PanelPainter() {
    }

    /** A raised grey panel with a black outline. */
    static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 1, y, x + w - 1, y + h, OUTLINE);
        graphics.fill(x, y + 1, x + w, y + h - 1, OUTLINE);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, FACE);
        graphics.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        graphics.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        graphics.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADE);
        graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADE);
    }

    /** A sunken area, for lists and the stock grid background. */
    static void inset(GuiGraphics graphics, int left, int top, Rect rect) {
        int x = left + rect.x() - 1;
        int y = top + rect.y() - 1;
        int r = left + rect.right() + 1;
        int b = top + rect.bottom() + 1;
        graphics.fill(x, y, r, b, LIGHT);
        graphics.fill(x, y, r - 1, b - 1, SLOT_DARK);
        graphics.fill(x + 1, y + 1, r - 1, b - 1, SLOT);
    }

    /** Slot frames for a block of slots whose first item origin is at the rectangle's corner. */
    static void slots(GuiGraphics graphics, int left, int top, Rect rect, int columns, int count) {
        for (int i = 0; i < count; i++) {
            int x = left + rect.x() + (i % columns) * 18 - 1;
            int y = top + rect.y() + (i / columns) * 18 - 1;
            graphics.fill(x, y, x + 18, y + 18, LIGHT);
            graphics.fill(x, y, x + 17, y + 17, SLOT_DARK);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
        }
    }
}
