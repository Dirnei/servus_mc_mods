package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.foundation.machine.menu.EnderAnchorMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ender Anchor GUI: energy bar and a status line. */
public class EnderAnchorScreen extends MachineScreen {

    private static final String[] STATUS_KEYS = {"no_power", "active", "team_limit", "ftb_chunks", "no_owner"};

    public EnderAnchorScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, MachineClient.ANCHOR);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        int status = ((EnderAnchorMenu) menu).status();
        String key = STATUS_KEYS[Math.max(0, Math.min(status, STATUS_KEYS.length - 1))];
        guiGraphics.text(font, Component.translatable("gui.servus_foundation.ender_anchor." + key), 8, 40, 0xFF404040, false);
    }
}
