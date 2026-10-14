package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Placement;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Relation;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalcinerLayoutTest {

    @Test
    void cellsOfEveryRole() {
        assertEquals(14, CalcinerLayout.cells().size());
        assertEquals(2, CalcinerLayout.cells(Role.STONE_BRICKS).size());
        assertEquals(2, CalcinerLayout.cells(Role.DEEPSLATE_TILES).size());
        assertEquals(2, CalcinerLayout.cells(Role.TANK).size());
        assertEquals(2, CalcinerLayout.cells(Role.DUCT).size());
        assertEquals(CalcinerLayout.SULFUR_OUTLET, CalcinerLayout.cells(Role.DUCT).getLast().index());
        assertEquals(CalcinerLayout.FIRST_DUCT, CalcinerLayout.cells(Role.DUCT).getFirst().index());
        assertEquals(Role.CALCINER, CalcinerLayout.cell(0).role());
        for (int i = 0; i < CalcinerLayout.cells().size(); i++) {
            assertEquals(i, CalcinerLayout.cell(i).index());
        }
    }

    @Test
    void patternIsTheFrontView() {
        assertEquals(List.of("DH_", "DLR", "S0S", "TFT", "EAE"), CalcinerLayout.patternRows());
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
            Offset receiver = new Offset(alembic.x() + facing.rightX(), alembic.y(), alembic.z() + facing.rightZ());
            assertEquals(Role.RECEIVER, CalcinerLayout.cellAt(receiver, facing).orElseThrow().role());
        }
    }

    @Test
    void tanksAreBesideTheFirebox() {
        Cell sulfur = CalcinerLayout.cell(CalcinerLayout.SULFUR_TANK);
        Cell mercury = CalcinerLayout.cell(CalcinerLayout.MERCURY_TANK);
        assertEquals(Role.TANK, sulfur.role());
        assertEquals(Role.TANK, mercury.role());
        assertEquals(new Placement(Relation.LEFT_OF, Role.FIREBOX), CalcinerLayout.placement(sulfur));
        assertEquals(new Placement(Relation.RIGHT_OF, Role.FIREBOX), CalcinerLayout.placement(mercury));
    }

    @Test
    void freeCornersAreNoCells() {
        for (Facing facing : Facing.values()) {
            Offset corner = new Offset(facing.rightX(), 2, facing.rightZ());
            assertTrue(CalcinerLayout.cellAt(corner, facing).isEmpty());
        }
    }

    @Test
    void placementNamesTheNeighbouringPart() {
        assertEquals(new Placement(Relation.RIGHT_OF, Role.ALEMBIC), CalcinerLayout.placement(CalcinerLayout.part(Role.RECEIVER)));
        assertEquals(new Placement(Relation.ABOVE, Role.ALEMBIC), CalcinerLayout.placement(CalcinerLayout.part(Role.HOOD)));
        assertEquals(new Placement(Relation.ABOVE, Role.CALCINER), CalcinerLayout.placement(CalcinerLayout.part(Role.ALEMBIC)));
        assertEquals(new Placement(Relation.BELOW, Role.CALCINER), CalcinerLayout.placement(CalcinerLayout.part(Role.FIREBOX)));
        assertEquals(new Placement(Relation.BELOW, Role.FIREBOX), CalcinerLayout.placement(CalcinerLayout.part(Role.ASH_PAN)));
        assertEquals(new Placement(Relation.LEFT_OF, Role.CALCINER), CalcinerLayout.placement(CalcinerLayout.cell(1)));
        assertEquals(new Placement(Relation.RIGHT_OF, Role.CALCINER), CalcinerLayout.placement(CalcinerLayout.cell(2)));
        assertEquals(new Placement(Relation.LEFT_OF, Role.ASH_PAN), CalcinerLayout.placement(CalcinerLayout.cell(3)));
        assertEquals(new Placement(Relation.RIGHT_OF, Role.ASH_PAN), CalcinerLayout.placement(CalcinerLayout.cell(4)));
        assertEquals(new Placement(Relation.LEFT_OF, Role.HOOD), CalcinerLayout.placement(CalcinerLayout.cell(CalcinerLayout.FIRST_DUCT)));
        assertEquals(new Placement(Relation.LEFT_OF, Role.ALEMBIC), CalcinerLayout.placement(CalcinerLayout.cell(CalcinerLayout.SULFUR_OUTLET)));
    }
}
