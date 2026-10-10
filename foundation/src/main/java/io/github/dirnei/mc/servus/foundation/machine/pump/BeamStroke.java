package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The rocking of the Burner Pump's beam: a steady sine stroke, and how far a
 * rod hanging from the beam moves for a given angle. Look only, no balance.
 */
public final class BeamStroke {

    private final float amplitude;
    private final float period;

    /**
     * @param amplitude the largest beam angle in degrees either way
     * @param period    ticks for one full stroke, up and back down
     */
    public BeamStroke(float amplitude, float period) {
        this.amplitude = amplitude;
        this.period = period;
    }

    /** The beam angle in degrees after {@code phase} ticks of work. */
    public float angle(double phase) {
        return (float) (amplitude * Math.sin(2 * Math.PI * phase / period));
    }

    /**
     * How far a rod hanging {@code arm} pixels from the pivot rises for a beam
     * angle; positive angles lift the end at positive {@code arm}.
     */
    public static float rodOffset(float angle, float arm) {
        return (float) (arm * Math.sin(Math.toRadians(angle)));
    }
}
