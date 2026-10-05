package io.github.dirnei.mc.servus.logistics.datagen;

import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

/** The duct drops its channel items from code, so the block itself drops nothing. */
final class ServusLogisticsBlockLoot extends BlockLootSubProvider {

    private static final List<Block> KNOWN = List.of(ServusLogisticsBlocks.UTILITY_DUCT.get());

    ServusLogisticsBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(ServusLogisticsBlocks.UTILITY_DUCT.get(), noDrop());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return KNOWN;
    }
}
