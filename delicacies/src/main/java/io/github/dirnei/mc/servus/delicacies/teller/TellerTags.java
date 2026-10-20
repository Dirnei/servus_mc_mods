package io.github.dirnei.mc.servus.delicacies.teller;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Item tags of the plated meals. */
public final class TellerTags {

    /** Dumplings that go on a Roast Pork Plate. */
    public static final TagKey<Item> DUMPLINGS = own("dumplings");
    /** Beers that go with the Weisswurst breakfast; the first one is handed back when the breakfast is finished. */
    public static final TagKey<Item> BREAKFAST_BEERS = own("breakfast_beers");

    private TellerTags() {
    }

    private static TagKey<Item> own(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, path));
    }
}
