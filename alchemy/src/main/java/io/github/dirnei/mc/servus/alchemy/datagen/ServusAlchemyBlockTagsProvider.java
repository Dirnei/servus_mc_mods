package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnome;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

final class ServusAlchemyBlockTagsProvider extends BlockTagsProvider {

    ServusAlchemyBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ServusAlchemyIds.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE).add(ServusAlchemyBlocks.PURIFIER.get()).add(ServusAlchemyBlocks.CATCH_BASIN.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ServusAlchemyBlocks.RESIDUE_CROCK.get())
                .add(ServusAlchemyBlocks.CALCINER.get())
                .add(ServusAlchemyBlocks.FIREBOX.get())
                .add(ServusAlchemyBlocks.ASH_PAN.get())
                .add(ServusAlchemyBlocks.ALEMBIC.get())
                .add(ServusAlchemyBlocks.RECEIVER.get())
                .add(ServusAlchemyBlocks.SULFUR_HOOD.get())
                .add(ServusAlchemyBlocks.COPPER_DUCT.get())
                .add(ServusAlchemyBlocks.VAT.get())
                .add(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get())
                .add(ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get())
                .add(ServusAlchemyBlocks.COAGULATOR.get())
                .add(ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get())
                .add(ServusAlchemyBlocks.ATHANOR.get())
                .add(ServusAlchemyBlocks.GNOME_HOLLOW.get())
                .add(ServusAlchemyBlocks.PRINCIPLE_DROPPER.get())
                .add(ServusAlchemyBlocks.SALAMANDER_FORGE.get())
                .add(ServusAlchemyBlocks.RECEPTACULUM.get())
                .add(ServusAlchemyBlocks.PRELUM.get())
                .add(ServusAlchemyBlocks.ANCIENT_HOLLOW.get());
        tag(OldGnome.WAKES).add(Blocks.DEEPSLATE);
    }
}
