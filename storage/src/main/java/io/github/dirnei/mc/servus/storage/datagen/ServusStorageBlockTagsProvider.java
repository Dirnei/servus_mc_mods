package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ServusStorageBlockTagsProvider extends BlockTagsProvider {

    ServusStorageBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                   ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ServusStorageIds.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE).add(ServusStorageBlocks.CRATE.get()).add(ServusStorageBlocks.DOUBLE_CRATE.get())
                .add(ServusStorageBlocks.QUAD_CRATE.get())
                .add(ServusStorageBlocks.COUNTER_TERMINAL.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get()).add(ServusStorageBlocks.RAIL.get())
                .add(ServusStorageBlocks.ELEVATOR.get()).add(ServusStorageBlocks.HEAVY_RACK.get())
                .add(ServusStorageBlocks.INBOUND_STATION.get()).add(ServusStorageBlocks.SHUTTLE_GARAGE.get());
    }
}
