package io.github.dirnei.mc.servus.foundation.side;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Which tanks one side of a Fluids layer fills or drains, from the side's mode and tank choice
 * and the tanks' roles and contents. Pure Java: fluids are plain keys.
 */
public final class FluidSideRules {

    /**
     * What one tank holds right now.
     *
     * @param key      the fluid in the tank, or null when it is empty
     * @param amount   how much it holds
     * @param capacity how much it can hold
     * @param role     which way sides may move fluid through it
     */
    public record TankState<K>(K key, long amount, long capacity, TankRole role) {

        public boolean isEmpty() {
            return key == null || amount <= 0;
        }

        public boolean hasRoom() {
            return amount < capacity;
        }
    }

    private FluidSideRules() {
    }

    /** Whether neighbours may fill the block through a side in this mode. */
    public static boolean allowsFill(SideMode mode) {
        return mode.allowsInsert();
    }

    /** Whether neighbours may drain the block through a side in this mode. */
    public static boolean allowsDrain(SideMode mode) {
        return mode.allowsExtract();
    }

    /** Whether the block gives fluid to the neighbour on a side in this mode. */
    public static boolean pushes(SideMode mode) {
        return mode.pushes();
    }

    /** Whether the block takes fluid from the neighbour on a side in this mode. */
    public static boolean pulls(SideMode mode) {
        return mode.pulls();
    }

    /**
     * The tanks to try, in order, for {@code offered} arriving through {@code side}: tanks already
     * holding it first, then empty tanks, each rank in tank order. Tanks holding another fluid, full
     * tanks, tanks the side does not reach and drain only tanks are left out.
     *
     * @param accepts whether a tank (by index) takes the fluid at all, for example a kept fluid
     */
    public static <K> List<Integer> fillOrder(FluidSides sides, RelativeSide side, List<TankState<K>> tanks, K offered,
                                              BiPredicate<Integer, K> accepts) {
        List<Integer> holding = new ArrayList<>();
        List<Integer> empty = new ArrayList<>();
        if (offered == null || !allowsFill(sides.mode(side))) {
            return holding;
        }
        for (int tank = 0; tank < tanks.size(); tank++) {
            TankState<K> state = tanks.get(tank);
            if (!sides.reaches(side, tank) || !state.role().fills() || !state.hasRoom() || !accepts.test(tank, offered)) {
                continue;
            }
            if (state.isEmpty()) {
                empty.add(tank);
            } else if (state.key().equals(offered)) {
                holding.add(tank);
            }
        }
        holding.addAll(empty);
        return holding;
    }

    /** The tanks {@code side} may drain, in tank order: reached, not fill only and not empty. */
    public static <K> List<Integer> drainOrder(FluidSides sides, RelativeSide side, List<TankState<K>> tanks) {
        List<Integer> order = new ArrayList<>();
        if (!allowsDrain(sides.mode(side))) {
            return order;
        }
        for (int tank = 0; tank < tanks.size(); tank++) {
            TankState<K> state = tanks.get(tank);
            if (sides.reaches(side, tank) && state.role().drains() && !state.isEmpty()) {
                order.add(tank);
            }
        }
        return order;
    }
}
