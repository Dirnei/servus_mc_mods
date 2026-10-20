package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

/** Barley is a crop that keeps its farmland; the Hop Pole keeps farmland too and is mined with an axe, as is the Malting Floor. */
final class ServusBreweryBlockTagsProvider extends BlockTagsProvider {

    ServusBreweryBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusBreweryIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.CROPS).add(ServusBreweryBlocks.BARLEY.get());
        tag(BlockTags.MAINTAINS_FARMLAND).add(ServusBreweryBlocks.BARLEY.get()).add(ServusBreweryBlocks.HOP_POLE.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ServusBreweryBlocks.HOP_POLE.get()).add(ServusBreweryBlocks.MALTING_FLOOR.get());
    }
}
