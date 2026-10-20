package io.github.dirnei.mc.servus.shuttles.gantry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.gantry.GantryLayout.Part;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryLayout.Run;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryLayout.Vec;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GantryLayoutTest {

    private static final Vec RAIL = new Vec(10, 64, -3);

    @Test
    void fivePartsAroundARailOnZ() {
        Map<Part, Vec> parts = GantryLayout.parts(RAIL, Run.Z);
        assertEquals(5, parts.size());
        assertEquals(new Vec(9, 64, -3), parts.get(Part.LEFT_LOW));
        assertEquals(new Vec(9, 65, -3), parts.get(Part.LEFT_HIGH));
        assertEquals(new Vec(11, 64, -3), parts.get(Part.RIGHT_LOW));
        assertEquals(new Vec(11, 65, -3), parts.get(Part.RIGHT_HIGH));
        assertEquals(new Vec(10, 65, -3), parts.get(Part.BEAM));
    }

    @Test
    void fivePartsAroundARailOnX() {
        Map<Part, Vec> parts = GantryLayout.parts(RAIL, Run.X);
        assertEquals(new Vec(10, 64, -4), parts.get(Part.LEFT_LOW));
        assertEquals(new Vec(10, 65, -4), parts.get(Part.LEFT_HIGH));
        assertEquals(new Vec(10, 64, -2), parts.get(Part.RIGHT_LOW));
        assertEquals(new Vec(10, 65, -2), parts.get(Part.RIGHT_HIGH));
        assertEquals(new Vec(10, 65, -3), parts.get(Part.BEAM));
    }

    @Test
    void everyPartFindsTheMaster() {
        for (Run run : Run.values()) {
            Map<Part, Vec> parts = GantryLayout.parts(RAIL, run);
            for (Map.Entry<Part, Vec> entry : parts.entrySet()) {
                assertEquals(parts.get(Part.LEFT_LOW), GantryLayout.master(entry.getKey(), entry.getValue(), run));
                assertEquals(RAIL, GantryLayout.rail(entry.getKey(), entry.getValue(), run));
            }
        }
    }

    @Test
    void leftIsTheNegativeSide() {
        assertTrue(GantryLayout.parts(RAIL, Run.Z).get(Part.LEFT_LOW).x() < RAIL.x(), "west of a north to south rail");
        assertTrue(GantryLayout.parts(RAIL, Run.X).get(Part.LEFT_LOW).z() < RAIL.z(), "north of an east to west rail");
        assertEquals(0, Part.LEFT_HIGH.tank());
        assertEquals(1, Part.RIGHT_LOW.tank());
        assertEquals(-1, Part.BEAM.tank());
    }
}
