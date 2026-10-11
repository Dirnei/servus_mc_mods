package io.github.dirnei.mc.servus.foundation.datagen.model;

import net.minecraft.resources.Identifier;

/** An item model under {@code models/item/}; the item model definition under {@code items/} is written with it. */
public class ItemModelBuilder extends ModelBuilder<ItemModelBuilder> {

    public ItemModelBuilder(Identifier location) {
        super(location);
    }
}
