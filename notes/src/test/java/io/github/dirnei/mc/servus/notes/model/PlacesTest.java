package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlacesTest {

    @Test
    void vanillaDimensionsHaveShortNames() {
        assertEquals("Overworld", Places.label("minecraft:overworld"));
        assertEquals("Nether", Places.label("minecraft:the_nether"));
        assertEquals("End", Places.label("minecraft:the_end"));
    }

    @Test
    void otherDimensionsShowTheirPathInTitleCase() {
        assertEquals("Mining Dimension", Places.label("allthemodium:mining_dimension"));
        assertEquals("Deep Dark", Places.label("deep_dark"));
        assertEquals("Forest Level", Places.label("twilight:forest/level"));
    }

    @Test
    void chipNamesCoordinatesAndDimension() {
        assertEquals("X -120 Y 64 Z 340 Nether", Places.chip(new Spot(-120, 64, 340, "minecraft:the_nether")));
        assertEquals("Portal room · X 120 Y 64 Z -30 Nether", Places.chip(new Spot(120, 64, -30, "minecraft:the_nether", "Portal room")));
    }
}
