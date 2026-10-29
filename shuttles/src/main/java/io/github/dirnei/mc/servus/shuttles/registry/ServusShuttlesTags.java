package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Item tags of Shuttles that blocks check. */
public final class ServusShuttlesTags {

    /** Items that place against a rack instead of opening its GUI: racks and the Warehouse Rail. */
    public static final TagKey<Item> PLACES_ON_RACKS = ItemTags.create(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "places_on_racks"));

    private ServusShuttlesTags() {
    }

    /** Whether a right click with {@code stack} on a rack places it instead of opening the rack's GUI. */
    public static boolean placesOnRack(ItemStack stack) {
        return stack.is(PLACES_ON_RACKS);
    }
}
