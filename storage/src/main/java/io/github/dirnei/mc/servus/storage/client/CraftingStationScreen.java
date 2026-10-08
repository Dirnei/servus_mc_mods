package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.RecipeEditorPopup;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

/**
 * Crafting Station GUI laid out like the Assembler: inputs on the left, the
 * recipe cells with a plus button, a progress bar and the outputs on the right,
 * and the shared recipe editor popup opened by plus or by clicking a cell.
 */
public class CraftingStationScreen extends AbstractContainerScreen<CraftingStationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/crafting_station.png");
    private static final int CELL = 18;
    private static final int CELL_COLUMNS = 2;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> side == RelativeSide.FRONT,
            side -> press(SideConfigMenus.buttonOf(side)));
    private final RecipeEditorPopup popup = new RecipeEditorPopup(CraftingStationMenu.POPUP_X, CraftingStationMenu.POPUP_Y,
            CraftingStationMenu.WIDTH, CraftingStationMenu.MACHINE_AREA_HEIGHT);

    public CraftingStationScreen(CraftingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = CraftingStationMenu.WIDTH;
        this.imageHeight = CraftingStationMenu.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = CraftingStationMenu.INVENTORY_X;
        this.inventoryLabelY = CraftingStationMenu.INVENTORY_Y - 11;
        panel.placeBeside(leftPos, topPos);
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private boolean editing() {
        return menu.isEditing();
    }

    private Level level() {
        return minecraft == null ? null : minecraft.level;
    }

    private boolean in(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private int cellX(int index) {
        return CraftingStationMenu.CELLS_X + (index % CELL_COLUMNS) * CELL;
    }

    private int cellY(int index) {
        return CraftingStationMenu.CELLS_Y + (index / CELL_COLUMNS) * CELL;
    }

    private int cellAt(double mouseX, double mouseY) {
        for (int i = 0; i < menu.recipeSlots(); i++) {
            if (in(mouseX, mouseY, cellX(i), cellY(i), CELL, CELL)) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack preview() {
        Level level = level();
        if (level == null) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> cells = new ArrayList<>(CraftingStationBlockEntity.GRID);
        for (int i = 0; i < CraftingStationBlockEntity.GRID; i++) {
            ItemStack cell = menu.station().pattern().getStackInSlot(i);
            cells.add(cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1));
        }
        return CraftingStationBlockEntity.match(level, cells)
                .map(holder -> holder.value().getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Level level = level();
        if (level == null) {
            return;
        }
        if (editing()) {
            if (popup.previewHovered(leftPos, topPos, mouseX, mouseY)) {
                ItemStack preview = preview();
                if (preview.isEmpty()) {
                    graphics.renderTooltip(font, Component.translatable("gui.servus_storage.crafting_station.no_match"), mouseX, mouseY);
                } else {
                    graphics.renderTooltip(font, preview, mouseX, mouseY);
                }
            } else {
                renderTooltip(graphics, mouseX, mouseY);
            }
            return;
        }
        int hovered = cellAt(mouseX, mouseY);
        List<CraftingStationBlockEntity.StationRecipe> recipes = menu.station().recipes();
        if (hovered >= 0) {
            List<Component> lines = new ArrayList<>();
            if (hovered < recipes.size()) {
                CraftingStationBlockEntity.StationRecipe recipe = recipes.get(hovered);
                ItemStack result = recipe.result(level);
                lines.add(Component.literal(result.getCount() + "x ").append(result.getHoverName()));
                for (ItemStack cell : recipe.template()) {
                    if (!cell.isEmpty()) {
                        lines.add(Component.literal("  " + cell.getCount() + "x ").append(cell.getHoverName()));
                    }
                }
                lines.add(Component.translatable("gui.servus_storage.crafting_station.edit"));
            } else {
                lines.add(Component.translatable("gui.servus_storage.crafting_station.empty_slot"));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (in(mouseX, mouseY, CraftingStationMenu.PLUS_X, CraftingStationMenu.PLUS_Y, CELL, CELL)) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.crafting_station.add"), mouseX, mouseY);
        } else if (!panel.renderTooltip(graphics, font, mouseX, mouseY)) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        panel.render(graphics, font, mouseX, mouseY);

        int px = leftPos + CraftingStationMenu.PROGRESS_X;
        int py = topPos + CraftingStationMenu.PROGRESS_Y;
        graphics.fill(px, py, px + 18, py + 2, 0xFF555555);
        if (menu.workTicks() > 0 && menu.progress() > 0) {
            int filled = Math.min(18, menu.progress() * 18 / menu.workTicks());
            graphics.fill(px, py, px + filled, py + 2, 0xFF60C060);
        }

        List<CraftingStationBlockEntity.StationRecipe> recipes = menu.station().recipes();
        int active = menu.activeIndex();
        int hovered = editing() ? -1 : cellAt(mouseX, mouseY);
        Level level = level();
        for (int i = 0; i < menu.recipeSlots(); i++) {
            int cx = leftPos + cellX(i);
            int cy = topPos + cellY(i);
            int color = i == hovered ? 0xFFFFFFFF : i == active ? 0xFF60C060 : 0xFF8B8B8B;
            graphics.fill(cx, cy, cx + CELL, cy + CELL, 0xFF373737);
            graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, color);
            // Items render above flat fills, so the popup could not cover them: skip them while editing.
            if (!editing() && i < recipes.size() && level != null) {
                ItemStack result = recipes.get(i).result(level);
                graphics.renderItem(result, cx + 1, cy + 1);
                graphics.renderItemDecorations(font, result, cx + 1, cy + 1);
            }
        }
        RecipeEditorPopup.drawButton(graphics, font, leftPos + CraftingStationMenu.PLUS_X, topPos + CraftingStationMenu.PLUS_Y,
                CELL, CELL, "+", !editing() && in(mouseX, mouseY, CraftingStationMenu.PLUS_X, CraftingStationMenu.PLUS_Y, CELL, CELL));

        if (editing()) {
            boolean existing = menu.editing() >= 0;
            Component title = existing
                    ? Component.translatable("gui.servus_storage.crafting_station.edit_recipe", menu.editing() + 1)
                    : Component.translatable("gui.servus_storage.crafting_station.new_recipe");
            popup.render(graphics, font, leftPos, topPos, mouseX, mouseY, title, existing, preview(),
                    Component.translatable("gui.servus_storage.crafting_station.save"),
                    Component.translatable("gui.servus_storage.crafting_station.delete"),
                    Component.translatable("gui.servus_storage.crafting_station.cancel"));
        }
    }

    /** The vanilla title and inventory labels would paint over the popup, so they are skipped while editing. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!editing()) {
            super.renderLabels(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editing()) {
            switch (popup.buttonAt(leftPos, topPos, mouseX, mouseY, menu.editing() >= 0)) {
                case SAVE -> {
                    press(RecipeEditorButtons.SAVE);
                    return true;
                }
                case DELETE -> {
                    press(RecipeEditorButtons.DELETE);
                    return true;
                }
                case CANCEL -> {
                    press(RecipeEditorButtons.CANCEL);
                    return true;
                }
                case NONE -> {
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (panel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            press(cell < menu.station().recipes().size() ? RecipeEditorButtons.OPEN_EDIT_FIRST + cell : RecipeEditorButtons.OPEN_NEW);
            return true;
        }
        if (in(mouseX, mouseY, CraftingStationMenu.PLUS_X, CraftingStationMenu.PLUS_Y, CELL, CELL)) {
            press(RecipeEditorButtons.OPEN_NEW);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            press(RecipeEditorButtons.CANCEL);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
