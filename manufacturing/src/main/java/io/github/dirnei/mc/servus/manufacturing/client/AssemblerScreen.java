package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.lib.client.RecipeEditorPopup;
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

    /** Height of the machine part of the GUI above the player inventory; dimmed while the popup is open. */
    private static final int MACHINE_AREA_HEIGHT = 82;

    private final RecipeEditorPopup popup =
            new RecipeEditorPopup(AssemblerMenu.POPUP_X, AssemblerMenu.POPUP_Y, 256, MACHINE_AREA_HEIGHT);

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
            if (popup.previewHovered(leftPos, topPos, mouseX, mouseY)) {
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
        boolean existing = assemblerMenu().editing() >= 0;
        Component title = existing
                ? Component.translatable("gui.servus_manufacturing.assembler.edit_recipe", assemblerMenu().editing() + 1)
                : Component.translatable("gui.servus_manufacturing.assembler.new_recipe");
        ItemStack preview = minecraft != null && minecraft.level != null
                ? previewRecipe().map(recipe -> recipe.displayResult(minecraft.level)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        popup.render(guiGraphics, font, leftPos, topPos, mouseX, mouseY, title, existing, preview,
                Component.translatable("gui.servus_manufacturing.assembler.save"),
                Component.translatable("gui.servus_manufacturing.assembler.delete"),
                Component.translatable("gui.servus_manufacturing.assembler.cancel"));
    }

    private void drawButton(GuiGraphics guiGraphics, int x, int y, int w, int h, String label, boolean hovered) {
        RecipeEditorPopup.drawButton(guiGraphics, font, leftPos + x, topPos + y, w, h, label, hovered);
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editing()) {
            switch (popup.buttonAt(leftPos, topPos, mouseX, mouseY, assemblerMenu().editing() >= 0)) {
                case SAVE -> {
                    press(AssemblerMenu.BUTTON_SAVE);
                    return true;
                }
                case DELETE -> {
                    press(AssemblerMenu.BUTTON_DELETE);
                    return true;
                }
                case CANCEL -> {
                    press(AssemblerMenu.BUTTON_CANCEL);
                    return true;
                }
                case NONE -> {
                }
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
