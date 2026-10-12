package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The rocking of a beam pump's beam: a steady sine stroke, and how far a
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
     * How many times the pump rod passed its lowest point after
     * {@code previous} up to and including {@code phase}. The pump rod hangs
     * on the beam's short side (negative arm), so it is lowest when the beam
     * angle is largest, a quarter period into each stroke.
     */
    public int bottoms(double previous, double phase) {
        if (phase <= previous) {
            return 0;
        }
        double quarter = period / 4.0;
        return (int) (Math.floor((phase - quarter) / period) - Math.floor((previous - quarter) / period));
    }

    /**
     * How far a rod hanging {@code arm} pixels from the pivot rises for a beam
     * angle; positive angles lift the end at positive {@code arm}.
     */
    public static float rodOffset(float angle, float arm) {
        return (float) (arm * Math.sin(Math.toRadians(angle)));
    }
}
