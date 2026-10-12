package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The Electric Pump's crank: a wheel turning once per stroke, whose pin drives
 * the beam through the con rod. The beam keeps the {@link BeamStroke} sine, and
 * the crank is timed so the pin is highest exactly when the beam end above it
 * is: both peak a quarter period into the stroke. The fan turns with the
 * crank, geared up. Look only, no balance.
 */
public final class CrankDrive {

    /** Fan turns per crank turn. */
    public static final float FAN_GEAR = 3;

    private final BeamStroke stroke;
    private final float period;
    private final float radius;

    /**
     * @param stroke the beam's stroke
     * @param period ticks for one stroke, the same as the stroke's
     * @param radius the crank pin's distance from the wheel centre in pixels
     */
    public CrankDrive(BeamStroke stroke, float period, float radius) {
        this.stroke = stroke;
        this.period = period;
        this.radius = radius;
    }

    /** The crank angle in degrees, 0 to below 360, counted from the pin level with the centre toward the drive side. */
    public float crankAngle(double phase) {
        double turns = phase / period;
        return (float) (360.0 * (turns - Math.floor(turns)));
    }

    /** The pin's offset from the wheel centre across, in pixels. */
    public float pinX(double phase) {
        return (float) (radius * Math.cos(Math.toRadians(crankAngle(phase))));
    }

    /** The pin's height above the wheel centre, in pixels. */
    public float pinY(double phase) {
        return (float) (radius * Math.sin(Math.toRadians(crankAngle(phase))));
    }

    /** The beam angle, the same as the stroke's, so the con rod follows the pin. */
    public float beamAngle(double phase) {
        return stroke.angle(phase);
    }

    /** The fan angle in degrees, 0 to below 360. */
    public float fanAngle(double phase) {
        float angle = crankAngle(phase) * FAN_GEAR;
        return angle - 360 * (float) Math.floor(angle / 360);
    }
}
