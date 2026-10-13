package io.github.dirnei.mc.servus.foundation.production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Explodes a wanted item into production steps: stock on hand is reserved
 * first, the rest is produced; when several recipes make an item, those
 * whose inputs are on hand go first, then the one whose plan misses least.
 * Steps come leaves first, surplus is reused.
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

        /** A copy to try an alternative on without touching this walk. */
        private Walk<K> copy() {
            Walk<K> copy = new Walk<>(stock, recipes, maxDepth);
            copy.adopt(this);
            return copy;
        }

        private void adopt(Walk<K> other) {
            if (other == this) {
                return;
            }
            reserved.clear();
            reserved.putAll(other.reserved);
            surplus.clear();
            surplus.putAll(other.surplus);
            missing.clear();
            missing.putAll(other.missing);
            steps.clear();
            steps.putAll(other.steps);
        }

        void need(K key, int amount, int depth) {
            int rest = amount;
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
            List<Recipe<K>> producers = depth < maxDepth ? producersOf(key) : List.of();
            if (producers.isEmpty()) {
                missing.merge(key, rest, Integer::sum);
            } else if (producers.size() == 1) {
                Recipe<K> recipe = producers.get(0);
                run(recipe, batchesFor(recipe, key, rest), key, rest, depth, Set.of());
            } else {
                chooseAmong(producers, key, rest, depth);
            }
        }

        /**
         * Several recipes make the key: use those whose inputs are on hand first,
         * the one covering the most batches first, then produce the inputs of the
         * alternative whose plan misses least.
         */
        private void chooseAmong(List<Recipe<K>> producers, K key, int amount, int depth) {
            int rest = amount;
            Set<Recipe<K>> used = new LinkedHashSet<>();
            Set<Recipe<K>> inserted = new LinkedHashSet<>();
            while (rest > 0) {
                Recipe<K> best = null;
                int bestCovered = 0;
                for (Recipe<K> recipe : producers) {
                    int covered = coveredBatches(recipe);
                    if (covered > bestCovered) {
                        best = recipe;
                        bestCovered = covered;
                    }
                }
                if (best == null) {
                    break;
                }
                int batches = Math.min(batchesFor(best, key, rest), bestCovered);
                if (!steps.containsKey(best)) {
                    inserted.add(best);
                }
                rest -= run(best, batches, key, rest, depth, Set.of());
                used.add(best);
            }
            if (rest <= 0) {
                return;
            }
            Walk<K> chosen = null;
            Recipe<K> chosenRecipe = null;
            int chosenMissing = 0;
            int chosenSteps = 0;
            int missingBefore = total(missing);
            for (Recipe<K> recipe : producers) {
                Walk<K> trial = copy();
                trial.run(recipe, batchesFor(recipe, key, rest), key, rest, depth, inserted);
                int addedMissing = total(trial.missing) - missingBefore;
                int addedSteps = trial.steps.size() - steps.size();
                if (chosen == null || better(addedMissing, addedSteps, used.contains(recipe),
                        chosenMissing, chosenSteps, used.contains(chosenRecipe))) {
                    chosen = trial;
                    chosenRecipe = recipe;
                    chosenMissing = addedMissing;
                    chosenSteps = addedSteps;
                }
            }
            adopt(chosen);
        }

        /** Nothing missing beats missing; then fewer new steps or less missing; then an alternative already used. */
        private static boolean better(int missing, int steps, boolean used, int bestMissing, int bestSteps, boolean bestUsed) {
            if ((missing == 0) != (bestMissing == 0)) {
                return missing == 0;
            }
            if (missing == 0) {
                if (steps != bestSteps) {
                    return steps < bestSteps;
                }
            } else if (missing != bestMissing) {
                return missing < bestMissing;
            }
            return used && !bestUsed;
        }

        /**
         * Runs a recipe for the key and records its step. A step this call added
         * earlier moves behind the steps its new inputs add, so steps stay leaves first.
         *
         * @return how much of {@code rest} the batches cover
         */
        private int run(Recipe<K> recipe, int batches, K key, int rest, int depth, Set<Recipe<K>> mayMove) {
            for (Map.Entry<K, Integer> input : recipe.inputs().entrySet()) {
                need(input.getKey(), input.getValue() * batches, depth + 1);
            }
            Integer earlier = mayMove.contains(recipe) ? steps.remove(recipe) : null;
            steps.merge(recipe, batches + (earlier == null ? 0 : earlier), Integer::sum);
            int covered = 0;
            for (Map.Entry<K, Integer> output : recipe.outputs().entrySet()) {
                int produced = output.getValue() * batches;
                int used = 0;
                if (output.getKey().equals(key)) {
                    used = Math.min(rest, produced);
                    covered = used;
                }
                if (produced > used) {
                    surplus.merge(output.getKey(), produced - used, Integer::sum);
                }
            }
            return covered;
        }

        /** How many batches of the recipe make at least {@code amount} of the key. */
        private static <K> int batchesFor(Recipe<K> recipe, K key, int amount) {
            int perBatch = recipe.outputs().get(key);
            return (amount + perBatch - 1) / perBatch;
        }

        /** How many whole batches the inputs on hand cover, without producing anything. */
        private int coveredBatches(Recipe<K> recipe) {
            int batches = Integer.MAX_VALUE;
            for (Map.Entry<K, Integer> input : recipe.inputs().entrySet()) {
                long available = surplus.getOrDefault(input.getKey(), 0)
                        + stock.getOrDefault(input.getKey(), 0L) - reserved.getOrDefault(input.getKey(), 0);
                batches = (int) Math.min(batches, Math.max(0, available / input.getValue()));
            }
            return batches == Integer.MAX_VALUE ? 0 : batches;
        }

        private static <K> int total(Map<K, Integer> map) {
            int sum = 0;
            for (int value : map.values()) {
                sum += value;
            }
            return sum;
        }

        private List<Recipe<K>> producersOf(K key) {
            List<Recipe<K>> producers = new ArrayList<>();
            for (Recipe<K> recipe : recipes) {
                Integer count = recipe.outputs().get(key);
                if (count != null && count > 0) {
                    producers.add(recipe);
                }
            }
            return producers;
        }
    }
}
