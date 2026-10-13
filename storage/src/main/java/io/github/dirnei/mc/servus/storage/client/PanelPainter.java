package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Paints vanilla style panels, insets, slot frames and boxes with fills. */
final class PanelPainter {

    static final int FACE = 0xFFC6C6C6;
    static final int SLOT = 0xFF8B8B8B;
    static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADE = 0xFF555555;
    private static final int OUTLINE = 0xFF000000;

    private PanelPainter() {
    }

    /** A raised grey panel with a black outline. */
    static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x + 1, y, x + w - 1, y + h, OUTLINE);
        graphics.fill(x, y + 1, x + w, y + h - 1, OUTLINE);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, FACE);
        graphics.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        graphics.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        graphics.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADE);
        graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADE);
    }

    /** A sunken area one pixel around a rectangle given relative to {@code left}, {@code top}, for lists and the stock grid. */
    static void inset(GuiGraphicsExtractor graphics, int left, int top, Rect rect) {
        int x = left + rect.x() - 1;
        int y = top + rect.y() - 1;
        int r = left + rect.right() + 1;
        int b = top + rect.bottom() + 1;
        graphics.fill(x, y, r, b, LIGHT);
        graphics.fill(x, y, r - 1, b - 1, DARK);
        graphics.fill(x + 1, y + 1, r - 1, b - 1, SLOT);
    }

    /** Slot frames for a block of slots whose first item origin is at the rectangle's corner. */
    static void slots(GuiGraphicsExtractor graphics, int left, int top, Rect rect, int columns, int count) {
        for (int i = 0; i < count; i++) {
            int x = left + rect.x() + (i % columns) * 18 - 1;
            int y = top + rect.y() + (i / columns) * 18 - 1;
            graphics.fill(x, y, x + 18, y + 18, LIGHT);
            graphics.fill(x, y, x + 17, y + 17, DARK);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
        }
    }

    /** The 27 inventory and 9 hotbar slot frames, nine wide from x 8, relative to {@code left}, {@code top}. */
    static void playerInventory(GuiGraphicsExtractor graphics, int left, int top, int inventoryY, int hotbarY) {
        slots(graphics, left, top, new Rect(8, inventoryY, 9 * 18, 3 * 18), 9, 27);
        slots(graphics, left, top, new Rect(8, hotbarY, 9 * 18, 18), 9, 9);
    }

    /** A sunken frame with one pixel edges, dark at the top and left, light at the bottom and right; absolute. */
    static void sunken(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, SLOT);
        graphics.fill(x, y, x + w, y + 1, DARK);
        graphics.fill(x, y, x + 1, y + h, DARK);
        graphics.fill(x, y + h - 1, x + w, y + h, LIGHT);
        graphics.fill(x + w - 1, y, x + w, y + h, LIGHT);
    }

    /** A filled box with a one pixel border around it; {@code x, y, w, h} is the inside, absolute. */
    static void bordered(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int border, int face) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        graphics.fill(x, y, x + w, y + h, face);
    }
}
