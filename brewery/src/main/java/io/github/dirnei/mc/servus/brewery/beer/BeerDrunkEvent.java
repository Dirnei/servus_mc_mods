package io.github.dirnei.mc.servus.brewery.beer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Posted on {@link NeoForge#EVENT_BUS} once each time a player finishes drinking a beer, on the server only. Listeners
 * such as the Rausch count drinks here; the event itself changes nothing.
 */
public final class BeerDrunkEvent extends PlayerEvent {

    private final ItemStack drink;

    public BeerDrunkEvent(Player player, ItemStack drink) {
        super(player);
        this.drink = drink;
    }

    /** A copy of the full glass as it was before drinking. */
    public ItemStack getDrink() {
        return drink;
    }
}
