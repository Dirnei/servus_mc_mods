package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigWrench;
import io.github.dirnei.mc.servus.foundation.block.TankLabel;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The side panel every block with sides shows left of its GUI: a 3 by 3 grid of
 * relative sides, one colour per mode, a click cycles the side through the menu.
 * A block with both layers gets two tabs above the grid, Items and Fluids. A Fluids
 * cell shows its mode's colour and a corner chip for its tank choice; a right click
 * cycles the tank.
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
    private static final int CHIP = 5;
    private static final int RIGHT_BUTTON = 1;

    /** Colour of a Fluids side set to All tanks. */
    public static final int FLUID_ALL = 0xFF3F7FBF;

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
    private ToIntFunction<RelativeSide> fluidSides;
    private Supplier<List<TankLabel>> labels = List::of;
    private Predicate<RelativeSide> fluidLocked = side -> false;
    private Consumer<RelativeSide> fluidClick;
    private Consumer<RelativeSide> tankClick;
    private SideLayer layer;
    private int x;
    private int y;

    /**
     * A panel with the Items layer.
     *
     * @param modes  the current mode of a side, usually from the menu's data slots
     * @param locked which sides are fixed by the block and drawn greyed
     * @param click  what to do when a side is clicked, usually send its menu button
     */
    public SideConfigPanel(Function<RelativeSide, SideMode> modes, Predicate<RelativeSide> locked, Consumer<RelativeSide> click) {
        this.modes = modes;
        this.locked = locked;
        this.click = click;
        this.layer = SideLayer.ITEMS;
    }

    /** A panel with only the Fluids layer and no tabs; add the layer with {@link #enableFluids}. */
    public static SideConfigPanel fluidsOnly() {
        SideConfigPanel panel = new SideConfigPanel(null, side -> true, side -> { });
        panel.layer = SideLayer.FLUIDS;
        return panel;
    }

    /**
     * Adds the Fluids layer, with a tab when the panel also has the Items layer.
     *
     * @param packed    the packed Fluids side (see {@link FluidSides#packed}), usually from the menu's data slots
     * @param labels    the block's tank labels in index order, read when drawn
     * @param locked    which Fluids sides are fixed
     * @param modeClick what a left click on a Fluids side does, usually send its mode button
     * @param tankClick what a right click on a Fluids side does, usually send its tank button
     */
    public void enableFluids(ToIntFunction<RelativeSide> packed, Supplier<List<TankLabel>> labels, Predicate<RelativeSide> locked,
                             Consumer<RelativeSide> modeClick, Consumer<RelativeSide> tankClick) {
        this.fluidSides = packed;
        this.labels = labels;
        this.fluidLocked = locked;
        this.fluidClick = modeClick;
        this.tankClick = tankClick;
    }

    private boolean hasItems() {
        return modes != null;
    }

    private boolean hasFluids() {
        return fluidSides != null;
    }

    private boolean hasTabs() {
        return hasItems() && hasFluids();
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

    /** The colour of a tank choice, shared with the in world markers: the All colour or the tank label's. */
    public static int tankColor(int tank, List<TankLabel> labels) {
        return tank >= 0 && tank < labels.size() ? labels.get(tank).color() : FLUID_ALL;
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
        if (!hasTabs()) {
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

    private boolean showsFluids() {
        return layer == SideLayer.FLUIDS && hasFluids();
    }

    private boolean isLocked(RelativeSide side) {
        return showsFluids() ? fluidLocked.test(side) : locked.test(side);
    }

    private SideMode fluidMode(RelativeSide side) {
        SideMode mode = FluidSides.modeOf(fluidSides.applyAsInt(side));
        return mode == null ? SideMode.DISABLED : mode;
    }

    private int fluidTank(RelativeSide side) {
        return FluidSides.tankOf(fluidSides.applyAsInt(side));
    }

    private int cellColor(RelativeSide side) {
        return color(showsFluids() ? fluidMode(side) : modes.apply(side));
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (!hasItems() && !hasFluids()) {
            return;
        }
        if (hasTabs()) {
            SideLayer hoveredTab = tabAt(mouseX, mouseY);
            for (SideLayer tab : SideLayer.values()) {
                int tx = tabX(tab);
                int ty = tabY();
                boolean selected = tab == layer;
                graphics.fill(tx, ty, tx + TAB_WIDTH, y + (selected ? 1 : 0), 0xFF373737);
                int fill = selected ? 0xFFC6C6C6 : tab == hoveredTab ? 0xFFB0B0B0 : 0xFF8B8B8B;
                graphics.fill(tx + 1, ty + 1, tx + TAB_WIDTH - 1, y + (selected ? 1 : 0), fill);
                ItemStack icon = new ItemStack(tab == SideLayer.FLUIDS ? Items.WATER_BUCKET : Items.CHEST);
                graphics.item(icon, tx + (TAB_WIDTH - 16) / 2, ty + 1);
            }
        }
        graphics.fill(x, y, x + SIZE, y + SIZE, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, 0xFFC6C6C6);
        if (hasTabs()) {
            // Join the selected tab to the panel.
            int tx = tabX(layer);
            graphics.fill(tx + 1, y, tx + TAB_WIDTH - 1, y + 1, 0xFFC6C6C6);
        }
        RelativeSide hovered = sideAt(mouseX, mouseY);
        List<TankLabel> tankLabels = showsFluids() ? labels.get() : List.of();
        for (RelativeSide side : RelativeSide.values()) {
            int cx = cellX(side);
            int cy = cellY(side);
            boolean fixed = isLocked(side);
            graphics.fill(cx, cy, cx + CELL, cy + CELL, side == hovered && !fixed ? 0xFFFFFFFF : 0xFF555555);
            graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, fixed ? 0xFF6E6E6E : cellColor(side));
            graphics.text(font, side.name().substring(0, 1), cx + 4, cy + 3, fixed ? 0xFF9A9A9A : 0xFF202020, false);
            if (showsFluids() && !fixed) {
                renderTankChip(graphics, font, cx, cy, fluidTank(side), tankLabels);
            }
        }
    }

    /** A small square in the bottom right of a cell: the label colour of the tank, or an "A" for All. */
    private void renderTankChip(GuiGraphicsExtractor graphics, Font font, int cx, int cy, int tank, List<TankLabel> tankLabels) {
        int chipX = cx + CELL - 1 - CHIP;
        int chipY = cy + CELL - 1 - CHIP;
        graphics.fill(chipX - 1, chipY - 1, chipX + CHIP, chipY + CHIP, 0xFF202020);
        graphics.fill(chipX, chipY, chipX + CHIP, chipY + CHIP, tankColor(tank, tankLabels));
        if (tank == FluidSides.ALL) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(chipX + 1, chipY);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.text(font, "A", 1, 1, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
        }
    }

    /** Draws the hovered side's or tab's tooltip; returns whether one was drawn. */
    public boolean renderTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        SideLayer tab = tabAt(mouseX, mouseY);
        if (tab != null) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_foundation.side_tab", SideConfigWrench.layerName(tab)),
                    mouseX, mouseY);
            return true;
        }
        RelativeSide side = sideAt(mouseX, mouseY);
        if (side == null || (!hasItems() && !hasFluids())) {
            return false;
        }
        Component text = showsFluids()
                ? SideConfigWrench.fluidMessage(side, fluidMode(side), fluidTank(side), labels.get())
                : SideConfigWrench.message(side, modes.apply(side));
        if (isLocked(side)) {
            text = text.copy().append(" ").append(Component.translatable("gui.servus_foundation.side_fixed"));
        }
        graphics.setTooltipForNextFrame(font, text, mouseX, mouseY);
        return true;
    }

    /** Handles a left click; returns whether it hit a side or a tab. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        return mouseClicked(mouseX, mouseY, 0);
    }

    /** Handles a click with a mouse button; a right click on a Fluids side cycles its tank. Returns whether it hit a side or a tab. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SideLayer tab = tabAt(mouseX, mouseY);
        if (tab != null) {
            layer = tab;
            return true;
        }
        RelativeSide side = sideAt(mouseX, mouseY);
        if (side == null || (!hasItems() && !hasFluids())) {
            return false;
        }
        if (isLocked(side)) {
            return true;
        }
        if (!showsFluids()) {
            click.accept(side);
        } else if (button == RIGHT_BUTTON) {
            tankClick.accept(side);
        } else {
            fluidClick.accept(side);
        }
        return true;
    }
}
