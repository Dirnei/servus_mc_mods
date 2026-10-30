package io.github.dirnei.mc.servus.shuttles.fluidstation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FluidKeepTest {

    @Test
    void amountClampedToZeroAndCapacity() {
        assertEquals(0, new FluidKeep<>("water", -1000, 0).clamp(8000).amount());
        assertEquals(8000, new FluidKeep<>("water", 18000, 0).clamp(8000).amount());
        assertEquals(5000, new FluidKeep<>("water", 5000, 0).clamp(8000).amount());
        assertEquals(new FluidKeep<>("water", 4000, 4000), new FluidKeep<>("water", 18000, 9000).clamp(4000));
    }

    @Test
    void noKeepAcceptsNothing() {
        FluidKeep<String> keep = FluidKeep.none();
        assertFalse(keep.isSet());
        assertFalse(keep.accepts("water"));
        assertEquals(0, new FluidKeep<String>(null, 4000, 1000).target());
        assertEquals(0, new FluidKeep<String>(null, 8000, 3000).restockLevel());
    }

    @Test
    void keepAcceptsOnlyItsFluid() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000, 1000);
        assertTrue(keep.accepts("water"));
        assertFalse(keep.accepts("lava"));
        assertEquals(8000, keep.target());
    }

    @Test
    void clearingTheFluidClearsTheLevels() {
        assertEquals(FluidKeep.none(), new FluidKeep<>("water", 8000, 2000).withFluid(null, 16000));
    }

    @Test
    void restockPushesKeepUp() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000, 6000).withRestockAt(10000, 16000);
        assertEquals(new FluidKeep<>("water", 10000, 10000), keep);
        assertEquals(new FluidKeep<>("water", 8000, 3000), new FluidKeep<>("water", 8000, 6000).withRestockAt(3000, 16000));
    }

    @Test
    void keepDragsRestockDown() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000, 6000).withAmount(4000, 16000);
        assertEquals(new FluidKeep<>("water", 4000, 4000), keep);
        assertEquals(new FluidKeep<>("water", 12000, 6000), new FluidKeep<>("water", 8000, 6000).withAmount(12000, 16000));
    }

    @Test
    void newKeepStartsFullAtOneBucket() {
        assertEquals(new FluidKeep<>("water", 16000, 1000), FluidKeep.<String>none().withFluid("water", 16000));
        assertEquals(new FluidKeep<>("water", 500, 500), FluidKeep.started("water", 500));
    }

    @Test
    void changedFluidKeepsLevels() {
        assertEquals(new FluidKeep<>("lava", 8000, 2000), new FluidKeep<>("water", 8000, 2000).withFluid("lava", 16000));
    }

    @Test
    void levelsStayWithinCapacity() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000, 4000);
        assertEquals(new FluidKeep<>("water", 16000, 4000), keep.withAmount(40000, 16000));
        assertEquals(new FluidKeep<>("water", 0, 0), keep.withAmount(-1000, 16000));
        assertEquals(new FluidKeep<>("water", 16000, 16000), keep.withRestockAt(40000, 16000));
        assertEquals(new FluidKeep<>("water", 8000, 0), keep.withRestockAt(-1000, 16000));
    }
}
