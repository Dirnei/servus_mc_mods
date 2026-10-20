package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelProvider;
import net.minecraft.data.PackOutput;

final class ServusBreweryItemModelProvider extends ItemModelProvider {

    ServusBreweryItemModelProvider(PackOutput output) {
        super(output, ServusBreweryIds.MOD_ID);
    }

    @Override
    protected void registerModels() {
        ServusBreweryItems.CREATIVE_TAB_ORDER.forEach(item -> basicItem(item.get()));
    }
}
