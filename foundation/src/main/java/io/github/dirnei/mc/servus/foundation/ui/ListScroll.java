package io.github.dirnei.mc.servus.foundation.ui;

/**
 * Scroll rules of a list that shows {@code visible} of {@code rows} rows from
 * a first row {@code offset}: the offset stays where the last page still
 * fills the list, and the scrollbar thumb spans the visible share of the
 * track. Rows, lines or pixels work alike as long as all values share one
 * unit. Pure, no Minecraft imports.
 */
public final class ListScroll {

    /** The shortest thumb, so it can still be grabbed on long lists. */
    public static final int MIN_THUMB = 6;

    private ListScroll() {
    }

    /** The offset kept between the first row and the first row of the last full page. */
    public static int clamp(int offset, int rows, int visible) {
        return Math.max(0, Math.min(offset, rows - visible));
    }

    /** The offset after scrolling {@code delta} rows (positive is down). */
    public static int scroll(int offset, int delta, int rows, int visible) {
        return clamp(offset + delta, rows, visible);
    }

    /** Whether the list needs a scrollbar. */
    public static boolean scrolls(int rows, int visible) {
        return rows > visible;
    }

    /** Length of the thumb on a track {@code track} long, at least {@link #MIN_THUMB} but never longer than the track. */
    public static int thumbLength(int rows, int visible, int track) {
        if (!scrolls(rows, visible)) {
            return track;
        }
        return Math.max(Math.min(MIN_THUMB, track), track * visible / rows);
    }

    /** Distance of the thumb from the top of the track. */
    public static int thumbStart(int offset, int rows, int visible, int track) {
        if (!scrolls(rows, visible)) {
            return 0;
        }
        int free = track - thumbLength(rows, visible, track);
        return free * clamp(offset, rows, visible) / (rows - visible);
    }

    /**
     * Where on the thumb a press at {@code along} from the top of the track
     * holds it: the pressed point when the press is on the thumb, else the
     * thumb's centre, so a press on the track centres the thumb there.
     */
    public static double grabAt(double along, int thumbStart, int thumbLength) {
        if (along >= thumbStart && along < thumbStart + thumbLength) {
            return along - thumbStart;
        }
        return thumbLength / 2.0;
    }

    /**
     * The offset, 0 to {@code maxOffset}, whose thumb, {@code thumb} long on a
     * track {@code track} long, puts its grab point {@code grab} at
     * {@code along} from the top of the track; for dragging the thumb with the
     * mouse.
     */
    public static int offsetFor(double along, double grab, int thumb, int track, int maxOffset) {
        int free = track - thumb;
        if (maxOffset <= 0 || free <= 0) {
            return 0;
        }
        long offset = Math.round((along - grab) / free * maxOffset);
        return (int) Math.max(0, Math.min(maxOffset, offset));
    }
}
