package io.github.dirnei.mc.servus.shuttles.locker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * How far each door of a Parcel Locker stands open on a client. Doors that
 * open together start one after another in box order, {@link #STAGGER_TICKS}
 * apart, and swing to {@link #OPEN_DEGREES} with an ease out over
 * {@link #SWING_TICKS}; a closing door eases back from where it stood. Times
 * are game ticks with the partial tick. Pure Java.
 */
public final class LockerDoors {

    public static final int SWING_TICKS = 10;
    public static final int STAGGER_TICKS = 8;
    public static final float OPEN_DEGREES = 108;

    private final boolean[] open;
    private final double[] start;
    private final float[] from;
    private final boolean[] announced;

    public LockerDoors(int boxes) {
        open = new boolean[boxes];
        start = new double[boxes];
        from = new float[boxes];
        announced = new boolean[boxes];
        Arrays.fill(announced, true);
    }

    /** Takes which doors should stand open at time {@code now}; newly opened ones start one after another. */
    public void update(boolean[] shouldOpen, double now) {
        int queued = 0;
        for (int box = 0; box < open.length; box++) {
            if (shouldOpen[box] == open[box]) {
                continue;
            }
            from[box] = angle(box, now);
            open[box] = shouldOpen[box];
            announced[box] = false;
            start[box] = shouldOpen[box] ? now + queued++ * STAGGER_TICKS : now;
        }
    }

    /** Degrees the box's door stands open at time {@code time}. */
    public float angle(int box, double time) {
        double progress = Math.clamp((time - start[box]) / SWING_TICKS, 0, 1);
        double eased = 1 - Math.pow(1 - progress, 3);
        return open[box] ? (float) (from[box] + (OPEN_DEGREES - from[box]) * eased) : (float) (from[box] * (1 - eased));
    }

    /** Whether the box's door stands open or is still moving at time {@code time}. */
    public boolean showsInside(int box, double time) {
        return angle(box, time) > 0;
    }

    /**
     * The doors that started to move since the last call, each once, as
     * {@code box} for an opening door and {@code -1 - box} for a closing one.
     */
    public List<Integer> started(double now) {
        List<Integer> started = new ArrayList<>();
        for (int box = 0; box < open.length; box++) {
            if (!announced[box] && now >= start[box]) {
                announced[box] = true;
                started.add(open[box] ? box : -1 - box);
            }
        }
        return started;
    }
}
