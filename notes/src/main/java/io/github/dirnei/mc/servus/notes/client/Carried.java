package io.github.dirnei.mc.servus.notes.client;

import net.minecraft.world.entity.player.Player;

/** How many of a goal's item a player carries: main inventory, armour and offhand, counted on their own client. */
public final class Carried {

    private Carried() {
    }

    /** The count of {@code itemId} in the player's inventory; 0 for an unknown item. */
    public static int count(Player player, String itemId) {
        return GoalItems.item(itemId).map(item -> player.getInventory().countItem(item)).orElse(0);
    }
}
