package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The Burner Pump's own parts in pixels on the shared {@link BeamPumpParts}
 * frame, built facing north, as {x1, y1, z1, x2, y2, z2}: firebox, boiler
 * drum, steam cylinder, piston rod and chimney. Upper half boxes are relative
 * to the upper block.
 */
public final class BurnerPumpParts {

    public static final float[] FIREBOX = {3, 2, 3, 13, 14, 13};
    public static final float[] DRUM_LOWER = {4, 14, 4, 12, 16, 12};

    public static final float[] DRUM_UPPER = {4, 0, 4, 12, 3, 12};
    public static final float[] CYLINDER = {10, 3, 2.5F, 14, 9, 6.5F};
    public static final float[] CHIMNEY = {3, 3, 9, 6, 15, 12};

    /** The piston rod from the cylinder up to the beam, upper half frame, at rest. */
    public static final float[] PISTON = {11.5F, 9, 3.5F, 12.5F, BeamPumpParts.PIVOT_Y, 4.5F};

    /** Smoke leaves here, upper half frame. */
    public static final float SMOKE_X = 4.5F;
    public static final float SMOKE_Y = 15;
    public static final float SMOKE_Z = 10.5F;

    private BurnerPumpParts() {
    }
}
