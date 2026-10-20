package io.github.dirnei.mc.servus.delicacies.kitchen;

import java.util.Optional;

/**
 * Balance numbers of the kitchen. Pure Java. Loaded from {@code data/servus_delicacies/kitchen/stats.json}; every field
 * is optional there and falls back to {@link #DEFAULTS}.
 *
 * @param stoveMaxLength the most Stove Tiles one Tiled Stove has
 * @param potCapacity    millibuckets the Pot's tank holds
 */
public record KitchenStats(int stoveMaxLength, int potCapacity) {

    public static final KitchenStats DEFAULTS = new KitchenStats(4, 4000);

    /** Partial stats as written in JSON; {@link KitchenStatsManager#CODEC} reads them. */
    public record Partial(Optional<Integer> stoveMaxLength, Optional<Integer> potCapacity) {

        public KitchenStats over(KitchenStats defaults) {
            return new KitchenStats(
                    Math.max(2, stoveMaxLength.orElse(defaults.stoveMaxLength())),
                    Math.max(1, potCapacity.orElse(defaults.potCapacity())));
        }
    }
}
