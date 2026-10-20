package io.github.dirnei.mc.servus.brewery.brewhouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Kind;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BrewhouseLayoutTest {

    /** A small world of loose blocks by column, row and layer. */
    private static final class World {
        private final Map<String, Kind> blocks = new HashMap<>();

        World put(int column, int row, int layer, Kind kind) {
            blocks.put(column + "," + row + "," + layer, kind);
            return this;
        }

        World remove(int column, int row, int layer) {
            blocks.remove(column + "," + row + "," + layer);
            return this;
        }

        Optional<Cell> clickAt(int column, int row, int layer) {
            Kind kind = blocks.get(column + "," + row + "," + layer);
            return BrewhouseLayout.find(kind, (dc, dr, dl, wanted) -> blocks.get((column + dc) + "," + (row + dr) + "," + (layer + dl)) == wanted);
        }
    }

    private static World complete() {
        World world = new World();
        for (Cell cell : Cell.values()) {
            world.put(cell.column(), cell.row(), cell.layer(), cell.kind());
        }
        return world;
    }

    @Test
    void everyClickedCellFindsTheLayout() {
        World world = complete();
        for (Cell cell : Cell.values()) {
            assertEquals(Optional.of(cell), world.clickAt(cell.column(), cell.row(), cell.layer()), cell.name());
        }
    }

    @Test
    void missingKettleBlockIsRefused() {
        World world = complete().remove(1, 1, 1);
        assertTrue(world.clickAt(0, 0, 0).isEmpty());
    }

    @Test
    void kettleOnTheWrongSideIsRefused() {
        World world = new World();
        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 2; row++) {
                world.put(column, row, 0, Kind.BRICK);
            }
        }
        for (int column = 1; column < 3; column++) {
            for (int row = 0; row < 2; row++) {
                world.put(column, row, 1, Kind.KETTLE);
            }
        }
        assertTrue(world.clickAt(0, 0, 0).isEmpty());
        assertTrue(world.clickAt(2, 0, 1).isEmpty());
    }

    @Test
    void missingBrickIsRefused() {
        World world = complete().remove(2, 1, 0);
        assertTrue(world.clickAt(0, 0, 0).isEmpty());
        assertTrue(world.clickAt(0, 0, 1).isEmpty());
    }

    @Test
    void threeByThreeBricksPreferTheFrontRow() {
        World world = complete();
        for (int column = 0; column < 3; column++) {
            world.put(column, 2, 0, Kind.BRICK);
        }
        world.put(0, 2, 1, Kind.KETTLE).put(1, 2, 1, Kind.KETTLE);
        assertEquals(Optional.of(Cell.KETTLE_FRONT_RIGHT), world.clickAt(1, 1, 0));
    }

    @Test
    void cellsKnowTheirRoles() {
        assertTrue(Cell.KILN_FRONT.isKiln() && Cell.KILN_FRONT.hasDoor());
        assertTrue(Cell.KETTLE_BACK_LEFT.underKettle() && !Cell.KETTLE_BACK_LEFT.hasDoor());
        assertTrue(!Cell.COPPER_FRONT_LEFT.underKettle() && !Cell.COPPER_FRONT_LEFT.hasDoor());
        assertEquals(Optional.of(Cell.COPPER_BACK_RIGHT), Cell.at(1, 1, 1));
        assertTrue(Cell.at(2, 0, 1).isEmpty());
    }
}
