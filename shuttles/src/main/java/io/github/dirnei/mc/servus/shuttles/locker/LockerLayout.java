package io.github.dirnei.mc.servus.shuttles.locker;

import java.util.ArrayList;
import java.util.List;

/**
 * The fixed layout of a Parcel Locker, five columns wide and two blocks
 * high, seen from the front, left to right: a column of four S boxes, a
 * column of two S boxes over one M box, the head (screen over the shuttle
 * dock), a column of two M boxes and a column of one L box. Heights are in
 * pixels from the locker's bottom, 0 to 32. Pure Java.
 */
public final class LockerLayout {

    /** Columns of a locker. */
    public static final int COLUMNS = 5;
    /** The head's column, where the dock and the screen stand. */
    public static final int HEAD_COLUMN = 2;
    /** Blocks a locker is high. */
    public static final int ROWS = 2;
    /** Pixels of one block. */
    public static final int BLOCK = 16;

    /** The size of a box; how many stacks each size holds comes from data. */
    public enum Size { S, M, L }

    /**
     * One box of the locker.
     *
     * @param index  its place in {@link #BOXES}, which is also the order of its slots
     * @param id     its label, such as {@code S-01}
     * @param column the column it stands in, 0 to 4 from the left
     * @param bottom its lower edge in pixels from the locker's bottom
     * @param top    its upper edge in pixels from the locker's bottom
     */
    public record Box(int index, String id, Size size, int column, int bottom, int top) {

        /** Whether the box reaches into the block of that row, 0 the lower and 1 the upper. */
        public boolean inRow(int row) {
            return bottom < (row + 1) * BLOCK && top > row * BLOCK;
        }
    }

    /** Where a box's slots lie in the locker's slots: {@code count} slots from {@code first}. */
    public record Range(int first, int count) {

        public boolean contains(int slot) {
            return slot >= first && slot < first + count;
        }
    }

    /** The ten boxes, top to bottom in each column, columns left to right. */
    public static final List<Box> BOXES = List.of(
            new Box(0, "S-01", Size.S, 0, 24, 32),
            new Box(1, "S-02", Size.S, 0, 16, 24),
            new Box(2, "S-03", Size.S, 0, 8, 16),
            new Box(3, "S-04", Size.S, 0, 0, 8),
            new Box(4, "S-05", Size.S, 1, 24, 32),
            new Box(5, "S-06", Size.S, 1, 16, 24),
            new Box(6, "M-01", Size.M, 1, 0, 16),
            new Box(7, "M-02", Size.M, 3, 16, 32),
            new Box(8, "M-03", Size.M, 3, 0, 16),
            new Box(9, "L-01", Size.L, 4, 0, 32));

    private LockerLayout() {
    }

    /** How many boxes of the size a locker has. */
    public static int count(Size size) {
        return (int) BOXES.stream().filter(box -> box.size() == size).count();
    }

    /** The boxes that reach into the block at that column and row, top to bottom. */
    public static List<Box> boxesIn(int column, int row) {
        List<Box> boxes = new ArrayList<>();
        for (Box box : BOXES) {
            if (box.column() == column && box.inRow(row)) {
                boxes.add(box);
            }
        }
        return boxes;
    }

    /**
     * The box a click hits in the block at that column and row, or null for
     * the head's column.
     *
     * @param hitY the height of the click inside the block in pixels, 0 to 16
     */
    public static Box boxAt(int column, int row, double hitY) {
        double y = row * BLOCK + Math.clamp(hitY, 0, BLOCK - 1e-6);
        for (Box box : BOXES) {
            if (box.column() == column && y >= box.bottom() && y < box.top()) {
                return box;
            }
        }
        return null;
    }

    /** The box with that label, or null. */
    public static Box box(String id) {
        for (Box box : BOXES) {
            if (box.id().equals(id)) {
                return box;
            }
        }
        return null;
    }

    /** The stacks each size holds, as data gives them. */
    public record Capacities(int small, int medium, int large) {

        public int of(Size size) {
            return switch (size) {
                case S -> small;
                case M -> medium;
                case L -> large;
            };
        }

        /** Slots of a whole locker: one slot per stack of every box. */
        public int total() {
            int sum = 0;
            for (Box box : BOXES) {
                sum += of(box.size());
            }
            return sum;
        }

        /** The slots of every box, in box order, one after the other. */
        public List<Range> ranges() {
            List<Range> ranges = new ArrayList<>(BOXES.size());
            int first = 0;
            for (Box box : BOXES) {
                int count = of(box.size());
                ranges.add(new Range(first, count));
                first += count;
            }
            return List.copyOf(ranges);
        }

        /** The index of the box a slot belongs to, or -1 outside the locker. */
        public int boxOf(int slot) {
            List<Range> ranges = ranges();
            for (int i = 0; i < ranges.size(); i++) {
                if (ranges.get(i).contains(slot)) {
                    return i;
                }
            }
            return -1;
        }
    }
}
