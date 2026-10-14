package io.github.dirnei.mc.servus.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServusAlchemyIdsTest {

    @Test
    void modIdMatchesGradleProject() {
        assertEquals("servus_alchemy", ServusAlchemyIds.MOD_ID);
    }
}
