package io.github.dirnei.mc.servus.manufacturing.alchemy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Handing items taken out of a build to the player. */
public final class PlayerItems {

    private PlayerItems() {
    }

    /** Puts the stack into the player's inventory and drops what does not fit; an empty stack does nothing. */
    public static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
