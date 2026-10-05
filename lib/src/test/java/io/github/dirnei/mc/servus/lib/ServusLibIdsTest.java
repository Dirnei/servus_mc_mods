package io.github.dirnei.mc.servus.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServusLibIdsTest {

    @Test
    void modIdMatchesGradleProject() {
        assertEquals("servus_lib", ServusLibIds.MOD_ID);
    }
}
