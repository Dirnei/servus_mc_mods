package io.github.dirnei.mc.servus.brewery.beer;

import java.util.Optional;

/**
 * Balance numbers of the Fermenting Vat and the Cask. Pure Java. Loaded from {@code data/servus_brewery/cellar/stats.json};
 * every field is optional there and falls back to {@link #DEFAULTS}.
 *
 * @param vatCapacity  millibuckets a Fermenting Vat holds
 * @param caskCapacity millibuckets a Cask holds
 */
public record CellarStats(int vatCapacity, int caskCapacity) {

    public static final CellarStats DEFAULTS = new CellarStats(4000, 16000);

    /** Partial stats as written in JSON; {@link CellarStatsManager#CODEC} reads them. */
    public record Partial(Optional<Integer> vatCapacity, Optional<Integer> caskCapacity) {

        public CellarStats over(CellarStats defaults) {
            return new CellarStats(
                    Math.max(1, vatCapacity.orElse(defaults.vatCapacity())),
                    Math.max(1, caskCapacity.orElse(defaults.caskCapacity())));
        }
    }
}
