package io.github.dirnei.mc.servus.foundation.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A popup drawn over a machine GUI that offers recipes as a grid of their results. The machine
 * area is dimmed, the panel sits centred over it, a scrollbar shows whenever there are more rows
 * than fit, and the selected entry is marked. The screen owns the open state and the tooltips;
 * all positions here are absolute screen coordinates.
 */
public final class RecipePickerPopup {

    public static final int CELL = 18;
    private static final int COLUMNS = 8;
    private static final int ROWS = 4;
    private static final int PAD = 6;
    private static final int TITLE_HEIGHT = 12;
    private static final int BAR_GAP = 3;
    private static final int BAR_WIDTH = 6;
    private static final int WIDTH = PAD + COLUMNS * CELL + BAR_GAP + BAR_WIDTH + PAD;
    private static final int HEIGHT = TITLE_HEIGHT + PAD + ROWS * CELL + PAD;
    private static final int DIM = 0x90000000;
    private static final int SELECTED = 0xFF5FA05F;
    private static final int HOVERED = 0x80FFFFFF;

    private int x;
    private int y;
    private int scroll;
    private boolean dragging;

    /** Centres the popup over a GUI area and resets the scroll so {@code selected} is visible. */
    public void open(int left, int top, int width, int height, int count, int selected) {
        x = left + (width - WIDTH) / 2;
        y = top + (height - HEIGHT) / 2;
        scroll = 0;
        dragging = false;
        if (selected >= 0) {
            scroll = Math.min(maxScroll(count), selected / COLUMNS);
        }
    }

    public void render(GuiGraphics graphics, Font font, int dimLeft, int dimTop, int dimWidth, int dimHeight,
                       Component title, List<ItemStack> results, int selected, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        graphics.fill(dimLeft, dimTop, dimLeft + dimWidth, dimTop + dimHeight, DIM);
        graphics.fill(x + 2, y + 2, x + WIDTH + 2, y + HEIGHT + 2, DIM);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF1E1E1E);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, 0xFFC6C6C6);
        graphics.drawString(font, title, x + PAD, y + 5, 0xFF404040, false);
        int gx = gridX();
        int gy = gridY();
        int hovered = indexAt(mouseX, mouseY, results.size());
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int sx = gx + col * CELL;
                int sy = gy + row * CELL;
                int index = (scroll + row) * COLUMNS + col;
                graphics.fill(sx, sy, sx + CELL, sy + CELL, 0xFF373737);
                graphics.fill(sx + 1, sy + 1, sx + CELL - 1, sy + CELL - 1, index == selected ? SELECTED : 0xFF8B8B8B);
                if (index < results.size()) {
                    graphics.renderItem(results.get(index), sx + 1, sy + 1);
                    if (index == hovered) {
                        graphics.fill(sx + 1, sy + 1, sx + CELL - 1, sy + CELL - 1, HOVERED);
                    }
                }
            }
        }
        int max = maxScroll(results.size());
        if (max > 0) {
            int bx = barX();
            graphics.fill(bx, gy, bx + BAR_WIDTH, gy + ROWS * CELL, 0xFF373737);
            int knob = knobHeight(results.size());
            int ky = gy + (ROWS * CELL - knob) * scroll / max;
            graphics.fill(bx + 1, ky + 1, bx + BAR_WIDTH - 1, ky + knob - 1, 0xFFDDDDDD);
        }
        graphics.pose().popPose();
    }

    /** Index of the recipe under the mouse, or -1. */
    public int indexAt(double mouseX, double mouseY, int count) {
        int gx = gridX();
        int gy = gridY();
        if (mouseX < gx || mouseX >= gx + COLUMNS * CELL || mouseY < gy || mouseY >= gy + ROWS * CELL) {
            return -1;
        }
        int index = (scroll + (int) ((mouseY - gy) / CELL)) * COLUMNS + (int) ((mouseX - gx) / CELL);
        return index < count ? index : -1;
    }

    /** Whether the point lies on the panel. */
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    /** Starts dragging when the scrollbar is clicked; returns whether it was. */
    public boolean clickBar(double mouseX, double mouseY, int count) {
        int bx = barX();
        int gy = gridY();
        if (maxScroll(count) > 0 && mouseX >= bx && mouseX < bx + BAR_WIDTH && mouseY >= gy && mouseY < gy + ROWS * CELL) {
            dragging = true;
            drag(mouseY, count);
            return true;
        }
        return false;
    }

    public void drag(double mouseY, int count) {
        if (!dragging) {
            return;
        }
        int max = maxScroll(count);
        int track = ROWS * CELL - knobHeight(count);
        double t = track <= 0 ? 0 : (mouseY - gridY() - knobHeight(count) / 2.0) / track;
        scroll = Math.max(0, Math.min(max, (int) Math.round(t * max)));
    }

    public void release() {
        dragging = false;
    }

    public void scroll(double amount, int count) {
        scroll = Math.max(0, Math.min(maxScroll(count), scroll - (int) Math.signum(amount)));
    }

    private int knobHeight(int count) {
        int rows = rows(count);
        return Math.max(8, ROWS * CELL * ROWS / Math.max(ROWS, rows));
    }

    private static int rows(int count) {
        return (count + COLUMNS - 1) / COLUMNS;
    }

    private static int maxScroll(int count) {
        return Math.max(0, rows(count) - ROWS);
    }

    private int gridX() {
        return x + PAD;
    }

    private int gridY() {
        return y + TITLE_HEIGHT + PAD;
    }

    private int barX() {
        return gridX() + COLUMNS * CELL + BAR_GAP;
    }
}
