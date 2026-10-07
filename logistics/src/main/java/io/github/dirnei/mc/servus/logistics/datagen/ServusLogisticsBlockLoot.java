package io.github.dirnei.mc.servus.logistics.datagen;

import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsComponents;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * The duct drops its line items from code, so the block itself drops nothing.
 * The Fluid Tank drops itself carrying the fluid of its share.
 */
final class ServusLogisticsBlockLoot extends BlockLootSubProvider {

    private static final List<Block> KNOWN = List.of(ServusLogisticsBlocks.UTILITY_DUCT.get(), ServusLogisticsBlocks.FLUID_TANK.get());

    ServusLogisticsBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(ServusLogisticsBlocks.UTILITY_DUCT.get(), noDrop());
        Block tank = ServusLogisticsBlocks.FLUID_TANK.get();
        add(tank, LootTable.lootTable().withPool(applyExplosionCondition(tank, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(tank)
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(ServusLogisticsComponents.FLUID.get()))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return KNOWN;
    }
}
