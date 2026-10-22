package io.github.dirnei.mc.servus.foundation.machine.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SinkStonesTest {

    @Test
    void eightEqualStonesGiveOne() {
        assertEquals(List.of("bricks"), SinkStones.distinct(Collections.nCopies(8, "bricks")));
    }

    @Test
    void sixAndTwoGiveBothInFirstSeenOrder() {
        List<String> stones = List.of("deepslate_bricks", "stone_bricks", "stone_bricks", "deepslate_bricks",
                "stone_bricks", "stone_bricks", "stone_bricks", "stone_bricks");
        assertEquals(List.of("deepslate_bricks", "stone_bricks"), SinkStones.distinct(stones));
    }

    @Test
    void threeDifferentKeepTheirOrder() {
        assertEquals(List.of("tuff", "bricks", "granite"), SinkStones.distinct(List.of("tuff", "bricks", "tuff", "granite", "bricks")));
    }

    @Test
    void emptyGivesNone() {
        assertTrue(SinkStones.distinct(List.of()).isEmpty());
    }
}
