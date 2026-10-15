package io.github.dirnei.mc.servus.shuttles.shuttle.module.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HandBuildTest {

    @Test
    void acceptsOnlyTheCurrentStep() {
        assertEquals(1, HandBuild.next(0, 0, 4));
        assertEquals(0, HandBuild.next(0, 2, 4), "a step out of order is ignored");
        assertEquals(2, HandBuild.next(2, 1, 4), "an old step again is ignored");
    }

    @Test
    void stopsAtTheEnd() {
        assertEquals(4, HandBuild.next(3, 3, 4));
        assertEquals(4, HandBuild.next(4, 4, 4), "nothing after the last step");
        assertTrue(HandBuild.done(4, 4));
        assertFalse(HandBuild.done(3, 4));
    }

    @Test
    void savedStepsClamp() {
        assertEquals(2, HandBuild.clamp(7, 2));
        assertEquals(0, HandBuild.clamp(-1, 3));
        assertEquals(1, HandBuild.clamp(1, 3));
    }

    @Test
    void seedsDifferPerStep() {
        assertNotEquals(HandBuild.seed(42, 0), HandBuild.seed(42, 1));
        assertEquals(HandBuild.seed(42, 1), HandBuild.seed(42, 1));
    }
}
