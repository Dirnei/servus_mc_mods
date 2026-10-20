package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelProvider;
import net.minecraft.data.PackOutput;

/** Flat item models from the item textures; the Malting Floor's item model comes from its block model. */
final class ServusBreweryItemModelProvider extends ItemModelProvider {

    ServusBreweryItemModelProvider(PackOutput output) {
        super(output, ServusBreweryIds.MOD_ID);
    }

    @Override
    protected void registerModels() {
        ServusBreweryItems.CREATIVE_TAB_ORDER.stream()
                .filter(item -> item != ServusBreweryItems.MALTING_FLOOR)
                .forEach(item -> basicItem(item.get()));
    }
}
