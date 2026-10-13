package io.github.dirnei.mc.servus.storage.shuttle.module.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The bolts step: spots spread over the board, tightened in order. Only a
 * click on the glowing spot counts; any other click does nothing, so the
 * step cannot fail. Pure Java.
 */
public final class BoltBoard {

    /** How close to a spot a click has to be, in pixels. */
    public static final int RADIUS = 5;
    private static final int MARGIN = 8;
    private static final int MIN_DISTANCE = 14;
    private static final int TRIES = 60;

    private final List<Spot> spots;
    private int tightened;

    public BoltBoard(long seed, int count, int width, int height) {
        Random random = new Random(seed);
        List<Spot> placed = new ArrayList<>();
        int spanX = Math.max(1, width - 2 * MARGIN);
        int spanY = Math.max(1, height - 2 * MARGIN);
        for (int i = 0; i < Math.max(1, count); i++) {
            Spot best = null;
            for (int attempt = 0; attempt < TRIES; attempt++) {
                Spot candidate = new Spot(MARGIN + random.nextInt(spanX), MARGIN + random.nextInt(spanY));
                best = candidate;
                if (clear(placed, candidate)) {
                    break;
                }
            }
            placed.add(best);
        }
        this.spots = List.copyOf(placed);
    }

    private static boolean clear(List<Spot> placed, Spot candidate) {
        for (Spot spot : placed) {
            if (spot.near(candidate.x(), candidate.y(), MIN_DISTANCE)) {
                return false;
            }
        }
        return true;
    }

    public List<Spot> spots() {
        return spots;
    }

    /** How many bolts are tightened; the next one glows. */
    public int tightened() {
        return tightened;
    }

    /** Tightens the glowing bolt when the click is on it; returns whether it was. */
    public boolean click(int x, int y) {
        if (done() || !spots.get(tightened).near(x, y, RADIUS)) {
            return false;
        }
        tightened++;
        return true;
    }

    public boolean done() {
        return tightened >= spots.size();
    }
}
