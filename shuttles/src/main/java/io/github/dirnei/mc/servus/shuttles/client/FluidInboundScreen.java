package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import io.github.dirnei.mc.servus.shuttles.menu.FluidInboundMenu;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Fluid Inbound GUI: a gauge per tank with its number below, the state
 * line, the player inventory and the Fluids side panel on the left. The
 * state line shows only while the warehouse is offline or a tank waits.
 */
public class FluidInboundScreen extends AbstractContainerScreen<FluidInboundMenu> {

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_ALERT = 0xFFB03030;
    private static final int MARGIN = 8;
    private static final int STATE_ROWS = 2;

    private final FluidGauge[] gauges;
    private final SideConfigPanel panel = SideConfigPanel.fluidsOnly();

    public FluidInboundScreen(FluidInboundMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FluidInboundMenu.WIDTH, FluidInboundMenu.IMAGE_HEIGHT);
        gauges = new FluidGauge[menu.tanks()];
        for (int tank = 0; tank < gauges.length; tank++) {
            gauges[tank] = new FluidGauge(FluidInboundMenu.GAUGE_WIDTH, FluidInboundMenu.GAUGE_HEIGHT);
        }
        ClientStationState.clear();
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = FluidInboundMenu.INVENTORY_Y - 11;
        for (int tank = 0; tank < gauges.length; tank++) {
            gauges[tank].place(leftPos + menu.gaugeX(tank), topPos + FluidInboundMenu.GAUGE_Y);
        }
        panel.placeBeside(leftPos, topPos);
        panel.enableFluids(menu::fluidSide, menu::tankLabels, side -> side == RelativeSide.FRONT,
                side -> MenuButtons.press(minecraft, menu, SideConfigMenus.buttonOf(SideLayer.FLUIDS, side)),
                side -> MenuButtons.press(minecraft, menu, SideConfigMenus.buttonOfTank(side)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (int tank = 0; tank < gauges.length; tank++) {
            gauges[tank].renderTooltip(graphics, font, menu.fluid(tank), menu.capacity(), mouseX, mouseY);
        }
        panel.renderTooltip(graphics, font, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (int tank = 0; tank < gauges.length; tank++) {
            gauges[tank].render(graphics, menu.fluid(tank), menu.capacity());
        }
        PanelPainter.playerInventory(graphics, leftPos, topPos, FluidInboundMenu.INVENTORY_Y, menu.hotbarY());
        panel.render(graphics, font, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        for (int tank = 0; tank < gauges.length; tank++) {
            String number = Integer.toString(tank + 1);
            int x = menu.gaugeX(tank) + (FluidInboundMenu.GAUGE_WIDTH - font.width(number)) / 2;
            graphics.text(font, number, x, FluidInboundMenu.NUMBER_Y, COLOUR_TEXT, false);
        }
        Component line = stateLine();
        if (line == null) {
            return;
        }
        List<FormattedCharSequence> rows = font.split(line, imageWidth - 2 * MARGIN);
        for (int i = 0; i < Math.min(STATE_ROWS, rows.size()); i++) {
            graphics.text(font, rows.get(i), MARGIN, FluidInboundMenu.STATE_Y + i * 9, COLOUR_ALERT, false);
        }
    }

    /** Why the warehouse is offline, else why a tank waits, else nothing: the lamps tell idle from working. */
    private Component stateLine() {
        StationStatePayload state = ClientStationState.state();
        if (!state.online()) {
            return state.reason().isEmpty() ? null : Component.translatable("gui.servus_shuttles.terminal." + state.reason());
        }
        return menu.waitingLine();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public void removed() {
        super.removed();
        ClientStationState.clear();
    }
}
