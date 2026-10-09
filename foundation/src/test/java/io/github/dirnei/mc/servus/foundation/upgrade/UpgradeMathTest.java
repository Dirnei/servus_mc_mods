package io.github.dirnei.mc.servus.foundation.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpgradeMathTest {

    @Test
    void twoDoublesGiveFour() {
        assertEquals(4.0, UpgradeMath.multiplier(List.of(2.0, 2.0)), 1e-9);
        assertEquals(8.0 * 4.0, 8.0 * UpgradeMath.multiplier(List.of(2.0, 2.0)), 1e-9);
        assertEquals(8.0, UpgradeMath.multiplier(List.of(2.0, 4.0)), 1e-9);
    }

    @Test
    void noUpgradesIsOne() {
        assertEquals(1.0, UpgradeMath.multiplier(List.of()), 1e-9);
    }
}
