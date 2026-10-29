package io.github.dirnei.mc.servus.delicacies.aquarium;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AquariumLookTest {

    @Test
    void halfFullDrawsHalfTheCap() {
        assertEquals(8, AquariumLook.drawnFish(50, 100, 16));
    }

    @Test
    void oneFishDrawsOne() {
        assertEquals(1, AquariumLook.drawnFish(1, 100, 16));
    }

    @Test
    void twoFishDrawTwo() {
        assertEquals(2, AquariumLook.drawnFish(2.5, 100, 16));
    }

    @Test
    void noFishDrawNone() {
        assertEquals(0, AquariumLook.drawnFish(0, 100, 16));
        assertEquals(0, AquariumLook.drawnFish(0.7, 100, 16));
    }

    @Test
    void fullDrawsTheCap() {
        assertEquals(16, AquariumLook.drawnFish(100, 100, 16));
    }

    @Test
    void fishWithoutWaterStillShow() {
        assertEquals(2, AquariumLook.drawnFish(30, 0, 16));
    }

    @Test
    void fullnessPercent() {
        assertEquals(30, AquariumLook.fullnessPercent(24, 80));
        assertEquals(0, AquariumLook.fullnessPercent(5, 0));
    }
}
