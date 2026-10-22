package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** The Machine Adapter drops itself and keeps its wood. */
final class AdapterBlockLoot extends BlockLootSubProvider {

    AdapterBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(AdapterContent.BLOCK.get(), createSingleItemTable(AdapterContent.BLOCK.get())
                .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(MaterialContent.MATERIAL.get())));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(AdapterContent.BLOCK.get());
    }
}
