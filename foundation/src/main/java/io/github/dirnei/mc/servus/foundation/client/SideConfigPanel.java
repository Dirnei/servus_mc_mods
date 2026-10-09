package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigWrench;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The side panel every block with sides shows left of its GUI: a 3 by 3 grid of
 * relative sides, one colour per mode, a click cycles the side through the menu.
 * A block with a Fluids layer also gets two tabs above the grid, Items and Fluids;
 * the Fluids grid colours each side by the tank it is set to.
 */
public final class SideConfigPanel {

    public static final int CELL = 14;
    public static final int SIZE = CELL * 3 + 4;
    /** Gap between the panel and the GUI it sits beside. */
    public static final int GAP = 4;
    public static final int TOP = 17;
    /** Height of a layer tab above the grid. */
    public static final int TAB_HEIGHT = 18;
    private static final int TAB_WIDTH = SIZE / 2;

    /** Colour of a Fluids side set to All, and of one set to Off. */
    public static final int FLUID_ALL = 0xFF3F7FBF;
    public static final int FLUID_OFF = 0xFF7A2E2E;

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
    private ToIntFunction<RelativeSide> fluidValues;
    private List<SideTank> tanks = List.of();
    private Predicate<RelativeSide> fluidLocked = side -> false;
    private Consumer<RelativeSide> fluidClick;
    private SideLayer layer = SideLayer.ITEMS;
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

    /**
     * Adds the Fluids layer and its tab.
     *
     * @param values the Fluids layer value of a side, usually from the menu's data slots
     * @param tanks  the block's tanks in index order
     * @param locked which Fluids sides are fixed
     * @param click  what to do when a Fluids side is clicked, usually send its menu button
     */
    public void enableFluids(ToIntFunction<RelativeSide> values, List<SideTank> tanks, Predicate<RelativeSide> locked,
                             Consumer<RelativeSide> click) {
        this.fluidValues = values;
        this.tanks = tanks;
        this.fluidLocked = locked;
        this.fluidClick = click;
    }

    private boolean hasFluids() {
        return fluidValues != null;
    }

    public SideLayer layer() {
        return layer;
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

    /** The colour of a Fluids layer value, shared with the in world markers. */
    public static int fluidColor(int value, List<SideTank> tanks) {
        if (value == FluidSides.OFF) {
            return FLUID_OFF;
        }
        int tank = value - FluidSides.TANK_FIRST;
        return tank >= 0 && tank < tanks.size() ? 0xFF000000 | tanks.get(tank).color() : FLUID_ALL;
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

    private int tabX(SideLayer tab) {
        return x + tab.ordinal() * TAB_WIDTH;
    }

    private int tabY() {
        return y - TAB_HEIGHT + 1;
    }

    private SideLayer tabAt(double mouseX, double mouseY) {
        if (!hasFluids()) {
            return null;
        }
        for (SideLayer tab : SideLayer.values()) {
            int tx = tabX(tab);
            if (mouseX >= tx && mouseX < tx + TAB_WIDTH && mouseY >= tabY() && mouseY < y) {
                return tab;
            }
        }
        return null;
    }

    private boolean isLocked(RelativeSide side) {
        return layer == SideLayer.FLUIDS ? fluidLocked.test(side) : locked.test(side);
    }

    private int cellColor(RelativeSide side) {
        return layer == SideLayer.FLUIDS ? fluidColor(fluidValues.applyAsInt(side), tanks) : color(modes.apply(side));
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (hasFluids()) {
            SideLayer hoveredTab = tabAt(mouseX, mouseY);
            for (SideLayer tab : SideLayer.values()) {
                int tx = tabX(tab);
                int ty = tabY();
                boolean selected = tab == layer;
                graphics.fill(tx, ty, tx + TAB_WIDTH, y + (selected ? 1 : 0), 0xFF373737);
                int fill = selected ? 0xFFC6C6C6 : tab == hoveredTab ? 0xFFB0B0B0 : 0xFF8B8B8B;
                graphics.fill(tx + 1, ty + 1, tx + TAB_WIDTH - 1, y + (selected ? 1 : 0), fill);
                ItemStack icon = new ItemStack(tab == SideLayer.FLUIDS ? Items.WATER_BUCKET : Items.CHEST);
                graphics.renderItem(icon, tx + (TAB_WIDTH - 16) / 2, ty + 1);
            }
        }
        graphics.fill(x, y, x + SIZE, y + SIZE, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, 0xFFC6C6C6);
        if (hasFluids()) {
            // Join the selected tab to the panel.
            int tx = tabX(layer);
            graphics.fill(tx + 1, y, tx + TAB_WIDTH - 1, y + 1, 0xFFC6C6C6);
        }
        RelativeSide hovered = sideAt(mouseX, mouseY);
        for (RelativeSide side : RelativeSide.values()) {
            int cx = cellX(side);
            int cy = cellY(side);
            boolean fixed = isLocked(side);
            graphics.fill(cx, cy, cx + CELL, cy + CELL, side == hovered && !fixed ? 0xFFFFFFFF : 0xFF555555);
            graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, fixed ? 0xFF6E6E6E : cellColor(side));
            graphics.drawString(font, side.name().substring(0, 1), cx + 4, cy + 3, fixed ? 0xFF9A9A9A : 0xFF202020, false);
        }
    }

    /** Draws the hovered side's or tab's tooltip; returns whether one was drawn. */
    public boolean renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        SideLayer tab = tabAt(mouseX, mouseY);
        if (tab != null) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_foundation.side_tab", SideConfigWrench.layerName(tab)),
                    mouseX, mouseY);
            return true;
        }
        RelativeSide side = sideAt(mouseX, mouseY);
        if (side == null) {
            return false;
        }
        Component text = layer == SideLayer.FLUIDS
                ? SideConfigWrench.fluidMessage(side, fluidValues.applyAsInt(side), tanks)
                : SideConfigWrench.message(side, modes.apply(side));
        if (isLocked(side)) {
            text = text.copy().append(" ").append(Component.translatable("gui.servus_foundation.side_fixed"));
        }
        graphics.renderTooltip(font, text, mouseX, mouseY);
        return true;
    }

    /** Handles a click; returns whether it hit a side or a tab. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        SideLayer tab = tabAt(mouseX, mouseY);
        if (tab != null) {
            layer = tab;
            return true;
        }
        RelativeSide side = sideAt(mouseX, mouseY);
        if (side == null) {
            return false;
        }
        if (!isLocked(side)) {
            (layer == SideLayer.FLUIDS ? fluidClick : click).accept(side);
        }
        return true;
    }
}
