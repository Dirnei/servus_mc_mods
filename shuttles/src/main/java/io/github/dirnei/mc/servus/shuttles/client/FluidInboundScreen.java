package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.menu.FluidInboundMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Fluid Inbound GUI: the shared station GUI. */
public class FluidInboundScreen extends FluidStationScreen<FluidInboundMenu> {

    public FluidInboundScreen(FluidInboundMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FluidInboundMenu.IMAGE_HEIGHT, FluidInboundMenu.GAUGE_Y, FluidInboundMenu.STATE_Y);
    }
}
