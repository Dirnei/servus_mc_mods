package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusDelicaciesLanguageProvider extends LanguageProvider {

    ServusDelicaciesLanguageProvider(PackOutput output) {
        super(output, ServusDelicaciesIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ServusDelicaciesIds.MOD_ID, "Servus: Delicacies");
        add(ServusDelicaciesItems.PRETZEL.get(), "Pretzel");
    }
}
