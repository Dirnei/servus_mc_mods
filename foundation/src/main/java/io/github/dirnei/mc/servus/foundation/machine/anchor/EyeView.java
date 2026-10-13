package io.github.dirnei.mc.servus.foundation.machine.anchor;

/**
 * Where one Ender Anchor's eye stands on the client: how far it is open, where
 * its iris looks and the frame time these belong to. Kept on the block entity
 * for its renderer, never saved or synced. Look only.
 */
public final class EyeView {

    /** 0 shut to 1 open. */
    public float openness;
    /** The iris offset in pixels inside the lens. */
    public float irisX;
    public float irisY;
    /** The game time in ticks of the last frame, or below 0 before the first. */
    public double time = -1;

    /** Seconds since the last frame, at most a quarter second so a long pause does not jump. */
    public float advance(double now) {
        double dt = time < 0 ? 0 : Math.max(0, Math.min(5, now - time)) / 20.0;
        time = now;
        return (float) dt;
    }
}
