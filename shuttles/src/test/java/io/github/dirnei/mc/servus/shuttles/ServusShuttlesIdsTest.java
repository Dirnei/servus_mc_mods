package io.github.dirnei.mc.servus.shuttles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServusShuttlesIdsTest {

    @Test
    void modIdMatchesGradleProject() {
        assertEquals("servus_shuttles", ServusShuttlesIds.MOD_ID);
    }
}
