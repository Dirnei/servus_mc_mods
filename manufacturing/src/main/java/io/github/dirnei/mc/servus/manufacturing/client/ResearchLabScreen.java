package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.manufacturing.research.ClientResearch;
import io.github.dirnei.mc.servus.manufacturing.research.Techs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Research Lab GUI: pack slots, progress, energy, current research and the tech tree button. */
public class ResearchLabScreen extends MachineScreen {

    public ResearchLabScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, MachineScreens.LAB);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.servus_manufacturing.tech_tree"),
                        button -> minecraft.setScreen(new TechTreeScreen(this)))
                .bounds(leftPos + 96, topPos + 56, 50, 16)
                .build());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        Component current = ClientResearch.current()
                .flatMap(id -> minecraft.level == null ? java.util.Optional.<Techs.Entry>empty() : Techs.get(minecraft.level.registryAccess(), id))
                .map(Techs.Entry::name)
                .orElse(Component.translatable("gui.servus_manufacturing.no_research"));
        guiGraphics.drawString(font, current, 8, 60, 0x404040, false);
    }
}
