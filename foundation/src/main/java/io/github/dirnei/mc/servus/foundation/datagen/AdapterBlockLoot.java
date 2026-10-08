package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;

/** The Machine Adapter drops itself. */
final class AdapterBlockLoot extends BlockLootSubProvider {

    AdapterBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(AdapterContent.BLOCK.get(), createSingleItemTable(AdapterContent.BLOCK.get())
                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                        .include(WoodContent.WOOD.get())));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(AdapterContent.BLOCK.get());
    }
}
