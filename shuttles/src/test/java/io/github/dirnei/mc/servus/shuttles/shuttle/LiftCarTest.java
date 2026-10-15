package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LiftCarTest {

    private static int ticksUntil(LiftCar car, double speed, java.util.function.BooleanSupplier done) {
        int ticks = 0;
        while (!done.getAsBoolean()) {
            car.tick(speed);
            ticks++;
            if (ticks > 10_000) {
                throw new AssertionError("car never got there");
            }
        }
        return ticks;
    }

    @Test
    void startsAtTheBottom() {
        LiftCar car = new LiftCar(64, 67);
        assertEquals(64, car.y(), 1e-9);
        assertNull(car.target());
        car.tick(0.1);
        assertFalse(car.moving());
    }

    @Test
    void boardsOnlyFirstCallerAtItsFloor() {
        LiftCar car = new LiftCar(0, 3);
        car.call(1, 2);
        car.call(2, 0);
        assertFalse(car.canBoard(2, 0), "second in line waits even with the car at its floor");
        assertFalse(car.canBoard(1, 2), "car is not there yet");
        ticksUntil(car, 0.1, () -> car.y() == 2);
        assertTrue(car.canBoard(1, 2));
        car.board(1);
        assertFalse(car.canBoard(2, 0));
    }

    @Test
    void servesCallsInOrder() {
        LiftCar car = new LiftCar(0, 4);
        car.call(9, 2);
        car.board(9);
        car.send(9, 2);
        ticksUntil(car, 0.5, () -> car.arrivedFor(9, 2));
        car.call(1, 0);
        car.call(2, 4);
        car.leave(9);
        assertEquals(0, car.target());
        ticksUntil(car, 0.5, () -> car.canBoard(1, 0));
        car.board(1);
        car.send(1, 3);
        ticksUntil(car, 0.5, () -> car.arrivedFor(1, 3));
        car.leave(1);
        assertEquals(4, car.target());
        ticksUntil(car, 0.5, () -> car.canBoard(2, 4));
    }

    @Test
    void callIsIdempotent() {
        LiftCar car = new LiftCar(0, 3);
        car.call(1, 2);
        car.call(1, 2);
        car.call(1, 3);
        assertEquals(List.of(new LiftCar.Call(1, 2)), car.calls());
    }

    @Test
    void rideOfThreeFloorsTakesThirtyTicks() {
        LiftCar car = new LiftCar(0, 3);
        car.call(1, 0);
        assertTrue(car.canBoard(1, 0));
        car.board(1);
        car.send(1, 3);
        int ticks = ticksUntil(car, 0.1, () -> car.arrivedFor(1, 3));
        assertEquals(30, ticks);
    }

    @Test
    void parksAtTheLastFloor() {
        LiftCar car = new LiftCar(0, 3);
        car.call(1, 0);
        car.board(1);
        car.send(1, 3);
        ticksUntil(car, 0.1, () -> car.arrivedFor(1, 3));
        car.leave(1);
        for (int i = 0; i < 50; i++) {
            car.tick(0.1);
        }
        assertEquals(3, car.y(), 1e-9);
        assertFalse(car.moving());
    }

    @Test
    void canCrossOnlyWithDeckDownOrCarIdleThere() {
        LiftCar car = new LiftCar(0, 4);
        assertTrue(car.canCross(0), "idle and empty at that floor");
        assertTrue(car.canCross(1), "one floor away the deck is down");
        car.call(1, 4);
        car.tick(0.1);
        assertTrue(car.moving());
        assertFalse(car.canCross(0), "car is moving away from the floor");
        assertFalse(car.canCross(1), "car is moving within one floor");
        ticksUntil(car, 0.1, () -> car.y() >= 2.0);
        assertTrue(car.canCross(1));
        assertTrue(car.canCross(0));
    }

    @Test
    void noCrossingWithRiderAtTheFloor() {
        LiftCar car = new LiftCar(0, 2);
        car.call(1, 0);
        car.board(1);
        assertFalse(car.canCross(0));
    }

    @Test
    void crosserInTheSweepHoldsDeparture() {
        LiftCar car = new LiftCar(0, 3);
        assertTrue(car.canCross(1));
        car.beginCross(7, 1);
        car.call(1, 3);
        for (int i = 0; i < 20; i++) {
            car.tick(0.1);
        }
        assertEquals(0, car.y(), 1e-9, "car must not fold the deck under the crosser");
        car.endCross(7);
        ticksUntil(car, 0.1, () -> car.y() == 3);
    }

    @Test
    void crosserOnTheParkedCarHoldsIt() {
        LiftCar car = new LiftCar(0, 3);
        assertTrue(car.canCross(0));
        car.beginCross(7, 0);
        car.call(1, 3);
        car.tick(0.1);
        assertEquals(0, car.y(), 1e-9);
        car.endCross(7);
        car.tick(0.1);
        assertTrue(car.moving());
    }

    @Test
    void crosserBehindTheCarDoesNotHoldIt() {
        LiftCar car = new LiftCar(0, 5);
        car.call(1, 3);
        ticksUntil(car, 0.1, () -> car.y() == 3);
        car.board(1);
        car.send(1, 5);
        car.beginCross(7, 0);
        ticksUntil(car, 0.1, () -> car.arrivedFor(1, 5));
    }

    @Test
    void resizeClampsTheCar() {
        LiftCar car = new LiftCar(0, 5);
        car.call(1, 5);
        ticksUntil(car, 0.5, () -> car.y() == 5);
        car.resize(0, 2);
        assertEquals(2, car.y(), 1e-9);
        assertEquals(2, car.topY());
    }

    @Test
    void removeDropsCallRideAndCrossing() {
        LiftCar car = new LiftCar(0, 3);
        car.call(1, 0);
        car.call(2, 3);
        car.remove(2);
        assertEquals(List.of(new LiftCar.Call(1, 0)), car.calls());
        car.board(1);
        car.send(1, 3);
        car.remove(1);
        assertFalse(car.carrying());
        assertNull(car.target());
        car.beginCross(5, 2);
        car.remove(5);
        assertFalse(car.isCrossing(5));
    }

    @Test
    void carSpeedsUpAndBrakesOntoItsFloor() {
        LiftCar car = new LiftCar(0, 4);
        car.call(1, 4);
        car.tick(0.1, 0.01);
        assertEquals(0.01, car.y(), 1e-9);
        car.tick(0.1, 0.01);
        assertEquals(0.03, car.y(), 1e-9);
        double fastest = 0;
        for (int i = 0; i < 400 && car.y() < 4; i++) {
            car.tick(0.1, 0.01);
            fastest = Math.max(fastest, car.velocity());
        }
        assertEquals(4, car.y(), 1e-9);
        assertEquals(0.1, fastest, 1e-9);
        assertEquals(0, car.velocity(), 1e-9);
    }
}
