package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Phases;

/**
 * Where a running ritual stands, from the ticks since the offering: how far the offering has risen, where the streams
 * are, how full and how grown each turned block is. The server and every client read the same numbers. Pure Java.
 */
public final class RitualTimeline {

    /** Ticks between one turned block and the next, so the streams and the growth do not move in lockstep. */
    public static final float STAGGER = 3.6F;
    /** Lights travelling along one stream, each a tick behind the one before. */
    public static final int MOTES = 10;

    private final AthanorRitual ritual;

    public RitualTimeline(AthanorRitual ritual) {
        this.ritual = ritual;
    }

    public AthanorRitual ritual() {
        return ritual;
    }

    private Phases phases() {
        return ritual.phases();
    }

    /** Whether the offering still hovers over the plate, before it bursts. */
    public boolean offeringShown(float t) {
        return t >= 0 && t < phases().burst() + 1;
    }

    /** How far the offering has risen, 0 to 1. */
    public float offeringRise(float t) {
        return ease(t / (phases().burst() * 0.83F));
    }

    /** How strongly the ritual's colour lies over the flame, 0 to 1. */
    public float flameTint(float t) {
        return clamp01(t / (phases().burst() * 0.67F));
    }

    /** The flame's tint: the ritual's colour, turning to the result colour after the flash. */
    public int flameColor(float t) {
        return mix(ritual.color(), ritual.resultColor(), clamp01((t - phases().settle()) / (ritual.duration() - phases().settle())));
    }

    /** The ticks turn {@code index} starts after the first. */
    public static float delay(int index) {
        return index * STAGGER;
    }

    private float streamTicks() {
        return Math.max(4, (phases().fill() - phases().burst()) * 5F / 6F);
    }

    /**
     * Where mote {@code mote} of the stream to turn {@code index} is along its arc, 0 at the plate and 1 at the block,
     * or a value outside 0 to 1 while it is not under way.
     */
    public float stream(float t, int index, int mote) {
        return (t - phases().burst() - 2 - delay(index) - mote) / streamTicks();
    }

    /** How full turned block {@code index} is, 0 to 1. */
    public float fill(float t, int index) {
        return ease((t - phases().fill() - delay(index)) / ((phases().grow() - phases().fill()) * 0.9F));
    }

    /** How far turned block {@code index} of {@code count} has grown, 0 to 1; every block is grown by the flash. */
    public float growth(float t, int index, int count) {
        float span = phases().settle() - phases().grow() - delay(Math.max(0, count - 1));
        return clamp01((t - phases().grow() - delay(index)) / Math.max(1, span));
    }

    /** How far the final flash is, 0 to 1, or outside while it does not shine. */
    public float flash(float t) {
        return (t - phases().settle()) / (ritual.duration() - phases().settle());
    }

    public static float clamp01(float value) {
        return Math.max(0, Math.min(1, value));
    }

    /** Fast at first, slowing to a stop at 1. */
    public static float ease(float value) {
        float rest = 1 - clamp01(value);
        return 1 - rest * rest * rest;
    }

    /** Like {@link #ease}, but overshoots a little before it settles at 1. */
    public static float easeBack(float value) {
        float v = clamp01(value) - 1;
        float c = 1.6F;
        return 1 + (c + 1) * v * v * v + c * v * v;
    }

    /** The RGB colour {@code t} of the way from {@code from} to {@code to}. */
    public static int mix(int from, int to, float t) {
        float f = clamp01(t);
        int r = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * f);
        int g = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * f);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return r << 16 | g << 8 | b;
    }
}
