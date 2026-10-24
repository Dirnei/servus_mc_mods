package io.github.dirnei.mc.servus.alchemy.coagulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SeedTextTest {

    @Test
    void seedGrowsResult() {
        assertEquals("Kandis grows Salt Crystal", SeedText.line("Kandis", "Salt Crystal"));
    }
}
