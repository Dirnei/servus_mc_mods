package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Calciner: input, progress, energy and the three principle gauges. */
public class CalcinerScreen extends MachineScreen {

    public static final Layout LAYOUT = Layout.BURNER;

    private final PrincipleGauges gauges;

    public CalcinerScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, LAYOUT);
        this.gauges = new PrincipleGauges((PrincipleMenu) menu);
    }

    @Override
    protected void init() {
        super.init();
        gauges.place(leftPos, topPos);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        gauges.render(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        gauges.renderTooltip(guiGraphics, font, mouseX, mouseY);
    }

    /** A bucket on the cursor dropped on a gauge takes that principle out. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft != null && gauges.click(minecraft, menu.containerId, menu.getCarried(), mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
