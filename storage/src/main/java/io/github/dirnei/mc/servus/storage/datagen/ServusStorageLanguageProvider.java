package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusStorageLanguageProvider extends LanguageProvider {

    ServusStorageLanguageProvider(PackOutput output) {
        super(output, ServusStorageIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusStorageBlocks.CRATE.get(), "Crate");
        add(ServusStorageBlocks.RACK.get(), "Shelving Rack");
        add(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), "Warehouse Controller");
    }
}
