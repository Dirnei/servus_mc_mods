package io.github.dirnei.mc.servus.foundation.ui;

/**
 * Scroll rules of a list that shows {@code visible} of {@code rows} rows from
 * a first row {@code offset}: the offset stays where the last page still
 * fills the list, and the scrollbar thumb spans the visible share of the
 * track. Pure, no Minecraft imports.
 */
public final class ListScroll {

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

    /** Length of the thumb on a track {@code track} long, at least 2. */
    public static int thumbLength(int rows, int visible, int track) {
        if (!scrolls(rows, visible)) {
            return track;
        }
        return Math.max(2, track * visible / rows);
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
     * The offset, 0 to {@code maxOffset}, whose thumb, {@code thumb} long on a
     * track {@code track} long, centres on {@code along} from the top of the
     * track; for dragging the thumb with the mouse.
     */
    public static int offsetAt(double along, int thumb, int track, int maxOffset) {
        int free = track - thumb;
        if (maxOffset <= 0 || free <= 0) {
            return 0;
        }
        long offset = Math.round((along - thumb / 2.0) / free * maxOffset);
        return (int) Math.max(0, Math.min(maxOffset, offset));
    }
}
