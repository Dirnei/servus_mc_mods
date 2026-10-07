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
                .add(ServusManufacturingBlocks.BURNER_EXTRACTOR.get())
                .add(ServusManufacturingBlocks.BURNER_SMELTER.get())
                .add(ServusManufacturingBlocks.COAL_GENERATOR.get())
                .add(ServusManufacturingBlocks.ASSEMBLER.get())
                .add(ServusManufacturingBlocks.RESEARCH_LAB.get())
                .add(ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get())
                .add(ServusManufacturingBlocks.CRUSHER.get())
                .add(ServusManufacturingBlocks.CHUNK_LOADER.get())
                .add(ServusManufacturingBlocks.BURNER_PUMP.get())
                .add(ServusManufacturingBlocks.ELECTRIC_PUMP.get());
    }
}
