package io.github.dirnei.mc.servus.delicacies.firebox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FireboxHeatTest {

    /** A fuel slot with {@code items} items of 100 ticks each. */
    private static final class Fuel implements FuelSupply {
        int items;

        Fuel(int items) {
            this.items = items;
        }

        @Override
        public int burnNext() {
            if (items == 0) {
                return 0;
            }
            items--;
            return 100;
        }
    }

    private static FireboxHeat firebox(Fuel fuel, ElectricHeatTest.Buffer buffer) {
        return new FireboxHeat(fuel, buffer, () -> ElectricHeatTest.STATS);
    }

    @Test
    void withoutElementTheFireBurnsFuel() {
        Fuel fuel = new Fuel(2);
        FireboxHeat heat = firebox(fuel, new ElectricHeatTest.Buffer(1000));
        heat.tick(true);
        assertTrue(heat.hot());
        assertEquals(1, heat.heat());
        assertEquals(1, fuel.items);
        assertEquals(0, heat.rate());
        assertFalse(heat.hasElement());
    }

    @Test
    void withElementNoFuelIsAskedAndFeIsDrawn() {
        Fuel fuel = new Fuel(2);
        ElectricHeatTest.Buffer buffer = new ElectricHeatTest.Buffer(1000);
        FireboxHeat heat = firebox(fuel, buffer);
        assertTrue(heat.installElement());
        for (int i = 0; i < 4; i++) {
            heat.tick(true);
        }
        assertEquals(2, fuel.items);
        assertEquals(40, buffer.drawn);
        assertFalse(heat.hot(), "still preheating");
        heat.tick(true);
        heat.tick(true);
        assertTrue(heat.hot());
        assertEquals(0.1F, heat.rate(), 1e-6);
    }

    @Test
    void installingPutsTheFireOut() {
        Fuel fuel = new Fuel(1);
        FireboxHeat heat = firebox(fuel, new ElectricHeatTest.Buffer(0));
        heat.tick(true);
        assertTrue(heat.hot());
        heat.installElement();
        assertFalse(heat.hot());
        assertEquals(0, heat.fire().burnLeft());
        heat.removeElement();
        assertFalse(heat.hot(), "the burnt item is gone");
    }

    @Test
    void secondInstallAndEmptyRemoveAreRefused() {
        FireboxHeat heat = firebox(new Fuel(0), new ElectricHeatTest.Buffer(0));
        assertFalse(heat.removeElement());
        assertTrue(heat.installElement());
        assertFalse(heat.installElement());
        assertTrue(heat.removeElement());
        assertFalse(heat.hasElement());
    }

    @Test
    void removingDropsTheHeatAndTheFireTakesOver() {
        Fuel fuel = new Fuel(1);
        FireboxHeat heat = firebox(fuel, new ElectricHeatTest.Buffer(1000));
        heat.installElement();
        for (int i = 0; i < 10; i++) {
            heat.tick(true);
        }
        assertTrue(heat.hot());
        heat.removeElement();
        assertEquals(0, heat.electric().heat());
        assertFalse(heat.hot());
        heat.tick(true);
        assertTrue(heat.hot());
        assertEquals(0, fuel.items);
    }

    @Test
    void restoreKeepsTheHeatOnlyWithElement() {
        FireboxHeat heat = firebox(new Fuel(0), new ElectricHeatTest.Buffer(0));
        heat.restore(true, 0, 0, 0.7F);
        assertTrue(heat.hasElement());
        assertEquals(0.7F, heat.heat(), 1e-6);
        heat.restore(false, 30, 100, 0.7F);
        assertFalse(heat.hasElement());
        assertEquals(0, heat.electric().heat());
        assertEquals(30, heat.fire().burnLeft());
        assertTrue(heat.hot());
    }
}
