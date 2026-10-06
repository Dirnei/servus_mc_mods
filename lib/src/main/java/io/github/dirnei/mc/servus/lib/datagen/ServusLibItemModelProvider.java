package io.github.dirnei.mc.servus.lib.datagen;

import io.github.dirnei.mc.servus.lib.ServusLibIds;
import io.github.dirnei.mc.servus.lib.registry.ServusLibItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusLibItemModelProvider extends ItemModelProvider {

    ServusLibItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ServusLibIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ServusLibItems.CONFIGURATION_CARROT.get());
        basicItem(ServusLibItems.GOGGLES.get());
    }
}
