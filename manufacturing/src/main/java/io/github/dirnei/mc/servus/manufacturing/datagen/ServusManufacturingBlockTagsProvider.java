package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

final class ServusManufacturingBlockTagsProvider extends BlockTagsProvider {

    ServusManufacturingBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusManufacturingIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE).add(ServusManufacturingBlocks.PURIFIER.get()).add(ServusManufacturingBlocks.CATCH_BASIN.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ServusManufacturingBlocks.RESIDUE_CROCK.get())
                .add(ServusManufacturingBlocks.CALCINER.get())
                .add(ServusManufacturingBlocks.FIREBOX.get())
                .add(ServusManufacturingBlocks.ASH_PAN.get())
                .add(ServusManufacturingBlocks.ALEMBIC.get())
                .add(ServusManufacturingBlocks.RECEIVER.get())
                .add(ServusManufacturingBlocks.SULFUR_HOOD.get())
                .add(ServusManufacturingBlocks.COPPER_DUCT.get())
                .add(ServusManufacturingBlocks.VAT.get())
                .add(ServusManufacturingBlocks.CALCINER_STONE_BRICKS.get())
                .add(ServusManufacturingBlocks.CALCINER_DEEPSLATE_TILES.get())
                .add(ServusManufacturingBlocks.COAGULATOR.get())
                .add(ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get())
                .add(ServusManufacturingBlocks.ATHANOR.get())
                .add(ServusManufacturingBlocks.GNOME_HOLLOW.get());
    }
}
