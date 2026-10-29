package io.github.dirnei.mc.servus.shuttles.rail;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RailFacingTest {

    @Test
    void clickedRailWins() {
        assertEquals(Optional.of(Side.WEST), RailFacing.railSide(Optional.of(Side.WEST), Set.of(Side.WEST, Side.NORTH), Side.NORTH, false));
    }

    @Test
    void railInLookDirectionWins() {
        assertEquals(Optional.of(Side.NORTH), RailFacing.railSide(Optional.empty(), Set.of(Side.WEST, Side.NORTH), Side.NORTH, false));
    }

    @Test
    void onlyRailBesideIsUsed() {
        assertEquals(Optional.of(Side.WEST), RailFacing.railSide(Optional.empty(), Set.of(Side.WEST), Side.NORTH, false));
    }

    @Test
    void severalRailsLeaveItToThePlayer() {
        assertEquals(Optional.empty(), RailFacing.railSide(Optional.empty(), Set.of(Side.WEST, Side.EAST), Side.NORTH, false));
    }

    @Test
    void noRailLeavesItToThePlayer() {
        assertEquals(Optional.empty(), RailFacing.railSide(Optional.empty(), Set.of(), Side.SOUTH, false));
    }

    @Test
    void sneakingIgnoresAClickedRail() {
        assertEquals(Optional.empty(), RailFacing.railSide(Optional.of(Side.WEST), Set.of(Side.WEST), Side.NORTH, true));
    }

    @Test
    void sneakingIgnoresTheOnlyRailBeside() {
        assertEquals(Optional.empty(), RailFacing.railSide(Optional.empty(), Set.of(Side.WEST), Side.NORTH, true));
    }

    @Test
    void sneakingWithoutRailFacesThePlayer() {
        assertEquals(Optional.empty(), RailFacing.railSide(Optional.empty(), Set.of(), Side.NORTH, true));
    }
}
