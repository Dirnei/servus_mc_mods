package io.github.dirnei.mc.servus.foundation.drive;

import java.util.Optional;

/**
 * Balance numbers of one driver. Pure Java. Loaded from {@code data/<ns>/drive/<name>.json}; every field is optional in the
 * file and falls back to the driver's code defaults.
 *
 * @param workPerTick       work a motor gives per working tick, before upgrades
 * @param workPerTurn       work a crank gives per full turn
 * @param exhaustionPerTurn food exhaustion a full crank turn costs the player
 */
public record DriveStats(int workPerTick, int workPerTurn, float exhaustionPerTurn) {

    /** The Hand Crank's defaults. */
    public static final DriveStats HAND_CRANK = new DriveStats(0, 25, 0.1F);
    /** The Electric Motor's defaults. */
    public static final DriveStats ELECTRIC_MOTOR = new DriveStats(5, 0, 0);

    /** Partial stats as written in JSON; {@link DriveStatsManager#CODEC} reads them. */
    public record Partial(Optional<Integer> workPerTick, Optional<Integer> workPerTurn, Optional<Float> exhaustionPerTurn) {

        public DriveStats over(DriveStats defaults) {
            return new DriveStats(
                    Math.max(0, workPerTick.orElse(defaults.workPerTick())),
                    Math.max(0, workPerTurn.orElse(defaults.workPerTurn())),
                    Math.max(0, exhaustionPerTurn.orElse(defaults.exhaustionPerTurn())));
        }
    }
}
