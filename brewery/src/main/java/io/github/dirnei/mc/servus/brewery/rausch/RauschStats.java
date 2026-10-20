package io.github.dirnei.mc.servus.brewery.rausch;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The Rausch balance numbers. Pure Java. Loaded from {@code data/servus_brewery/rausch/stats.json}; every field is
 * optional there and falls back to {@link #DEFAULTS}.
 *
 * @param perDrink   points one beer adds
 * @param decayTicks ticks in the game per point that wears off
 * @param max        the most points a player can reach
 * @param levels     every level's numbers
 * @param hangover   the Hangover's numbers
 */
public record RauschStats(int perDrink, int decayTicks, int max, Map<RauschLevel, LevelStats> levels, HangoverStats hangover) {

    public static final RauschStats DEFAULTS = new RauschStats(1, 2400, 8, Map.of(
            RauschLevel.TIPSY, LevelStats.mild(1, 1, 2),
            RauschLevel.MERRY, LevelStats.mild(2, 2, 4),
            RauschLevel.DRUNK, new LevelStats(4, 2, 6, 0.5, -0.15, 1.5, true, 0, 0),
            RauschLevel.PLASTERED, new LevelStats(5, 3, 8, 0, -0.15, 2, true, 0.25, 0.25)),
            new HangoverStats(4, 6000, -0.3, 0.1, 1));

    public RauschStats {
        perDrink = Math.max(0, perDrink);
        decayTicks = Math.max(1, decayTicks);
        max = Math.max(1, max);
        EnumMap<RauschLevel, LevelStats> complete = new EnumMap<>(RauschLevel.class);
        for (RauschLevel level : RauschLevel.values()) {
            LevelStats stats = levels.get(level);
            complete.put(level, stats != null ? stats : DEFAULTS.levels().get(level));
        }
        levels = Map.copyOf(complete);
    }

    /** One level's numbers. */
    public LevelStats level(RauschLevel level) {
        return levels.get(level);
    }

    /** The strongest level whose threshold {@code points} reach, or empty when sober. */
    public Optional<RauschLevel> levelAt(int points) {
        RauschLevel reached = null;
        for (RauschLevel level : RauschLevel.values()) {
            int from = Math.max(1, level(level).from());
            if (points >= from && (reached == null || from >= Math.max(1, level(reached).from()))) {
                reached = level;
            }
        }
        return Optional.ofNullable(reached);
    }
}
