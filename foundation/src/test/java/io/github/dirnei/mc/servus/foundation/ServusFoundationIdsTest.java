package io.github.dirnei.mc.servus.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServusFoundationIdsTest {

    @Test
    void modIdMatchesGradleProject() {
        assertEquals("servus_foundation", ServusFoundationIds.MOD_ID);
    }
}
