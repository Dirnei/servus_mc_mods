package io.github.dirnei.mc.servus.alchemy.sight;

/**
 * How dangerous a drink of an elixir is: the chance it poisons, the extra chance when the drinker still has Elemental Sight,
 * and how long Poison and Nausea last.
 *
 * @param poisonChance            chance from 0 to 1 that one drink poisons
 * @param extraChanceWhileSighted added to the chance when the drinker still has Elemental Sight
 * @param poisonTicks             how long Poison lasts, 0 for none
 * @param nauseaTicks             how long Nausea lasts, 0 for none
 */
public record ElixirDose(double poisonChance, double extraChanceWhileSighted, int poisonTicks, int nauseaTicks) {

    /** A dose that never poisons. */
    public static final ElixirDose HARMLESS = new ElixirDose(0, 0, 0, 0);

    public ElixirDose {
        requireChance(poisonChance, "poisonChance");
        requireChance(extraChanceWhileSighted, "extraChanceWhileSighted");
        if (poisonTicks < 0 || nauseaTicks < 0) {
            throw new IllegalArgumentException("effect ticks must not be negative");
        }
    }

    /** The chance that a drink poisons, raised when the drinker still has Elemental Sight and capped at 1. */
    public double chance(boolean sighted) {
        return Math.min(1, poisonChance + (sighted ? extraChanceWhileSighted : 0));
    }

    /** Whether a drink poisons for a {@code roll} from 0 (inclusive) to 1 (exclusive). */
    public boolean poisons(boolean sighted, double roll) {
        return roll < chance(sighted);
    }

    private static void requireChance(double chance, String name) {
        if (!(chance >= 0 && chance <= 1)) {
            throw new IllegalArgumentException(name + " must be between 0 and 1, was " + chance);
        }
    }
}
