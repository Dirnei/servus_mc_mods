package io.github.dirnei.mc.servus.lib.datagen;

import io.github.dirnei.mc.servus.lib.registry.ServusLibBlocks;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

/** The Data Cable drops itself. */
final class ServusLibBlockLoot extends BlockLootSubProvider {

    ServusLibBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ServusLibBlocks.DATA_CABLE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(ServusLibBlocks.DATA_CABLE.get());
    }
}
