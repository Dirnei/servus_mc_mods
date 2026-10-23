package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShuttleLabelFormTest {

    @Test
    void numberAndPlate() {
        assertEquals(ShuttleLabelForm.NUMBERED_NAMED, ShuttleLabelForm.of(true, true));
    }

    @Test
    void numberOnly() {
        assertEquals(ShuttleLabelForm.NUMBERED, ShuttleLabelForm.of(false, true));
    }

    @Test
    void plateOnly() {
        assertEquals(ShuttleLabelForm.NAMED, ShuttleLabelForm.of(true, false));
    }

    @Test
    void neither() {
        assertEquals(ShuttleLabelForm.PLAIN, ShuttleLabelForm.of(false, false));
    }
}
