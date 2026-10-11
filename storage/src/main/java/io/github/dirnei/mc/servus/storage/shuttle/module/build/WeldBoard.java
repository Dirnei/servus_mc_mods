package io.github.dirnei.mc.servus.storage.shuttle.module.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The weld step: one seam after another, each a gentle curve across the
 * board. Dragging over a seam welds the points close to the pointer; a seam
 * is done once most of it is welded. Pure, no Minecraft imports.
 */
public final class WeldBoard {

    /** How close to a seam point the pointer has to pass, in pixels. */
    public static final int RADIUS = 6;
    /** The share of a seam that has to be welded. */
    public static final double DONE_SHARE = 0.9;
    private static final int POINTS = 24;
    private static final int MARGIN = 10;

    private final List<List<Spot>> seams;
    private final List<boolean[]> welded;
    private int current;

    public WeldBoard(long seed, int count, int width, int height) {
        Random random = new Random(seed);
        int n = Math.max(1, count);
        List<List<Spot>> lines = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            int baseY = height * (s + 1) / (n + 1);
            double amplitude = Math.min(height / (2.0 * (n + 1)), 6 + random.nextInt(6));
            double phase = random.nextDouble() * Math.PI * 2;
            double waves = 1 + random.nextDouble();
            List<Spot> points = new ArrayList<>();
            for (int i = 0; i < POINTS; i++) {
                double t = i / (double) (POINTS - 1);
                int x = MARGIN + (int) Math.round(t * Math.max(1, width - 2 * MARGIN));
                int y = baseY + (int) Math.round(amplitude * Math.sin(phase + t * waves * Math.PI * 2));
                points.add(new Spot(x, y));
            }
            lines.add(List.copyOf(points));
        }
        this.seams = List.copyOf(lines);
        this.welded = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            welded.add(new boolean[POINTS]);
        }
    }

    public List<List<Spot>> seams() {
        return seams;
    }

    /** The seam being welded now; equal to the seam count when all are done. */
    public int current() {
        return current;
    }

    /** Whether that point of that seam is welded. */
    public boolean welded(int seam, int point) {
        return seam < current || welded.get(seam)[point];
    }

    /** Welds the points of the current seam near the pointer; returns whether any new point was welded. */
    public boolean cover(int x, int y) {
        if (done()) {
            return false;
        }
        List<Spot> points = seams.get(current);
        boolean[] marks = welded.get(current);
        boolean any = false;
        for (int i = 0; i < points.size(); i++) {
            if (!marks[i] && points.get(i).near(x, y, RADIUS)) {
                marks[i] = true;
                any = true;
            }
        }
        if (share(marks) >= DONE_SHARE) {
            current++;
        }
        return any;
    }

    private static double share(boolean[] marks) {
        int count = 0;
        for (boolean mark : marks) {
            if (mark) {
                count++;
            }
        }
        return count / (double) marks.length;
    }

    public boolean done() {
        return current >= seams.size();
    }
}
