package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.crop.CropStages;
import io.github.dirnei.mc.servus.delicacies.crop.DelicaciesCropBlock;
import io.github.dirnei.mc.servus.delicacies.meat.LeberkaeseBlock;
import io.github.dirnei.mc.servus.delicacies.meat.LoafSlices;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.teller.MealBlock;
import io.github.dirnei.mc.servus.delicacies.teller.MealPortions;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Crop drops like vanilla (wheat style for seeded crops, carrot style for self planting ones); kitchen blocks drop themselves. */
final class ServusDelicaciesBlockLoot extends BlockLootSubProvider {

    ServusDelicaciesBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (CropKind crop : CropKind.values()) {
            DelicaciesCropBlock block = ServusDelicaciesBlocks.crop(crop);
            Item harvest = ServusDelicaciesItems.harvest(crop);
            add(block, crop.selfPlanting()
                    ? selfPlantingDrops(block, harvest, ripe(block))
                    : createCropDrops(block, harvest, ServusDelicaciesItems.planting(crop), ripe(block)));
        }
        for (Block block : kitchenBlocks()) {
            dropSelf(block);
        }
        add(ServusDelicaciesBlocks.LEBERKAESE.get(), loafDrops(ServusDelicaciesBlocks.LEBERKAESE.get()));
        dropSelf(ServusDelicaciesBlocks.PLATE.get());
        ServusDelicaciesBlocks.MEALS.values().forEach(meal -> add(meal.get(), mealDrops(meal.get())));
    }

    /** A full meal drops itself, a started one only its Plate. */
    private static LootTable.Builder mealDrops(MealBlock meal) {
        LootItemCondition.Builder full = LootItemBlockStatePropertyCondition.hasBlockStateProperties(meal)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(MealBlock.PORTIONS, MealPortions.FULL));
        return LootTable.lootTable()
                .withPool(LootPool.lootPool().when(full).add(LootItem.lootTableItem(meal)))
                .withPool(LootPool.lootPool().when(full.invert()).add(LootItem.lootTableItem(ServusDelicaciesItems.PLATE.get())));
    }

    /** An uncut loaf drops itself, a cut one the slices still on it. */
    private static LootTable.Builder loafDrops(Block loaf) {
        LootTable.Builder table = LootTable.lootTable()
                .withPool(LootPool.lootPool().when(cutIs(loaf, 0)).add(LootItem.lootTableItem(loaf)));
        for (int cut = 1; cut <= LoafSlices.MAX_CUT; cut++) {
            table.withPool(LootPool.lootPool().when(cutIs(loaf, cut))
                    .add(LootItem.lootTableItem(ServusDelicaciesItems.LEBERKAESE_SLICE.get())
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(LoafSlices.left(cut))))));
        }
        return table;
    }

    private static LootItemCondition.Builder cutIs(Block loaf, int cut) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(loaf)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(LeberkaeseBlock.CUT, cut));
    }

    /** One item at any age, and a Fortune raised binomial bonus when ripe, like vanilla carrots. */
    private LootTable.Builder selfPlantingDrops(Block block, Item item, LootItemCondition.Builder ripe) {
        return applyExplosionDecay(block, LootTable.lootTable()
                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(item)))
                .withPool(LootPool.lootPool()
                        .when(ripe)
                        .add(LootItem.lootTableItem(item)
                                .apply(ApplyBonusCount.addBonusBinomialDistributionCount(
                                        registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                                        0.5714286F, 3)))));
    }

    private static LootItemCondition.Builder ripe(Block block) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, CropStages.MAX_AGE));
    }

    private static List<Block> kitchenBlocks() {
        return List.of(ServusDelicaciesBlocks.STOVE_TILE.get(), ServusDelicaciesBlocks.POT.get(), ServusDelicaciesBlocks.PAN.get(),
                ServusDelicaciesBlocks.BAKING_OVEN.get(), ServusDelicaciesBlocks.CUTTING_BOARD.get(),
                ServusDelicaciesBlocks.FERMENTING_CROCK.get(), ServusDelicaciesBlocks.AQUARIUM_FLOOR.get(),
                ServusDelicaciesBlocks.AQUARIUM_LID.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.of(Arrays.stream(CropKind.values()).<Block>map(ServusDelicaciesBlocks::crop), kitchenBlocks().stream(),
                Stream.<Block>of(ServusDelicaciesBlocks.LEBERKAESE.get(), ServusDelicaciesBlocks.PLATE.get()),
                ServusDelicaciesBlocks.MEALS.values().stream().<Block>map(meal -> meal.get())).flatMap(blocks -> blocks).toList();
    }
}
