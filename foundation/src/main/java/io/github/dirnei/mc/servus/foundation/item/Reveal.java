package io.github.dirnei.mc.servus.foundation.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * The reveal rules every Servus view follows. Configuring shows side markers,
 * analysing shows analysis views such as the data duct load, and either one
 * turns facade panels see-through. Worn goggles make both true.
 */
public final class Reveal {

    private Reveal() {
    }

    /** Goggles worn or a Configuration Carrot in either hand. */
    public static boolean configuring(Player player) {
        return GogglesItem.isWearing(player) || holds(player, ConfigurationCarrotItem.class);
    }

    /** Goggles worn or a Data Potato in either hand. */
    public static boolean analysing(Player player) {
        return GogglesItem.isWearing(player) || holds(player, DataPotatoItem.class);
    }

    /** Configuring or analysing. */
    public static boolean seesThroughPanels(Player player) {
        return configuring(player) || analysing(player);
    }

    private static boolean holds(Player player, Class<? extends Item> type) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (type.isInstance(player.getItemInHand(hand).getItem())) {
                return true;
            }
        }
        return false;
    }
}
