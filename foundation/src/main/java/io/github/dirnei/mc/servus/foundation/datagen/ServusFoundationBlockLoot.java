package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationBlocks;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

/** The Data Cable drops itself. */
final class ServusFoundationBlockLoot extends BlockLootSubProvider {

    ServusFoundationBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ServusFoundationBlocks.DATA_CABLE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(ServusFoundationBlocks.DATA_CABLE.get());
    }
}
