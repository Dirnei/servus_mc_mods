package io.github.dirnei.mc.servus.storage.rail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShaftFootingTest {

    @Test
    void standsWhenAbove() {
        assertTrue(ShaftFooting.stands(true, false));
    }

    @Test
    void descendingPasses() {
        assertFalse(ShaftFooting.stands(true, true));
    }

    @Test
    void insidePasses() {
        assertFalse(ShaftFooting.stands(false, false));
        assertFalse(ShaftFooting.stands(false, true));
    }
}
