package io.github.dirnei.mc.servus.shuttles.gantry;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of a gantry's tanks: each holds one fluid at a time and has a
 * lock, one fluid or Any (null). A locked tank takes only its fluid, also
 * while empty; a tank on Any takes any fluid while empty and then only the one
 * it holds. A tank whose lock differs from its content keeps the content,
 * takes nothing and shows a warning. Pure Java; {@code K} is a fluid key
 * compared with {@code equals}.
 */
public final class GantryTanks {

    /**
     * One tank as the rules see it.
     *
     * @param lock     the locked fluid, null for Any
     * @param content  the fluid held, null when empty
     * @param amount   how much it holds
     * @param capacity how much it may hold
     */
    public record Tank<K>(K lock, K content, int amount, int capacity) {

        public boolean isEmpty() {
            return content == null || amount <= 0;
        }

        public int room() {
            return Math.max(0, capacity - amount);
        }

        /** Whether the tank takes this fluid at all, room aside. */
        public boolean accepts(K key) {
            if (lock != null && !lock.equals(key)) {
                return false;
            }
            return isEmpty() || content.equals(key) && (lock == null || lock.equals(content));
        }

        /** Whether the tank holds a fluid other than its lock. */
        public boolean warning() {
            return lock != null && !isEmpty() && !lock.equals(content);
        }
    }

    private GantryTanks() {
    }

    /**
     * The tanks that may take {@code key}, in the order they fill: locked to
     * it first, then on Any holding it, then empty on Any. Full tanks are left out.
     */
    public static <K> List<Integer> fillOrder(List<Tank<K>> tanks, K key) {
        List<Integer> order = new ArrayList<>();
        for (int rank = 0; rank < 3; rank++) {
            for (int index = 0; index < tanks.size(); index++) {
                Tank<K> tank = tanks.get(index);
                if (tank.room() > 0 && tank.accepts(key) && rank(tank) == rank) {
                    order.add(index);
                }
            }
        }
        return order;
    }

    private static int rank(Tank<?> tank) {
        if (tank.lock() != null) {
            return 0;
        }
        return tank.isEmpty() ? 2 : 1;
    }

    /** The first tank that holds a fluid other than its lock, or -1. */
    public static int firstWarning(List<? extends Tank<?>> tanks) {
        for (int index = 0; index < tanks.size(); index++) {
            if (tanks.get(index).warning()) {
                return index;
            }
        }
        return -1;
    }
}
