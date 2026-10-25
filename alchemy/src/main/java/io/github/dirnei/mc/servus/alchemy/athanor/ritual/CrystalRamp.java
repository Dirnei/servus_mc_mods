package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import java.util.Arrays;

/**
 * The colours of a ritual crystal from shadow to highlight. Shading picks a colour along the ramp instead of darkening one
 * colour, so shaded faces stay clear instead of turning brown. Pure Java.
 */
public record CrystalRamp(int[] stops) {

    /** The brightness that gives the first stop. */
    public static final float DARKEST = 0.45F;
    /** The brightness that gives the last stop. */
    public static final float BRIGHTEST = 1.1F;

    public CrystalRamp {
        if (stops.length < 2) {
            throw new IllegalArgumentException("a ramp needs at least two stops");
        }
        stops = stops.clone();
    }

    public static CrystalRamp of(int... stops) {
        return new CrystalRamp(stops);
    }

    /** The RGB colour at {@code brightness}, clamped to {@link #DARKEST} and {@link #BRIGHTEST}. */
    public int at(float brightness) {
        float x = RitualTimeline.clamp01((brightness - DARKEST) / (BRIGHTEST - DARKEST)) * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) x);
        return RitualTimeline.mix(stops[i], stops[i + 1], x - i);
    }

    /** The ramp {@code t} of the way from this one to {@code to}, stop by stop; both must have as many stops. */
    public CrystalRamp mix(CrystalRamp to, float t) {
        int[] mixed = new int[stops.length];
        for (int i = 0; i < mixed.length; i++) {
            mixed[i] = RitualTimeline.mix(stops[i], to.stops[i], t);
        }
        return new CrystalRamp(mixed);
    }

    /** This ramp with every stop {@code t} of the way to the single colour {@code rgb}. */
    public CrystalRamp toward(int rgb, float t) {
        int[] mixed = new int[stops.length];
        for (int i = 0; i < mixed.length; i++) {
            mixed[i] = RitualTimeline.mix(stops[i], rgb, t);
        }
        return new CrystalRamp(mixed);
    }

    /** The stop at {@code index}. */
    public int stop(int index) {
        return stops[index];
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CrystalRamp ramp && Arrays.equals(stops, ramp.stops);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(stops);
    }

    @Override
    public String toString() {
        return "CrystalRamp" + Arrays.toString(stops);
    }
}
