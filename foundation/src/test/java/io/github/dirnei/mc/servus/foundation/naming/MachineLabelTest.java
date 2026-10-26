package io.github.dirnei.mc.servus.foundation.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineLabelTest {

    private static String text(int number, String name) {
        return MachineLabel.of(number, name).text("Crafting Station", "Crafting Station");
    }

    @Test
    void numberedWithoutName() {
        assertEquals(MachineLabel.Form.NUMBERED, MachineLabel.of(2, "").form());
        assertEquals("Crafting Station #2", text(2, ""));
    }

    @Test
    void numberedWithNameCarriesTheKind() {
        assertEquals(MachineLabel.Form.NUMBERED_NAMED, MachineLabel.of(2, "Sticks").form());
        assertEquals("Sticks (Crafting Station #2)", text(2, "Sticks"));
    }

    @Test
    void unnumberedWithoutName() {
        assertEquals(MachineLabel.Form.PLAIN, MachineLabel.of(0, null).form());
        assertEquals("Crafting Station", text(0, null));
    }

    @Test
    void unnumberedWithName() {
        assertEquals(MachineLabel.Form.NAMED, MachineLabel.of(0, "Big Forge").form());
        assertEquals("Big Forge", text(0, "Big Forge"));
    }

    @Test
    void negativeNumberCountsAsNone() {
        assertEquals(MachineLabel.Form.PLAIN, MachineLabel.of(-1, "").form());
    }

    @Test
    void nameIsCleaned() {
        assertEquals("WheatNorth (Crafting Station #2)", text(2, "  Wheat\nNorth  "));
    }

    @Test
    void nameThatCleansToEmptyCountsAsNoName() {
        assertEquals(MachineLabel.Form.NUMBERED, MachineLabel.of(3, " \n\t ").form());
        assertEquals("Crafting Station #3", text(3, " \n\t "));
    }
}
