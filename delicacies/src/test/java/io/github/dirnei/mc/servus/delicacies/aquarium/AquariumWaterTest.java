package io.github.dirnei.mc.servus.delicacies.aquarium;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AquariumWaterTest {

    @Test
    void drainingATenthTakesATenthOfTheDirt() {
        assertEquals(20, AquariumWater.dirtTaken(200, 10_000, 100_000), 1e-9);
    }

    @Test
    void drainingEverythingTakesAllDirt() {
        assertEquals(200, AquariumWater.dirtTaken(200, 100_000, 100_000), 1e-9);
        assertEquals(200, AquariumWater.dirtTaken(200, 5_000, 0), 1e-9);
    }

    @Test
    void drainingNothingTakesNoDirt() {
        assertEquals(0, AquariumWater.dirtTaken(200, 0, 100_000), 1e-9);
    }
}
