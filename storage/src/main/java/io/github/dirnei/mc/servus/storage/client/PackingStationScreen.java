package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.upgrade.Upgrades;
import io.github.dirnei.mc.servus.storage.menu.PackingStationMenu;
import io.github.dirnei.mc.servus.storage.packing.PackingStatusPayload;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/** Two read-only rows, To storage and To network, the upgrade row with the rate, a status line and the player inventory. */
public class PackingStationScreen extends AbstractContainerScreen<PackingStationMenu> {

    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_ALERT = 0xB03030;
    private static final int WIDTH = 176;

    public PackingStationScreen(PackingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = menu.imageHeight();
        ClientPackingStatus.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = menu.inventoryY() - 11;
    }

    private Rect toStorageRow() {
        return new Rect(8, menu.toStorageY(), PackingStationMenu.COLUMNS * 18, rows(menu.toStorageSlots()) * 18);
    }

    private Rect toNetworkRow() {
        return new Rect(8, menu.toNetworkY(), PackingStationMenu.COLUMNS * 18, rows(menu.toNetworkSlots()) * 18);
    }

    private Rect upgradesRow() {
        return new Rect(8, menu.upgradesY(), menu.upgradeSlots() * 18, 18);
    }

    private static int rows(int slots) {
        return Math.max(1, (slots + PackingStationMenu.COLUMNS - 1) / PackingStationMenu.COLUMNS);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem()) {
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (over(x, y, menu.toStorageY() - 10, toStorageRow())) {
            rowTooltip(graphics, "to_storage", mouseX, mouseY);
        } else if (over(x, y, menu.toNetworkLabelY(), toNetworkRow())) {
            rowTooltip(graphics, "to_network", mouseX, mouseY);
        } else if (menu.upgradeSlots() > 0 && over(x, y, menu.upgradesLabelY(), upgradesRow())) {
            rowTooltip(graphics, "upgrades", mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    /** Whether the mouse is over a row or its label above it. */
    private static boolean over(int x, int y, int labelY, Rect row) {
        return x >= row.x() - 1 && x < row.x() + row.w() + 1 && y >= labelY && y < row.y() + row.h();
    }

    private void rowTooltip(GuiGraphics graphics, String row, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = font.split(
                Component.translatable("gui.servus_storage.packing_station." + row + ".tooltip"), 180);
        graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        PanelPainter.slots(graphics, leftPos, topPos, toStorageRow(), PackingStationMenu.COLUMNS, menu.toStorageSlots());
        PanelPainter.slots(graphics, leftPos, topPos, toNetworkRow(), PackingStationMenu.COLUMNS, menu.toNetworkSlots());
        if (menu.upgradeSlots() > 0) {
            PanelPainter.slots(graphics, leftPos, topPos, upgradesRow(), menu.upgradeSlots(), menu.upgradeSlots());
        }
        PanelPainter.slots(graphics, leftPos, topPos, new Rect(8, menu.inventoryY(), 9 * 18, 3 * 18), 9, 27);
        PanelPainter.slots(graphics, leftPos, topPos, new Rect(8, menu.hotbarY(), 9 * 18, 18), 9, 9);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.servus_storage.packing_station.to_storage"),
                8, PackingStationMenu.FIRST_LABEL_Y, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.packing_station.to_network"),
                8, menu.toNetworkLabelY(), COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.packing_station.upgrades"),
                8, menu.upgradesLabelY(), COLOUR_TEXT, false);
        PackingStatusPayload status = ClientPackingStatus.status();
        if (status.rate() > 0) {
            Component rate = Component.translatable("gui.servus_storage.packing_station.rate", Upgrades.format(status.rate()));
            graphics.drawString(font, rate, imageWidth - 8 - font.width(rate), menu.upgradesLabelY(), COLOUR_TEXT, false);
        }
        if (!status.line().getString().isEmpty()) {
            FormattedCharSequence line = font.split(status.line(), imageWidth - 16).get(0);
            graphics.drawString(font, line, 8, menu.statusY(), status.alert() ? COLOUR_ALERT : COLOUR_TEXT, false);
        }
    }

    @Override
    public void removed() {
        super.removed();
        ClientPackingStatus.clear();
    }
}
