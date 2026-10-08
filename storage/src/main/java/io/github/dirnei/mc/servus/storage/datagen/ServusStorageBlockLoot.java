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

final class ServusStorageBlockLoot extends BlockLootSubProvider {

    private static final List<Block> SELF_DROPPING = List.of(ServusStorageBlocks.CRATE.get(), ServusStorageBlocks.DOUBLE_CRATE.get(),
            ServusStorageBlocks.QUAD_CRATE.get(),
            ServusStorageBlocks.WAREHOUSE_CONTROLLER.get(), ServusStorageBlocks.COUNTER_TERMINAL.get(),
            ServusStorageBlocks.RAIL.get(), ServusStorageBlocks.ELEVATOR.get(),
            ServusStorageBlocks.INBOUND_STATION.get(), ServusStorageBlocks.ACCESS_POINT.get(), ServusStorageBlocks.SHUTTLE_GARAGE.get(), ServusStorageBlocks.OUTBOUND_STATION.get(),
            ServusStorageBlocks.CRAFTING_STATION.get(), ServusStorageBlocks.ORDER_BOARD.get());

    ServusStorageBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(this::dropSelf);
        // The rack keeps the wood it was crafted from.
        add(ServusStorageBlocks.HEAVY_RACK.get(), createSingleItemTable(ServusStorageBlocks.HEAVY_RACK.get())
                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                        .include(ServusStorageComponents.RACK_WOOD.get())));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> known = new ArrayList<>(SELF_DROPPING);
        known.add(ServusStorageBlocks.HEAVY_RACK.get());
        return known;
    }
}
