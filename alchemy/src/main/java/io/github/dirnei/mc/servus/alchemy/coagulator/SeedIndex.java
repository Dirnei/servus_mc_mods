package io.github.dirnei.mc.servus.alchemy.coagulator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which coagulation recipe a seed item grows. Every seed item belongs to at most one recipe: when two recipes name the
 * same item, the recipe whose id sorts first keeps it and the conflict is recorded, whatever order the recipes come in.
 * Pure Java, no Minecraft types; items and recipes are named by their ids.
 *
 * @param <R> the recipe a lookup gives back
 */
public final class SeedIndex<R> {

    /** One recipe with the ids of the items that seed it. */
    public record Entry<R>(String recipeId, List<String> seedItems, R recipe) {
    }

    /** A seed item two recipes name: {@code kept} grows from it, {@code rejected} lost it. */
    public record Conflict(String item, String kept, String rejected) {
    }

    private final Map<String, Entry<R>> bySeed;
    private final Map<String, List<String>> seedsByRecipe;
    private final List<Conflict> conflicts;

    private SeedIndex(Map<String, Entry<R>> bySeed, Map<String, List<String>> seedsByRecipe, List<Conflict> conflicts) {
        this.bySeed = bySeed;
        this.seedsByRecipe = seedsByRecipe;
        this.conflicts = conflicts;
    }

    /** Indexes the entries; a seed item named twice stays with the entry whose recipe id sorts first. */
    public static <R> SeedIndex<R> build(List<Entry<R>> entries) {
        List<Entry<R>> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(Entry::recipeId));
        Map<String, Entry<R>> bySeed = new HashMap<>();
        Map<String, List<String>> seedsByRecipe = new LinkedHashMap<>();
        List<Conflict> conflicts = new ArrayList<>();
        for (Entry<R> entry : sorted) {
            List<String> kept = new ArrayList<>();
            for (String item : entry.seedItems()) {
                Entry<R> owner = bySeed.get(item);
                if (owner == null) {
                    bySeed.put(item, entry);
                    kept.add(item);
                } else if (owner != entry) {
                    conflicts.add(new Conflict(item, owner.recipeId(), entry.recipeId()));
                }
            }
            seedsByRecipe.put(entry.recipeId(), List.copyOf(kept));
        }
        return new SeedIndex<>(Map.copyOf(bySeed), seedsByRecipe, List.copyOf(conflicts));
    }

    /** The recipe the item seeds, empty when it seeds none. */
    public Optional<R> lookup(String item) {
        return Optional.ofNullable(bySeed.get(item)).map(Entry::recipe);
    }

    /** The seed items a recipe kept, in the order it named them; empty for an unknown recipe. */
    public List<String> seedsOf(String recipeId) {
        return seedsByRecipe.getOrDefault(recipeId, List.of());
    }

    /** Every seed item that was named by more than one recipe, in recipe id order. */
    public List<Conflict> conflicts() {
        return conflicts;
    }
}
