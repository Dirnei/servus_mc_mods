package io.github.dirnei.mc.servus.delicacies.kitchen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Item tags of the kitchen. */
public final class KitchenTags {

    /** {@code c:tools/knife}: knives of any kitchen mod cut on the Cutting Board. */
    public static final TagKey<Item> KNIVES = common("tools/knife");
    /** {@code c:dusts/salt}: table salt of any mod. */
    public static final TagKey<Item> SALT = common("dusts/salt");
    /** {@code c:dusts/ash}: ash of any mod, for lye. */
    public static final TagKey<Item> ASH = common("dusts/ash");
    /** {@code c:flours}: flour of any grain. */
    public static final TagKey<Item> FLOURS = common("flours");
    /** {@code c:flours/wheat}: wheat flour of any mod, kneaded into dough. */
    public static final TagKey<Item> WHEAT_FLOUR = common("flours/wheat");
    /** {@code c:foods/dough/wheat}: wheat dough. */
    public static final TagKey<Item> WHEAT_DOUGH = common("foods/dough/wheat");
    /** {@code c:foods/cheese}: cheese of any mod, for the Cordon Bleu. */
    public static final TagKey<Item> CHEESE = common("foods/cheese");
    /** {@code c:flours/mustard}: ground mustard seed. */
    public static final TagKey<Item> MUSTARD_FLOUR = common("flours/mustard");

    private KitchenTags() {
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}
