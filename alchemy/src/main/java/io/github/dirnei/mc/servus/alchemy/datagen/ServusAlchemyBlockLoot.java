package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

final class ServusAlchemyBlockLoot extends BlockLootSubProvider {

    private static final List<Block> SELF_DROPPING = List.of(
            ServusAlchemyBlocks.CALCINER.get(),
            ServusAlchemyBlocks.FIREBOX.get(),
            ServusAlchemyBlocks.ASH_PAN.get(),
            ServusAlchemyBlocks.ALEMBIC.get(),
            ServusAlchemyBlocks.RECEIVER.get(),
            ServusAlchemyBlocks.SULFUR_HOOD.get(),
            ServusAlchemyBlocks.COPPER_DUCT.get(),
            ServusAlchemyBlocks.PURIFIER.get(),
            ServusAlchemyBlocks.CATCH_BASIN.get(),
            ServusAlchemyBlocks.RESIDUE_CROCK.get(),
            ServusAlchemyBlocks.VAT.get(),
            ServusAlchemyBlocks.COAGULATOR.get(),
            ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get(),
            ServusAlchemyBlocks.ATHANOR.get(),
            ServusAlchemyBlocks.GNOME_HOLLOW.get());

    ServusAlchemyBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(this::dropSelf);
        dropOther(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get(), Items.STONE_BRICKS);
        dropOther(ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get(), Items.DEEPSLATE_TILES);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(SELF_DROPPING.stream(), Stream.of(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get(),
                ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get())).toList();
    }
}
