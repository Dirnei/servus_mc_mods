package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

/** Composter chances of the crop items and the soaked and sprouted grain, as vanilla wheat seeds and wheat. */
final class ServusBreweryDataMapProvider extends DataMapProvider {

    private static final float SEED_CHANCE = 0.3F;
    private static final float CROP_CHANCE = 0.65F;

    ServusBreweryDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(NeoForgeDataMaps.COMPOSTABLES)
                .add(ServusBreweryItems.BARLEY_SEEDS.getKey(), new Compostable(SEED_CHANCE), false)
                .add(ServusBreweryItems.HOP_RHIZOME.getKey(), new Compostable(SEED_CHANCE), false)
                .add(ServusBreweryItems.BARLEY.getKey(), new Compostable(CROP_CHANCE), false)
                .add(ServusBreweryItems.HOPS.getKey(), new Compostable(CROP_CHANCE), false)
                .add(ServusBreweryItems.SOAKED_BARLEY.getKey(), new Compostable(CROP_CHANCE), false)
                .add(ServusBreweryItems.SOAKED_WHEAT.getKey(), new Compostable(CROP_CHANCE), false)
                .add(ServusBreweryItems.GREEN_MALT.getKey(), new Compostable(CROP_CHANCE), false)
                .add(ServusBreweryItems.GREEN_WHEAT_MALT.getKey(), new Compostable(CROP_CHANCE), false);
    }
}
