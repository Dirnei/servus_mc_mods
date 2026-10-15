package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSettings.Direction;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSettings.Sort;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalSort.Key;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminalSortTest {

    private static final Key COBBLE = new Key("Cobblestone", 50, "minecraft");
    private static final Key APPLE = new Key("Apple", 10, "minecraft");
    private static final Key BREAD = new Key("Bread", 20, "minecraft");
    private static final Key GEAR = new Key("Iron Gear", 20, "servus_alchemy");

    private static List<String> sorted(Sort sort, Direction direction, Key... keys) {
        List<Key> list = new ArrayList<>(List.of(keys));
        list.sort(TerminalSort.comparator(sort, direction));
        return list.stream().map(Key::name).toList();
    }

    @Test
    void byName() {
        assertEquals(List.of("Apple", "Bread", "Cobblestone"), sorted(Sort.NAME, Direction.ASCENDING, COBBLE, APPLE, BREAD));
        assertEquals(List.of("Cobblestone", "Bread", "Apple"), sorted(Sort.NAME, Direction.DESCENDING, COBBLE, APPLE, BREAD));
    }

    @Test
    void byAmount() {
        assertEquals(List.of("Cobblestone", "Bread", "Apple"), sorted(Sort.AMOUNT, Direction.DESCENDING, APPLE, COBBLE, BREAD));
        assertEquals(List.of("Apple", "Bread", "Cobblestone"), sorted(Sort.AMOUNT, Direction.ASCENDING, COBBLE, BREAD, APPLE));
    }

    @Test
    void byMod() {
        assertEquals(List.of("Apple", "Bread", "Iron Gear"), sorted(Sort.MOD, Direction.ASCENDING, GEAR, BREAD, APPLE));
        assertEquals(List.of("Iron Gear", "Apple", "Bread"), sorted(Sort.MOD, Direction.DESCENDING, BREAD, GEAR, APPLE));
    }

    @Test
    void tiesFallBackToName() {
        assertEquals(List.of("Bread", "Iron Gear"), sorted(Sort.AMOUNT, Direction.DESCENDING, GEAR, BREAD));
    }
}
