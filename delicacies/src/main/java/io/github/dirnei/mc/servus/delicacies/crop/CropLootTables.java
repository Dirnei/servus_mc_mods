package io.github.dirnei.mc.servus.delicacies.crop;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/** The loot tables that hand out the first Delicacies planting items, and the vanilla tables they are added to. */
public final class CropLootTables {

    /** Rolled into short and tall grass broken without shears. */
    public static final ResourceKey<LootTable> GRASS_SEEDS = key("gameplay/grass_seeds");

    /** Rolled into the chests of village houses. */
    public static final ResourceKey<LootTable> VILLAGE_SEEDS = key("chests/village_seeds");

    /** Grass blocks whose drops gain {@link #GRASS_SEEDS}. */
    public static final List<Identifier> GRASS = List.of(
            Identifier.withDefaultNamespace("blocks/short_grass"),
            Identifier.withDefaultNamespace("blocks/tall_grass"));

    /** Village house chests that gain {@link #VILLAGE_SEEDS}. */
    public static final List<ResourceKey<LootTable>> VILLAGE_HOUSES = List.of(
            BuiltInLootTables.VILLAGE_PLAINS_HOUSE, BuiltInLootTables.VILLAGE_DESERT_HOUSE,
            BuiltInLootTables.VILLAGE_SAVANNA_HOUSE, BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
            BuiltInLootTables.VILLAGE_TAIGA_HOUSE);

    private CropLootTables() {
    }

    private static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, path));
    }
}
