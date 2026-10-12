package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The stroke phase of one beam pump on the client: advanced one tick per
 * client tick while the pump works, read by its renderer for the motion and by
 * its client ticker for the splash. Never saved or synced. Look only.
 */
public final class StrokeClock {

    /** Ticks for one full stroke, up and back down. */
    public static final float PERIOD = 40;
    /** Twelve degrees either way, one stroke every two seconds. */
    public static final BeamStroke STROKE = new BeamStroke(12, PERIOD);

    private double phase;

    /** Advances one tick while working; returns how often the pump rod passed its lowest point. */
    public int tick(boolean working) {
        if (!working) {
            return 0;
        }
        double previous = phase;
        phase += 1;
        return STROKE.bottoms(previous, phase);
    }

    /** The phase for a frame: between ticks it runs on while working and stands while not. */
    public double phase(float partialTick, boolean working) {
        return working ? phase + partialTick : phase;
    }
}
