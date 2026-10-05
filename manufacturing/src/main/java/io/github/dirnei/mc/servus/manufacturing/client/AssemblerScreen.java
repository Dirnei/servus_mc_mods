package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.menu.AssemblerMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Assembler GUI: machine screen plus a recipe panel on the right. */
public class AssemblerScreen extends MachineScreen {

    private static final int PANEL_GAP = 4;
    private static final int PANEL_PADDING = 4;
    private static final int COLUMNS = 3;
    private static final int CELL = 18;

    public AssemblerScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Layout.ASSEMBLER);
    }

    private AssemblerMenu assemblerMenu() {
        return (AssemblerMenu) menu;
    }

    private int panelX() {
        return leftPos + imageWidth + PANEL_GAP;
    }

    private int panelY() {
        return topPos;
    }

    private int panelWidth() {
        return PANEL_PADDING * 2 + COLUMNS * CELL;
    }

    private int panelHeight() {
        int rows = Math.max(1, (assemblerMenu().recipes().size() + COLUMNS - 1) / COLUMNS);
        return PANEL_PADDING * 2 + rows * CELL;
    }

    private int cellX(int index) {
        return panelX() + PANEL_PADDING + (index % COLUMNS) * CELL;
    }

    private int cellY(int index) {
        return panelY() + PANEL_PADDING + (index / COLUMNS) * CELL;
    }

    private int cellAt(double mouseX, double mouseY) {
        List<RecipeHolder<AssemblingRecipe>> recipes = assemblerMenu().recipes();
        for (int i = 0; i < recipes.size(); i++) {
            int x = cellX(i);
            int y = cellY(i);
            if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int hovered = cellAt(mouseX, mouseY);
        if (hovered >= 0) {
            AssemblingRecipe recipe = assemblerMenu().recipes().get(hovered).value();
            List<Component> lines = new ArrayList<>();
            ItemStack result = recipe.result();
            lines.add(Component.literal(result.getCount() + "x ").append(result.getHoverName()));
            for (SizedIngredient ingredient : recipe.ingredients()) {
                ItemStack[] options = ingredient.getItems();
                Component name = options.length > 0 ? options[0].getHoverName() : Component.literal("?");
                lines.add(Component.literal("  " + ingredient.count() + "x ").append(name));
            }
            lines.add(Component.literal(String.format("  %.1f s", recipe.time() / 20.0)));
            if (!assemblerMenu().isUnlocked(hovered)) {
                lines.add(Component.translatable("gui.servus_manufacturing.locked"));
            }
            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        int x = panelX();
        int y = panelY();
        guiGraphics.fill(x, y, x + panelWidth(), y + panelHeight(), 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + panelWidth() - 1, y + panelHeight() - 1, 0xFFC6C6C6);
        List<RecipeHolder<AssemblingRecipe>> recipes = assemblerMenu().recipes();
        int selected = assemblerMenu().selectedIndex();
        int hovered = cellAt(mouseX, mouseY);
        for (int i = 0; i < recipes.size(); i++) {
            int cx = cellX(i);
            int cy = cellY(i);
            if (!assemblerMenu().isUnlocked(i)) {
                guiGraphics.fill(cx, cy, cx + CELL, cy + CELL, 0xFF404040);
            } else if (i == selected) {
                guiGraphics.fill(cx, cy, cx + CELL, cy + CELL, 0xFF60C060);
            } else if (i == hovered) {
                guiGraphics.fill(cx, cy, cx + CELL, cy + CELL, 0xFFFFFFFF);
            } else {
                guiGraphics.fill(cx, cy, cx + CELL, cy + CELL, 0xFF8B8B8B);
            }
            guiGraphics.renderItem(recipes.get(i).value().result(), cx + 1, cy + 1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int clicked = cellAt(mouseX, mouseY);
        if (clicked < 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!assemblerMenu().isUnlocked(clicked)) {
            return true;
        }
        if (minecraft != null && minecraft.gameMode != null) {
            int id = clicked == assemblerMenu().selectedIndex() ? assemblerMenu().recipes().size() : clicked;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
        return true;
    }
}
