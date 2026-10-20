package io.github.dirnei.mc.servus.delicacies.firebox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElectricHeatTest {

    /** 10 FE per tick, 10 ticks to full heat, 40 ticks to cold, hot from half heat. */
    static final HeatingStats STATS = new HeatingStats(10, 1000, 10, 40, 0.5F);

    /** An FE buffer that counts what was drawn. */
    static final class Buffer implements EnergyDraw {
        int stored;
        int drawn;

        Buffer(int stored) {
            this.stored = stored;
        }

        @Override
        public boolean draw(int amount) {
            if (stored < amount) {
                return false;
            }
            stored -= amount;
            drawn += amount;
            return true;
        }
    }

    private static void ticks(HeatSource heat, int count, boolean demanded) {
        for (int i = 0; i < count; i++) {
            heat.tick(demanded);
        }
    }

    @Test
    void startsCold() {
        ElectricHeat heat = new ElectricHeat(new Buffer(1000), () -> STATS);
        assertEquals(0, heat.heat());
        assertFalse(heat.hot());
        assertEquals(0, heat.rate());
    }

    @Test
    void risesOnlyWhileDemandedAndPowered() {
        Buffer buffer = new Buffer(1000);
        ElectricHeat heat = new ElectricHeat(buffer, () -> STATS);
        ticks(heat, 5, false);
        assertEquals(0, heat.heat());
        assertEquals(0, buffer.drawn);
        ticks(heat, 3, true);
        assertEquals(0.3F, heat.heat(), 1e-5);
        assertEquals(30, buffer.drawn);
        assertTrue(heat.powered());
    }

    @Test
    void hotOnlyFromTheThreshold() {
        ElectricHeat heat = new ElectricHeat(new Buffer(1000), () -> STATS);
        ticks(heat, 4, true);
        assertFalse(heat.hot());
        ticks(heat, 2, true);
        assertTrue(heat.hot());
    }

    @Test
    void fullAfterTheHeatUpTimeAndStaysThere() {
        Buffer buffer = new Buffer(1000);
        ElectricHeat heat = new ElectricHeat(buffer, () -> STATS);
        ticks(heat, 10, true);
        assertEquals(1, heat.heat(), 1e-5);
        ticks(heat, 5, true);
        assertEquals(1, heat.heat());
        assertEquals(0, heat.rate());
        assertEquals(150, buffer.drawn);
    }

    @Test
    void coolsSlowlyWithoutPower() {
        Buffer buffer = new Buffer(100);
        ElectricHeat heat = new ElectricHeat(buffer, () -> STATS);
        ticks(heat, 10, true);
        assertEquals(0, buffer.stored);
        heat.tick(true);
        assertFalse(heat.powered());
        assertEquals(1 - 1 / 40F, heat.heat(), 1e-5);
        assertEquals(-1 / 40F, heat.rate(), 1e-6);
        ticks(heat, 18, true);
        assertTrue(heat.hot(), "still above the threshold after 19 ticks without power");
        ticks(heat, 2, true);
        assertFalse(heat.hot());
    }

    @Test
    void coolsWithoutDemandAndDrawsNothing() {
        Buffer buffer = new Buffer(1000);
        ElectricHeat heat = new ElectricHeat(buffer, () -> STATS);
        heat.restore(1);
        ticks(heat, 10, false);
        assertEquals(0.75F, heat.heat(), 1e-5);
        assertEquals(0, buffer.drawn);
    }

    @Test
    void neverBelowColdOrAboveFull() {
        ElectricHeat heat = new ElectricHeat(new Buffer(100_000), () -> STATS);
        ticks(heat, 100, false);
        assertEquals(0, heat.heat());
        assertEquals(0, heat.rate());
        ticks(heat, 100, true);
        assertEquals(1, heat.heat());
    }

    @Test
    void ratePerState() {
        ElectricHeat heat = new ElectricHeat(new Buffer(1000), () -> STATS);
        heat.tick(true);
        assertEquals(0.1F, heat.rate(), 1e-6);
        heat.tick(false);
        assertEquals(-1 / 40F, heat.rate(), 1e-6);
    }

    @Test
    void restoreClampsAndForgetsPower() {
        ElectricHeat heat = new ElectricHeat(new Buffer(1000), () -> STATS);
        heat.tick(true);
        heat.restore(3);
        assertEquals(1, heat.heat());
        assertFalse(heat.powered());
        heat.restore(-1);
        assertEquals(0, heat.heat());
    }

    @Test
    void statsChangeAtOnce() {
        HeatingStats[] stats = {STATS};
        Buffer buffer = new Buffer(1000);
        ElectricHeat heat = new ElectricHeat(buffer, () -> stats[0]);
        heat.tick(true);
        stats[0] = new HeatingStats(50, 1000, 2, 40, 0.5F);
        heat.tick(true);
        assertEquals(60, buffer.drawn);
        assertEquals(0.6F, heat.heat(), 1e-5);
    }
}
