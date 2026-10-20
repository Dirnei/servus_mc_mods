package io.github.dirnei.mc.servus.delicacies.firebox;

import java.util.Optional;

/**
 * Balance numbers of a firebox heated by a Heating Element. Pure Java. Loaded from {@code data/<ns>/heating/<firebox>.json};
 * every field is optional in the file and falls back to the firebox's code defaults.
 *
 * @param energyPerTick FE drawn on every heating tick
 * @param energyBuffer  FE the firebox stores
 * @param heatUpTicks   ticks from cold to full heat while it heats
 * @param coolDownTicks ticks from full heat to cold while it does not
 * @param hotAt         share of full heat from which the spots cook, above 0 and at most 1
 */
public record HeatingStats(int energyPerTick, int energyBuffer, int heatUpTicks, int coolDownTicks, float hotAt) {

    /** The Tiled Stove's defaults. */
    public static final HeatingStats TILED_STOVE = new HeatingStats(20, 4000, 200, 1200, 0.6F);

    private static final float LOWEST_HOT_AT = 0.01F;

    /** How much the heat rises on a heating tick. */
    public float heatUpPerTick() {
        return 1.0F / heatUpTicks;
    }

    /** How much the heat sinks on any other tick. */
    public float coolDownPerTick() {
        return 1.0F / coolDownTicks;
    }

    /** Partial stats as written in JSON. */
    public record Partial(Optional<Integer> energyPerTick, Optional<Integer> energyBuffer, Optional<Integer> heatUpTicks,
                          Optional<Integer> coolDownTicks, Optional<Float> hotAt) {

        /** These fields over {@code defaults}, kept in range. */
        public HeatingStats over(HeatingStats defaults) {
            int perTick = Math.max(0, energyPerTick.orElse(defaults.energyPerTick()));
            return new HeatingStats(
                    perTick,
                    Math.max(perTick, energyBuffer.orElse(defaults.energyBuffer())),
                    Math.max(1, heatUpTicks.orElse(defaults.heatUpTicks())),
                    Math.max(1, coolDownTicks.orElse(defaults.coolDownTicks())),
                    Math.max(LOWEST_HOT_AT, Math.min(1, hotAt.orElse(defaults.hotAt()))));
        }
    }
}
