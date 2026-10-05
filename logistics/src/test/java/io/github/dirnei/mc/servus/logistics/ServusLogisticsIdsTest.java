package io.github.dirnei.mc.servus.logistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServusLogisticsIdsTest {

    @Test
    void modIdMatchesGradleProject() {
        assertEquals("servus_logistics", ServusLogisticsIds.MOD_ID);
    }
}
