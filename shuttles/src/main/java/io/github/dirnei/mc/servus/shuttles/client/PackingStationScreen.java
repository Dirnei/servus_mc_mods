package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.upgrade.Upgrades;
import io.github.dirnei.mc.servus.shuttles.menu.PackingStationMenu;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStatusPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/** The name field as title, two read-only rows, To storage and To network, the upgrade row with the rate, a status line and the player inventory. */
public class PackingStationScreen extends AbstractContainerScreen<PackingStationMenu> {

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_ALERT = 0xFFB03030;
    private static final int WIDTH = 176;

    public PackingStationScreen(PackingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIDTH, menu.imageHeight());
        ClientPackingStatus.clear();
    }

    /** Null while the client has not loaded the station; the plain title shows then. */
    private StationNameField name;

    /** The name is the title: a borderless field in the title row, showing the numbered label until a name is typed. */
    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = menu.inventoryY() - 11;
        if (menu.station() != null) {
            name = new StationNameField(font, leftPos + titleLabelX, topPos + titleLabelY, imageWidth - 2 * titleLabelX, menu.station(), COLOUR_TEXT);
            addRenderableWidget(name.box());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (name != null) {
            name.tick();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return (name != null && name.keyPressed(event)) || super.keyPressed(event);
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem()) {
            return;
        }
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (toStorageRow().coversRow(x, y, menu.toStorageY() - 10)) {
            rowTooltip(graphics, "to_storage", mouseX, mouseY);
        } else if (toNetworkRow().coversRow(x, y, menu.toNetworkLabelY())) {
            rowTooltip(graphics, "to_network", mouseX, mouseY);
        } else if (menu.upgradeSlots() > 0 && upgradesRow().coversRow(x, y, menu.upgradesLabelY())) {
            rowTooltip(graphics, "upgrades", mouseX, mouseY);
        }
    }

    private void rowTooltip(GuiGraphicsExtractor graphics, String row, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = font.split(
                Component.translatable("gui.servus_shuttles.packing_station." + row + ".tooltip"), 180);
        graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        PanelPainter.slots(graphics, leftPos, topPos, toStorageRow(), PackingStationMenu.COLUMNS, menu.toStorageSlots());
        PanelPainter.slots(graphics, leftPos, topPos, toNetworkRow(), PackingStationMenu.COLUMNS, menu.toNetworkSlots());
        if (menu.upgradeSlots() > 0) {
            PanelPainter.slots(graphics, leftPos, topPos, upgradesRow(), menu.upgradeSlots(), menu.upgradeSlots());
        }
        PanelPainter.playerInventory(graphics, leftPos, topPos, menu.inventoryY(), menu.hotbarY());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (name == null) {
            super.extractLabels(graphics, mouseX, mouseY);
        } else {
            graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOUR_TEXT, false);
            name.renderNumber(graphics, font, imageWidth - titleLabelX, titleLabelY, COLOUR_TEXT);
        }
        graphics.text(font, Component.translatable("gui.servus_shuttles.packing_station.to_storage"),
                8, PackingStationMenu.FIRST_LABEL_Y, COLOUR_TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_shuttles.packing_station.to_network"),
                8, menu.toNetworkLabelY(), COLOUR_TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_shuttles.packing_station.upgrades"),
                8, menu.upgradesLabelY(), COLOUR_TEXT, false);
        PackingStatusPayload status = ClientPackingStatus.status();
        if (status.rate() > 0) {
            Component rate = Component.translatable("gui.servus_shuttles.packing_station.rate", Upgrades.format(status.rate()));
            graphics.text(font, rate, imageWidth - 8 - font.width(rate), menu.upgradesLabelY(), COLOUR_TEXT, false);
        }
        if (!status.line().getString().isEmpty()) {
            FormattedCharSequence line = font.split(status.line(), imageWidth - 16).get(0);
            graphics.text(font, line, 8, menu.statusY(), status.alert() ? COLOUR_ALERT : COLOUR_TEXT, false);
        }
    }

    @Override
    public void removed() {
        if (name != null) {
            name.send();
        }
        super.removed();
        ClientPackingStatus.clear();
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        LockOverlay.draw(graphics, menu, menu.locks());
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!LockOverlay.tooltip(graphics, font, hoveredSlot, menu.locks(), mouseX, mouseY)) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
    }
}
