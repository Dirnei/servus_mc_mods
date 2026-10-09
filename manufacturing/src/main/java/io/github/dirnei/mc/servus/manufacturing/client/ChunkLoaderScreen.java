package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.MachineScreen;
import io.github.dirnei.mc.servus.manufacturing.menu.ChunkLoaderMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Chunk loader GUI: energy bar and a status line. */
public class ChunkLoaderScreen extends MachineScreen {

    private static final String[] STATUS_KEYS = {"no_power", "active", "team_limit", "ftb_chunks", "no_owner"};

    public ChunkLoaderScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, MachineScreens.LOADER);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        int status = ((ChunkLoaderMenu) menu).status();
        String key = STATUS_KEYS[Math.max(0, Math.min(status, STATUS_KEYS.length - 1))];
        guiGraphics.drawString(font, Component.translatable("gui.servus_manufacturing.chunk_loader." + key), 8, 40, 0x404040, false);
    }
}
