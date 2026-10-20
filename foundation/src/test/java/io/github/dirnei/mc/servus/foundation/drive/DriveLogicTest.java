package io.github.dirnei.mc.servus.foundation.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DriveLogicTest {

    @Nested
    class WorkProgressTest {

        @Test
        void reachesTheGoal() {
            WorkProgress progress = new WorkProgress();
            assertFalse(progress.add(150, 200));
            assertEquals(150, progress.value());
            assertTrue(progress.add(50, 200));
            assertEquals(0, progress.value());
        }

        @Test
        void dropsTheRest() {
            WorkProgress progress = new WorkProgress();
            progress.add(190, 200);
            assertTrue(progress.add(60, 200));
            assertEquals(0, progress.value(), "nothing carried over to the next item");
        }

        @Test
        void negativeWorkCountsAsNone() {
            WorkProgress progress = new WorkProgress();
            progress.add(10, 200);
            assertFalse(progress.add(-5, 200));
            assertEquals(10, progress.value());
        }

        @Test
        void resetAndRestore() {
            WorkProgress progress = new WorkProgress();
            progress.add(10, 200);
            progress.reset();
            assertEquals(0, progress.value());
            progress.set(42);
            assertEquals(42, progress.value());
        }
    }

    @Nested
    class MotorRateTest {

        @Test
        void baseRate() {
            assertEquals(new MotorRate(5, 20), MotorRate.of(5, 20, 1));
        }

        @Test
        void speedUpgradeDoublesBoth() {
            assertEquals(new MotorRate(10, 40), MotorRate.of(5, 20, 2));
        }

        @Test
        void energyPerWorkStaysTheSame() {
            for (double multiplier : new double[] {1, 2, 4, 8}) {
                MotorRate rate = MotorRate.of(5, 20, multiplier);
                assertEquals(4.0, (double) rate.energyPerTick() / rate.workPerTick(), 1e-9, "multiplier " + multiplier);
            }
        }

        @Test
        void alwaysAtLeastOneWork() {
            assertEquals(1, MotorRate.of(1, 4, 0.1).workPerTick());
        }
    }

    @Nested
    class CrankTurnsTest {

        @Test
        void sharesAddUpToTheTurnsWork() {
            CrankTurns turns = new CrankTurns();
            int work = 0;
            for (int i = 0; i < CrankTurns.TICKS_PER_TURN; i++) {
                CrankTurns.Step step = turns.step(25);
                assertTrue(step.work() == 1 || step.work() == 2, "work spread evenly over the turn");
                work += step.work();
            }
            assertEquals(25, work);
        }

        @Test
        void turnDoneEverySixteenSteps() {
            CrankTurns turns = new CrankTurns();
            int done = 0;
            for (int i = 1; i <= 3 * CrankTurns.TICKS_PER_TURN; i++) {
                CrankTurns.Step step = turns.step(25);
                assertEquals(i % CrankTurns.TICKS_PER_TURN == 0, step.turnDone(), "tick " + i);
                done += step.turnDone() ? 1 : 0;
            }
            assertEquals(3, done);
            assertEquals(0, turns.ticks());
        }

        @Test
        void aClickDrivesUntilTheNextIsDueAndHoldsLonger() {
            CrankTurns turns = new CrankTurns();
            assertFalse(turns.held(100), "never clicked");
            turns.click(100);
            assertTrue(turns.driving(100) && turns.driving(104), "until the next click is due");
            assertFalse(turns.driving(105));
            assertTrue(turns.held(108));
            assertFalse(turns.held(109));
        }

        @Test
        void heldClicksDriveEveryTick() {
            CrankTurns turns = new CrankTurns();
            for (long now = 0; now < 40; now++) {
                if (now % CrankTurns.TICKS_PER_CLICK == 0) {
                    turns.click(now);
                }
                assertTrue(turns.driving(now), "tick " + now);
            }
        }
    }

    @Nested
    class FlywheelTest {

        @Test
        void spinsUpThenTurnsAtConstantSpeed() {
            Flywheel wheel = new Flywheel(10, 4, 20);
            for (int i = 0; i < 4; i++) {
                wheel.tick(true);
            }
            assertEquals(1, wheel.speed(), 1e-6);
            float before = wheel.angle(1);
            wheel.tick(true);
            assertEquals(10, wheel.angle(1) - before, 1e-4);
            wheel.tick(true);
            assertEquals(10, wheel.angle(1) - wheel.angle(0), 1e-4);
        }

        @Test
        void interpolatesBetweenTicks() {
            Flywheel wheel = new Flywheel(10, 1, 20);
            wheel.tick(true);
            wheel.tick(true);
            assertEquals(10, wheel.angle(0), 1e-4);
            assertEquals(15, wheel.angle(0.5F), 1e-4);
            assertEquals(20, wheel.angle(1), 1e-4);
        }

        @Test
        void coastsToAStopWithinTheCoastTicks() {
            Flywheel wheel = new Flywheel(10, 1, 20);
            wheel.tick(true);
            float last = wheel.speed();
            for (int i = 0; i < 19; i++) {
                wheel.tick(false);
                assertTrue(wheel.speed() < last && wheel.turning(), "slows down but still turns, tick " + i);
                last = wheel.speed();
            }
            wheel.tick(false);
            assertFalse(wheel.turning());
        }

        @Test
        void standsWhenStopped() {
            Flywheel wheel = new Flywheel(10, 1, 1);
            wheel.tick(true);
            wheel.tick(false);
            float angle = wheel.angle(1);
            wheel.tick(false);
            assertEquals(angle, wheel.angle(0), 1e-6);
            assertEquals(angle, wheel.angle(0.7F), 1e-6);
        }

        @Test
        void wrapsAroundWithoutAJump() {
            Flywheel wheel = new Flywheel(90, 1, 20);
            for (int i = 0; i < 4; i++) {
                wheel.tick(true);
            }
            assertEquals(0, wheel.angle(1), 1e-4);
            assertEquals(-45, wheel.angle(0.5F), 1e-4);
        }
    }
}
