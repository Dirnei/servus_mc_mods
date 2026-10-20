package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusBreweryLanguageProvider extends LanguageProvider {

    ServusBreweryLanguageProvider(PackOutput output) {
        super(output, ServusBreweryIds.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ServusBreweryIds.MOD_ID, "Servus: Brewery");
        add(ServusBreweryItems.BEER_GLASS.get(), "Beer Glass");
        add(ServusBreweryBlocks.BARLEY.get(), "Barley");
        add(ServusBreweryItems.BARLEY.get(), "Barley");
        add(ServusBreweryItems.BARLEY_SEEDS.get(), "Barley Seeds");
        add(ServusBreweryItems.HOPS.get(), "Hops");
        add(ServusBreweryItems.HOP_RHIZOME.get(), "Hop Rhizome");
        add(ServusBreweryBlocks.HOP_POLE.get(), "Hop Pole");
    }
}
