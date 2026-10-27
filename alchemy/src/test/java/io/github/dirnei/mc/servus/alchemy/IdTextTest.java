package io.github.dirnei.mc.servus.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IdTextTest {

    @Test
    void oneWordIsCapitalised() {
        assertEquals("Kandis", IdText.titleCase("kandis"));
    }

    @Test
    void wordsAreSplitAtUnderscores() {
        assertEquals("Caput Mortuum", IdText.titleCase("caput_mortuum"));
    }

    @Test
    void emptyPartsAreSkipped() {
        assertEquals("Salt Crystal", IdText.titleCase("_salt__crystal_"));
    }

    @Test
    void emptyPathGivesEmptyText() {
        assertEquals("", IdText.titleCase(""));
    }
}
