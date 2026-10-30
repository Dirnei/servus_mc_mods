package io.github.dirnei.mc.servus.alchemy.venter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PutrefactionTextTest {

    @Test
    void line() {
        assertEquals("8 rot: 1 Humus, 60 s in fresh straw, 240 s cold", PutrefactionText.line(8, "rot", 1, "Humus", 60, 240));
    }

    @Test
    void mixLine() {
        assertEquals("4 Sand, 1 Clay Ball, 1 Humus, 250 mB water: 8 Dirt",
                PutrefactionText.mixLine(List.of("4 Sand", "1 Clay Ball", "1 Humus"), 250, 8, "Dirt"));
    }

    @Test
    void mixLineWithoutWater() {
        assertEquals("2 Dirt, 1 Gravel: 2 Coarse Dirt", PutrefactionText.mixLine(List.of("2 Dirt", "1 Gravel"), 0, 2, "Coarse Dirt"));
    }
}
