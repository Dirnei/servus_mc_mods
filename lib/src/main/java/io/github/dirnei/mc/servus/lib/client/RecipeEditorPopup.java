package io.github.dirnei.mc.servus.lib.client;

import io.github.dirnei.mc.servus.lib.block.RecipeEditorButtons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The recipe editor popup drawn over a machine GUI: the machine area dimmed,
 * a framed panel with a title, the frames of the 3 by 3 ghost grid (the menu
 * owns the slots), a preview of the matched result and Save, Delete and Cancel
 * buttons. All positions are relative to the GUI's top left corner. The Assembler
 * and the Crafting Station share it.
 */
public final class RecipeEditorPopup {

    /** A button of the popup, or none. */
    public enum Button {
        NONE, SAVE, DELETE, CANCEL
    }

    public static final int CELL = 18;
    public static final int WIDTH = 160;
    public static final int HEIGHT = 72;
    private static final int PREVIEW_OFFSET_X = 68;
    private static final int PREVIEW_OFFSET_Y = 34;
    private static final int BUTTON_OFFSET_X = 92;
    private static final int BUTTON_W = 62;
    private static final int BUTTON_H = 14;
    private static final int SAVE_OFFSET_Y = 16;
    private static final int DELETE_OFFSET_Y = 34;
    private static final int CANCEL_OFFSET_Y = 52;
    private static final int DIM = 0x90000000;

    private final int x;
    private final int y;
    private final int dimWidth;
    private final int dimHeight;

    /**
     * @param x         popup left edge
     * @param y         popup top edge
     * @param dimWidth  width of the machine area dimmed behind the popup
     * @param dimHeight height of the machine area dimmed behind the popup
     */
    public RecipeEditorPopup(int x, int y, int dimWidth, int dimHeight) {
        this.x = x;
        this.y = y;
        this.dimWidth = dimWidth;
        this.dimHeight = dimHeight;
    }

    /** Left edge of the ghost grid, where the menu puts its first pattern slot. */
    public int gridX() {
        return RecipeEditorButtons.gridX(x);
    }

    /** Top edge of the ghost grid. */
    public int gridY() {
        return RecipeEditorButtons.gridY(y);
    }

    public boolean previewHovered(int left, int top, double mouseX, double mouseY) {
        return in(left, top, mouseX, mouseY, x + PREVIEW_OFFSET_X, y + PREVIEW_OFFSET_Y, CELL, CELL);
    }

    /**
     * Draws the popup.
     *
     * @param existing whether an existing recipe is edited, which shows Delete
     * @param preview  the matched result, empty when the grid matches nothing
     */
    public void render(GuiGraphics graphics, Font font, int left, int top, int mouseX, int mouseY, Component title,
                       boolean existing, ItemStack preview, Component save, Component delete, Component cancel) {
        graphics.fill(left, top, left + dimWidth, top + dimHeight, DIM);
        int px = left + x;
        int py = top + y;
        graphics.fill(px + 2, py + 2, px + WIDTH + 2, py + HEIGHT + 2, DIM);
        graphics.fill(px, py, px + WIDTH, py + HEIGHT, 0xFF1E1E1E);
        graphics.fill(px + 1, py + 1, px + WIDTH - 1, py + HEIGHT - 1, 0xFFFFFFFF);
        graphics.fill(px + 2, py + 2, px + WIDTH - 2, py + HEIGHT - 2, 0xFFC6C6C6);
        graphics.drawString(font, title, px + 6, py + 4, 0xFF404040, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = left + gridX() + col * CELL - 1;
                int sy = top + gridY() + row * CELL - 1;
                graphics.fill(sx, sy, sx + CELL, sy + CELL, 0xFF373737);
                graphics.fill(sx + 1, sy + 1, sx + CELL - 1, sy + CELL - 1, 0xFF8B8B8B);
            }
        }
        int vx = px + PREVIEW_OFFSET_X;
        int vy = py + PREVIEW_OFFSET_Y;
        graphics.fill(vx - 1, vy - 1, vx + CELL - 1, vy + CELL - 1, 0xFF373737);
        graphics.fill(vx, vy, vx + CELL - 2, vy + CELL - 2, 0xFF8B8B8B);
        graphics.drawString(font, "=", vx - 8, vy + 4, 0xFF404040, false);
        if (!preview.isEmpty()) {
            graphics.renderItem(preview, vx, vy);
            graphics.renderItemDecorations(font, preview, vx, vy);
        }
        int bx = x + BUTTON_OFFSET_X;
        drawButton(graphics, font, left + bx, top + y + SAVE_OFFSET_Y, BUTTON_W, BUTTON_H, save.getString(),
                in(left, top, mouseX, mouseY, bx, y + SAVE_OFFSET_Y, BUTTON_W, BUTTON_H));
        if (existing) {
            drawButton(graphics, font, left + bx, top + y + DELETE_OFFSET_Y, BUTTON_W, BUTTON_H, delete.getString(),
                    in(left, top, mouseX, mouseY, bx, y + DELETE_OFFSET_Y, BUTTON_W, BUTTON_H));
        }
        drawButton(graphics, font, left + bx, top + y + CANCEL_OFFSET_Y, BUTTON_W, BUTTON_H, cancel.getString(),
                in(left, top, mouseX, mouseY, bx, y + CANCEL_OFFSET_Y, BUTTON_W, BUTTON_H));
    }

    /** The button under the mouse; Delete only counts when an existing recipe is edited. */
    public Button buttonAt(int left, int top, double mouseX, double mouseY, boolean existing) {
        int bx = x + BUTTON_OFFSET_X;
        if (in(left, top, mouseX, mouseY, bx, y + SAVE_OFFSET_Y, BUTTON_W, BUTTON_H)) {
            return Button.SAVE;
        }
        if (existing && in(left, top, mouseX, mouseY, bx, y + DELETE_OFFSET_Y, BUTTON_W, BUTTON_H)) {
            return Button.DELETE;
        }
        if (in(left, top, mouseX, mouseY, bx, y + CANCEL_OFFSET_Y, BUTTON_W, BUTTON_H)) {
            return Button.CANCEL;
        }
        return Button.NONE;
    }

    /** A flat button in the editor's style, at absolute screen coordinates. */
    public static void drawButton(GuiGraphics graphics, Font font, int x, int y, int w, int h, String label, boolean hovered) {
        graphics.fill(x, y, x + w, y + h, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFFDDDDDD : 0xFFA0A0A0);
        graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, 0xFF202020, false);
    }

    private static boolean in(int left, int top, double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= left + x && mouseX < left + x + w && mouseY >= top + y && mouseY < top + y + h;
    }
}
