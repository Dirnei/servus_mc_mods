package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.crop.CropLootTables;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

/** Adds the seed tables to grass and to village house chests, one {@code neoforge:add_table} modifier per target. */
final class ServusDelicaciesLootModifierProvider extends GlobalLootModifierProvider {

    ServusDelicaciesLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, ServusDelicaciesIds.MOD_ID);
    }

    @Override
    protected void start() {
        for (Identifier grass : CropLootTables.GRASS) {
            addTable("grass_seeds/" + grass.getPath().substring("blocks/".length()), grass, CropLootTables.GRASS_SEEDS);
        }
        for (ResourceKey<LootTable> house : CropLootTables.VILLAGE_HOUSES) {
            Identifier id = house.identifier();
            addTable("village_seeds/" + id.getPath().substring(id.getPath().lastIndexOf('/') + 1), id, CropLootTables.VILLAGE_SEEDS);
        }
    }

    private void addTable(String name, Identifier target, ResourceKey<LootTable> table) {
        add(name, new AddTableLootModifier(new LootItemCondition[] {LootTableIdCondition.builder(target).build()}, 0, table));
    }
}
