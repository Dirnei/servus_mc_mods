package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoalTest {

    private static final Person ANNA = new Person(new UUID(0, 1), "Anna");

    private static Goal goal(int count, int target) {
        return new Goal(new UUID(5, 1), "minecraft:blackstone", target, count, ANNA);
    }

    @Test
    void carriedItemsAreAddedToTheCount() {
        assertEquals(2640, goal(1840, 4000).countWithCarried(800));
    }

    @Test
    void addingStopsAtTheTarget() {
        assertEquals(64, goal(40, 64).countWithCarried(100));
    }

    @Test
    void nothingCarriedOrAlreadyReachedChangesNothing() {
        assertEquals(10, goal(10, 64).countWithCarried(0));
        assertEquals(70, goal(70, 64).countWithCarried(5));
        assertEquals(10, goal(10, 64).countWithCarried(-3));
    }
}
