package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

/** Adds the Brewery seed tables to short grass and to plains and taiga village house chests. */
final class ServusBreweryLootModifierProvider extends GlobalLootModifierProvider {

    ServusBreweryLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, ServusBreweryIds.MOD_ID);
    }

    @Override
    protected void start() {
        addTable("short_grass", Blocks.SHORT_GRASS.getLootTable().orElseThrow(), ServusBreweryExtraLoot.SHORT_GRASS);
        addTable("village_plains_house", BuiltInLootTables.VILLAGE_PLAINS_HOUSE, ServusBreweryExtraLoot.VILLAGE_HOUSE);
        addTable("village_taiga_house", BuiltInLootTables.VILLAGE_TAIGA_HOUSE, ServusBreweryExtraLoot.VILLAGE_HOUSE);
    }

    private void addTable(String name, ResourceKey<LootTable> target, ResourceKey<LootTable> table) {
        LootItemCondition[] onTarget = {LootTableIdCondition.builder(target.identifier()).build()};
        add(name, new AddTableLootModifier(onTarget, IGlobalLootModifier.DEFAULT_PRIORITY, table));
    }
}
