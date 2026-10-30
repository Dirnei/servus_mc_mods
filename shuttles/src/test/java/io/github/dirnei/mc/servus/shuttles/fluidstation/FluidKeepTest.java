package io.github.dirnei.mc.servus.shuttles.fluidstation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FluidKeepTest {

    @Test
    void amountClampedToZeroAndCapacity() {
        assertEquals(0, new FluidKeep<>("water", -1000).clamp(8000).amount());
        assertEquals(8000, new FluidKeep<>("water", 18000).clamp(8000).amount());
        assertEquals(5000, new FluidKeep<>("water", 5000).clamp(8000).amount());
    }

    @Test
    void stepsStayWithinCapacity() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000);
        assertEquals(8000, keep.step(10000, 8000).amount());
        assertEquals(0, keep.step(-10000, 8000).amount());
        assertEquals(7000, keep.step(-1000, 8000).amount());
    }

    @Test
    void noKeepAcceptsNothing() {
        FluidKeep<String> keep = FluidKeep.none();
        assertFalse(keep.isSet());
        assertFalse(keep.accepts("water"));
        assertEquals(0, new FluidKeep<String>(null, 4000).target());
    }

    @Test
    void keepAcceptsOnlyItsFluid() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000);
        assertTrue(keep.accepts("water"));
        assertFalse(keep.accepts("lava"));
        assertEquals(8000, keep.target());
    }

    @Test
    void clearingTheFluidClearsTheAmount() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000);
        assertEquals(FluidKeep.none(), keep.withFluid(null));
        assertEquals(new FluidKeep<>("lava", 8000), keep.withFluid("lava"));
    }

    @Test
    void restockLevelFollowsKeepWhileEqual() {
        FluidKeep<String> keep = new FluidKeep<String>("water", 0).step(8000, 16000);
        assertEquals(8000, keep.restockAt());
        FluidKeep<String> lowered = keep.stepRestock(-4000).step(2000, 16000);
        assertEquals(10000, lowered.amount());
        assertEquals(4000, lowered.restockAt());
    }

    @Test
    void lowerKeepClampsRestockLevel() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000, 6000).step(-4000, 16000);
        assertEquals(4000, keep.amount());
        assertEquals(4000, keep.restockAt());
        assertEquals(new FluidKeep<>("water", 4000, 4000), new FluidKeep<>("water", 18000, 9000).clamp(4000));
    }

    @Test
    void restockLevelStaysWithinKeep() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000);
        assertEquals(8000, keep.stepRestock(1000).restockAt());
        assertEquals(4000, keep.stepRestock(-4000).restockAt());
        assertEquals(0, keep.stepRestock(-10000).restockAt());
        assertEquals(8000, keep.stepRestock(-4000).amount());
    }

    @Test
    void withFluidKeepsRestockLevel() {
        FluidKeep<String> keep = new FluidKeep<>("water", 8000, 3000);
        assertEquals(new FluidKeep<>("lava", 8000, 3000), keep.withFluid("lava"));
        assertEquals(0, new FluidKeep<String>(null, 8000, 3000).restockLevel());
    }
}
