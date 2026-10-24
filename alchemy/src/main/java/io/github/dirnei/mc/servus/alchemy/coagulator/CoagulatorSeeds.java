package io.github.dirnei.mc.servus.alchemy.coagulator;

import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;

/**
 * The seeds of the loaded coagulation recipes, on the server and on the client: a {@link SeedIndex} built once per recipe map,
 * so a reload builds it again. A server build logs every seed item two recipes share.
 */
public final class CoagulatorSeeds {

    private record Cached(RecipeMap recipes, SeedIndex<RecipeHolder<CoagulationRecipe>> index) {
    }

    private static volatile Cached server;
    private static volatile Cached client;

    private CoagulatorSeeds() {
    }

    /** The recipe a seed item grows, empty for an item that seeds nothing. */
    public static Optional<RecipeHolder<CoagulationRecipe>> recipeFor(Level level, ItemStack seed) {
        return seed.isEmpty() ? Optional.empty() : index(level).lookup(itemId(seed.getItem()));
    }

    /** Whether the item seeds a coagulation recipe. */
    public static boolean isSeed(Level level, ItemStack stack) {
        return recipeFor(level, stack).isPresent();
    }

    /** The seed items a recipe kept after duplicates were settled. */
    public static List<Item> seedsOf(Level level, Identifier recipeId) {
        return index(level).seedsOf(recipeId.toString()).stream()
                .map(Identifier::parse)
                .flatMap(id -> BuiltInRegistries.ITEM.getOptional(id).stream())
                .toList();
    }

    private static SeedIndex<RecipeHolder<CoagulationRecipe>> index(Level level) {
        boolean onServer = level instanceof ServerLevel;
        RecipeMap recipes = ServusRecipes.recipeMap(level);
        Cached cached = onServer ? server : client;
        if (cached == null || cached.recipes() != recipes) {
            cached = new Cached(recipes, build(level, onServer));
            if (onServer) {
                server = cached;
            } else {
                client = cached;
            }
        }
        return cached.index();
    }

    private static SeedIndex<RecipeHolder<CoagulationRecipe>> build(Level level, boolean log) {
        List<SeedIndex.Entry<RecipeHolder<CoagulationRecipe>>> entries = ServusRecipes.all(level, ServusAlchemyRecipes.COAGULATION.get())
                .stream()
                .map(holder -> new SeedIndex.Entry<>(holder.id().identifier().toString(),
                        holder.value().seedIngredient().items().map(CoagulatorSeeds::itemId).toList(), holder))
                .toList();
        SeedIndex<RecipeHolder<CoagulationRecipe>> index = SeedIndex.build(entries);
        if (log) {
            index.conflicts().forEach(conflict -> ServusAlchemy.LOGGER.error(
                    "Seed {} of coagulation recipe {} is already the seed of {}; ignored", conflict.item(), conflict.rejected(), conflict.kept()));
        }
        return index;
    }

    private static String itemId(Holder<Item> item) {
        return itemId(item.value());
    }

    private static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}
