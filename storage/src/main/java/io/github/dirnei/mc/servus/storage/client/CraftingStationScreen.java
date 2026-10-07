package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.lib.block.SideConfigMenus;
import io.github.dirnei.mc.servus.lib.client.SideConfigPanel;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Pattern grid with Save, recipe cells, inputs, outputs, progress line, player inventory. */
public class CraftingStationScreen extends AbstractContainerScreen<CraftingStationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/crafting_station.png");
    private static final int CELLS_X = 80;
    private static final int CELLS_Y = 16;
    private static final int SAVE_Y = 74;
    private static final int MESSAGE_Y = 92;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_ALERT = 0xB03030;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> press(SideConfigMenus.buttonOf(side)));

    public CraftingStationScreen(CraftingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = CraftingStationMenu.INVENTORY_Y - 11;
        panel.placeBeside(leftPos, topPos);
        addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.crafting_station.save"), button -> press(CraftingStationMenu.BUTTON_SAVE))
                .bounds(leftPos + CraftingStationMenu.GRID_X, topPos + SAVE_Y, 54, 14).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            menu.clickMenuButton(minecraft.player, id);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private int cellAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos - CELLS_X;
        int y = (int) mouseY - topPos - CELLS_Y;
        if (y < 0 || y >= 18 || x < 0) {
            return -1;
        }
        int cell = x / 18;
        return cell < menu.station().recipeSlots() ? cell : -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cell = cellAt(mouseX, mouseY);
        List<CraftingStationBlockEntity.StationRecipe> recipes = menu.station().recipes();
        if (cell >= 0 && cell < recipes.size() && menu.station().getLevel() != null) {
            ItemStack result = recipes.get(cell).result(menu.station().getLevel());
            graphics.renderTooltip(font, List.of(result.getHoverName(), Component.translatable("gui.servus_storage.crafting_station.remove_hint")),
                    java.util.Optional.empty(), mouseX, mouseY);
        } else if (!panel.renderTooltip(graphics, font, mouseX, mouseY)) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        panel.render(graphics, font, mouseX, mouseY);
        int slots = menu.station().recipeSlots();
        for (int i = 0; i < slots; i++) {
            int x = leftPos + CELLS_X + i * 18;
            int y = topPos + CELLS_Y;
            graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
            graphics.fill(x, y, x + 18, y + 1, 0xFF373737);
            graphics.fill(x, y, x + 1, y + 18, 0xFF373737);
            graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
            graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        }
        List<CraftingStationBlockEntity.StationRecipe> recipes = menu.station().recipes();
        if (menu.station().getLevel() != null) {
            for (int i = 0; i < recipes.size() && i < slots; i++) {
                ItemStack result = recipes.get(i).result(menu.station().getLevel());
                graphics.renderItem(result, leftPos + CELLS_X + i * 18 + 1, topPos + CELLS_Y + 1);
                graphics.renderItemDecorations(font, result, leftPos + CELLS_X + i * 18 + 1, topPos + CELLS_Y + 1);
            }
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0 && cell < recipes.size()) {
            int x = leftPos + CELLS_X + cell * 18 + 1;
            graphics.fill(x, topPos + CELLS_Y + 1, x + 16, topPos + CELLS_Y + 17, 0x80FFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.servus_storage.crafting_station.recipes"), CELLS_X, CELLS_Y - 10, COLOUR_TEXT, false);
        String message = menu.message();
        if (!message.isEmpty()) {
            boolean good = message.equals("saved");
            graphics.drawString(font, Component.translatable("gui.servus_storage.crafting_station.message." + message), CraftingStationMenu.GRID_X, MESSAGE_Y,
                    good ? COLOUR_TEXT : COLOUR_ALERT, false);
        } else if (menu.workTicks() > 0 && menu.progress() > 0) {
            int percent = menu.progress() * 100 / menu.workTicks();
            graphics.drawString(font, Component.translatable("gui.servus_storage.crafting_station.progress", percent), CraftingStationMenu.GRID_X, MESSAGE_Y, COLOUR_TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (panel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0 && cell < menu.station().recipes().size()) {
            press(CraftingStationMenu.BUTTON_REMOVE_FIRST + cell);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
