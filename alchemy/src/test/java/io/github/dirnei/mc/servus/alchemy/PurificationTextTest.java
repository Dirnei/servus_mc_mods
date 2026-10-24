package io.github.dirnei.mc.servus.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PurificationTextTest {

    @Test
    void saltAndResidue() {
        assertEquals("8 Sugar, 500 mB water: 100 Salt, 1 Kandis", PurificationText.line(8, "Sugar", 500, 100, 1, "Kandis"));
    }

    @Test
    void washNamesOnlyTheResidue() {
        assertEquals("8 Red Concrete Powder, 250 mB water: 8 Red Concrete",
                PurificationText.line(8, "Red Concrete Powder", 250, 0, 8, "Red Concrete"));
    }

    @Test
    void saltWithoutResidue() {
        assertEquals("4 Dried Kelp, 250 mB water: 200 Salt", PurificationText.line(4, "Dried Kelp", 250, 200, 0, ""));
    }

    @Test
    void neitherSaysNothing() {
        assertEquals("1 Stone, 100 mB water: nothing", PurificationText.line(1, "Stone", 100, 0, 0, ""));
    }

    @Test
    void kindLineStandsForEveryColour() {
        assertEquals("8 Concrete Powder of any colour, 250 mB water: its Concrete",
                PurificationText.kindLine(8, "Concrete Powder of any colour", 250, "its Concrete"));
    }
}
