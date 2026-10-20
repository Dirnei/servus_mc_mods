package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.datagen.model.ItemModelProvider;
import net.minecraft.data.PackOutput;

final class ServusDelicaciesItemModelProvider extends ItemModelProvider {

    ServusDelicaciesItemModelProvider(PackOutput output) {
        super(output, ServusDelicaciesIds.MOD_ID);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusDelicaciesItems.PRETZEL.get());
    }
}
