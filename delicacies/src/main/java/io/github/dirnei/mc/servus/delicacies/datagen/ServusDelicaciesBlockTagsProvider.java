package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

/** The crop blocks count as crops for bees and villagers and keep their farmland from drying back to dirt. */
final class ServusDelicaciesBlockTagsProvider extends BlockTagsProvider {

    ServusDelicaciesBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusDelicaciesIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (CropKind crop : CropKind.values()) {
            tag(BlockTags.CROPS).add(ServusDelicaciesBlocks.crop(crop));
            tag(BlockTags.MAINTAINS_FARMLAND).add(ServusDelicaciesBlocks.crop(crop));
        }
    }
}
