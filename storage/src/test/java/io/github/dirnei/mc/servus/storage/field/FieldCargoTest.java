package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FieldCargoTest {

    private static FieldCargo.Entry<String> entry(String key, int count) {
        return new FieldCargo.Entry<>(key, count, 64);
    }

    @Test
    void wheatAndSeedsFitTwoSlots() {
        FieldCargo<String> cargo = new FieldCargo<>(2);
        assertTrue(cargo.fits(List.of(entry("wheat", 1), entry("seeds", 2))));
        assertTrue(cargo.add(List.of(entry("wheat", 1), entry("seeds", 2))).isEmpty());
        assertFalse(cargo.fits(List.of(entry("carrot", 1))));
        assertTrue(cargo.fits(List.of(entry("wheat", 63), entry("seeds", 62))));
    }

    @Test
    void mergesIntoPartialStacksThenNewSlots() {
        FieldCargo<String> cargo = new FieldCargo<>(3);
        cargo.add(List.of(entry("wheat", 60)));
        cargo.add(List.of(entry("wheat", 10)));
        assertEquals(List.of(entry("wheat", 64), entry("wheat", 6)), cargo.entries());
        assertEquals(70, cargo.count("wheat"));
    }

    @Test
    void fullCargoReturnsOverflow() {
        FieldCargo<String> cargo = new FieldCargo<>(1);
        List<FieldCargo.Entry<String>> overflow = cargo.add(List.of(entry("wheat", 70)));
        assertEquals(List.of(entry("wheat", 6)), overflow);
        assertFalse(cargo.fits(List.of(entry("wheat", 1))));
        assertEquals(0, cargo.room("wheat", 64));
    }

    @Test
    void fitsDoesNotChangeTheCargo() {
        FieldCargo<String> cargo = new FieldCargo<>(2);
        cargo.fits(List.of(entry("wheat", 10)));
        assertTrue(cargo.isEmpty());
    }

    @Test
    void roomCountsPartialAndFreeSlots() {
        FieldCargo<String> cargo = new FieldCargo<>(2);
        cargo.add(List.of(entry("seeds", 40)));
        assertEquals(24 + 64, cargo.room("seeds", 64));
        assertEquals(64, cargo.room("wheat", 64));
    }

    @Test
    void takeEmptiesSlots() {
        FieldCargo<String> cargo = new FieldCargo<>(2);
        cargo.add(List.of(entry("seeds", 3), entry("wheat", 5)));
        assertEquals(3, cargo.take("seeds", 5));
        assertEquals(0, cargo.count("seeds"));
        assertEquals(List.of(entry("wheat", 5)), cargo.entries());
        assertEquals(0, cargo.take("seeds", 1));
    }

    @Test
    void unloadKeepsUpToAStackOfSeeds() {
        FieldCargo<String> cargo = new FieldCargo<>(4);
        cargo.add(List.of(entry("wheat", 70), entry("seeds", 80)));
        List<FieldCargo.Entry<String>> out = cargo.unload("seeds", 64);
        assertEquals(List.of(entry("wheat", 64), entry("wheat", 6), entry("seeds", 16)), out);
        assertEquals(List.of(entry("seeds", 64)), cargo.entries());
    }

    @Test
    void unloadWithoutKeepEmptiesAll() {
        FieldCargo<String> cargo = new FieldCargo<>(2);
        cargo.add(List.of(entry("wheat", 3), entry("seeds", 4)));
        assertEquals(2, cargo.unload(null, 64).size());
        assertTrue(cargo.isEmpty());
    }
}
