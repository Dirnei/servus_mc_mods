package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.crop.HopPoleBlock;
import io.github.dirnei.mc.servus.brewery.crop.HopStage;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import java.util.List;
import java.util.Set;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Barley drops by age like wheat; a Hop Pole drops itself, its rhizome at the foot and Hops from cones; the Malting
 * Floor, the Brewhouse parts, the Fermenting Vat and the Cask drop themselves.
 */
final class ServusBreweryBlockLoot extends BlockLootSubProvider {

    ServusBreweryBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        CropBlock barley = ServusBreweryBlocks.BARLEY.get();
        add(barley, createCropDrops(barley, ServusBreweryItems.BARLEY.get(), ServusBreweryItems.BARLEY_SEEDS.get(),
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(barley)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, CropBlock.MAX_AGE))));

        HopPoleBlock pole = ServusBreweryBlocks.HOP_POLE.get();
        add(pole, createSingleItemTable(pole)
                .withPool(LootPool.lootPool()
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(pole)
                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(HopPoleBlock.ROOTED, true)))
                        .add(LootItem.lootTableItem(ServusBreweryItems.HOP_RHIZOME.get())))
                .withPool(LootPool.lootPool()
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(pole)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(HopPoleBlock.HOP, HopStage.CONES.ordinal())))
                        .add(LootItem.lootTableItem(ServusBreweryItems.HOPS.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))));

        dropSelf(ServusBreweryBlocks.MALTING_FLOOR.get());
        dropSelf(ServusBreweryBlocks.BREWHOUSE_BRICKS.get());
        dropSelf(ServusBreweryBlocks.BREW_KETTLE.get());
        dropSelf(ServusBreweryBlocks.FERMENTING_VAT.get());
        dropSelf(ServusBreweryBlocks.CASK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(ServusBreweryBlocks.BARLEY.get(), ServusBreweryBlocks.HOP_POLE.get(), ServusBreweryBlocks.MALTING_FLOOR.get(),
                ServusBreweryBlocks.BREWHOUSE_BRICKS.get(), ServusBreweryBlocks.BREW_KETTLE.get(), ServusBreweryBlocks.FERMENTING_VAT.get(),
                ServusBreweryBlocks.CASK.get());
    }
}
