package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.datagen.LogisticsDataParts;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusFoundationBlockTagsProvider extends BlockTagsProvider {

    ServusFoundationBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ServusFoundationIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        LogisticsDataParts.pickaxeBlocks().forEach(block -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block));
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(AdapterContent.BLOCK.get());
        MachineDataParts.selfDropping().forEach(block -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block));
    }
}
