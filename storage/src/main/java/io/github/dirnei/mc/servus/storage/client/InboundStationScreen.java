package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Buffer row, station state, player inventory, the shared side panel on the left. */
public class InboundStationScreen extends AbstractContainerScreen<InboundStationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/inbound_station.png");
    private static final int COLOUR_ALERT = 0xB03030;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> side == io.github.dirnei.mc.servus.foundation.side.RelativeSide.FRONT, side -> {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SideConfigMenus.buttonOf(side));
        }
    });

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
        panel.placeBeside(leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (panel.renderTooltip(graphics, font, mouseX, mouseY)) {
            return;
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        panel.render(graphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        StationStatePayload state = ClientStationState.state();
        if (!state.online()) {
            Component reason = Component.translatable("gui.servus_storage.terminal." + state.reason());
            graphics.drawString(font, reason, 8, InboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
            return;
        }
        // The lamps on the block tell idle from working; the GUI only explains a red lamp.
        if (!state.blockedReason().isEmpty()) {
            graphics.drawString(font, status(state), 8, InboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
        }
    }

    private Component status(StationStatePayload state) {
        String item = state.blockedStack().getHoverName().getString();
        String reason = state.blockedReason().toLowerCase(Locale.ROOT);
        if (reason.equals("no_room") && state.blockedZone().isEmpty()) {
            return Component.translatable("gui.servus_storage.station.blocked.no_room_general", item);
        }
        return Component.translatable("gui.servus_storage.station.blocked." + reason, item, state.blockedZone());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (panel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        super.removed();
        ClientStationState.clear();
    }
}
