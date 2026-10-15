package io.github.dirnei.mc.servus.storage.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkMapLayoutTest {

    @Test
    void singleChunkIsCentredAtMaxCell() {
        long home = SiteChunks.key(4, 4);
        ChunkMapLayout.Layout layout = ChunkMapLayout.layout(List.of(home), home, 60);
        assertEquals(ChunkMapLayout.MAX_CELL, layout.cell());
        assertEquals(1, layout.cells().size());
        assertEquals(new ChunkMapLayout.Cell(25, 25, true), layout.cells().get(0));
    }

    @Test
    void twoChunksSideBySide() {
        long home = SiteChunks.key(0, 0);
        long east = SiteChunks.key(1, 0);
        ChunkMapLayout.Layout layout = ChunkMapLayout.layout(List.of(home, east), home, 60);
        assertEquals(10, layout.cell());
        assertEquals(new ChunkMapLayout.Cell(20, 25, true), layout.cells().get(0));
        assertEquals(new ChunkMapLayout.Cell(30, 25, false), layout.cells().get(1));
    }

    @Test
    void cellSizeShrinksWithTheSpan() {
        long home = SiteChunks.key(0, 0);
        List<Long> row = new ArrayList<>();
        for (int x = 0; x < 12; x++) {
            row.add(SiteChunks.key(x, 0));
        }
        assertEquals(5, ChunkMapLayout.layout(row, home, 60).cell());
    }

    @Test
    void wideSpanIsWindowedAroundHome() {
        long home = SiteChunks.key(0, 0);
        List<Long> row = new ArrayList<>();
        for (int x = 0; x < 100; x++) {
            row.add(SiteChunks.key(x, 0));
        }
        ChunkMapLayout.Layout layout = ChunkMapLayout.layout(row, home, 60);
        assertEquals(ChunkMapLayout.MIN_CELL, layout.cell());
        assertEquals(15, layout.cells().size(), "home and the 14 chunks east of it fit the right half of the window");
        assertTrue(layout.cells().stream().anyMatch(ChunkMapLayout.Cell::home));
        assertTrue(layout.cells().stream().allMatch(cell -> cell.x() >= 0 && cell.x() + layout.cell() <= 60));
    }
}
