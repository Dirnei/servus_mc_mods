package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Buffer row, station state, player inventory. */
public class InboundStationScreen extends AbstractContainerScreen<InboundStationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/inbound_station.png");
    private static final int STATUS_Y = 40;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_ALERT = 0xB03030;

    public InboundStationScreen(InboundStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 140;
        ClientStationState.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = InboundStationMenu.INVENTORY_Y - 11;
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
        StationStatePayload state = ClientStationState.state();
        if (!state.online()) {
            Component reason = Component.translatable("gui.servus_storage.terminal." + state.reason());
            graphics.drawString(font, reason, 8, STATUS_Y, COLOUR_ALERT, false);
            return;
        }
        graphics.drawString(font, status(state), 8, STATUS_Y, state.blockedReason().isEmpty() ? COLOUR_TEXT : COLOUR_ALERT, false);
    }

    private Component status(StationStatePayload state) {
        if (state.blockedReason().isEmpty()) {
            return Component.translatable(state.idle() ? "gui.servus_storage.station.idle" : "gui.servus_storage.station.busy");
        }
        String item = state.blockedStack().getHoverName().getString();
        String reason = state.blockedReason().toLowerCase(Locale.ROOT);
        if (reason.equals("no_room") && state.blockedZone().isEmpty()) {
            return Component.translatable("gui.servus_storage.station.blocked.no_room_general", item);
        }
        return Component.translatable("gui.servus_storage.station.blocked." + reason, item, state.blockedZone());
    }

    @Override
    public void removed() {
        super.removed();
        ClientStationState.clear();
    }
}
