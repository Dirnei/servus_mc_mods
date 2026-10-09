package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Offset;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Placement;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Relation;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalcinerLayoutTest {

    @Test
    void twelveCellsWithSixBricks() {
        assertEquals(12, CalcinerLayout.cells().size());
        assertEquals(6, CalcinerLayout.cells().stream().filter(cell -> cell.role() == Role.BRICK).count());
        assertEquals(Role.CALCINER, CalcinerLayout.cell(0).role());
        for (int i = 0; i < CalcinerLayout.cells().size(); i++) {
            assertEquals(i, CalcinerLayout.cell(i).index());
        }
    }

    @Test
    void patternIsTheFrontView() {
        assertEquals(List.of("_H_", "_LR", "B0B", "BFB", "BAB"), CalcinerLayout.patternRows());
    }

    @Test
    void roundTripForEveryCellAndFacing() {
        for (Facing facing : Facing.values()) {
            for (Cell cell : CalcinerLayout.cells()) {
                Offset offset = CalcinerLayout.worldOffset(cell, facing);
                assertEquals(new Offset(0, 0, 0), new Offset(
                        offset.x() + CalcinerLayout.controllerOffset(cell, facing).x(),
                        offset.y() + CalcinerLayout.controllerOffset(cell, facing).y(),
                        offset.z() + CalcinerLayout.controllerOffset(cell, facing).z()));
                assertEquals(cell, CalcinerLayout.cellAt(offset, facing).orElseThrow());
            }
        }
    }

    @Test
    void receiverIsOnThePlayersRight() {
        // Facing north the player looks south, so their right is west.
        assertEquals(new Offset(-1, 1, 0), CalcinerLayout.worldOffset(CalcinerLayout.part(Role.RECEIVER), Facing.NORTH));
        assertEquals(new Offset(1, 1, 0), CalcinerLayout.worldOffset(CalcinerLayout.part(Role.RECEIVER), Facing.SOUTH));
        assertEquals(new Offset(0, 1, -1), CalcinerLayout.worldOffset(CalcinerLayout.part(Role.RECEIVER), Facing.EAST));
        assertEquals(new Offset(0, 1, 1), CalcinerLayout.worldOffset(CalcinerLayout.part(Role.RECEIVER), Facing.WEST));
        for (Facing facing : Facing.values()) {
            Offset alembic = CalcinerLayout.worldOffset(CalcinerLayout.part(Role.ALEMBIC), facing);
            Offset right = new Offset(alembic.x() + facing.rightX(), alembic.y(), alembic.z() + facing.rightZ());
            assertEquals(Role.RECEIVER, CalcinerLayout.cellAt(right, facing).orElseThrow().role());
        }
    }

    @Test
    void freeCornersAreNoCells() {
        for (Facing facing : Facing.values()) {
            for (Offset corner : List.of(new Offset(-1, 2, 0), new Offset(1, 2, 0), new Offset(-1, 1, 0))) {
                Offset rotated = new Offset(corner.x() * facing.rightX(), corner.y(), corner.x() * facing.rightZ());
                assertTrue(CalcinerLayout.cellAt(rotated, facing).isEmpty());
            }
        }
    }

    @Test
    void placementNamesTheNeighbouringPart() {
        assertEquals(new Placement(Relation.RIGHT_OF, Role.ALEMBIC), CalcinerLayout.placement(CalcinerLayout.part(Role.RECEIVER)));
        assertEquals(new Placement(Relation.ABOVE, Role.ALEMBIC), CalcinerLayout.placement(CalcinerLayout.part(Role.HOOD)));
        assertEquals(new Placement(Relation.ABOVE, Role.CALCINER), CalcinerLayout.placement(CalcinerLayout.part(Role.ALEMBIC)));
        assertEquals(new Placement(Relation.BELOW, Role.CALCINER), CalcinerLayout.placement(CalcinerLayout.part(Role.FIREBOX)));
        assertEquals(new Placement(Relation.BELOW, Role.FIREBOX), CalcinerLayout.placement(CalcinerLayout.part(Role.ASH_PAN)));
        assertEquals(new Placement(Relation.LEFT_OF, Role.ASH_PAN), CalcinerLayout.placement(CalcinerLayout.cell(5)));
        assertEquals(new Placement(Relation.RIGHT_OF, Role.CALCINER), CalcinerLayout.placement(CalcinerLayout.cell(2)));
    }
}
