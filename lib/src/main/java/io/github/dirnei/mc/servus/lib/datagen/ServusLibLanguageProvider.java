package io.github.dirnei.mc.servus.lib.datagen;

import io.github.dirnei.mc.servus.lib.ServusLibIds;
import io.github.dirnei.mc.servus.lib.registry.ServusLibItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusLibLanguageProvider extends LanguageProvider {

    ServusLibLanguageProvider(PackOutput output) {
        super(output, ServusLibIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ServusLibItems.WRENCH.get(), "Wrench");
        add(ServusLibItems.GOGGLES.get(), "Engineer Goggles");
        add("itemGroup.servus", "Servus");
        add("servus_lib.configuration.wrench", "Wrench");
        add("servus_lib.configuration.wrenchRotatesBlocks", "Wrench rotates blocks");
    }
}
