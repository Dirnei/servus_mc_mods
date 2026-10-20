package io.github.dirnei.mc.servus.foundation.drive;

/**
 * What a motor gives and spends per working tick. Pure Java.
 *
 * @param workPerTick   work units handed to the driven block
 * @param energyPerTick FE spent for them
 */
public record MotorRate(int workPerTick, int energyPerTick) {

    /**
     * The rate with upgrades: work and energy both multiplied by {@code multiplier}, so the energy per unit of work
     * stays the same. A running motor always gives at least one unit of work.
     */
    public static MotorRate of(int baseWork, int baseEnergy, double multiplier) {
        double factor = Math.max(0, multiplier);
        return new MotorRate(Math.max(1, (int) Math.round(baseWork * factor)), Math.max(0, (int) Math.round(baseEnergy * factor)));
    }
}
