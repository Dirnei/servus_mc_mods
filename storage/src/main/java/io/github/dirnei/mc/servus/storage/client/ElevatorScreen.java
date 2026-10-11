package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.menu.ElevatorMenu;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/** The shaft's Speed upgrade row with a label, and the player inventory. */
public class ElevatorScreen extends AbstractContainerScreen<ElevatorMenu> {

    private static final int COLOUR_TEXT = 0x404040;
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
        Rect row = upgradesRow();
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        boolean overRow = x >= row.x() - 1 && x < row.x() + row.w() + 1 && y >= ElevatorMenu.UPGRADES_Y - 12 && y < row.y() + row.h();
        if (menu.getCarried().isEmpty() && overRow && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("gui.servus_storage.elevator.upgrades.tooltip"), 180);
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        } else {
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        if (menu.upgradeSlots() > 0) {
            PanelPainter.slots(graphics, leftPos, topPos, upgradesRow(), menu.upgradeSlots(), menu.upgradeSlots());
        }
        PanelPainter.slots(graphics, leftPos, topPos, new Rect(8, ElevatorMenu.INVENTORY_Y, 9 * 18, 3 * 18), 9, 27);
        PanelPainter.slots(graphics, leftPos, topPos, new Rect(8, menu.hotbarY(), 9 * 18, 18), 9, 9);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component label = Component.translatable("gui.servus_storage.elevator.upgrades");
        graphics.text(font, label, (imageWidth - font.width(label)) / 2, ElevatorMenu.UPGRADES_Y - 11, COLOUR_TEXT, false);
    }
}
