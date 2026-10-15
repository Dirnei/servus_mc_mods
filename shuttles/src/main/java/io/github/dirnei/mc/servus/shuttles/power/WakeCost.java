package io.github.dirnei.mc.servus.shuttles.power;

/** What a wake costs: FE per chunk of the site each tick it is held, and one second of that to start. */
public final class WakeCost {

    /** Ticks of FE a sleeping site must hold to wake. */
    public static final int START_TICKS = 20;

    private WakeCost() {
    }

    /** FE one tick of a wake costs. */
    public static long perTick(int chunks, double perChunk) {
        if (chunks <= 0 || perChunk <= 0) {
            return 0;
        }
        return (long) Math.ceil(chunks * perChunk);
    }

    /** FE a site must hold to wake: one second awake. */
    public static long toStart(int chunks, double perChunk) {
        return perTick(chunks, perChunk) * START_TICKS;
    }
}
