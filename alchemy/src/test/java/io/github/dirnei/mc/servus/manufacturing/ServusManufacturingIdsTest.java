package io.github.dirnei.mc.servus.manufacturing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServusManufacturingIdsTest {

    @Test
    void modIdMatchesGradleProject() {
        assertEquals("servus_manufacturing", ServusManufacturingIds.MOD_ID);
    }
}
