package io.github.dirnei.mc.servus.foundation.logistics.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusLogisticsBlockTagsProvider extends BlockTagsProvider {

    ServusLogisticsBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                     ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ServusFoundationIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ServusLogisticsBlocks.UTILITY_DUCT.get(), ServusLogisticsBlocks.FLUID_TANK.get());
    }
}
