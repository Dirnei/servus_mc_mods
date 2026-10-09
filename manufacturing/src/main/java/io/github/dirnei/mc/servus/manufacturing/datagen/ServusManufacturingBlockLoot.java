package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

final class ServusManufacturingBlockLoot extends BlockLootSubProvider {

    private static final List<Block> SELF_DROPPING = List.of(
            ServusManufacturingBlocks.BURNER_EXTRACTOR.get(),
            ServusManufacturingBlocks.BURNER_SMELTER.get(),
            ServusManufacturingBlocks.ASSEMBLER.get(),
            ServusManufacturingBlocks.RESEARCH_LAB.get(),
            ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get(),
            ServusManufacturingBlocks.CRUSHER.get());

    ServusManufacturingBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(this::dropSelf);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return SELF_DROPPING;
    }
}
