package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.CropLootTables;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.loot.CanItemPerformAbility;

/** The tables that hand out the first planting items: from grass and from village house chests. */
final class ServusDelicaciesSeedLoot {

    private static final float GRASS_CHANCE = 0.06F;
    private static final float VILLAGE_CHANCE = 0.5F;

    private ServusDelicaciesSeedLoot() {
    }

    /** Grass broken without shears sometimes gives one planting item; small seeds are three times as likely as a vegetable. */
    static Function<HolderLookup.Provider, LootTableSubProvider> grass() {
        return registries -> output -> {
            LootPool.Builder pool = LootPool.lootPool()
                    .when(CanItemPerformAbility.canItemPerformAbility(ItemAbilities.SHEARS_DIG).invert())
                    .when(LootItemRandomChanceCondition.randomChance(GRASS_CHANCE));
            for (CropKind crop : CropKind.values()) {
                pool.add(LootItem.lootTableItem(ServusDelicaciesItems.planting(crop)).setWeight(crop.seedId().isPresent() ? 3 : 1));
            }
            output.accept(CropLootTables.GRASS_SEEDS, LootTable.lootTable().withPool(pool));
        };
    }

    /** A village house chest sometimes holds one to three of one planting item. */
    static Function<HolderLookup.Provider, LootTableSubProvider> village() {
        return registries -> output -> {
            LootPool.Builder pool = LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(VILLAGE_CHANCE));
            for (CropKind crop : CropKind.values()) {
                pool.add(LootItem.lootTableItem(ServusDelicaciesItems.planting(crop))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))));
            }
            output.accept(CropLootTables.VILLAGE_SEEDS, LootTable.lootTable().withPool(pool));
        };
    }
}
