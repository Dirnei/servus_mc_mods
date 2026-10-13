package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.menu.ElevatorMenu;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The shaft's Speed upgrade row with a label, and the player inventory. */
public class ElevatorScreen extends AbstractContainerScreen<ElevatorMenu> {

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int WIDTH = 176;

    public ElevatorScreen(ElevatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIDTH, ElevatorMenu.IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ElevatorMenu.INVENTORY_Y - 11;
    }

    private Rect upgradesRow() {
        return new Rect(menu.upgradeX(0), ElevatorMenu.UPGRADES_Y, menu.upgradeSlots() * 18, 18);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        boolean overRow = upgradesRow().coversRow(mouseX - leftPos, mouseY - topPos, ElevatorMenu.UPGRADES_Y - 12);
        if (menu.getCarried().isEmpty() && overRow && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            graphics.setTooltipForNextFrame(font, font.split(Component.translatable("gui.servus_storage.elevator.upgrades.tooltip"), 180),
                    mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        if (menu.upgradeSlots() > 0) {
            PanelPainter.slots(graphics, leftPos, topPos, upgradesRow(), menu.upgradeSlots(), menu.upgradeSlots());
        }
        PanelPainter.playerInventory(graphics, leftPos, topPos, ElevatorMenu.INVENTORY_Y, menu.hotbarY());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component label = Component.translatable("gui.servus_storage.elevator.upgrades");
        graphics.text(font, label, (imageWidth - font.width(label)) / 2, ElevatorMenu.UPGRADES_Y - 11, COLOUR_TEXT, false);
    }
}
