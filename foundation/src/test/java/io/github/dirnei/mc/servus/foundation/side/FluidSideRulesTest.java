package io.github.dirnei.mc.servus.foundation.side;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.side.FluidSideRules.TankState;
import java.util.List;
import org.junit.jupiter.api.Test;

class FluidSideRulesTest {

    private static final RelativeSide SIDE = RelativeSide.LEFT;

    private static FluidSides sides(int tanks, SideMode mode) {
        FluidSides sides = new FluidSides(tanks, SideConfig.MACHINE_MODES, SideMode.AUTO);
        sides.setMode(SIDE, mode);
        return sides;
    }

    private static TankState<String> tank(String key, long amount, TankRole role) {
        return new TankState<>(key, amount, 1000, role);
    }

    private static TankState<String> empty() {
        return tank(null, 0, TankRole.ANY);
    }

    private static List<Integer> fill(FluidSides sides, List<TankState<String>> tanks, String offered) {
        return FluidSideRules.fillOrder(sides, SIDE, tanks, offered, (tank, key) -> true);
    }

    @Test
    void holdingTankComesBeforeEmptyTank() {
        List<TankState<String>> tanks = List.of(empty(), tank("water", 500, TankRole.ANY));
        assertEquals(List.of(1, 0), fill(sides(2, SideMode.INPUT), tanks, "water"));
    }

    @Test
    void tankHoldingAnotherFluidIsNeverFilled() {
        List<TankState<String>> tanks = List.of(tank("lava", 100, TankRole.ANY), empty());
        assertEquals(List.of(1), fill(sides(2, SideMode.BOTH), tanks, "water"));
    }

    @Test
    void fullTankIsSkipped() {
        List<TankState<String>> tanks = List.of(tank("water", 1000, TankRole.ANY), empty());
        assertEquals(List.of(1), fill(sides(2, SideMode.INPUT), tanks, "water"));
    }

    @Test
    void sideOnOneTankReachesOnlyIt() {
        FluidSides sides = sides(3, SideMode.BOTH);
        sides.setTank(SIDE, 1);
        List<TankState<String>> tanks = List.of(tank("water", 10, TankRole.ANY), tank("water", 10, TankRole.ANY), empty());
        assertEquals(List.of(1), fill(sides, tanks, "water"));
        assertEquals(List.of(1), FluidSideRules.drainOrder(sides, SIDE, tanks));
    }

    @Test
    void fillOnlyTankIsNeverDrained() {
        List<TankState<String>> tanks = List.of(tank("water", 10, TankRole.FILL_ONLY), tank("lava", 10, TankRole.DRAIN_ONLY));
        assertEquals(List.of(1), FluidSideRules.drainOrder(sides(2, SideMode.PUSH), SIDE, tanks));
    }

    @Test
    void drainOnlyTankIsNeverFilled() {
        List<TankState<String>> tanks = List.of(tank(null, 0, TankRole.DRAIN_ONLY), empty());
        assertEquals(List.of(1), fill(sides(2, SideMode.BOTH), tanks, "water"));
    }

    @Test
    void offReachesNothing() {
        FluidSides sides = sides(2, SideMode.DISABLED);
        List<TankState<String>> tanks = List.of(tank("water", 10, TankRole.ANY), empty());
        assertEquals(List.of(), fill(sides, tanks, "water"));
        assertEquals(List.of(), FluidSideRules.drainOrder(sides, SIDE, tanks));
    }

    @Test
    void modeLimitsDirection() {
        List<TankState<String>> tanks = List.of(tank("water", 10, TankRole.ANY));
        assertEquals(List.of(), FluidSideRules.drainOrder(sides(1, SideMode.INPUT), SIDE, tanks));
        assertEquals(List.of(), fill(sides(1, SideMode.OUTPUT), tanks, "water"));
        assertEquals(List.of(0), fill(sides(1, SideMode.PULL), tanks, "water"));
    }

    @Test
    void predicateRefusesKeptFluidMismatch() {
        List<TankState<String>> tanks = List.of(empty(), empty());
        List<Integer> order = FluidSideRules.fillOrder(sides(2, SideMode.INPUT), SIDE, tanks, "water",
                (tank, key) -> tank != 0 || key.equals("lava"));
        assertEquals(List.of(1), order);
    }

    @Test
    void drainOrderIsTankOrderAndSkipsEmptyTanks() {
        List<TankState<String>> tanks = List.of(tank("water", 10, TankRole.ANY), empty(), tank("lava", 5, TankRole.ANY));
        assertEquals(List.of(0, 2), FluidSideRules.drainOrder(sides(3, SideMode.OUTPUT), SIDE, tanks));
    }
}
