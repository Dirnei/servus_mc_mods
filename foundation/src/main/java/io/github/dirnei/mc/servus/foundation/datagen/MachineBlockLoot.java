package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.machine.TwoHalves;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Foundation's machines drop themselves; their contents drop from the block
 * entity. Two block machines drop only from the lower half, like a door.
 */
final class MachineBlockLoot extends BlockLootSubProvider {

    MachineBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : MachineDataParts.selfDropping()) {
            if (block.defaultBlockState().hasProperty(TwoHalves.HALF)) {
                add(block, createSinglePropConditionTable(block, TwoHalves.HALF, DoubleBlockHalf.LOWER));
            } else {
                dropSelf(block);
            }
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return MachineDataParts.selfDropping();
    }
}
