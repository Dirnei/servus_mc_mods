package io.github.dirnei.mc.servus.shuttles.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.storage.StorageFill.Bay;
import io.github.dirnei.mc.servus.shuttles.storage.StorageFill.Report;
import io.github.dirnei.mc.servus.shuttles.storage.StorageFill.Row;
import io.github.dirnei.mc.servus.shuttles.storage.StorageFill.Tank;
import java.util.List;
import org.junit.jupiter.api.Test;

class StorageFillTest {

    private static Bay zoned(String name, int used, int total) {
        return new Bay(name.toLowerCase(), name, used, total);
    }

    private static Bay unzoned(int used, int total) {
        return new Bay(StorageFill.NO_ZONE, "", used, total);
    }

    @Test
    void sumsAllBays() {
        Report report = StorageFill.of(List.of(unzoned(10, 27), unzoned(20, 27), unzoned(0, 27)), List.of());
        assertEquals(30, report.items().used());
        assertEquals(81, report.items().total());
        assertEquals(3, report.bays());
    }

    @Test
    void fullZoneFirstThenFullest() {
        Report report = StorageFill.of(List.of(zoned("Food", 30, 81), unzoned(300, 567), zoned("Ores", 81, 81)), List.of());
        List<String> order = report.zones().stream().map(Row::key).toList();
        assertEquals(List.of("ores", StorageFill.NO_ZONE, "food"), order);
        assertTrue(report.zones().get(0).full());
        assertFalse(report.zones().get(1).full());
    }

    @Test
    void zoneRowsAddUpToTheWarehouse() {
        Report report = StorageFill.of(List.of(zoned("Ores", 40, 54), zoned("Ores", 27, 27), unzoned(5, 27), zoned("Food", 1, 27)), List.of());
        long used = report.zones().stream().mapToLong(Row::used).sum();
        long total = report.zones().stream().mapToLong(Row::total).sum();
        assertEquals(report.items().used(), used);
        assertEquals(report.items().total(), total);
        Row ores = report.zones().stream().filter(row -> row.key().equals("ores")).findFirst().orElseThrow();
        assertEquals(67, ores.used());
        assertEquals(81, ores.total());
        assertEquals("Ores", ores.name());
    }

    @Test
    void unzonedRowAfterZonesOfTheSameShare() {
        Report report = StorageFill.of(List.of(unzoned(9, 27), zoned("Wood", 9, 27), zoned("Alpha", 9, 27)), List.of());
        assertEquals(List.of("alpha", "wood", StorageFill.NO_ZONE), report.zones().stream().map(Row::key).toList());
    }

    @Test
    void noZonesGiveOneUnzonedRow() {
        Report report = StorageFill.of(List.of(unzoned(1, 27), unzoned(2, 27)), List.of());
        assertEquals(1, report.zones().size());
        assertTrue(report.zones().get(0).unzoned());
        assertEquals(3, report.zones().get(0).used());
    }

    @Test
    void emptyWarehouse() {
        Report report = StorageFill.of(List.of(), List.of());
        assertEquals(0, report.items().total());
        assertTrue(report.zones().isEmpty());
        assertFalse(report.items().full());
        assertEquals(0.0, report.items().share());
    }

    @Test
    void sumsTanks() {
        Report report = StorageFill.of(List.of(), List.of(new Tank(16_000, 16_000), new Tank(8_000, 16_000), new Tank(0, 32_000)));
        assertEquals(24_000, report.fluid());
        assertEquals(64_000, report.fluidCapacity());
        assertEquals(3, report.tanks());
        assertFalse(report.fluidFull());
    }
}
