package io.github.dirnei.mc.servus.alchemy.venter;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The straw of a Venter Equinus: up to {@code capacity} bales, each with the work ticks it keeps the
 * bed warm. Only the oldest bale rots, one tick per working tick, and is gone when spent. Pure Java.
 */
public final class StrawBed {

    private final int capacity;
    /** Each bale as its full and its remaining warmth, oldest first. */
    private final Deque<int[]> bales = new ArrayDeque<>();

    public StrawBed(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public int bales() {
        return bales.size();
    }

    public int room() {
        return capacity - bales.size();
    }

    /** The work ticks of warmth left in all bales. */
    public long warmthLeft() {
        return bales.stream().mapToLong(bale -> bale[1]).sum();
    }

    /** Lays in one bale that keeps the bed warm for {@code warmth} work ticks; false when the bed is full. */
    public boolean add(int warmth) {
        if (warmth <= 0 || bales.size() >= capacity) {
            return false;
        }
        bales.addLast(new int[] {warmth, warmth});
        return true;
    }

    /** One working tick: the oldest bale rots by one tick and is dropped when spent. */
    public void burn() {
        int[] oldest = bales.peekFirst();
        if (oldest != null && --oldest[1] <= 0) {
            bales.removeFirst();
        }
    }

    /** The straw left as a share of a full bed, in thousandths; the oldest bale counts by what is left of it. */
    public int levelMilli() {
        int[] oldest = bales.peekFirst();
        if (oldest == null || capacity <= 0) {
            return 0;
        }
        long left = (long) (bales.size() - 1) * oldest[0] + oldest[1];
        return (int) (left * 1000 / ((long) oldest[0] * capacity));
    }

    public void clear() {
        bales.clear();
    }

    /** The bales as pairs of full and remaining warmth, oldest first, for saving. */
    public int[] save() {
        int[] pairs = new int[bales.size() * 2];
        int i = 0;
        for (int[] bale : bales) {
            pairs[i++] = bale[0];
            pairs[i++] = bale[1];
        }
        return pairs;
    }

    /** Replaces the bales with the pairs written by {@link #save()}, dropping any beyond the capacity. */
    public void load(int[] pairs) {
        bales.clear();
        for (int i = 0; i + 1 < pairs.length && bales.size() < capacity; i += 2) {
            if (pairs[i] > 0 && pairs[i + 1] > 0) {
                bales.addLast(new int[] {pairs[i], Math.min(pairs[i], pairs[i + 1])});
            }
        }
    }
}
