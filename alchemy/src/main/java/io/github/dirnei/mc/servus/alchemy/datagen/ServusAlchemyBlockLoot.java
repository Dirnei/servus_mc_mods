package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.MobEffectsPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

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
            ServusAlchemyBlocks.VAT.get(),
            ServusAlchemyBlocks.COAGULATOR.get(),
            ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get(),
            ServusAlchemyBlocks.ATHANOR.get(),
            ServusAlchemyBlocks.PRINCIPLE_DROPPER.get(),
            ServusAlchemyBlocks.SALAMANDER_FORGE.get());

    ServusAlchemyBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        SELF_DROPPING.forEach(this::dropSelf);
        dropOther(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get(), Items.STONE_BRICKS);
        dropOther(ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get(), Items.DEEPSLATE_TILES);
        add(ServusAlchemyBlocks.GNOME_HOLLOW.get(), gnomeHollow());
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
        return Stream.concat(SELF_DROPPING.stream(), Stream.of(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get(),
                ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get(), ServusAlchemyBlocks.GNOME_HOLLOW.get())).toList();
    }
}
