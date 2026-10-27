package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.menu.FluidInboundMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Fluid Inbound GUI: the shared station GUI with each tank's number below its gauge. */
public class FluidInboundScreen extends FluidStationScreen<FluidInboundMenu> {

    public FluidInboundScreen(FluidInboundMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FluidInboundMenu.IMAGE_HEIGHT, FluidInboundMenu.GAUGE_Y, FluidInboundMenu.STATE_Y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        for (int tank = 0; tank < menu.tanks(); tank++) {
            String number = Integer.toString(tank + 1);
            int x = menu.gaugeX(tank) + (FluidInboundMenu.GAUGE_WIDTH - font.width(number)) / 2;
            graphics.text(font, number, x, FluidInboundMenu.NUMBER_Y, COLOUR_TEXT, false);
        }
    }
}
