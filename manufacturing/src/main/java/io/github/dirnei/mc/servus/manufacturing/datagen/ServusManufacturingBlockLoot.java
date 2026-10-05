package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

final class ServusManufacturingBlockLoot extends BlockLootSubProvider {

    ServusManufacturingBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ServusManufacturingBlocks.BURNER_EXTRACTOR.get());
        dropSelf(ServusManufacturingBlocks.BURNER_SMELTER.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(
                ServusManufacturingBlocks.BURNER_EXTRACTOR.get(),
                ServusManufacturingBlocks.BURNER_SMELTER.get());
    }
}
