package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleTraffic.Car;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleTraffic.Limit;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleTraffic.Step;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShuttleTrafficTest {

    private static final double GAP = 1.0;
    private static final double WINDOW = 8;
    private static final double ACCEL = 0.01;
    private static final int EAST = ShuttleTraffic.direction(new NodePos(0, 0, 0), new NodePos(1, 0, 0));
    private static final int WEST = ShuttleTraffic.direction(new NodePos(1, 0, 0), new NodePos(0, 0, 0));

    /** Nodes along x at z, from x0 to x1 in that order. */
    private static List<NodePos> alongX(int x0, int x1, int z) {
        List<NodePos> path = new ArrayList<>();
        int step = x1 >= x0 ? 1 : -1;
        for (int x = x0; x != x1 + step; x += step) {
            path.add(new NodePos(x, 0, z));
        }
        return path;
    }

    /** Nodes along z at x, from z0 to z1 in that order. */
    private static List<NodePos> alongZ(int z0, int z1, int x) {
        List<NodePos> path = new ArrayList<>();
        int step = z1 >= z0 ? 1 : -1;
        for (int z = z0; z != z1 + step; z += step) {
            path.add(new NodePos(x, 0, z));
        }
        return path;
    }

    private static List<NodePos> join(List<NodePos> first, List<NodePos> second) {
        List<NodePos> path = new ArrayList<>(first);
        path.addAll(second.subList(1, second.size()));
        return path;
    }

    private static Car moving(int id, List<NodePos> path, int segment, double progress, double speed) {
        return new Car(id, id, path, segment, progress, ShuttleTraffic.NONE, false, true, true, speed, ACCEL, 0);
    }

    private static Car standing(int id, List<NodePos> path, int segment, int stopTicks) {
        return new Car(id, id, path, segment, 0, ShuttleTraffic.NONE, false, true, false, 0, ACCEL, stopTicks);
    }

    private static Car inGarage(int id, long order, List<NodePos> path) {
        return new Car(id, order, path, 0, 0, ShuttleTraffic.NONE, true, true, true, 0, ACCEL, 0);
    }

    private static Map<Integer, Limit> limits(Car... cars) {
        return ShuttleTraffic.limits(List.of(cars), GAP, WINDOW);
    }

    @Test
    void followerOnTheSameRailKeepsTheGap() {
        List<NodePos> aisle = alongX(0, 10, 0);
        Map<Integer, Limit> limits = limits(moving(1, aisle, 4, 0.5, 0.1), moving(2, aisle, 1, 0.25, 0.15));
        assertEquals(4.5 - 1.25 - GAP, limits.get(2).room(), 1e-9);
        assertEquals(1, limits.get(2).leader());
        assertEquals(0.1, limits.get(2).leaderSpeed(), 1e-9);
        assertFalse(limits.get(1).led(), "the car ahead has nobody ahead");
    }

    @Test
    void followerOnAnotherPathSharingTheRailKeepsTheGap() {
        Map<Integer, Limit> limits = limits(moving(1, alongX(3, 10, 0), 0, 0.5, 0.1), moving(2, alongX(0, 6, 0), 1, 0, 0.1));
        assertEquals(3.5 - 1 - GAP, limits.get(2).room(), 1e-9);
    }

    @Test
    void oppositeHeadingsPass() {
        Map<Integer, Limit> limits = limits(moving(1, alongX(0, 10, 0), 3, 0, 0.1), moving(2, alongX(10, 0, 0), 4, 0, 0.1));
        assertFalse(limits.get(1).led());
        assertFalse(limits.get(2).led());
    }

    @Test
    void standingCarHoldsTheCarBehindAndNotAnOncomingOne() {
        List<NodePos> toBay = alongX(0, 5, 0);
        Car loading = standing(1, toBay, 5, 10);
        Limit behind = limits(loading, moving(2, alongX(0, 9, 0), 2, 0, 0.1)).get(2);
        assertEquals(1, behind.leader());
        assertEquals(0, behind.leaderSpeed(), 1e-9);
        assertEquals(5 - 2 - GAP, behind.room(), 1e-9);
        Limit oncoming = limits(loading, moving(3, alongX(9, 0, 0), 2, 0, 0.1)).get(3);
        assertFalse(oncoming.led(), "a car driving the other way passes the loading one");
    }

    @Test
    void carTurningAtAStandingCarsNodeIsHeld() {
        Car loading = standing(1, alongX(0, 5, 0), 5, 10);
        Car turning = moving(2, join(alongX(2, 5, 0), alongZ(0, 4, 5)), 0, 0, 0.1);
        assertEquals(1, limits(loading, turning).get(2).leader());
    }

    @Test
    void carMergingIntoAStandingCarsHeadingIsHeld() {
        Car waiting = standing(1, alongX(0, 5, 0), 5, 0);
        Car merging = moving(2, join(alongZ(-3, 0, 5), alongX(5, 9, 0)), 0, 0, 0.1);
        Limit limit = limits(waiting, merging).get(2);
        assertEquals(1, limit.leader());
        assertEquals(3 - GAP, limit.room(), 1e-9);
    }

    @Test
    void perpendicularCrossingPasses() {
        Car east = moving(1, alongX(0, 10, 0), 4, 0.5, 0.1);
        Car north = moving(2, alongZ(5, -5, 5), 4, 0.5, 0.1);
        Map<Integer, Limit> limits = limits(east, north);
        assertFalse(limits.get(1).led());
        assertFalse(limits.get(2).led());
    }

    @Test
    void mergeGoesToTheNearerCar() {
        Car fromWest = moving(1, alongX(2, 9, 0), 0, 0.5, 0.1);
        Car fromNorth = moving(2, join(alongZ(-4, 0, 5), alongX(5, 9, 0)), 0, 0, 0.1);
        Map<Integer, Limit> limits = limits(fromWest, fromNorth);
        assertFalse(limits.get(1).led(), "the car 2.5 blocks from the junction goes first");
        assertEquals(1, limits.get(2).leader());
        assertEquals(4 - 2.5 - GAP, limits.get(2).room(), 1e-9);
    }

    @Test
    void mergeTieGoesToTheLowerOrder() {
        Car west = new Car(1, 4, alongX(2, 9, 0), 0, 0, ShuttleTraffic.NONE, false, true, true, 0.1, ACCEL, 0);
        Car north = new Car(2, 2, join(alongZ(-3, 0, 5), alongX(5, 9, 0)), 0, 0, ShuttleTraffic.NONE, false, true, true, 0.1, ACCEL, 0);
        Map<Integer, Limit> limits = limits(west, north);
        assertEquals(2, limits.get(1).leader(), "order 2 goes before order 4");
        assertFalse(limits.get(2).led());
    }

    @Test
    void carsOfOneGarageLeaveByOrder() {
        List<NodePos> out = alongX(0, 6, 0);
        Map<Integer, Limit> limits = limits(inGarage(1, 7, out), inGarage(2, 3, out));
        assertEquals(2, limits.get(1).leader());
        assertTrue(limits.get(1).room() < 0, "the later car waits");
        assertFalse(limits.get(2).led());
        Limit afterFirstMoved = limits(inGarage(1, 7, out), moving(2, out, 0, 0.05, 0.02)).get(1);
        assertEquals(0.05 - GAP, afterFirstMoved.room(), 1e-9);
    }

    @Test
    void inGarageCarWaitsForOneLeavingAGarageAheadInItsLane() {
        Map<Integer, Limit> limits = limits(inGarage(1, 1, alongX(1, 9, 0)), inGarage(2, 9, alongX(2, 9, 0)));
        assertEquals(2, limits.get(1).leader(), "the garage ahead goes first whatever the order");
        assertEquals(1 - GAP, limits.get(1).room(), 1e-9);
        assertFalse(limits.get(2).led());
    }

    @Test
    void inGarageCarYieldsToACarThatCannotStop() {
        List<NodePos> out = alongX(5, 10, 0);
        Car fast = moving(2, alongX(0, 10, 0), 3, 0, 0.3);
        Limit limit = limits(inGarage(1, 1, out), fast).get(1);
        assertEquals(2, limit.leader());
        assertTrue(limit.room() < 0);
    }

    @Test
    void inGarageCarLeavesBeforeAFarOrStandingCar() {
        List<NodePos> out = alongX(5, 10, 0);
        Car far = moving(2, alongX(-10, 10, 0), 1, 0, 0.1);
        assertFalse(limits(inGarage(1, 1, out), far).get(1).led());
        Car standingBehind = new Car(3, 3, alongX(0, 10, 0), 3, 0, ShuttleTraffic.NONE, false, true, false, 0, ACCEL, 0);
        assertFalse(limits(inGarage(1, 1, out), standingBehind).get(1).led());
    }

    @Test
    void inGarageCarWaitsForACarStandingOnTheHomeRail() {
        List<NodePos> out = alongX(5, 10, 0);
        Car onHome = standing(2, alongX(0, 5, 0), 5, 10);
        assertEquals(2, limits(inGarage(1, 1, out), onHome).get(1).leader());
    }

    @Test
    void carsOnTheRailsIgnoreACarInItsGarage() {
        Car leaving = inGarage(1, 1, alongX(5, 10, 0));
        Car passing = moving(2, alongX(0, 10, 0), 4, 0.5, 0.1);
        assertFalse(limits(leaving, passing).get(2).led());
    }

    @Test
    void carRidingALiftIsNoObstacle() {
        List<NodePos> aisle = alongX(0, 10, 0);
        Car riding = new Car(1, 1, aisle, 4, 0, ShuttleTraffic.NONE, false, false, false, 0, ACCEL, 0);
        assertFalse(limits(riding, moving(2, aisle, 1, 0, 0.1)).get(2).led());
    }

    @Test
    void lastHeadingCountsAtTheFirstNode() {
        Car standingAtStart = new Car(1, 1, List.of(new NodePos(5, 0, 0)), 0, 0, EAST, false, true, false, 0, ACCEL, 0);
        assertEquals(1, limits(standingAtStart, moving(2, alongX(0, 9, 0), 1, 0, 0.1)).get(2).leader());
        assertFalse(limits(standingAtStart, moving(3, alongX(9, 0, 0), 1, 0, 0.1)).get(3).led());
    }

    @Test
    void carTurningBackAtItsFirstNodeIsNoLeaderOfTheCarItTurnedAwayFrom() {
        Car turningBack = new Car(1, 1, alongX(5, 9, 0), 0, 0, WEST, false, true, false, 0, ACCEL, 0);
        assertFalse(limits(turningBack, moving(2, alongX(9, 0, 0), 2, 0, 0.1)).get(2).led(), "they face each other and pass");
        assertEquals(1, limits(turningBack, moving(3, alongX(0, 9, 0), 2, 0, 0.1)).get(3).leader(), "it still leads a car behind it");
    }

    @Test
    void carsDoNotLookPastTheStartOfALiftRide() {
        NodePos shaftTop = new NodePos(5, 2, 0);
        NodePos shaftMiddle = new NodePos(5, 1, 0);
        NodePos shaftBottom = new NodePos(5, 0, 0);
        List<NodePos> fromTop = List.of(new NodePos(4, 2, 0), shaftTop, shaftMiddle, shaftBottom, new NodePos(6, 0, 0), new NodePos(7, 0, 0));
        List<NodePos> fromMiddle = List.of(new NodePos(6, 1, 0), shaftMiddle, shaftBottom, new NodePos(6, 0, 0), new NodePos(7, 0, 0));
        Map<Integer, Limit> limits = limits(standing(1, fromTop, 0, 0), standing(2, fromMiddle, 0, 0));
        assertFalse(limits.get(1).led(), "the car decides who rides down first");
        assertFalse(limits.get(2).led());
    }

    @Test
    void ringsAreFoundAndTailsLeftOut() {
        Map<Integer, Integer> leaderOf = new LinkedHashMap<>();
        leaderOf.put(5, 1);
        leaderOf.put(1, 2);
        leaderOf.put(2, 3);
        leaderOf.put(3, 1);
        leaderOf.put(7, 8);
        assertEquals(List.of(List.of(1, 2, 3)), ShuttleTraffic.rings(leaderOf));
    }

    @Test
    void chainWalkFindsACarAhead() {
        Map<Integer, Integer> leaderOf = new LinkedHashMap<>();
        leaderOf.put(1, 2);
        leaderOf.put(2, 3);
        leaderOf.put(3, 2);
        assertTrue(ShuttleTraffic.chainReaches(leaderOf, 1, id -> id == 3));
        assertFalse(ShuttleTraffic.chainReaches(leaderOf, 1, id -> id == 1), "the start itself does not count and loops end");
    }

    @Test
    void stepLoadsCountSameHeadingCarsAndStopTicks() {
        List<NodePos> aisle = alongX(0, 10, 0);
        Map<Step, Double> loads = ShuttleTraffic.stepLoads(
                List.of(moving(1, aisle, 2, 0.5, 0.1), moving(2, aisle, 2, 0.2, 0.1), standing(3, alongX(0, 6, 0), 6, 9),
                        inGarage(4, 4, aisle)), 4);
        assertEquals(8, loads.get(new Step(new NodePos(2, 0, 0), new NodePos(3, 0, 0))), 1e-9);
        assertEquals(13, loads.get(new Step(new NodePos(5, 0, 0), new NodePos(6, 0, 0))), 1e-9);
        assertFalse(loads.containsKey(new Step(new NodePos(3, 0, 0), new NodePos(2, 0, 0))), "the other direction stays free");
        assertFalse(loads.containsKey(new Step(new NodePos(0, 0, 0), new NodePos(1, 0, 0))), "a car in its garage loads nothing");
    }

    @Test
    void stepLoadOfACarStandingAtItsFirstNodeUsesItsLastHeading() {
        Car standingAtStart = new Car(1, 1, List.of(new NodePos(5, 0, 0)), 0, 0, EAST, false, true, false, 0, ACCEL, 6);
        Map<Step, Double> loads = ShuttleTraffic.stepLoads(List.of(standingAtStart), 4);
        assertEquals(10, loads.get(new Step(new NodePos(4, 0, 0), new NodePos(5, 0, 0))), 1e-9);
    }
}
