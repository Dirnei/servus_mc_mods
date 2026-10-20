package io.github.dirnei.mc.servus.brewery.brewhouse;

import java.util.Optional;

/**
 * Balance numbers of the Brewhouse. Pure Java. Loaded from {@code data/servus_brewery/brewhouse/stats.json}; every field
 * is optional there and falls back to {@link #DEFAULTS}.
 *
 * @param kettleCapacity millibuckets the Brew Kettle's water tank holds
 * @param wortCapacity   millibuckets the Brew Kettle's wort tank holds
 */
public record BrewhouseStats(int kettleCapacity, int wortCapacity) {

    public static final BrewhouseStats DEFAULTS = new BrewhouseStats(4000, 4000);

    /** Partial stats as written in JSON; {@link BrewhouseStatsManager#CODEC} reads them. */
    public record Partial(Optional<Integer> kettleCapacity, Optional<Integer> wortCapacity) {

        public BrewhouseStats over(BrewhouseStats defaults) {
            return new BrewhouseStats(
                    Math.max(1, kettleCapacity.orElse(defaults.kettleCapacity())),
                    Math.max(1, wortCapacity.orElse(defaults.wortCapacity())));
        }
    }
}
