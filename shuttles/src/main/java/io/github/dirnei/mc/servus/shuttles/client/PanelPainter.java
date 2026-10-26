package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Paints vanilla style panels, insets, slot frames and boxes with fills. */
final class PanelPainter {

    static final int FACE = 0xFFC6C6C6;
    static final int SLOT = 0xFF8B8B8B;
    static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADE = 0xFF555555;
    private static final int OUTLINE = 0xFF000000;
    private static final int TAB_INACTIVE = 0xFF9A9A9A;

    private PanelPainter() {
    }

    /** A flat button drawn into a list line, lighter while hovered; absolute coordinates. */
    static void flatButton(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, String label, boolean hovered) {
        graphics.fill(x, y, x + w, y + h, DARK);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFFDDDDDD : 0xFFA0A0A0);
        graphics.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, 0xFF202020, false);
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
        playerInventory(graphics, left, top, 8, inventoryY, hotbarY);
    }

    /** The 27 inventory and 9 hotbar slot frames, nine wide from {@code inventoryX}, relative to {@code left}, {@code top}. */
    static void playerInventory(GuiGraphicsExtractor graphics, int left, int top, int inventoryX, int inventoryY, int hotbarY) {
        slots(graphics, left, top, new Rect(inventoryX, inventoryY, 9 * 18, 3 * 18), 9, 27);
        slots(graphics, left, top, new Rect(inventoryX, hotbarY, 9 * 18, 18), 9, 9);
    }

    /** A sunken frame with one pixel edges, dark at the top and left, light at the bottom and right; absolute. */
    static void sunken(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, SLOT);
        graphics.fill(x, y, x + w, y + 1, DARK);
        graphics.fill(x, y, x + 1, y + h, DARK);
        graphics.fill(x, y + h - 1, x + w, y + h, LIGHT);
        graphics.fill(x + w - 1, y, x + w, y + h, LIGHT);
    }

    /** A bar with a one pixel frame in {@code background}, filled from the left by value of max; absolute. */
    static void bar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, long value, long max, int colour, int background) {
        graphics.fill(x, y, x + w, y + h, background);
        int filled = max <= 0 ? 0 : (int) Math.min(w - 2, Math.max(0, value) * (w - 2) / max);
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, colour);
    }

    /** A filled box with a one pixel border around it; {@code x, y, w, h} is the inside, absolute. */
    static void bordered(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int border, int face) {
        PopupPanel.bordered(graphics, x, y, w, h, border, face);
    }

    /**
     * A tab on the top edge of a panel, creative tab style; absolute. The
     * active tab is drawn in the face colour and reaches {@code join} pixels
     * down into the panel to cover its frame there, so tab and panel read as
     * one; the others are darker and 2 pixels lower.
     */
    static void tab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean active, int join) {
        int top = active ? y : y + 2;
        graphics.fill(x, top, x + w, y + h, OUTLINE);
        int bottom = active ? y + h + join : y + h;
        graphics.fill(x + 1, top + 1, x + w - 1, bottom, active ? FACE : TAB_INACTIVE);
        if (active) {
            graphics.fill(x + 1, top + 1, x + w - 1, top + 2, LIGHT);
            graphics.fill(x + 1, top + 1, x + 2, bottom, LIGHT);
        }
    }

    /** Half size text right aligned at {@code right}, drawn above items; absolute. */
    static void smallText(GuiGraphicsExtractor graphics, Font font, Component text, int right, int y, int colour) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(right - font.width(text) * 0.5F, y);
        graphics.pose().scale(0.5F, 0.5F);
        graphics.text(font, text, 0, 0, colour, true);
        graphics.pose().popMatrix();
    }
}
