package io.github.dirnei.mc.servus.alchemy.calciner;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the most specific of several recipes that match one item: an ingredient that names its
 * items beats a tag, fewer matched items beat more, and the lower recipe id breaks a tie. The
 * order the candidates come in never matters. Pure Java.
 */
public final class RecipeSpecificity {

    /**
     * How specific a recipe's ingredient is.
     *
     * @param tag   whether the ingredient is a tag rather than named items
     * @param items how many items the ingredient matches
     * @param id    the recipe id, for ties
     */
    public record Rank(boolean tag, int items, String id) {
    }

    /** A matching recipe with its rank. */
    public record Candidate<T>(T recipe, Rank rank) {
    }

    /** Most specific first. */
    public static final Comparator<Rank> ORDER = Comparator.comparing(Rank::tag)
            .thenComparingInt(Rank::items)
            .thenComparing(Rank::id);

    private RecipeSpecificity() {
    }

    /** The most specific candidate's recipe, empty when there is none. */
    public static <T> Optional<T> best(List<Candidate<T>> candidates) {
        return candidates.stream()
                .min(Comparator.comparing(Candidate::rank, ORDER))
                .map(Candidate::recipe);
    }
}
