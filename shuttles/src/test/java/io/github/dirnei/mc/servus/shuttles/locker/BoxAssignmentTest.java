package io.github.dirnei.mc.servus.shuttles.locker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BoxAssignmentTest {

    /** Every box of an empty locker with S 3, M 6 and L 9 stacks. */
    private static List<BoxAssignment.Free> emptyLocker() {
        LockerLayout.Capacities capacities = new LockerLayout.Capacities(3, 6, 9);
        List<BoxAssignment.Free> free = new ArrayList<>();
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            free.add(new BoxAssignment.Free(box.index(), capacities.of(box.size())));
        }
        return free;
    }

    private static List<String> ids(Optional<List<Integer>> boxes) {
        return boxes.orElseThrow().stream().map(index -> LockerLayout.BOXES.get(index).id()).sorted().toList();
    }

    @Test
    void smallOrderTakesOneSmallBox() {
        assertEquals(List.of("S-01"), ids(BoxAssignment.assign(emptyLocker(), 2)));
    }

    @Test
    void twelveStacksTakeLargeAndSmall() {
        assertEquals(List.of("L-01", "S-01"), ids(BoxAssignment.assign(emptyLocker(), 12)));
    }

    @Test
    void sevenStacksWithoutLarge() {
        List<BoxAssignment.Free> free = List.of(new BoxAssignment.Free(0, 3), new BoxAssignment.Free(1, 3), new BoxAssignment.Free(6, 6));
        assertEquals(List.of("M-01", "S-01"), ids(BoxAssignment.assign(free, 7)));
    }

    @Test
    void tooBigIsRefused() {
        assertTrue(BoxAssignment.assign(emptyLocker(), 50).isEmpty());
        assertTrue(BoxAssignment.assign(emptyLocker(), 0).isEmpty());
        assertTrue(BoxAssignment.assign(List.of(), 1).isEmpty());
    }

    @Test
    void stacksRoundedPerEntry() {
        assertEquals(2, BoxAssignment.stacksNeeded(List.of(new BoxAssignment.Need(128, 64))));
        assertEquals(3, BoxAssignment.stacksNeeded(List.of(new BoxAssignment.Need(65, 64), new BoxAssignment.Need(1, 16))));
        assertEquals(1, BoxAssignment.stacksNeeded(List.of(new BoxAssignment.Need(1, 1), new BoxAssignment.Need(0, 64))));
    }
}
