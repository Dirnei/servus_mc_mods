package io.github.dirnei.mc.servus.storage.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShuttleProfileTest {

    static final ModuleSpec CARGO_HOLD = new ModuleSpec("cargo_hold", ModuleSlot.UTILITY, false, true);
    static final ModuleSpec HARVESTER = new ModuleSpec("harvester", ModuleSlot.TOOL, true, false);
    static final ModuleSpec PROBE = new ModuleSpec("probe", ModuleSlot.TOOL, false, false);

    @Test
    void plainShuttleCarriesOneStack() {
        ShuttleProfile profile = ShuttleProfile.of(List.of(), 4);
        assertEquals(ShuttleProfile.PLAIN, profile);
        assertEquals(1, profile.stacks());
    }

    @Test
    void cargoHoldTakesTheGivenStackCount() {
        assertEquals(4, ShuttleProfile.of(List.of(CARGO_HOLD), 4).stacks());
        assertEquals(6, ShuttleProfile.of(List.of(CARGO_HOLD), 6).stacks());
        assertEquals(1, ShuttleProfile.of(List.of(CARGO_HOLD), 0).stacks(), "never less than one stack");
    }

    @Test
    void anyFieldOnlyModuleMakesTheShuttleFieldOnly() {
        assertTrue(ShuttleProfile.of(List.of(HARVESTER, CARGO_HOLD), 4).fieldOnly());
        assertFalse(ShuttleProfile.of(List.of(PROBE, CARGO_HOLD), 4).fieldOnly());
    }

    @Test
    void collectsModuleIds() {
        assertEquals(Set.of("harvester", "cargo_hold"), ShuttleProfile.of(List.of(HARVESTER, CARGO_HOLD), 4).modules());
    }
}
