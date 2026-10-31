package io.github.dirnei.mc.servus.shuttles.locker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Which free boxes of a locker an order takes: the set that holds all its
 * stacks with the least spare room, then the fewest boxes, then the smallest
 * boxes. Tries every set, at most 2^10 for a locker. Pure Java.
 */
public final class BoxAssignment {

    /** One free box: its index in the locker and how many stacks it holds. */
    public record Free(int box, int capacity) {
    }

    /** One entry of an order: how many items and how many of them make a stack. */
    public record Need(long amount, int stackSize) {
    }

    private BoxAssignment() {
    }

    /** The stacks an order fills, each entry rounded up to whole stacks on its own. */
    public static int stacksNeeded(Collection<Need> needs) {
        long stacks = 0;
        for (Need need : needs) {
            if (need.amount() > 0) {
                int size = Math.max(1, need.stackSize());
                stacks += (need.amount() + size - 1) / size;
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, stacks);
    }

    /**
     * The boxes, as indices into the locker, that take {@code stacks}, or
     * empty when all free boxes together hold fewer or nothing is needed.
     */
    public static Optional<List<Integer>> assign(List<Free> free, int stacks) {
        if (stacks <= 0 || free.size() >= Integer.SIZE - 1) {
            return Optional.empty();
        }
        int best = -1;
        for (int set = 1; set < 1 << free.size(); set++) {
            if (room(free, set) >= stacks && (best < 0 || better(free, set, best, stacks))) {
                best = set;
            }
        }
        return best < 0 ? Optional.empty() : Optional.of(boxes(free, best));
    }

    private static boolean better(List<Free> free, int set, int than, int stacks) {
        int spare = room(free, set) - stacks;
        int thanSpare = room(free, than) - stacks;
        if (spare != thanSpare) {
            return spare < thanSpare;
        }
        if (Integer.bitCount(set) != Integer.bitCount(than)) {
            return Integer.bitCount(set) < Integer.bitCount(than);
        }
        return smallerFirst(capacities(free, set), capacities(free, than));
    }

    /** Whether the sorted capacities start with a smaller box, so small boxes fill before large ones. */
    private static boolean smallerFirst(List<Integer> a, List<Integer> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return a.get(i) < b.get(i);
            }
        }
        return false;
    }

    private static int room(List<Free> free, int set) {
        int sum = 0;
        for (int i = 0; i < free.size(); i++) {
            if ((set & 1 << i) != 0) {
                sum += free.get(i).capacity();
            }
        }
        return sum;
    }

    private static List<Integer> capacities(List<Free> free, int set) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < free.size(); i++) {
            if ((set & 1 << i) != 0) {
                list.add(free.get(i).capacity());
            }
        }
        list.sort(Integer::compare);
        return list;
    }

    private static List<Integer> boxes(List<Free> free, int set) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < free.size(); i++) {
            if ((set & 1 << i) != 0) {
                list.add(free.get(i).box());
            }
        }
        return List.copyOf(list);
    }
}
