package io.github.dirnei.mc.servus.foundation.filter;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Item tags of the filter chain. */
public final class FilterTags {

    /** {@code servus_foundation:filter_binders}: the starch that binds a Carbon Filter (wheat, any flour). */
    public static final TagKey<Item> FILTER_BINDERS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "filter_binders"));
    /** {@code c:flours}: flour of any grain, filled by food mods. */
    public static final TagKey<Item> FLOURS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "flours"));

    private FilterTags() {
    }
}
