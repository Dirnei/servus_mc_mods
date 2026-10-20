package io.github.dirnei.mc.servus.delicacies.firebox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GaugeSyncTest {

    private static final float STEP = 1.0F / GaugeSync.STEPS;

    @Test
    void firstReadingGoesOut() {
        assertTrue(new GaugeSync().changed(0, false, 0));
    }

    @Test
    void sameStepStaysQuiet() {
        GaugeSync sync = new GaugeSync();
        sync.changed(STEP * 2.1F, false, 0.01F);
        assertFalse(sync.changed(STEP * 2.5F, false, 0.01F));
        assertFalse(sync.changed(STEP * 2.9F, false, 0.02F));
    }

    @Test
    void newStepGoesOut() {
        GaugeSync sync = new GaugeSync();
        sync.changed(STEP * 2.9F, false, 0.01F);
        assertTrue(sync.changed(STEP * 3.05F, false, 0.01F));
    }

    @Test
    void thresholdGoesOut() {
        GaugeSync sync = new GaugeSync();
        sync.changed(0.59F, false, 0.01F);
        assertTrue(sync.changed(0.6F, true, 0.01F));
    }

    @Test
    void turnFromHeatingToCoolingGoesOut() {
        GaugeSync sync = new GaugeSync();
        sync.changed(1, true, 0);
        assertFalse(sync.changed(1, true, 0));
        assertTrue(sync.changed(1, true, -0.001F));
        assertTrue(sync.changed(1, true, 0.01F));
    }

    @Test
    void resetSendsTheNextReading() {
        GaugeSync sync = new GaugeSync();
        sync.changed(0.5F, false, 0);
        sync.reset();
        assertTrue(sync.changed(0.5F, false, 0));
    }
}
