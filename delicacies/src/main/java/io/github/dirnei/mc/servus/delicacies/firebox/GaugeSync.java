package io.github.dirnei.mc.servus.delicacies.firebox;

/**
 * Decides when the clients need a new thermometer reading. Pure Java. A reading is due when the heat crosses one of
 * {@link #STEPS} coarse steps, when it crosses the threshold, or when heating turns into cooling or the other way;
 * between those the client carries the needle on by the last rate.
 */
public final class GaugeSync {

    /** Coarse steps of the heat between 0 and 1. */
    public static final int STEPS = 16;

    private int step = -1;
    private boolean hot;
    private int direction;

    /** Whether this reading must go to the clients; remembers it when so. */
    public boolean changed(float heat, boolean hot, float rate) {
        int nowStep = Math.min(STEPS, (int) Math.floor(heat * STEPS));
        int nowDirection = (int) Math.signum(rate);
        if (nowStep == step && hot == this.hot && nowDirection == direction) {
            return false;
        }
        step = nowStep;
        this.hot = hot;
        direction = nowDirection;
        return true;
    }

    /** Forgets the last reading, so the next one goes out. */
    public void reset() {
        step = -1;
    }
}
