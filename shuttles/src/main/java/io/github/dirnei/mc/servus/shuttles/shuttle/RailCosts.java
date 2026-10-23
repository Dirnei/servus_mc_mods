package io.github.dirnei.mc.servus.shuttles.shuttle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Cheapest ways over the rail graph: Dijkstra over (previous node, node)
 * states, so the cost of a step may depend on the step before it, such as a
 * corner or the start of a lift ride. Pure Java.
 */
public final class RailCosts {

    private RailCosts() {
    }

    /** The cost of one step; {@code previous} is null on the first step of a way. */
    @FunctionalInterface
    public interface StepCost<N> {
        double step(N previous, N from, N to);
    }

    private record State<N>(N previous, N node) {
    }

    private record Entry<N>(State<N> state, double cost, long order) {
    }

    /** Every node reachable from the starts with its cheapest cost and way. */
    public static final class Search<N> {
        private final Map<N, Double> best = new HashMap<>();
        private final Map<N, State<N>> bestState = new HashMap<>();
        private final Map<State<N>, State<N>> cameFrom = new HashMap<>();
        private final Map<N, Long> reachedOrder = new HashMap<>();

        /** The cheapest cost to the node, infinite when no way leads there. */
        public double costTo(N node) {
            return best.getOrDefault(node, Double.POSITIVE_INFINITY);
        }

        /** The cheapest cost to any of the nodes, infinite when none is reachable. */
        public double costToAny(Collection<N> nodes) {
            double cost = Double.POSITIVE_INFINITY;
            for (N node : nodes) {
                cost = Math.min(cost, costTo(node));
            }
            return cost;
        }

        /** The cheapest reachable node passing the test; the one reached first on equal costs. */
        public Optional<N> cheapest(Predicate<N> goal) {
            N found = null;
            for (Map.Entry<N, Double> entry : best.entrySet()) {
                N node = entry.getKey();
                if (!goal.test(node)) {
                    continue;
                }
                if (found == null || entry.getValue() < best.get(found)
                        || entry.getValue().equals(best.get(found)) && reachedOrder.get(node) < reachedOrder.get(found)) {
                    found = node;
                }
            }
            return Optional.ofNullable(found);
        }

        /** The cheapest way from a start to the node, start first; empty when unreachable. */
        public List<N> pathTo(N node) {
            State<N> state = bestState.get(node);
            if (state == null) {
                return List.of();
            }
            List<N> path = new ArrayList<>();
            while (state != null) {
                path.add(state.node());
                state = cameFrom.get(state);
            }
            Collections.reverse(path);
            return path;
        }

        /** The cheapest way to the cheapest node passing the test; empty when none is reachable. */
        public List<N> pathToCheapest(Predicate<N> goal) {
            return cheapest(goal).map(this::pathTo).orElse(List.of());
        }
    }

    /** Searches from one start. */
    public static <N> Search<N> search(N start, Function<N, ? extends Collection<N>> ways, StepCost<N> cost) {
        return search(List.of(start), ways, cost);
    }

    /** Searches from every start at cost zero. */
    public static <N> Search<N> search(Collection<N> starts, Function<N, ? extends Collection<N>> ways, StepCost<N> cost) {
        return search(starts, ways, cost, node -> false);
    }

    /**
     * Searches from every start at cost zero and stops once a node passing
     * {@code stopAt} is settled: that node is then the cheapest one passing it,
     * and nodes further away may be missing from the search.
     */
    public static <N> Search<N> search(Collection<N> starts, Function<N, ? extends Collection<N>> ways, StepCost<N> cost, Predicate<N> stopAt) {
        Search<N> search = new Search<>();
        Map<State<N>, Double> settled = new HashMap<>();
        PriorityQueue<Entry<N>> queue = new PriorityQueue<>(Comparator.comparingDouble((Entry<N> entry) -> entry.cost())
                .thenComparingLong(Entry::order));
        long[] order = {0};
        for (N start : starts) {
            queue.add(new Entry<>(new State<>(null, start), 0, order[0]++));
        }
        Map<State<N>, Double> known = new HashMap<>();
        while (!queue.isEmpty()) {
            Entry<N> entry = queue.poll();
            State<N> state = entry.state();
            if (settled.containsKey(state)) {
                continue;
            }
            settled.put(state, entry.cost());
            N node = state.node();
            if (entry.cost() < search.costTo(node)) {
                search.best.put(node, entry.cost());
                search.bestState.put(node, state);
                search.reachedOrder.putIfAbsent(node, entry.order());
            }
            if (stopAt.test(node)) {
                break;
            }
            for (N next : ways.apply(node)) {
                if (next.equals(state.previous())) {
                    continue;
                }
                State<N> nextState = new State<>(node, next);
                if (settled.containsKey(nextState)) {
                    continue;
                }
                double nextCost = entry.cost() + cost.step(state.previous(), node, next);
                Double before = known.get(nextState);
                if (before == null || nextCost < before) {
                    known.put(nextState, nextCost);
                    search.cameFrom.put(nextState, state);
                    queue.add(new Entry<>(nextState, nextCost, order[0]++));
                }
            }
        }
        return search;
    }
}
