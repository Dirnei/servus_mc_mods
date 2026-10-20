package io.github.dirnei.mc.servus.delicacies.firebox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class FuelFireTest {

    /** A fuel slot holding items with the given burn times. */
    private static final class Fuel implements FuelSupply {
        private final Deque<Integer> items;

        Fuel(Integer... burnTimes) {
            items = new ArrayDeque<>(List.of(burnTimes));
        }

        @Override
        public int burnNext() {
            return items.isEmpty() ? 0 : items.poll();
        }

        int left() {
            return items.size();
        }
    }

    @Test
    void staysColdWithoutDemandAndKeepsItsFuel() {
        Fuel fuel = new Fuel(100);
        FuelFire fire = new FuelFire(fuel);
        for (int i = 0; i < 50; i++) {
            fire.tick(false);
        }
        assertFalse(fire.hot());
        assertEquals(1, fuel.left());
    }

    @Test
    void lightsOnDemandAndIsHotAtOnce() {
        Fuel fuel = new Fuel(100);
        FuelFire fire = new FuelFire(fuel);
        fire.tick(true);
        assertTrue(fire.hot());
        assertEquals(1.0F, fire.heat());
        assertEquals(0, fuel.left());
    }

    @Test
    void burnsTheFullBurnTime() {
        FuelFire fire = new FuelFire(new Fuel(5));
        fire.tick(true);
        int hotTicks = 1;
        for (int i = 0; i < 10; i++) {
            fire.tick(false);
            if (fire.hot()) {
                hotTicks++;
            }
        }
        assertEquals(5, hotTicks);
    }

    @Test
    void burnsOutWhenTheDemandStops() {
        Fuel fuel = new Fuel(3, 3);
        FuelFire fire = new FuelFire(fuel);
        fire.tick(true);
        fire.tick(false);
        fire.tick(false);
        assertTrue(fire.hot(), "a lit item burns on without demand");
        fire.tick(false);
        assertFalse(fire.hot());
        assertEquals(1, fuel.left(), "no new item without demand");
    }

    @Test
    void relightsFromTheNextItemWhileDemanded() {
        Fuel fuel = new Fuel(2, 2);
        FuelFire fire = new FuelFire(fuel);
        for (int i = 0; i < 4; i++) {
            fire.tick(true);
            assertTrue(fire.hot(), "tick " + i);
        }
        assertEquals(0, fuel.left());
    }

    @Test
    void goesColdWithoutFuel() {
        FuelFire fire = new FuelFire(new Fuel(1));
        fire.tick(true);
        fire.tick(true);
        assertFalse(fire.hot());
        assertEquals(0.0F, fire.heat());
    }

    @Test
    void restoresASavedFire() {
        FuelFire fire = new FuelFire(new Fuel());
        fire.restore(2, 1600);
        assertTrue(fire.hot());
        assertEquals(1600, fire.burnTotal());
        fire.tick(false);
        fire.tick(false);
        assertFalse(fire.hot());
    }
}
