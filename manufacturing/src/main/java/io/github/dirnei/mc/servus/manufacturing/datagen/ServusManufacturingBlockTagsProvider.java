package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusManufacturingBlockTagsProvider extends BlockTagsProvider {

    ServusManufacturingBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                         ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ServusManufacturingIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ServusManufacturingBlocks.CALCINER.get())
                .add(ServusManufacturingBlocks.COAGULATOR.get())
                .add(ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get())
                .add(ServusManufacturingBlocks.ATHANOR.get());
    }
}
