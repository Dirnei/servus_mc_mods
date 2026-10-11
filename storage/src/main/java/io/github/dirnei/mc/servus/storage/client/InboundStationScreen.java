package io.github.dirnei.mc.servus.storage.client;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Buffer row, station state, player inventory, the shared side panel on the left. */
public class InboundStationScreen extends AbstractContainerScreen<InboundStationMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/inbound_station.png");
    private static final int COLOUR_ALERT = 0xB03030;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> side == io.github.dirnei.mc.servus.foundation.side.RelativeSide.FRONT, side -> {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SideConfigMenus.buttonOf(side));
        }
    });

    public InboundStationScreen(InboundStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 140);
        ClientStationState.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = InboundStationMenu.INVENTORY_Y - 11;
        panel.placeBeside(leftPos, topPos);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (panel.renderTooltip(graphics, font, mouseX, mouseY)) {
            return;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        panel.render(graphics, font, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        StationStatePayload state = ClientStationState.state();
        if (!state.online()) {
            Component reason = Component.translatable("gui.servus_storage.terminal." + state.reason());
            graphics.text(font, reason, 8, InboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
            return;
        }
        // The lamps on the block tell idle from working; the GUI only explains a red lamp.
        if (!state.blockedReason().isEmpty()) {
            graphics.text(font, status(state), 8, InboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (panel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void removed() {
        super.removed();
        ClientStationState.clear();
    }
}
