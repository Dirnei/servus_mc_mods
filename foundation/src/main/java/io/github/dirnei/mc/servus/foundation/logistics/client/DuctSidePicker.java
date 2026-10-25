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
 * The side picker row of the endpoint GUI: one button per side showing the
 * item of the block beside it. Sides toward another duct show that duct greyed,
 * empty sides an empty frame; neither can be picked. Reads the client level
 * every frame, so it follows blocks placed or broken while the GUI is open.
 */
final class DuctSidePicker {

    static final int SIZE = 20;
    private static final int GAP = 4;
    private static final int DARK = 0xFF373737;
    private static final int FACE = 0xFF8B8B8B;
    private static final int SELECTED = 0xFFFFFFFF;
    private static final int HOVERED = 0x60FFFFFF;
    private static final int GREYED = 0xB0707070;

    private final UtilityDuctBlockEntity duct;
    private int left;
    private int top;

    DuctSidePicker(UtilityDuctBlockEntity duct) {
        this.duct = duct;
    }

    /** Width of the whole row. */
    static int width() {
        return Direction.values().length * (SIZE + GAP) - GAP;
    }

    /** Where the row starts, absolute. */
    void place(int x, int y) {
        left = x;
        top = y;
    }

    private int buttonX(Direction side) {
        return left + side.ordinal() * (SIZE + GAP);
    }

    /** The side whose button is under the mouse, or null. */
    Direction sideAt(double mouseX, double mouseY) {
        if (mouseY < top || mouseY >= top + SIZE) {
            return null;
        }
        for (Direction side : Direction.values()) {
            int x = buttonX(side);
            if (mouseX >= x && mouseX < x + SIZE) {
                return side;
            }
        }
        return null;
    }

    void render(GuiGraphicsExtractor graphics, Direction selected, int mouseX, int mouseY) {
        Direction hovered = sideAt(mouseX, mouseY);
        for (Direction side : Direction.values()) {
            int x = buttonX(side);
            graphics.fill(x, top, x + SIZE, top + SIZE, side == selected ? SELECTED : DARK);
            graphics.fill(x + 1, top + 1, x + SIZE - 1, top + SIZE - 1, FACE);
            ItemStack icon = icon(side);
            if (!icon.isEmpty()) {
                graphics.item(icon, x + 2, top + 2);
            }
            if (isDuct(side)) {
                graphics.fill(x + 1, top + 1, x + SIZE - 1, top + SIZE - 1, GREYED);
            } else if (side == hovered && duct.isSelectable(side)) {
                graphics.fill(x + 1, top + 1, x + SIZE - 1, top + SIZE - 1, HOVERED);
            }
        }
    }

    /** The tooltip of the hovered button: the side's name and what is beside it. Returns whether one was drawn. */
    boolean renderTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        Direction side = sideAt(mouseX, mouseY);
        if (side == null) {
            return false;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(sideName(side));
        if (isDuct(side)) {
            lines.add(Component.translatable("gui.servus_foundation.side_picker.duct").withColor(0x808080));
        } else if (!duct.isSelectable(side)) {
            lines.add(Component.translatable("gui.servus_foundation.side_picker.empty").withColor(0x808080));
        } else {
            lines.add(duct.touchedBlockName(side));
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        return true;
    }

    static Component sideName(Direction side) {
        return Component.translatable("gui.servus_foundation.side." + side.getSerializedName());
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
