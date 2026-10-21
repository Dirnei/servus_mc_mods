package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
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

final class ServusShuttlesBlockLoot extends BlockLootSubProvider {

    /** Blocks that keep the wood of their recipe. */
    private static final List<Block> WOODEN = List.of(
            ServusShuttlesBlocks.STORAGE_TERMINAL.get(), ServusShuttlesBlocks.INBOUND_STATION.get(), ServusShuttlesBlocks.PACKING_STATION.get(),
            ServusShuttlesBlocks.SHUTTLE_GARAGE.get(), ServusShuttlesBlocks.OUTBOUND_STATION.get(), ServusShuttlesBlocks.CRAFTING_STATION.get(),
            ServusShuttlesBlocks.CRAFTING_CPU.get(), ServusShuttlesBlocks.CPU_BUFFER.get(), ServusShuttlesBlocks.PRODUCTION_TERMINAL.get(),
            ServusShuttlesBlocks.FIELD_HEAD.get(), ServusShuttlesBlocks.RAIL.get(), ServusShuttlesBlocks.FARM_RAIL.get(),
            ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get(), ServusShuttlesBlocks.ELEVATOR.get());

    ServusShuttlesBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : WOODEN) {
            add(block, createSingleItemTable(block)
                    .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                            .include(WoodContent.WOOD.get())));
        }
        // The controller keeps its site name and FE.
        add(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get(), createSingleItemTable(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get())
                .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(WoodContent.WOOD.get())
                        .include(ServusShuttlesComponents.SITE_NAME.get())
                        .include(ServusShuttlesComponents.CONTROLLER_ENERGY.get())));
        // The rack keeps the wood it was crafted from.
        add(ServusShuttlesBlocks.HEAVY_RACK.get(), createSingleItemTable(ServusShuttlesBlocks.HEAVY_RACK.get())
                .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(ServusShuttlesComponents.RACK_WOOD.get())));
        // Pattern Storage keeps its patterns.
        add(ServusShuttlesBlocks.PATTERN_STORAGE.get(), createSingleItemTable(ServusShuttlesBlocks.PATTERN_STORAGE.get())
                .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(ServusShuttlesComponents.PATTERNS.get())
                        .include(WoodContent.WOOD.get())));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> known = new ArrayList<>(WOODEN);
        known.add(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get());
        known.add(ServusShuttlesBlocks.HEAVY_RACK.get());
        known.add(ServusShuttlesBlocks.PATTERN_STORAGE.get());
        return known;
    }
}
