package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.MachineDockMenu;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Feed row, collect row, state line, player inventory. */
public class MachineDockScreen extends AbstractContainerScreen<MachineDockMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/machine_dock.png");
    private static final int STATUS_Y = 62;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_ALERT = 0xB03030;

    public MachineDockScreen(MachineDockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        ClientStationState.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = MachineDockMenu.INVENTORY_Y - 11;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.servus_storage.dock.feed"), 100, MachineDockMenu.FEED_Y - 10, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.dock.collect"), 8, MachineDockMenu.COLLECT_Y + 19, COLOUR_TEXT, false);
        StationStatePayload state = ClientStationState.state();
        if (!state.online()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal." + state.reason()), 8, STATUS_Y + 8, COLOUR_ALERT, false);
        } else if (!state.blockedReason().isEmpty()) {
            String item = state.blockedStack().getHoverName().getString();
            String reason = state.blockedReason().toLowerCase(Locale.ROOT);
            Component text = reason.equals("no_room") && state.blockedZone().isEmpty()
                    ? Component.translatable("gui.servus_storage.station.blocked.no_room_general", item)
                    : Component.translatable("gui.servus_storage.station.blocked." + reason, item, state.blockedZone());
            graphics.drawString(font, text, 8, STATUS_Y + 8, COLOUR_ALERT, false);
        }
    }

    @Override
    public void removed() {
        super.removed();
        ClientStationState.clear();
    }
}
