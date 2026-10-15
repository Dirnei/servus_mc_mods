package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;

final class ServusStorageBlockLoot extends BlockLootSubProvider {

    private static final List<Block> SELF_DROPPING = List.of(ServusStorageBlocks.ELEVATOR.get());

    /** Blocks that keep the wood of their recipe. */
    private static final List<Block> WOODEN = List.of(ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(),
            ServusStorageBlocks.STORAGE_TERMINAL.get(), ServusStorageBlocks.INBOUND_STATION.get(), ServusStorageBlocks.PACKING_STATION.get(),
            ServusStorageBlocks.SHUTTLE_GARAGE.get(), ServusStorageBlocks.OUTBOUND_STATION.get(), ServusStorageBlocks.CRAFTING_STATION.get(),
            ServusStorageBlocks.CRAFTING_CPU.get(), ServusStorageBlocks.CPU_BUFFER.get(), ServusStorageBlocks.PRODUCTION_TERMINAL.get(),
            ServusStorageBlocks.FIELD_HEAD.get(), ServusStorageBlocks.RAIL.get(), ServusStorageBlocks.FARM_RAIL.get(),
            ServusStorageBlocks.SHUTTLE_WORKSHOP.get());

    ServusStorageBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(this::dropSelf);
        for (Block block : WOODEN) {
            add(block, createSingleItemTable(block)
                    .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                            .include(WoodContent.WOOD.get())));
        }
        // The rack keeps the wood it was crafted from.
        add(ServusStorageBlocks.HEAVY_RACK.get(), createSingleItemTable(ServusStorageBlocks.HEAVY_RACK.get())
                .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(ServusStorageComponents.RACK_WOOD.get())));
        // Pattern Storage keeps its patterns.
        add(ServusStorageBlocks.PATTERN_STORAGE.get(), createSingleItemTable(ServusStorageBlocks.PATTERN_STORAGE.get())
                .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(ServusStorageComponents.PATTERNS.get())
                        .include(WoodContent.WOOD.get())));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> known = new ArrayList<>(SELF_DROPPING);
        known.addAll(WOODEN);
        known.add(ServusStorageBlocks.HEAVY_RACK.get());
        known.add(ServusStorageBlocks.PATTERN_STORAGE.get());
        return known;
    }
}
