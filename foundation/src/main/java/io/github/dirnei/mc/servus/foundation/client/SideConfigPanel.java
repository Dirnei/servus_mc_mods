package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigWrench;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The side panel every block with sides shows left of its GUI: a 3 by 3 grid of
 * relative sides, one colour per mode, a click cycles the side through the menu.
 */
public final class SideConfigPanel {

    public static final int CELL = 14;
    public static final int SIZE = CELL * 3 + 4;
    /** Gap between the panel and the GUI it sits beside. */
    public static final int GAP = 4;
    public static final int TOP = 17;

    /** Grid position (column, row) of each relative side. */
    private static final int[][] CELLS = {
            {1, 1}, // FRONT
            {1, 2}, // BACK
            {0, 1}, // LEFT
            {2, 1}, // RIGHT
            {1, 0}, // TOP
            {2, 2}, // BOTTOM
    };

    private final Function<RelativeSide, SideMode> modes;
    private final Predicate<RelativeSide> locked;
    private final Consumer<RelativeSide> click;
    private int x;
    private int y;

    /**
     * @param modes the current mode of a side, usually from the menu's data slots
     * @param click what to do when a side is clicked, usually send its menu button
     */
    public SideConfigPanel(Function<RelativeSide, SideMode> modes, Consumer<RelativeSide> click) {
        this(modes, side -> false, click);
    }

    /** @param locked which sides are fixed by the block and drawn greyed */
    public SideConfigPanel(Function<RelativeSide, SideMode> modes, Predicate<RelativeSide> locked, Consumer<RelativeSide> click) {
        this.modes = modes;
        this.locked = locked;
        this.click = click;
    }

    /** Places the panel left of a GUI whose top left corner is at the given position. */
    public void placeBeside(int guiLeft, int guiTop) {
        this.x = guiLeft - GAP - SIZE;
        this.y = guiTop + TOP;
    }

    /** The colour of a mode, shared with the in world markers. */
    public static int color(SideMode mode) {
        return switch (mode) {
            case AUTO -> 0xFF8B8B8B;
            case INPUT -> 0xFF4C7FD6;
            case OUTPUT -> 0xFFE08A2E;
            case BOTH -> 0xFF5CB85C;
            case PULL -> 0xFF7FC4F0;
            case PUSH -> 0xFFB85C1E;
            case DISABLED -> 0xFF7A2E2E;
        };
    }

    private int cellX(RelativeSide side) {
        return x + 2 + CELLS[side.ordinal()][0] * CELL;
    }

    private int cellY(RelativeSide side) {
        return y + 2 + CELLS[side.ordinal()][1] * CELL;
    }

    public RelativeSide sideAt(double mouseX, double mouseY) {
        for (RelativeSide side : RelativeSide.values()) {
            int cx = cellX(side);
            int cy = cellY(side);
            if (mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL) {
                return side;
            }
        }
        return null;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        graphics.fill(x, y, x + SIZE, y + SIZE, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, 0xFFC6C6C6);
        RelativeSide hovered = sideAt(mouseX, mouseY);
        for (RelativeSide side : RelativeSide.values()) {
            int cx = cellX(side);
            int cy = cellY(side);
            boolean fixed = locked.test(side);
            graphics.fill(cx, cy, cx + CELL, cy + CELL, side == hovered && !fixed ? 0xFFFFFFFF : 0xFF555555);
            graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, fixed ? 0xFF6E6E6E : color(modes.apply(side)));
            graphics.drawString(font, side.name().substring(0, 1), cx + 4, cy + 3, fixed ? 0xFF9A9A9A : 0xFF202020, false);
        }
    }

    /** Draws the hovered side's tooltip; returns whether one was drawn. */
    public boolean renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        RelativeSide side = sideAt(mouseX, mouseY);
        if (side == null) {
            return false;
        }
        Component text = SideConfigWrench.message(side, modes.apply(side));
        if (locked.test(side)) {
            text = text.copy().append(" ").append(Component.translatable("gui.servus_foundation.side_fixed"));
        }
        graphics.renderTooltip(font, text, mouseX, mouseY);
        return true;
    }

    /** Handles a click; returns whether it hit a side. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        RelativeSide side = sideAt(mouseX, mouseY);
        if (side == null) {
            return false;
        }
        if (!locked.test(side)) {
            click.accept(side);
        }
        return true;
    }
}
