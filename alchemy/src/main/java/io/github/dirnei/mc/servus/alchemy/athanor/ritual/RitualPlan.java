package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.Placed;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Turn;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.List;
import java.util.function.Function;

/**
 * Whether a ritual can start on an Athanor, and what it turns. Pure Java.
 *
 * @param outcome whether it can start, or why not
 * @param missing the first turn whose block is not in place, turned with the front, for {@link Outcome#MISSING}
 * @param turns   every turn, turned with the front, for {@link Outcome#READY}
 */
public record RitualPlan(Outcome outcome, Turn missing, List<Turn> turns) {

    public enum Outcome {
        READY,
        /** The Athanor's tier is lower than the ritual's. */
        TIER_TOO_LOW,
        /** The Athanor's tier is higher than the ritual's. */
        TIER_TOO_HIGH,
        MISSING
    }

    /**
     * Checks {@code ritual} on an Athanor of tier {@code tier} facing {@code facing}; {@code blockAt} reads what stands at
     * an offset already turned with the front. A block the ritual already turned counts as in place, so a ritual can be cast
     * again after one of its blocks was broken and put back.
     */
    public static RitualPlan check(AthanorRitual ritual, int tier, Facing facing, Function<BlockAt, Placed> blockAt) {
        if (tier < ritual.tier()) {
            return new RitualPlan(Outcome.TIER_TOO_LOW, null, List.of());
        }
        if (tier > ritual.tier()) {
            return new RitualPlan(Outcome.TIER_TOO_HIGH, null, List.of());
        }
        List<Turn> turns = ritual.turns(facing);
        for (Turn turn : turns) {
            Placed placed = blockAt.apply(turn.from());
            if (!turn.from().matches(placed) && !turn.to().matches(placed)) {
                return new RitualPlan(Outcome.MISSING, turn, List.of());
            }
        }
        return new RitualPlan(Outcome.READY, null, turns);
    }

    /**
     * Of the rituals taking the same offering, the one to try on an Athanor of tier {@code tier}: the one of that tier,
     * else the nearest above it, else the nearest below.
     */
    public static AthanorRitual choose(List<AthanorRitual> rituals, int tier) {
        AthanorRitual best = null;
        for (AthanorRitual ritual : rituals) {
            if (best == null || rank(ritual.tier(), tier) < rank(best.tier(), tier)) {
                best = ritual;
            }
        }
        return best;
    }

    private static int rank(int ritualTier, int tier) {
        return ritualTier >= tier ? 2 * (ritualTier - tier) : 2 * (tier - ritualTier) + 1;
    }
}
