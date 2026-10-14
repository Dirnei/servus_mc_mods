package io.github.dirnei.mc.servus.foundation.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class MachineDefaultsTest {

    @Test
    void keepsRegistrationOrderAndLooksUpStats() {
        MachineDefaults.register("test_order:b", "powered", List.of(), MachineStats.powered(40, 20, 8000));
        MachineDefaults.register("test_order:a", "burner", List.of("minecraft:smelting"), MachineStats.burner(80));

        List<String> ids = MachineDefaults.all().keySet().stream().filter(id -> id.startsWith("test_order:")).toList();
        assertEquals(List.of("test_order:b", "test_order:a"), ids);
        assertEquals(80, MachineDefaults.stats("test_order:a").workTicks());
        assertEquals("burner", MachineDefaults.info("test_order:a").kind());
        assertEquals(List.of("minecraft:smelting"), MachineDefaults.info("test_order:a").recipeTypes());
    }

    @Test
    void refusesTheSameIdTwice() {
        MachineDefaults.register("test_twice:pump", "burner", List.of(), MachineStats.burner(20));

        assertThrows(IllegalStateException.class,
                () -> MachineDefaults.register("test_twice:pump", "burner", List.of(), MachineStats.burner(20)));
    }

    @Test
    void factoriesHaveNoItemBuffer() {
        assertEquals(0, MachineStats.burner(20).itemCapacity());
        assertEquals(0, MachineStats.powered(40, 20, 8000).itemCapacity());
        assertEquals(0, MachineStats.burnerPump(20, 100, 4000, 2).itemCapacity());
    }

    @Test
    void registeredItemCapacityIsLookedUp() {
        MachineDefaults.register("test_items:pan", "burner", List.of(),
                new MachineStats(1, 0, 0, 1, 0, 1, 0, 0, 0, 0, "", 0, 64));

        assertEquals(64, MachineDefaults.stats("test_items:pan").itemCapacity());
    }

    @Test
    void unknownMachineFails() {
        assertThrows(NoSuchElementException.class, () -> MachineDefaults.info("test_unknown:nope"));
    }
}
