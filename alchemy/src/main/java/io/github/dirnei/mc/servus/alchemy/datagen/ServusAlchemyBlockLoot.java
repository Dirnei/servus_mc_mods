package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RubedoObeliskBlock;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatCrystal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.MobEffectsPredicate;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

final class ServusAlchemyBlockLoot extends BlockLootSubProvider {

    private static final List<Block> SELF_DROPPING = List.of(
            ServusAlchemyBlocks.CALCINER.get(),
            ServusAlchemyBlocks.FIREBOX.get(),
            ServusAlchemyBlocks.ASH_PAN.get(),
            ServusAlchemyBlocks.ALEMBIC.get(),
            ServusAlchemyBlocks.RECEIVER.get(),
            ServusAlchemyBlocks.SULFUR_HOOD.get(),
            ServusAlchemyBlocks.COPPER_DUCT.get(),
            ServusAlchemyBlocks.PURIFIER.get(),
            ServusAlchemyBlocks.CATCH_BASIN.get(),
            ServusAlchemyBlocks.RESIDUE_CROCK.get(),
            ServusAlchemyBlocks.CUCURBIT.get(),
            ServusAlchemyBlocks.DUNG_BED.get(),
            ServusAlchemyBlocks.BED_HATCH.get(),
            ServusAlchemyBlocks.COAGULATOR.get(),
            ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get(),
            ServusAlchemyBlocks.ATHANOR.get(),
            ServusAlchemyBlocks.PRINCIPLE_DROPPER.get(),
            ServusAlchemyBlocks.SALAMANDER_FORGE.get(),
            ServusAlchemyBlocks.RECEPTACULUM.get(),
            ServusAlchemyBlocks.PRELUM.get(),
            ServusAlchemyBlocks.VAS_HERMETICUM.get());

    ServusAlchemyBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(this::dropSelf);
        dropOther(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get(), Items.STONE_BRICKS);
        dropOther(ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get(), Items.DEEPSLATE_TILES);
        add(ServusAlchemyBlocks.GNOME_HOLLOW.get(), gnomeHollow());
        add(ServusAlchemyBlocks.VAT.get(), vat());
        add(ServusAlchemyBlocks.ANCIENT_HOLLOW.get(), ancientHollow());
        add(ServusAlchemyBlocks.RUBEDO_OBELISK.get(), rubedoObelisk());
    }

    /** The vat itself, and from a grown vat a few Amethyst Shards of its crystal. */
    private LootTable.Builder vat() {
        Block vat = ServusAlchemyBlocks.VAT.get();
        LootItemCondition.Builder grown = LootItemBlockStatePropertyCondition.hasBlockStateProperties(vat)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(VatBlock.CRYSTAL, VatCrystal.NONE))
                .invert();
        return createSingleItemTable(vat).withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(grown)
                .add(applyExplosionDecay(vat, LootItem.lootTableItem(Items.AMETHYST_SHARD)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))));
    }

    /** The Obsidian the obelisk grew out of, from its lower half only. */
    private LootTable.Builder rubedoObelisk() {
        Block obelisk = ServusAlchemyBlocks.RUBEDO_OBELISK.get();
        return LootTable.lootTable().withPool(applyExplosionCondition(obelisk, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(obelisk)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(RubedoObeliskBlock.HALF, DoubleBlockHalf.LOWER)))
                .add(LootItem.lootTableItem(Items.OBSIDIAN))));
    }

    /** What Deepslate drops: itself with Silk Touch, else Cobbled Deepslate. */
    private LootTable.Builder ancientHollow() {
        return LootTable.lootTable().withPool(applyExplosionCondition(ServusAlchemyBlocks.ANCIENT_HOLLOW.get(), LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(Items.DEEPSLATE).when(hasSilkTouch())
                        .otherwise(LootItem.lootTableItem(Items.COBBLED_DEEPSLATE)))));
    }

    /** Itself to a breaker under Elemental Sight, else what stone drops. */
    private LootTable.Builder gnomeHollow() {
        LootItemCondition.Builder sight = LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                EntityPredicate.Builder.entity().effects(MobEffectsPredicate.Builder.effects().and(ElementalSight.EFFECT)));
        return LootTable.lootTable().withPool(applyExplosionCondition(ServusAlchemyBlocks.GNOME_HOLLOW.get(), LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(ServusAlchemyBlocks.GNOME_HOLLOW.get()).when(sight)
                        .otherwise(LootItem.lootTableItem(Items.STONE).when(hasSilkTouch()))
                        .otherwise(LootItem.lootTableItem(Items.COBBLESTONE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(SELF_DROPPING.stream(), Stream.of(ServusAlchemyBlocks.VAT.get(), ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get(),
                ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get(), ServusAlchemyBlocks.GNOME_HOLLOW.get(),
                ServusAlchemyBlocks.ANCIENT_HOLLOW.get(), ServusAlchemyBlocks.RUBEDO_OBELISK.get())).toList();
    }
}
