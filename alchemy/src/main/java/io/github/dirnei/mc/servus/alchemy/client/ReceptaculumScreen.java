package io.github.dirnei.mc.servus.alchemy.client;

import io.github.dirnei.mc.servus.alchemy.homunculus.ReceptaculumBlockEntity;
import io.github.dirnei.mc.servus.alchemy.homunculus.ReceptaculumMenu;
import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Receptaculum's screen: the gift slots and the Elixir slot with their names above the 27 yield slots. */
public class ReceptaculumScreen extends MachineScreen {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 204;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final Component GIFTS = Component.translatable("gui.servus_alchemy.receptaculum.gifts");
    private static final Component ELIXIR = Component.translatable("gui.servus_alchemy.receptaculum.elixir");

    public ReceptaculumScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, MachineScreens.RECEPTACULUM, WIDTH, HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        int labelY = ReceptaculumMenu.GIFT_Y + 5;
        guiGraphics.text(font, GIFTS, 8 + ReceptaculumBlockEntity.GIFT_SLOTS * 18 + 4, labelY, LABEL_COLOR, false);
        guiGraphics.text(font, ELIXIR, ReceptaculumMenu.ELIXIR_X - 4 - font.width(ELIXIR), labelY, LABEL_COLOR, false);
    }
}
