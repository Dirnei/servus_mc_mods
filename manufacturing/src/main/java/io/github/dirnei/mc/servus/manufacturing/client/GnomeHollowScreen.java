package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.hollow.GnomeHollowMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Gnome Hollow: sample, output, the three principle gauges, the vein's ripeness and its gnome. */
public class GnomeHollowScreen extends MachineScreen {

    public static final Layout LAYOUT = new Layout(false, true, false, MachineScreens.gui("coagulator"));

    private final PrincipleGauges gauges;

    public GnomeHollowScreen(MachineMenu menu, Inventory playerInventory, Component title) {
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
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        GnomeHollowMenu hollow = (GnomeHollowMenu) menu;
        Component status;
        if (hollow.steps() == 0) {
            status = Component.translatable("gui.servus_manufacturing.gnome_hollow.empty");
        } else if (hollow.bound()) {
            status = Component.translatable("gui.servus_manufacturing.gnome_hollow.gnome");
        } else {
            status = Component.translatable("gui.servus_manufacturing.gnome_hollow.ripeness", hollow.ripeness(), hollow.steps());
        }
        guiGraphics.drawString(font, status, imageWidth - 8 - font.width(status), titleLabelY, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        gauges.renderTooltip(guiGraphics, font, mouseX, mouseY);
    }
}
