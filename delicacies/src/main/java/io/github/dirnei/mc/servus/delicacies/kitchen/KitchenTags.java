package io.github.dirnei.mc.servus.delicacies.kitchen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Item tags of the kitchen. */
public final class KitchenTags {

    /** {@code c:tools/knife}: knives of any kitchen mod cut on the Cutting Board. */
    public static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/knife"));

    private KitchenTags() {
    }
}
