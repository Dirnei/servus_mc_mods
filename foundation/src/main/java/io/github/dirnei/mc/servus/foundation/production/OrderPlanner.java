package io.github.dirnei.mc.servus.foundation.production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Explodes a wanted item into production steps: stock on hand is reserved
 * first, the rest is produced with the first recipe that makes the item,
 * leaves before the steps that need them, surplus reused. Pure, no
 * Minecraft imports.
 */
public final class OrderPlanner {

    private OrderPlanner() {
    }

    /**
     * A recipe as the planner sees it.
     *
     * @param id      anything that identifies the recipe to the caller
     * @param inputs  consumed per batch
     * @param outputs produced per batch
     * @param ticks   time per batch
     */
    public record Recipe<K>(Object id, Map<K, Integer> inputs, Map<K, Integer> outputs, int ticks) {
    }

    /** Run a recipe this many times. */
    public record Step<K>(Recipe<K> recipe, int batches) {
    }

    /**
     * @param steps    steps in the order to run them, leaves first
     * @param reserved stock on hand the plan uses, per key
     * @param missing  raw material the plan cannot get, per key
     */
    public record Plan<K>(List<Step<K>> steps, Map<K, Integer> reserved, Map<K, Integer> missing) {
        public boolean complete() {
            return missing.isEmpty();
        }

        /** Total ticks of all steps run one after another. */
        public long ticks() {
            long sum = 0;
            for (Step<K> step : steps) {
                sum += (long) step.recipe().ticks() * step.batches();
            }
            return sum;
        }
    }

    /**
     * @param target   the wanted item
     * @param amount   how many
     * @param stock    what is on hand per key
     * @param recipes  recipes that may be used, in order of preference
     * @param maxDepth how many recipe levels to follow before giving up on an input
     */
    public static <K> Plan<K> plan(K target, int amount, Map<K, Long> stock, List<Recipe<K>> recipes, int maxDepth) {
        Walk<K> walk = new Walk<>(stock, recipes, maxDepth);
        walk.need(target, amount, 0);
        List<Step<K>> steps = new ArrayList<>();
        walk.steps.forEach((recipe, batches) -> steps.add(new Step<>(recipe, batches)));
        return new Plan<>(Collections.unmodifiableList(steps), Collections.unmodifiableMap(walk.reserved),
                Collections.unmodifiableMap(walk.missing));
    }

    private static final class Walk<K> {
        private final Map<K, Long> stock;
        private final List<Recipe<K>> recipes;
        private final int maxDepth;
        private final Map<K, Integer> reserved = new HashMap<>();
        private final Map<K, Integer> surplus = new HashMap<>();
        private final Map<K, Integer> missing = new HashMap<>();
        private final LinkedHashMap<Recipe<K>, Integer> steps = new LinkedHashMap<>();

        Walk(Map<K, Long> stock, List<Recipe<K>> recipes, int maxDepth) {
            this.stock = stock;
            this.recipes = recipes;
            this.maxDepth = maxDepth;
        }

        void need(K key, int amount, int depth) {
            int rest = amount;
            // Surplus from earlier steps first, then stock on hand.
            int fromSurplus = Math.min(rest, surplus.getOrDefault(key, 0));
            if (fromSurplus > 0) {
                surplus.merge(key, -fromSurplus, Integer::sum);
                rest -= fromSurplus;
            }
            long onHand = stock.getOrDefault(key, 0L) - reserved.getOrDefault(key, 0);
            int fromStock = (int) Math.max(0, Math.min(rest, onHand));
            if (fromStock > 0) {
                reserved.merge(key, fromStock, Integer::sum);
                rest -= fromStock;
            }
            if (rest <= 0) {
                return;
            }
            Recipe<K> recipe = depth < maxDepth ? producerOf(key) : null;
            if (recipe == null) {
                missing.merge(key, rest, Integer::sum);
                return;
            }
            int perBatch = recipe.outputs().get(key);
            int batches = (rest + perBatch - 1) / perBatch;
            for (Map.Entry<K, Integer> input : recipe.inputs().entrySet()) {
                need(input.getKey(), input.getValue() * batches, depth + 1);
            }
            steps.merge(recipe, batches, Integer::sum);
            for (Map.Entry<K, Integer> output : recipe.outputs().entrySet()) {
                int produced = output.getValue() * batches;
                int used = output.getKey().equals(key) ? rest : 0;
                if (produced > used) {
                    surplus.merge(output.getKey(), produced - used, Integer::sum);
                }
            }
        }

        private Recipe<K> producerOf(K key) {
            for (Recipe<K> recipe : recipes) {
                Integer count = recipe.outputs().get(key);
                if (count != null && count > 0) {
                    return recipe;
                }
            }
            return null;
        }
    }
}
