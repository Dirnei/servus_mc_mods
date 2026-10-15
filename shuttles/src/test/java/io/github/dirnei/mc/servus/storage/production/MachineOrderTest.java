package io.github.dirnei.mc.servus.storage.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MachineOrderTest {

    private record Row(String state, long pos) {
    }

    @Test
    void problemsFirstThenPosition() {
        List<Row> rows = new ArrayList<>(List.of(new Row("idle", 1), new Row("out_of_fuel", 9), new Row("crafting", 0),
                new Row("blocked", 3), new Row("upkeep_only", 2)));
        rows.sort(MachineOrder.comparator(Row::state, Row::pos));
        assertEquals(List.of("blocked", "out_of_fuel", "crafting", "idle", "upkeep_only"), rows.stream().map(Row::state).toList());
    }

    @Test
    void problemStates() {
        assertTrue(MachineOrder.problem("no_machine"));
        assertTrue(MachineOrder.problem("no_recipes"));
        assertFalse(MachineOrder.problem("idle"));
        assertFalse(MachineOrder.problem("upkeep_only"));
    }
}
