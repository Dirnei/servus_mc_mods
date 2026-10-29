package io.github.dirnei.mc.servus.delicacies.aquarium;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AquariumPowerTest {

    @Test
    void powerCostOfAFourByTwoByTwoAquarium() {
        assertEquals(40, AquariumPower.cost(16, 8, AquariumStats.DEFAULTS));
        assertEquals(160, AquariumPower.buffer(40, AquariumStats.DEFAULTS));
    }

    @Test
    void poweredShare() {
        assertEquals(0.5, AquariumPower.share(600, 1200), 1e-9);
        assertEquals(0, AquariumPower.share(0, 0), 1e-9);
    }
}
