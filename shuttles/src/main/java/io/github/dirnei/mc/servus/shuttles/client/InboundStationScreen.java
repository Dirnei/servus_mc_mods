package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Buffer row, station state, player inventory, the shared side panel on the left. */
public class InboundStationScreen extends AbstractContainerScreen<InboundStationMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "textures/gui/inbound_station.png");
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_ALERT = 0xFFB03030;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> side == RelativeSide.FRONT,
            side -> MenuButtons.press(minecraft, menu, SideConfigMenus.buttonOf(side)));

    public InboundStationScreen(InboundStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 140);
        ClientStationState.clear();
    }

    private StationNameField name;

    /** The name is the title: a borderless field in the title row, showing the numbered label until a name is typed. */
    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = InboundStationMenu.INVENTORY_Y - 11;
        panel.placeBeside(leftPos, topPos);
        name = new StationNameField(font, leftPos + titleLabelX, topPos + titleLabelY, imageWidth - 2 * titleLabelX, menu.station(), COLOUR_TEXT);
        addRenderableWidget(name.box());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        name.tick();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return name.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        panel.renderTooltip(graphics, font, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        panel.render(graphics, font, mouseX, mouseY);
    }

    /** The state line only explains a red lamp; the lamps on the block tell idle from working. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOUR_TEXT, false);
        name.renderNumber(graphics, font, imageWidth - titleLabelX, titleLabelY, COLOUR_TEXT);
        StationStatePayload state = ClientStationState.state();
        if (!state.online()) {
            Component reason = Component.translatable("gui.servus_shuttles.terminal." + state.reason());
            graphics.text(font, reason, 8, InboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
            return;
        }
        if (!state.blockedReason().isEmpty()) {
            graphics.text(font, status(state), 8, InboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
        }
    }

    private Component status(StationStatePayload state) {
        String item = state.blockedStack().getHoverName().getString();
        String reason = state.blockedReason().toLowerCase(Locale.ROOT);
        if (reason.equals("no_room") && state.blockedZone().isEmpty()) {
            return Component.translatable("gui.servus_shuttles.station.blocked.no_room_general", item);
        }
        return Component.translatable("gui.servus_shuttles.station.blocked." + reason, item, state.blockedZone());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public void removed() {
        name.send();
        super.removed();
        ClientStationState.clear();
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
