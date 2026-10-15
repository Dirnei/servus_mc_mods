package io.github.dirnei.mc.servus.storage.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShuttleProfileTest {

    static final ModuleSpec HARVESTER = new ModuleSpec("harvester", true, true);
    static final ModuleSpec PROBE = new ModuleSpec("probe", false, false);

    private static ShuttleProfile profile(int tier, Optional<ModuleSpec> tool, int speed, int handling) {
        return ShuttleProfile.of(ShuttleTiers.DEFAULT, tier, tool, speed, handling);
    }

    @Test
    void stacksComeFromTheTier() {
        assertEquals(2, profile(1, Optional.empty(), 0, 0).stacks());
        assertEquals(4, profile(2, Optional.empty(), 0, 0).stacks());
        assertEquals(8, profile(3, Optional.empty(), 0, 0).stacks());
    }

    @Test
    void fieldOnlyComesFromTheTool() {
        assertTrue(profile(1, Optional.of(HARVESTER), 0, 0).fieldOnly());
        assertFalse(profile(1, Optional.of(PROBE), 0, 0).fieldOnly());
        assertFalse(profile(1, Optional.empty(), 0, 0).fieldOnly());
    }

    @Test
    void liftlessComesFromAWideTool() {
        assertTrue(profile(1, Optional.of(HARVESTER), 0, 0).liftless());
        assertFalse(profile(1, Optional.of(PROBE), 0, 0).liftless());
        assertFalse(ShuttleProfile.plain().liftless());
    }

    @Test
    void toolIdIsTheModuleSet() {
        assertEquals(Set.of("harvester"), profile(2, Optional.of(HARVESTER), 0, 0).modules());
        assertEquals(Set.of(), ShuttleProfile.plain().modules());
    }

    @Test
    void speedAndHandlingComeFromCounts() {
        ShuttleProfile fast = profile(1, Optional.empty(), 2, 2);
        assertEquals(0.23, fast.speed(), 1e-9);
        assertEquals(0.64, fast.handling(), 1e-9);
        assertEquals(0.15, ShuttleProfile.plain().speed(), 1e-9);
        assertEquals(1.0, ShuttleProfile.plain().handling(), 1e-9);
    }
}
