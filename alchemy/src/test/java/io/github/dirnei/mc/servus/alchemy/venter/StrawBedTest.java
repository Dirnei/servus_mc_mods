package io.github.dirnei.mc.servus.alchemy.venter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StrawBedTest {

    @Test
    void fullBedRefuses() {
        StrawBed bed = new StrawBed(2);
        assertTrue(bed.add(100));
        assertTrue(bed.add(100));
        assertFalse(bed.add(100));
        assertEquals(2, bed.bales());
        assertEquals(0, bed.room());
        assertEquals(1000, bed.levelMilli());
    }

    @Test
    void noWarmthIsNoBedding() {
        assertFalse(new StrawBed(2).add(0));
    }

    @Test
    void burnDropsTheOldestBale() {
        StrawBed bed = new StrawBed(8);
        bed.add(3);
        bed.add(50);
        bed.burn();
        bed.burn();
        assertEquals(2, bed.bales());
        bed.burn();
        assertEquals(1, bed.bales());
        assertArrayEquals(new int[] {50, 50}, bed.save());
    }

    @Test
    void levelCountsThePartBurnedBale() {
        StrawBed bed = new StrawBed(4);
        bed.add(100);
        bed.add(100);
        for (int i = 0; i < 50; i++) {
            bed.burn();
        }
        assertEquals(375, bed.levelMilli());
    }

    @Test
    void emptyLevelIsZero() {
        StrawBed bed = new StrawBed(8);
        assertEquals(0, bed.levelMilli());
        bed.add(1);
        bed.burn();
        assertEquals(0, bed.bales());
        assertEquals(0, bed.levelMilli());
        bed.burn();
        assertEquals(0, bed.bales());
    }

    @Test
    void mixedWarmths() {
        StrawBed bed = new StrawBed(2);
        bed.add(10);
        bed.add(1000);
        for (int i = 0; i < 5; i++) {
            bed.burn();
        }
        assertEquals(750, bed.levelMilli());
        for (int i = 0; i < 5; i++) {
            bed.burn();
        }
        assertEquals(500, bed.levelMilli());
        assertArrayEquals(new int[] {1000, 1000}, bed.save());
    }

    @Test
    void warmthLeftSumsTheBales() {
        StrawBed bed = new StrawBed(8);
        bed.add(10);
        bed.add(20);
        bed.burn();
        assertEquals(29, bed.warmthLeft());
    }

    @Test
    void saveAndLoadPairs() {
        StrawBed bed = new StrawBed(8);
        bed.add(6000);
        bed.add(400);
        bed.burn();
        int[] saved = bed.save();
        assertArrayEquals(new int[] {6000, 5999, 400, 400}, saved);
        StrawBed loaded = new StrawBed(8);
        loaded.load(saved);
        assertArrayEquals(saved, loaded.save());
        assertEquals(bed.levelMilli(), loaded.levelMilli());
    }

    @Test
    void loadDropsBalesBeyondTheCapacity() {
        StrawBed bed = new StrawBed(1);
        bed.load(new int[] {10, 5, 20, 20});
        assertArrayEquals(new int[] {10, 5}, bed.save());
    }
}
