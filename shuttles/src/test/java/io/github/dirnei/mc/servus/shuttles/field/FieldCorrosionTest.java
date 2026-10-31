package io.github.dirnei.mc.servus.shuttles.field;

import static io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion.CORRODED;
import static io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion.COPPER;
import static io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion.EXPOSED;
import static io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion.OXIDIZED;
import static io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion.WEATHERED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FieldCorrosionTest {

    @Test
    void stepsAndBounds() {
        assertEquals(EXPOSED, FieldCorrosion.next(COPPER));
        assertEquals(CORRODED, FieldCorrosion.next(CORRODED));
        assertEquals(OXIDIZED, FieldCorrosion.previous(CORRODED));
        assertEquals(COPPER, FieldCorrosion.previous(COPPER));
        assertEquals("weathered", FieldCorrosion.name(WEATHERED));
        assertEquals("copper", FieldCorrosion.name(-1));
    }

    @Test
    void agingCountsSteps() {
        assertEquals(new FieldCorrosion.Aged(300, 0, 0), FieldCorrosion.age(0, 300, 1000, 0));
        assertEquals(new FieldCorrosion.Aged(200, 0, 1), FieldCorrosion.age(900, 300, 1000, 0));
        assertEquals(new FieldCorrosion.Aged(500, 0, 2), FieldCorrosion.age(0, 2500, 1000, 0));
    }

    @Test
    void agingUsesUpkeepFirst() {
        assertEquals(new FieldCorrosion.Aged(0, 1, 0), FieldCorrosion.age(0, 1000, 1000, 3));
        assertEquals(new FieldCorrosion.Aged(0, 2, 1), FieldCorrosion.age(0, 3000, 1000, 2));
    }

    @Test
    void agingCountsStepsPastCorroded() {
        assertEquals(new FieldCorrosion.Aged(0, 0, 7), FieldCorrosion.age(0, 7000, 1000, 0));
    }

    @Test
    void agingOffWithoutStep() {
        assertEquals(new FieldCorrosion.Aged(0, 0, 0), FieldCorrosion.age(0, 100000, 0, 5));
    }

    @Test
    void progressPercent() {
        assertEquals(0, FieldCorrosion.progress(0, 1000));
        assertEquals(50, FieldCorrosion.progress(500, 1000));
        assertEquals(99, FieldCorrosion.progress(999, 1000));
        assertEquals(0, FieldCorrosion.progress(500, 0));
    }

    private static Function<Integer, List<Integer>> line(int size) {
        return i -> {
            List<Integer> next = new ArrayList<>();
            if (i > 0) {
                next.add(i - 1);
            }
            if (i < size - 1) {
                next.add(i + 1);
            }
            return next;
        };
    }

    private static Function<Integer, List<Integer>> loop(int size) {
        return i -> List.of((i + size - 1) % size, (i + 1) % size);
    }

    @Test
    void wetStopsAtCorrodedMiddle() {
        assertEquals(Set.of(0, 1), FieldCorrosion.wet(0, line(6), i -> i == 2));
        assertEquals(Set.of(0), FieldCorrosion.wet(0, line(4), i -> i == 1));
    }

    @Test
    void wetNoneFromCorrodedStart() {
        assertEquals(Set.of(), FieldCorrosion.wet(0, line(4), i -> i == 0));
    }

    @Test
    void wetGoesAroundLoop() {
        assertEquals(Set.of(0, 1, 2, 4, 5), FieldCorrosion.wet(0, loop(6), i -> i == 3));
    }

    @Test
    void wetAllButCorrodedFarEnd() {
        assertEquals(Set.of(0, 1, 2, 3), FieldCorrosion.wet(0, line(5), i -> i == 4));
    }

    @Test
    void scrapeFullBudget() {
        FieldCorrosion.Scrape plan = FieldCorrosion.scrape(List.of(OXIDIZED, OXIDIZED, OXIDIZED), List.of(0, 0, 0), 4, 100);
        assertArrayEquals(new int[] {WEATHERED, WEATHERED, WEATHERED}, plan.stages());
        assertArrayEquals(new int[] {0, 0, 0}, plan.progress());
        assertEquals(12, plan.spent());
        assertEquals(3, plan.scraped());
        assertEquals(0, plan.left());
    }

    @Test
    void scrapeWornBudgetLeavesProgress() {
        FieldCorrosion.Scrape plan = FieldCorrosion.scrape(List.of(WEATHERED, WEATHERED, WEATHERED), List.of(0, 0, 0), 4, 10);
        assertArrayEquals(new int[] {EXPOSED, EXPOSED, WEATHERED}, plan.stages());
        assertArrayEquals(new int[] {0, 0, 2}, plan.progress());
        assertEquals(10, plan.spent());
        assertEquals(2, plan.scraped());
        assertEquals(1, plan.left());
    }

    @Test
    void scrapeProgressFinishesRail() {
        FieldCorrosion.Scrape plan = FieldCorrosion.scrape(List.of(WEATHERED), List.of(2), 4, 100);
        assertArrayEquals(new int[] {EXPOSED}, plan.stages());
        assertArrayEquals(new int[] {0}, plan.progress());
        assertEquals(2, plan.spent());
    }

    @Test
    void scrapeSkipsCopperForFree() {
        FieldCorrosion.Scrape plan = FieldCorrosion.scrape(List.of(COPPER, EXPOSED, COPPER), List.of(0, 0, 0), 4, 4);
        assertArrayEquals(new int[] {COPPER, COPPER, COPPER}, plan.stages());
        assertEquals(4, plan.spent());
        assertEquals(1, plan.scraped());
        assertEquals(0, plan.left());
        assertEquals(0, FieldCorrosion.scrape(List.of(COPPER, COPPER), List.of(0, 0), 4, 50).spent());
    }

    @Test
    void scrapeUnlimitedBudget() {
        FieldCorrosion.Scrape plan = FieldCorrosion.scrape(List.of(CORRODED, EXPOSED), List.of(0, 0), 4, -1);
        assertArrayEquals(new int[] {OXIDIZED, COPPER}, plan.stages());
        assertEquals(8, plan.spent());
        assertEquals(2, plan.scraped());
    }

    @Test
    void scrapeAtMostOneStagePerRail() {
        FieldCorrosion.Scrape plan = FieldCorrosion.scrape(List.of(CORRODED), List.of(0), 4, 1000);
        assertArrayEquals(new int[] {OXIDIZED}, plan.stages());
        assertEquals(4, plan.spent());
    }

    @Test
    void scrapeAllRemainingLeftWhenBudgetGone() {
        FieldCorrosion.Scrape plan = FieldCorrosion.scrape(List.of(WEATHERED, WEATHERED, WEATHERED, WEATHERED, WEATHERED),
                List.of(0, 0, 0, 0, 0), 4, 10);
        assertEquals(2, plan.scraped());
        assertEquals(3, plan.left());
        assertArrayEquals(new int[] {0, 0, 2, 0, 0}, plan.progress());
    }
}
