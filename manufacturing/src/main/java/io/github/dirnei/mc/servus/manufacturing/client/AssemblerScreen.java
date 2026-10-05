package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.machine.assembler.ConfiguredRecipe;
import io.github.dirnei.mc.servus.manufacturing.menu.AssemblerMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.MachineMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Assembler GUI: nine inputs, a ghost pattern grid with add and clear buttons,
 * the output with a progress bar, and the configured recipes in a panel on the
 * right. Clicking a configured recipe removes it.
 */
public class AssemblerScreen extends MachineScreen {

    private static final int ADD_X = 124;
    private static final int ADD_Y = 17;
    private static final int CLEAR_X = 124;
    private static final int CLEAR_Y = 55;
    private static final int BUTTON_W = 18;
    private static final int BUTTON_H = 12;
    private static final int PROGRESS_X = 124;
    private static final int PROGRESS_Y = 53;

    private static final int PANEL_GAP = 4;
    private static final int PANEL_PADDING = 4;
    private static final int CELL = 18;

    public AssemblerScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Layout.ASSEMBLER);
    }

    private AssemblerMenu assemblerMenu() {
        return (AssemblerMenu) menu;
    }

    // Configured recipe panel (right)

    private int panelX() {
        return leftPos + imageWidth + PANEL_GAP;
    }

    private int panelY() {
        return topPos;
    }

    private int panelWidth() {
        return PANEL_PADDING * 2 + CELL;
    }

    private int panelHeight() {
        return PANEL_PADDING * 2 + Math.max(1, assemblerMenu().recipeSlots()) * CELL + 10;
    }

    private int cellX() {
        return panelX() + PANEL_PADDING;
    }

    private int cellY(int index) {
        return panelY() + PANEL_PADDING + 10 + index * CELL;
    }

    private int recipeAt(double mouseX, double mouseY) {
        int count = assemblerMenu().recipeSlots();
        for (int i = 0; i < count; i++) {
            int x = cellX();
            int y = cellY(i);
            if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                return i;
            }
        }
        return -1;
    }

    private boolean inButton(double mouseX, double mouseY, int x, int y) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + BUTTON_W && mouseY >= topPos + y && mouseY < topPos + y + BUTTON_H;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int hovered = recipeAt(mouseX, mouseY);
        List<ConfiguredRecipe> recipes = assemblerMenu().recipes();
        if (hovered >= 0 && minecraft != null && minecraft.level != null) {
            List<Component> lines = new ArrayList<>();
            if (hovered < recipes.size()) {
                ConfiguredRecipe recipe = recipes.get(hovered);
                ItemStack result = recipe.displayResult(minecraft.level);
                lines.add(Component.literal(result.getCount() + "x ").append(result.getHoverName()));
                for (ItemStack cell : recipe.template()) {
                    if (!cell.isEmpty()) {
                        lines.add(Component.literal("  " + cell.getCount() + "x ").append(cell.getHoverName()));
                    }
                }
                lines.add(Component.translatable("gui.servus_manufacturing.assembler.remove"));
            } else {
                lines.add(Component.translatable("gui.servus_manufacturing.assembler.empty_slot"));
            }
            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (inButton(mouseX, mouseY, ADD_X, ADD_Y)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.servus_manufacturing.assembler.add"), mouseX, mouseY);
        } else if (inButton(mouseX, mouseY, CLEAR_X, CLEAR_Y)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.servus_manufacturing.assembler.clear"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        drawButton(guiGraphics, ADD_X, ADD_Y, "+", inButton(mouseX, mouseY, ADD_X, ADD_Y));
        drawButton(guiGraphics, CLEAR_X, CLEAR_Y, "x", inButton(mouseX, mouseY, CLEAR_X, CLEAR_Y));

        int px = leftPos + PROGRESS_X;
        int py = topPos + PROGRESS_Y;
        guiGraphics.fill(px, py, px + 18, py + 2, 0xFF555555);
        int filled = menu.progressScaled(18);
        if (filled > 0) {
            guiGraphics.fill(px, py, px + filled, py + 2, 0xFF60C060);
        }

        int x = panelX();
        int y = panelY();
        guiGraphics.fill(x, y, x + panelWidth(), y + panelHeight(), 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + panelWidth() - 1, y + panelHeight() - 1, 0xFFC6C6C6);
        guiGraphics.drawString(font, Component.translatable("gui.servus_manufacturing.assembler.recipes"), x + 3, y + 3, 0xFF404040, false);
        List<ConfiguredRecipe> recipes = assemblerMenu().recipes();
        int active = assemblerMenu().activeIndex();
        int hovered = recipeAt(mouseX, mouseY);
        for (int i = 0; i < assemblerMenu().recipeSlots(); i++) {
            int cx = cellX();
            int cy = cellY(i);
            int color = i == hovered ? 0xFFFFFFFF : i == active ? 0xFF60C060 : 0xFF8B8B8B;
            guiGraphics.fill(cx, cy, cx + CELL, cy + CELL, color);
            if (i < recipes.size() && minecraft != null && minecraft.level != null) {
                guiGraphics.renderItem(recipes.get(i).displayResult(minecraft.level), cx + 1, cy + 1);
            }
        }
    }

    private void drawButton(GuiGraphics guiGraphics, int x, int y, String label, boolean hovered) {
        int bx = leftPos + x;
        int by = topPos + y;
        guiGraphics.fill(bx, by, bx + BUTTON_W, by + BUTTON_H, 0xFF373737);
        guiGraphics.fill(bx + 1, by + 1, bx + BUTTON_W - 1, by + BUTTON_H - 1, hovered ? 0xFFDDDDDD : 0xFFA0A0A0);
        guiGraphics.drawCenteredString(font, label, bx + BUTTON_W / 2, by + 2, 0xFF202020);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft == null || minecraft.gameMode == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int recipe = recipeAt(mouseX, mouseY);
        if (recipe >= 0) {
            if (recipe < assemblerMenu().recipes().size()) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AssemblerMenu.BUTTON_REMOVE_FIRST + recipe);
            }
            return true;
        }
        if (inButton(mouseX, mouseY, ADD_X, ADD_Y)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AssemblerMenu.BUTTON_ADD_PATTERN);
            return true;
        }
        if (inButton(mouseX, mouseY, CLEAR_X, CLEAR_Y)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AssemblerMenu.BUTTON_CLEAR_PATTERN);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
