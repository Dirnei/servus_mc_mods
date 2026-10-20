package io.github.dirnei.mc.servus.shuttles.garage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GarageFacingTest {

    @Test
    void clickedRailWins() {
        assertEquals(Side.WEST, GarageFacing.doorway(Optional.of(Side.WEST), Set.of(Side.WEST, Side.NORTH), Side.NORTH));
    }

    @Test
    void railInLookDirectionWins() {
        assertEquals(Side.NORTH, GarageFacing.doorway(Optional.empty(), Set.of(Side.WEST, Side.NORTH), Side.NORTH));
    }

    @Test
    void onlyRailBesideIsUsed() {
        assertEquals(Side.WEST, GarageFacing.doorway(Optional.empty(), Set.of(Side.WEST), Side.NORTH));
    }

    @Test
    void severalRailsFallBackToLook() {
        assertEquals(Side.NORTH, GarageFacing.doorway(Optional.empty(), Set.of(Side.WEST, Side.EAST), Side.NORTH));
    }

    @Test
    void noRailFallsBackToLook() {
        assertEquals(Side.SOUTH, GarageFacing.doorway(Optional.empty(), Set.of(), Side.SOUTH));
    }
}
