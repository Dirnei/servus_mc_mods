package io.github.dirnei.mc.servus.foundation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Sends menu button clicks from a screen to the server. Client only. */
public final class MenuButtons {

    private MenuButtons() {
    }

    /**
     * Sends {@code id} to the menu's {@code clickMenuButton} on the server.
     * Does nothing without a game mode; returns whether the click was sent.
     */
    public static boolean press(Minecraft minecraft, AbstractContainerMenu menu, int id) {
        if (minecraft == null || minecraft.gameMode == null) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }
}
