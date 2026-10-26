package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The side tabs on top of the endpoint GUI: one fixed slot per side in the
 * order down, up, north, south, west, east, each tab showing the item of the
 * block beside that side. A side toward another duct shows a greyed tab that
 * cannot be selected; a side with nothing beside it shows no tab, leaving its
 * slot free so every side keeps its place. The selected tab joins the page.
 * Reads the client level every frame, so it follows blocks placed or broken
 * while the GUI is open.
 */
final class DuctSideTabs {

    static final int WIDTH = 26;
    static final int HEIGHT = 24;
    /** How far the selected tab reaches over the page's top border. */
    private static final int JOIN = 3;
    private static final int STRIDE = 28;
    private static final int INSET = 4;
    private static final int DARK = 0xFF373737;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int UNSELECTED = 0xFF9A9A9A;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int HOVERED = 0x60FFFFFF;
    private static final int GREYED = 0xB0707070;

    private final UtilityDuctBlockEntity duct;
    private int left;
    private int pageTop;

    DuctSideTabs(UtilityDuctBlockEntity duct) {
        this.duct = duct;
    }

    /** Places the strip on the top edge of the page at {@code pageLeft, pageTop}, absolute. */
    void place(int pageLeft, int pageTop) {
        this.left = pageLeft + INSET;
        this.pageTop = pageTop;
    }

    private int tabX(Direction side) {
        return left + side.ordinal() * STRIDE;
    }

    private int tabTop() {
        return pageTop - HEIGHT;
    }

    /** The side whose tab is under the mouse, or null; sides with nothing beside them have no tab. */
    Direction sideAt(double mouseX, double mouseY) {
        if (mouseY < tabTop() || mouseY >= pageTop) {
            return null;
        }
        for (Direction side : Direction.values()) {
            int x = tabX(side);
            if (mouseX >= x && mouseX < x + WIDTH && hasTab(side)) {
                return side;
            }
        }
        return null;
    }

    /** The tabs behind the page: every tab but the selected one. */
    void renderBehind(GuiGraphicsExtractor graphics, Direction selected, int mouseX, int mouseY) {
        Direction hovered = sideAt(mouseX, mouseY);
        for (Direction side : Direction.values()) {
            if (side == selected || !hasTab(side)) {
                continue;
            }
            int x = tabX(side);
            int top = tabTop() + 2;
            graphics.fill(x, top, x + WIDTH, pageTop, DARK);
            graphics.fill(x + 1, top + 1, x + WIDTH - 1, pageTop, UNSELECTED);
            graphics.fill(x + 1, top + 1, x + WIDTH - 1, top + 2, LIGHT);
            graphics.item(icon(side), x + (WIDTH - 16) / 2, top + 4);
            if (isDuct(side)) {
                graphics.fill(x + 1, top + 1, x + WIDTH - 1, pageTop, GREYED);
            } else if (side == hovered) {
                graphics.fill(x + 1, top + 1, x + WIDTH - 1, pageTop, HOVERED);
            }
        }
    }

    /** The selected tab, drawn after the page so it covers the page's top border and joins it. */
    void renderSelected(GuiGraphicsExtractor graphics, Direction selected) {
        if (selected == null || !hasTab(selected)) {
            return;
        }
        int x = tabX(selected);
        int top = tabTop();
        graphics.fill(x, top, x + WIDTH, pageTop + JOIN, DARK);
        graphics.fill(x + 1, top + 1, x + WIDTH - 1, pageTop + JOIN, PANEL);
        graphics.fill(x + 1, top + 1, x + WIDTH - 1, top + 2, LIGHT);
        graphics.fill(x + 1, top + 1, x + 2, pageTop + JOIN, LIGHT);
        graphics.item(icon(selected), x + (WIDTH - 16) / 2, top + 5);
    }

    /** The tooltip of the hovered tab: the side's name and what is beside it. Returns whether one was drawn. */
    boolean renderTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        Direction side = sideAt(mouseX, mouseY);
        if (side == null) {
            return false;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(sideName(side));
        if (isDuct(side)) {
            lines.add(Component.translatable("gui.servus_foundation.side_picker.duct").withColor(0x808080));
        } else {
            lines.add(duct.touchedBlockName(side));
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        return true;
    }

    static Component sideName(Direction side) {
        return Component.translatable("gui.servus_foundation.side." + side.getSerializedName());
    }

    /** Whether the side gets a tab: a block other than a duct (selectable) or another duct (greyed) is beside it. */
    private boolean hasTab(Direction side) {
        return duct.isSelectable(side) || isDuct(side);
    }

    private boolean isDuct(Direction side) {
        Level level = duct.getLevel();
        return level != null && level.getBlockEntity(duct.getBlockPos().relative(side)) instanceof UtilityDuctBlockEntity;
    }

    /** The pick block item of the neighbour, its block item as a fallback, empty for air or a block without one. */
    private ItemStack icon(Direction side) {
        Level level = duct.getLevel();
        if (level == null) {
            return ItemStack.EMPTY;
        }
        BlockPos pos = duct.getBlockPos().relative(side);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return ItemStack.EMPTY;
        }
        ItemStack picked = ItemStack.EMPTY;
        try {
            picked = state.getBlock().getCloneItemStack(level, pos, state, false, Minecraft.getInstance().player);
        } catch (RuntimeException e) {
            // a modded block whose pick block fails still gets its plain block item
        }
        return picked.isEmpty() ? new ItemStack(state.getBlock().asItem()) : picked;
    }
}
