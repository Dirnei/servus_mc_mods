package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

final class ServusStorageBlockLoot extends BlockLootSubProvider {

    private static final List<Block> SELF_DROPPING = List.of(ServusStorageBlocks.CRATE.get(), ServusStorageBlocks.DOUBLE_CRATE.get(),
            ServusStorageBlocks.QUAD_CRATE.get(),
            ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), ServusStorageBlocks.COUNTER_TERMINAL.get(),
            ServusStorageBlocks.RAIL.get(), ServusStorageBlocks.ELEVATOR.get(), ServusStorageBlocks.HEAVY_RACK.get(),
            ServusStorageBlocks.DISPATCH_STATION.get());

    ServusStorageBlockLoot(HolderLookup.Provider registries) {
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
