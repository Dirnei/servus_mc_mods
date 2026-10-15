package io.github.dirnei.mc.servus.storage.production;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Machine rows with a problem first, then by position. */
public final class MachineOrder {

    /** States a player should look at. */
    public static final Set<String> PROBLEMS = Set.of("out_of_fuel", "blocked", "no_machine", "no_recipes");

    private MachineOrder() {
    }

    public static boolean problem(String state) {
        return PROBLEMS.contains(state);
    }

    /** Problems first, then ascending position. */
    public static <T> Comparator<T> comparator(Function<T, String> state, ToLongFunction<T> position) {
        Comparator<T> problemsFirst = Comparator.comparing(row -> problem(state.apply(row)) ? 0 : 1);
        return problemsFirst.thenComparingLong(position);
    }
}
