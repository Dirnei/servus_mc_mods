package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import io.github.dirnei.mc.servus.shuttles.menu.FluidStationMenu;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * The GUI shared by the fluid stations: the tank's gauge, the state line,
 * the player inventory and the Fluids side panel on the left. The state
 * line shows only while the warehouse is offline or the station waits.
 */
public abstract class FluidStationScreen<M extends FluidStationMenu> extends AbstractContainerScreen<M> {

    protected static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_ALERT = 0xFFB03030;
    private static final int MARGIN = 8;
    private static final int STATE_ROWS = 2;

    private final FluidGauge gauge = new FluidGauge(FluidStationMenu.GAUGE_WIDTH, FluidStationMenu.GAUGE_HEIGHT);
    private final SideConfigPanel panel = SideConfigPanel.fluidsOnly();
    private final int gaugeY;
    private final int stateY;

    protected FluidStationScreen(M menu, Inventory playerInventory, Component title, int imageHeight, int gaugeY, int stateY) {
        super(menu, playerInventory, title, FluidStationMenu.WIDTH, imageHeight);
        this.gaugeY = gaugeY;
        this.stateY = stateY;
        ClientStationState.clear();
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = menu.inventoryY() - 11;
        gauge.place(leftPos + menu.gaugeX(), topPos + gaugeY);
        panel.placeBeside(leftPos, topPos);
        panel.enableFluids(menu::fluidSide, menu::tankLabels, side -> side == RelativeSide.FRONT,
                side -> MenuButtons.press(minecraft, menu, SideConfigMenus.buttonOf(SideLayer.FLUIDS, side)),
                side -> MenuButtons.press(minecraft, menu, SideConfigMenus.buttonOfTank(side)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        gauge.renderTooltip(graphics, font, menu.fluid(), menu.capacity(), mouseX, mouseY);
        panel.renderTooltip(graphics, font, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        gauge.render(graphics, menu.fluid(), menu.capacity());
        PanelPainter.playerInventory(graphics, leftPos, topPos, menu.inventoryY(), menu.hotbarY());
        panel.render(graphics, font, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component line = stateLine();
        if (line == null) {
            return;
        }
        List<FormattedCharSequence> rows = font.split(line, imageWidth - 2 * MARGIN);
        for (int i = 0; i < Math.min(STATE_ROWS, rows.size()); i++) {
            graphics.text(font, rows.get(i), MARGIN, stateY + i * 9, COLOUR_ALERT, false);
        }
    }

    /** Why the warehouse is offline, else why the station waits, else nothing: the lamps tell idle from working. */
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
