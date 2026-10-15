package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.salamander.Salamander;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.world.level.biome.Biomes;

final class ServusAlchemyBiomeTagsProvider extends BiomeTagsProvider {

    ServusAlchemyBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusAlchemyIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(Salamander.LAVA_BIOMES).add(Biomes.NETHER_WASTES).add(Biomes.BASALT_DELTAS);
    }
}
