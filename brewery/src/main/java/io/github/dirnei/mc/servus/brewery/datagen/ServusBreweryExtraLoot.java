package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.crop.HopPoleBlock;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import java.util.function.BiConsumer;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * The Brewery loot tables that belong to no block: what short grass and
 * village chests add (through loot modifiers) and what picking hop cones gives.
 * The chances here are the defaults a datapack may override.
 */
final class ServusBreweryExtraLoot {

    static final ResourceKey<LootTable> SHORT_GRASS = key("gameplay/short_grass");
    static final ResourceKey<LootTable> VILLAGE_HOUSE = key("chests/village_house");

    private ServusBreweryExtraLoot() {
    }

    private static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, path));
    }

    /** Seeds and rhizomes from short grass broken without shears. */
    record Grass(HolderLookup.Provider registries) implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            LootItemCondition.Builder noShears = MatchTool.toolMatches(
                    ItemPredicate.Builder.item().of(registries.lookupOrThrow(Registries.ITEM), Items.SHEARS)).invert();
            output.accept(SHORT_GRASS, LootTable.lootTable()
                    .withPool(LootPool.lootPool().when(noShears).when(LootItemRandomChanceCondition.randomChance(0.0625F))
                            .add(LootItem.lootTableItem(ServusBreweryItems.BARLEY_SEEDS.get())))
                    .withPool(LootPool.lootPool().when(noShears).when(LootItemRandomChanceCondition.randomChance(0.015F))
                            .add(LootItem.lootTableItem(ServusBreweryItems.HOP_RHIZOME.get()))));
        }
    }

    /** Seeds and rhizomes in plains and taiga village house chests. */
    record Chests(HolderLookup.Provider registries) implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            output.accept(VILLAGE_HOUSE, LootTable.lootTable()
                    .withPool(LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(0.4F))
                            .add(LootItem.lootTableItem(ServusBreweryItems.BARLEY_SEEDS.get())
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))))
                    .withPool(LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(0.3F))
                            .add(LootItem.lootTableItem(ServusBreweryItems.HOP_RHIZOME.get()))));
        }
    }

    /** Hops from a right click on a hop pole part with cones. */
    record Harvest(HolderLookup.Provider registries) implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            output.accept(HopPoleBlock.HARVEST_LOOT, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .add(LootItem.lootTableItem(ServusBreweryItems.HOPS.get())
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));
        }
    }
}
