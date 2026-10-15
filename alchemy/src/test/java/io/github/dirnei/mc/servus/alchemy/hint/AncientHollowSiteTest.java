package io.github.dirnei.mc.servus.alchemy.hint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowSite.Probe;
import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowSite.Site;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AncientHollowSiteTest {

    /** A two high tunnel along x behind the broken block at the origin, dug from the west. */
    private static final Probe TUNNEL = (x, y, z) -> z == 0 && (y == 0 || y == 1) && x >= -10 && x <= -1;
    private static final Probe ROCK = (x, y, z) -> !TUNNEL.test(x, y, z);

    @Test
    void picksTheTunnelWall() {
        Optional<Site> site = AncientHollowSite.choose(0, ROCK, TUNNEL);
        assertEquals(Optional.of(new Site(-3, 0, -1, 2)), site);
    }

    @Test
    void startDirectionChangesTheChoice() {
        Probe air = (x, y, z) -> (x == 0 && z == -4) || (x == 4 && z == 0);
        Probe everywhere = (x, y, z) -> true;
        assertEquals(Optional.of(new Site(0, 0, -3, 0)), AncientHollowSite.choose(0, everywhere, air));
        assertEquals(Optional.of(new Site(3, 0, 0, 1)), AncientHollowSite.choose(1, everywhere, air));
    }

    @Test
    void noneWithoutAir() {
        assertTrue(AncientHollowSite.choose(2, (x, y, z) -> true, (x, y, z) -> false).isEmpty());
    }

    @Test
    void skipsTheBrokenBlockItself() {
        for (int start = 0; start < AncientHollowSite.SIDES.length; start++) {
            for (int[] offset : AncientHollowSite.candidates(start)) {
                assertFalse(offset[0] == 0 && offset[1] == 0 && offset[2] == 0, "the broken block is never a candidate");
            }
        }
    }

    @Test
    void sideFacesTheAir() {
        Probe air = (x, y, z) -> x == 4 && y == 0 && z == 1;
        Optional<Site> site = AncientHollowSite.choose(1, (x, y, z) -> true, air);
        assertEquals(Optional.of(new Site(3, 0, 1, 1)), site);
        int[] side = AncientHollowSite.SIDES[site.orElseThrow().side()];
        assertTrue(air.test(3 + side[0], 0, 1 + side[1]), "the open face looks into the air");
    }
}
