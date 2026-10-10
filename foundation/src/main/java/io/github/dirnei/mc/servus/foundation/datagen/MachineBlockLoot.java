package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlock;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Foundation's machines drop themselves; their contents drop from the block entity. */
final class MachineBlockLoot extends BlockLootSubProvider {

    MachineBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : MachineDataParts.selfDropping()) {
            if (block instanceof BurnerPumpBlock) {
                // Two blocks tall: only the lower half drops the item, like a door.
                add(block, createSinglePropConditionTable(block, BurnerPumpBlock.HALF, DoubleBlockHalf.LOWER));
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
