package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Athanor: item, result, the three principle gauges and the stage its structure stands for. */
public class AthanorScreen extends MachineScreen {

    public static final Layout LAYOUT = new Layout(false, true, false, MachineScreens.gui("coagulator"));
    private static final String[] STAGES = {"nigredo", "albedo", "citrinitas", "rubedo"};

    private final PrincipleGauges gauges;

    public AthanorScreen(MachineMenu menu, Inventory playerInventory, Component title) {
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
        int tier = Math.max(0, Math.min(STAGES.length - 1, ((AthanorMenu) menu).tier()));
        Component stage = Component.translatable("gui.servus_manufacturing.athanor.stage." + STAGES[tier]);
        guiGraphics.drawString(font, stage, imageWidth - 8 - font.width(stage), titleLabelY, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        gauges.renderTooltip(guiGraphics, font, mouseX, mouseY);
    }
}
