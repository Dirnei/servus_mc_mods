package io.github.dirnei.mc.servus.storage.shuttle.module.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The wires step: contacts in the duct colours on the left, the same colours
 * shuffled on the right. A wire only connects a left contact to the right
 * contact of its colour; a wrong pair does nothing. Colours are channel
 * indexes 0 to 7. Pure Java.
 */
public final class WireBoard {

    private static final int COLOURS = 8;

    private final List<Integer> left;
    private final List<Integer> right;
    /** For each left contact, the right contact it is wired to, or -1. */
    private final int[] wired;
    private int selected = -1;

    public WireBoard(long seed, int count) {
        Random random = new Random(seed);
        List<Integer> colours = new ArrayList<>();
        for (int i = 0; i < COLOURS; i++) {
            colours.add(i);
        }
        Collections.shuffle(colours, random);
        int n = Math.max(1, Math.min(COLOURS, count));
        this.left = List.copyOf(colours.subList(0, n));
        List<Integer> shuffled = new ArrayList<>(left);
        Collections.shuffle(shuffled, random);
        if (n > 1 && shuffled.equals(left)) {
            // A board where every wire runs straight across is no puzzle; turn it once.
            Collections.rotate(shuffled, 1);
        }
        this.right = List.copyOf(shuffled);
        this.wired = new int[n];
        Arrays.fill(wired, -1);
    }

    /** The colour of each left contact, top to bottom. */
    public List<Integer> left() {
        return left;
    }

    /** The colour of each right contact, top to bottom. */
    public List<Integer> right() {
        return right;
    }

    /** The left contact picked up, or -1. */
    public int selected() {
        return selected;
    }

    /** The right contact a left contact is wired to, or -1. */
    public int wiredTo(int leftIndex) {
        return leftIndex < 0 || leftIndex >= wired.length ? -1 : wired[leftIndex];
    }

    /** Picks up an open left contact; a wired one cannot be picked up again. */
    public boolean select(int leftIndex) {
        if (leftIndex < 0 || leftIndex >= left.size() || wired[leftIndex] >= 0) {
            return false;
        }
        selected = leftIndex;
        return true;
    }

    /** Wires the picked up contact to a right contact of the same colour; returns whether it connected. */
    public boolean connect(int rightIndex) {
        if (selected < 0 || rightIndex < 0 || rightIndex >= right.size() || !right.get(rightIndex).equals(left.get(selected))) {
            return false;
        }
        for (int target : wired) {
            if (target == rightIndex) {
                return false;
            }
        }
        wired[selected] = rightIndex;
        selected = -1;
        return true;
    }

    public boolean done() {
        for (int target : wired) {
            if (target < 0) {
                return false;
            }
        }
        return true;
    }
}
