package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

final class ServusManufacturingBlockLoot extends BlockLootSubProvider {

    private static final List<Block> SELF_DROPPING = List.of(
            ServusManufacturingBlocks.CALCINER.get(),
            ServusManufacturingBlocks.FIREBOX.get(),
            ServusManufacturingBlocks.ASH_PAN.get(),
            ServusManufacturingBlocks.ALEMBIC.get(),
            ServusManufacturingBlocks.RECEIVER.get(),
            ServusManufacturingBlocks.SULFUR_HOOD.get(),
            ServusManufacturingBlocks.SULFUR_DUCT.get(),
            ServusManufacturingBlocks.PURIFIER.get(),
            ServusManufacturingBlocks.CATCH_BASIN.get(),
            ServusManufacturingBlocks.VAT.get(),
            ServusManufacturingBlocks.COAGULATOR.get(),
            ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get(),
            ServusManufacturingBlocks.ATHANOR.get(),
            ServusManufacturingBlocks.GNOME_HOLLOW.get());

    ServusManufacturingBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(this::dropSelf);
        // A brick of a formed Calciner gives the Bricks back.
        dropOther(ServusManufacturingBlocks.CALCINER_SHELL.get(), Items.BRICKS);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(SELF_DROPPING.stream(), Stream.of(ServusManufacturingBlocks.CALCINER_SHELL.get())).toList();
    }
}
