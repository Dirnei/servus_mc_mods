package io.github.dirnei.mc.servus.foundation.datagen;

import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

/** Foundation's machines drop themselves; their contents drop from the block entity. */
final class MachineBlockLoot extends BlockLootSubProvider {

    MachineBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        MachineDataParts.selfDropping().forEach(this::dropSelf);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return MachineDataParts.selfDropping();
    }
}
