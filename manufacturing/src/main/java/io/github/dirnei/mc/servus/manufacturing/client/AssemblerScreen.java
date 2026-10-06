package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.ConfiguredRecipe;
import io.github.dirnei.mc.servus.manufacturing.menu.AssemblerMenu;
import io.github.dirnei.mc.servus.manufacturing.menu.MachineMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Assembler GUI: inputs, the configured recipe cells with a plus button, the
 * outputs with a progress bar, and the recipe editor popup opened by plus or
 * by clicking a recipe cell.
 */
public class AssemblerScreen extends MachineScreen {

    private static final int CELL = 18;
    private static final int CELLS_X = 124;
    private static final int CELLS_Y = 17;
    private static final int PLUS_X = 146;
    private static final int PLUS_Y = 17;
    private static final int PROGRESS_X = 182;
    private static final int PROGRESS_Y = 38;

    private static final int PREVIEW_X = 116;
    private static final int PREVIEW_Y = 38;
    private static final int BUTTON_X = 140;
    private static final int BUTTON_W = 62;
    private static final int BUTTON_H = 14;
    private static final int SAVE_Y = 20;
    private static final int DELETE_Y = 38;
    private static final int CANCEL_Y = 56;

    /** Height of the machine part of the GUI above the player inventory; dimmed while the popup is open. */
    private static final int MACHINE_AREA_HEIGHT = 82;
    private static final int DIM = 0x90000000;

    public AssemblerScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, Layout.ASSEMBLER);
        this.imageWidth = 256;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 48;
    }

    @Override
    protected int energyX() {
        return 240;
    }

    private AssemblerMenu assemblerMenu() {
        return (AssemblerMenu) menu;
    }

    private boolean editing() {
        return assemblerMenu().isEditing();
    }

    private boolean in(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private int cellAt(double mouseX, double mouseY) {
        for (int i = 0; i < assemblerMenu().recipeSlots(); i++) {
            if (in(mouseX, mouseY, CELLS_X, CELLS_Y + i * CELL, CELL, CELL)) {
                return i;
            }
        }
        return -1;
    }

    private Optional<ConfiguredRecipe> previewRecipe() {
        if (minecraft == null || minecraft.level == null) {
            return Optional.empty();
        }
        List<ItemStack> cells = new ArrayList<>(ConfiguredRecipe.GRID);
        for (int i = 0; i < ConfiguredRecipe.GRID; i++) {
            cells.add(assemblerMenu().assembler().pattern().getStackInSlot(i));
        }
        if (cells.stream().allMatch(ItemStack::isEmpty)) {
            return Optional.empty();
        }
        return AssemblerBlockEntity.match(minecraft.level, cells);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        if (editing()) {
            if (in(mouseX, mouseY, PREVIEW_X, PREVIEW_Y, CELL, CELL)) {
                Optional<ConfiguredRecipe> preview = previewRecipe();
                if (preview.isPresent()) {
                    guiGraphics.renderTooltip(font, preview.get().displayResult(minecraft.level), mouseX, mouseY);
                } else {
                    guiGraphics.renderTooltip(font, Component.translatable("gui.servus_manufacturing.assembler.no_match"), mouseX, mouseY);
                }
            }
            return;
        }
        int hovered = cellAt(mouseX, mouseY);
        List<ConfiguredRecipe> recipes = assemblerMenu().recipes();
        if (hovered >= 0) {
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
                lines.add(Component.translatable("gui.servus_manufacturing.assembler.edit"));
            } else {
                lines.add(Component.translatable("gui.servus_manufacturing.assembler.empty_slot"));
            }
            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (in(mouseX, mouseY, PLUS_X, PLUS_Y, CELL, CELL)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.servus_manufacturing.assembler.add"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        int px = leftPos + PROGRESS_X;
        int py = topPos + PROGRESS_Y;
        guiGraphics.fill(px, py, px + 18, py + 2, 0xFF555555);
        int filled = menu.progressScaled(18);
        if (filled > 0) {
            guiGraphics.fill(px, py, px + filled, py + 2, 0xFF60C060);
        }

        List<ConfiguredRecipe> recipes = assemblerMenu().recipes();
        int active = assemblerMenu().activeIndex();
        int hovered = editing() ? -1 : cellAt(mouseX, mouseY);
        for (int i = 0; i < assemblerMenu().recipeSlots(); i++) {
            int cx = leftPos + CELLS_X;
            int cy = topPos + CELLS_Y + i * CELL;
            int color = i == hovered ? 0xFFFFFFFF : i == active ? 0xFF60C060 : 0xFF8B8B8B;
            guiGraphics.fill(cx, cy, cx + CELL, cy + CELL, 0xFF373737);
            guiGraphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, color);
            // Items render above flat fills, so the popup could not cover them: skip them while editing.
            if (!editing() && i < recipes.size() && minecraft != null && minecraft.level != null) {
                guiGraphics.renderItem(recipes.get(i).displayResult(minecraft.level), cx + 1, cy + 1);
            }
        }
        drawButton(guiGraphics, PLUS_X, PLUS_Y, CELL, CELL, "+", !editing() && in(mouseX, mouseY, PLUS_X, PLUS_Y, CELL, CELL));

        if (editing()) {
            renderPopup(guiGraphics, mouseX, mouseY);
        }
    }

    /** The vanilla title and inventory labels would paint over the popup, so they are skipped while editing. */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!editing()) {
            super.renderLabels(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderPopup(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + MACHINE_AREA_HEIGHT, DIM);
        int x = leftPos + AssemblerMenu.POPUP_X;
        int y = topPos + AssemblerMenu.POPUP_Y;
        guiGraphics.fill(x + 2, y + 2, x + AssemblerMenu.POPUP_WIDTH + 2, y + AssemblerMenu.POPUP_HEIGHT + 2, DIM);
        guiGraphics.fill(x, y, x + AssemblerMenu.POPUP_WIDTH, y + AssemblerMenu.POPUP_HEIGHT, 0xFF1E1E1E);
        guiGraphics.fill(x + 1, y + 1, x + AssemblerMenu.POPUP_WIDTH - 1, y + AssemblerMenu.POPUP_HEIGHT - 1, 0xFFFFFFFF);
        guiGraphics.fill(x + 2, y + 2, x + AssemblerMenu.POPUP_WIDTH - 2, y + AssemblerMenu.POPUP_HEIGHT - 2, 0xFFC6C6C6);
        boolean existing = assemblerMenu().editing() >= 0;
        Component title = existing
                ? Component.translatable("gui.servus_manufacturing.assembler.edit_recipe", assemblerMenu().editing() + 1)
                : Component.translatable("gui.servus_manufacturing.assembler.new_recipe");
        guiGraphics.drawString(font, title, x + 6, y + 4, 0xFF404040, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = leftPos + AssemblerMenu.GRID_X + col * CELL - 1;
                int sy = topPos + AssemblerMenu.GRID_Y + row * CELL - 1;
                guiGraphics.fill(sx, sy, sx + CELL, sy + CELL, 0xFF373737);
                guiGraphics.fill(sx + 1, sy + 1, sx + CELL - 1, sy + CELL - 1, 0xFF8B8B8B);
            }
        }
        int vx = leftPos + PREVIEW_X;
        int vy = topPos + PREVIEW_Y;
        guiGraphics.fill(vx - 1, vy - 1, vx + CELL - 1, vy + CELL - 1, 0xFF373737);
        guiGraphics.fill(vx, vy, vx + CELL - 2, vy + CELL - 2, 0xFF8B8B8B);
        guiGraphics.drawString(font, "=", vx - 8, vy + 4, 0xFF404040, false);
        if (minecraft != null && minecraft.level != null) {
            Optional<ConfiguredRecipe> preview = previewRecipe();
            if (preview.isPresent()) {
                ItemStack result = preview.get().displayResult(minecraft.level);
                guiGraphics.renderItem(result, vx, vy);
                guiGraphics.renderItemDecorations(font, result, vx, vy);
            }
        }
        drawButton(guiGraphics, BUTTON_X, SAVE_Y, BUTTON_W, BUTTON_H,
                Component.translatable("gui.servus_manufacturing.assembler.save").getString(), in(mouseX, mouseY, BUTTON_X, SAVE_Y, BUTTON_W, BUTTON_H));
        if (existing) {
            drawButton(guiGraphics, BUTTON_X, DELETE_Y, BUTTON_W, BUTTON_H,
                    Component.translatable("gui.servus_manufacturing.assembler.delete").getString(), in(mouseX, mouseY, BUTTON_X, DELETE_Y, BUTTON_W, BUTTON_H));
        }
        drawButton(guiGraphics, BUTTON_X, CANCEL_Y, BUTTON_W, BUTTON_H,
                Component.translatable("gui.servus_manufacturing.assembler.cancel").getString(), in(mouseX, mouseY, BUTTON_X, CANCEL_Y, BUTTON_W, BUTTON_H));
    }

    private void drawButton(GuiGraphics guiGraphics, int x, int y, int w, int h, String label, boolean hovered) {
        int bx = leftPos + x;
        int by = topPos + y;
        guiGraphics.fill(bx, by, bx + w, by + h, 0xFF373737);
        guiGraphics.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, hovered ? 0xFFDDDDDD : 0xFFA0A0A0);
        guiGraphics.drawString(font, label, bx + (w - font.width(label)) / 2, by + (h - 8) / 2, 0xFF202020, false);
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editing()) {
            if (in(mouseX, mouseY, BUTTON_X, SAVE_Y, BUTTON_W, BUTTON_H)) {
                press(AssemblerMenu.BUTTON_SAVE);
                return true;
            }
            if (assemblerMenu().editing() >= 0 && in(mouseX, mouseY, BUTTON_X, DELETE_Y, BUTTON_W, BUTTON_H)) {
                press(AssemblerMenu.BUTTON_DELETE);
                return true;
            }
            if (in(mouseX, mouseY, BUTTON_X, CANCEL_Y, BUTTON_W, BUTTON_H)) {
                press(AssemblerMenu.BUTTON_CANCEL);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            if (cell < assemblerMenu().recipes().size()) {
                press(AssemblerMenu.BUTTON_OPEN_EDIT_FIRST + cell);
            } else {
                press(AssemblerMenu.BUTTON_OPEN_NEW);
            }
            return true;
        }
        if (in(mouseX, mouseY, PLUS_X, PLUS_Y, CELL, CELL)) {
            press(AssemblerMenu.BUTTON_OPEN_NEW);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing() && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            press(AssemblerMenu.BUTTON_CANCEL);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
