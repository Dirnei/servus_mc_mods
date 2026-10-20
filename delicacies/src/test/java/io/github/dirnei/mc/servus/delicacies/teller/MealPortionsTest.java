package io.github.dirnei.mc.servus.delicacies.teller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MealPortionsTest {

    @Test
    void onlyTheLastPortionFinishesTheMeal() {
        for (int left = MealPortions.FULL; left > 1; left--) {
            assertFalse(MealPortions.isLast(left), "left " + left);
        }
        assertTrue(MealPortions.isLast(1));
    }

    @Test
    void eatingTakesOnePortion() {
        assertEquals(3, MealPortions.afterEating(4));
        assertEquals(1, MealPortions.afterEating(2));
        assertThrows(IllegalArgumentException.class, () -> MealPortions.afterEating(1));
    }

    @Test
    void outOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> MealPortions.isLast(0));
        assertThrows(IllegalArgumentException.class, () -> MealPortions.isLast(MealPortions.FULL + 1));
        assertThrows(IllegalArgumentException.class, () -> MealPortions.afterEating(-1));
    }
}
