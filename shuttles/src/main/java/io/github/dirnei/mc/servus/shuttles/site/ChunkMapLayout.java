package io.github.dirnei.mc.servus.shuttles.site;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Places the chunks of a site in a square box of the controller GUI. Cells
 * get a size between {@link #MIN_CELL} and {@link #MAX_CELL} pixels; a span
 * too wide for the smallest cells shows the window around the home chunk.
 */
public final class ChunkMapLayout {

    public static final int MIN_CELL = 2;
    public static final int MAX_CELL = 10;

    /** One chunk drawn at x, y (pixels inside the box) with the cell size of the layout. */
    public record Cell(int x, int y, boolean home) {
    }

    public record Layout(int cell, List<Cell> cells) {
    }

    private ChunkMapLayout() {
    }

    public static Layout layout(Collection<Long> chunks, long home, int box) {
        int homeX = SiteChunks.x(home);
        int homeZ = SiteChunks.z(home);
        int minX = homeX;
        int maxX = homeX;
        int minZ = homeZ;
        int maxZ = homeZ;
        for (long key : chunks) {
            minX = Math.min(minX, SiteChunks.x(key));
            maxX = Math.max(maxX, SiteChunks.x(key));
            minZ = Math.min(minZ, SiteChunks.z(key));
            maxZ = Math.max(maxZ, SiteChunks.z(key));
        }
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        int cell = Math.max(MIN_CELL, Math.min(MAX_CELL, box / Math.max(width, depth)));
        int fit = Math.max(1, box / cell);
        // Too wide or deep for the box: keep the window of fit cells around home on that axis.
        if (width > fit) {
            minX = homeX - fit / 2;
            width = fit;
        }
        if (depth > fit) {
            minZ = homeZ - fit / 2;
            depth = fit;
        }
        int offsetX = (box - width * cell) / 2;
        int offsetY = (box - depth * cell) / 2;
        List<Cell> cells = new ArrayList<>();
        boolean homeDrawn = false;
        for (long key : chunks) {
            int column = SiteChunks.x(key) - minX;
            int row = SiteChunks.z(key) - minZ;
            if (column < 0 || column >= width || row < 0 || row >= depth) {
                continue;
            }
            boolean isHome = key == home;
            homeDrawn |= isHome;
            cells.add(new Cell(offsetX + column * cell, offsetY + row * cell, isHome));
        }
        if (!homeDrawn) {
            cells.add(new Cell(offsetX + (homeX - minX) * cell, offsetY + (homeZ - minZ) * cell, true));
        }
        return new Layout(cell, cells);
    }
}
