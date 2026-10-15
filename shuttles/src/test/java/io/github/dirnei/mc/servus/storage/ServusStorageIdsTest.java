package io.github.dirnei.mc.servus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServusStorageIdsTest {

    @Test
    void modIdMatchesGradleProject() {
        assertEquals("servus_storage", ServusStorageIds.MOD_ID);
    }
}
