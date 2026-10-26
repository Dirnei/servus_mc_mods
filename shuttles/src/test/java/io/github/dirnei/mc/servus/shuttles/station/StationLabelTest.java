package io.github.dirnei.mc.servus.shuttles.station;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StationLabelTest {

    private static String text(int number, String name) {
        return StationLabel.of(number, name).text("Outbound", "Outbound Station");
    }

    @Test
    void numberedWithoutName() {
        assertEquals(StationLabel.Form.NUMBERED, StationLabel.of(3, "").form());
        assertEquals("Outbound #3", text(3, ""));
    }

    @Test
    void numberedWithName() {
        assertEquals(StationLabel.Form.NUMBERED_NAMED, StationLabel.of(3, "Compost Dock").form());
        assertEquals("Compost Dock (#3)", text(3, "Compost Dock"));
    }

    @Test
    void unnumberedWithoutName() {
        assertEquals(StationLabel.Form.PLAIN, StationLabel.of(0, null).form());
        assertEquals("Outbound Station", text(0, null));
    }

    @Test
    void unnumberedWithName() {
        assertEquals(StationLabel.Form.NAMED, StationLabel.of(0, "Compost Dock").form());
        assertEquals("Compost Dock", text(0, "Compost Dock"));
    }

    @Test
    void nameIsCleaned() {
        assertEquals("CompostDock (#3)", text(3, "  Compost\nDock  "));
    }

    @Test
    void nameThatCleansToEmptyCountsAsNoName() {
        assertEquals(StationLabel.Form.NUMBERED, StationLabel.of(3, " \n\t ").form());
        assertEquals("Outbound #3", text(3, " \n\t "));
    }
}
