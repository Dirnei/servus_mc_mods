package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Mixer's screen: the machine screen with the arrow and the side panel, plus the tank's gauge right of the output.
 * It has its own gauge, since the machine screen's gauge comes with a pump's status line.
 */
public class MixerScreen extends MachineScreen {

    private static final int GAUGE_X = 148;
    private static final int GAUGE_Y = 17;

    private final FluidGauge gauge = new FluidGauge(FLUID_WIDTH, FLUID_HEIGHT);

    public MixerScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, MachineClient.MIXER);
    }

    @Override
    protected void init() {
        super.init();
        gauge.place(leftPos + GAUGE_X, topPos + GAUGE_Y);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        gauge.render(guiGraphics, menu.fluid(), menu.fluidCapacity());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        gauge.renderTooltip(guiGraphics, font, menu.fluid(), menu.fluidCapacity(), mouseX, mouseY);
    }
}
