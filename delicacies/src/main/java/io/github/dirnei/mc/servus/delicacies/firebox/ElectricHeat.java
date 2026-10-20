package io.github.dirnei.mc.servus.delicacies.firebox;

import java.util.function.Supplier;

/**
 * The heat of a Heating Element. Pure Java. Its temperature runs from 0 (cold) to 1 (full heat): it rises on every
 * tick a spot asks for heat and the FE of that tick can be drawn, and sinks slowly on every other tick. The spots cook
 * only from the threshold of its {@link HeatingStats} on.
 */
public final class ElectricHeat implements HeatSource {

    private final EnergyDraw power;
    private final Supplier<HeatingStats> stats;
    private float heat;
    private boolean powered;

    public ElectricHeat(EnergyDraw power, Supplier<HeatingStats> stats) {
        this.power = power;
        this.stats = stats;
    }

    @Override
    public void tick(boolean demanded) {
        HeatingStats now = stats.get();
        powered = demanded && power.draw(now.energyPerTick());
        heat = clamp(heat + (powered ? now.heatUpPerTick() : -now.coolDownPerTick()));
    }

    @Override
    public boolean hot() {
        return heat > 0 && heat >= stats.get().hotAt();
    }

    @Override
    public float heat() {
        return heat;
    }

    /** Whether the last tick drew FE and heated. */
    public boolean powered() {
        return powered;
    }

    /** How much the heat changes per tick while things stay as on the last tick: rising, sinking, or 0 at either end. */
    public float rate() {
        HeatingStats now = stats.get();
        if (powered) {
            return heat < 1 ? now.heatUpPerTick() : 0;
        }
        return heat > 0 ? -now.coolDownPerTick() : 0;
    }

    /** Restores a saved temperature; the next tick decides whether it heats. */
    public void restore(float heat) {
        this.heat = clamp(heat);
        this.powered = false;
    }

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
