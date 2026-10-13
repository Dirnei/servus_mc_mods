package io.github.dirnei.mc.servus.storage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Sends menu button clicks from a screen to the server. */
final class MenuButtons {

    private MenuButtons() {
    }

    /** Sends {@code id} to the menu's {@code clickMenuButton} on the server; does nothing without a game mode. */
    static void press(Minecraft minecraft, AbstractContainerMenu menu, int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
