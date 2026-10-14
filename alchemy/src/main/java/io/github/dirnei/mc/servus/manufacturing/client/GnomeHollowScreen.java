package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.hollow.GnomeHollowMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        gauges.render(guiGraphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        GnomeHollowMenu hollow = (GnomeHollowMenu) menu;
        Component status;
        if (hollow.steps() == 0) {
            status = Component.translatable("gui.servus_manufacturing.gnome_hollow.empty");
        } else if (hollow.bound()) {
            status = Component.translatable("gui.servus_manufacturing.gnome_hollow.gnome");
        } else {
            status = Component.translatable("gui.servus_manufacturing.gnome_hollow.ripeness", hollow.ripeness(), hollow.steps());
        }
        guiGraphics.text(font, status, imageWidth - 8 - font.width(status), titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        gauges.renderTooltip(guiGraphics, font, mouseX, mouseY);
    }
}
