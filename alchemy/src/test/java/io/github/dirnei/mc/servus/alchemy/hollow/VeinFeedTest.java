package io.github.dirnei.mc.servus.alchemy.hollow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinFeed.Entry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VeinFeedTest {

    private static final VeinFeed IRON = new VeinFeed(Map.of(
            Principle.SALT, new Entry(10, 1.0),
            Principle.MERCURY, new Entry(4, 0.5),
            Principle.SULFUR, new Entry(2, 0.5)));

    @Test
    void emptyFeedGivesFactorOneAndCostsNothing() {
        Principles plenty = new Principles(1000, 1000, 1000);
        assertEquals(1, VeinFeed.NONE.factor(plenty));
        assertEquals(Principles.NONE, VeinFeed.NONE.cost(plenty));
        assertTrue(VeinFeed.NONE.isEmpty());
    }

    @Test
    void belowItsAmountIsNotFed() {
        Principles held = new Principles(9, 0, 0);
        assertFalse(IRON.fed(Principle.SALT, held));
        assertEquals(1, IRON.factor(held));
        assertEquals(Principles.NONE, IRON.cost(held));
    }

    @Test
    void saltOnlyAddsItsBonus() {
        Principles held = new Principles(10, 0, 0);
        assertTrue(IRON.fed(Principle.SALT, held));
        assertEquals(2, IRON.factor(held));
    }

    @Test
    void allThreeSum() {
        assertEquals(3, IRON.factor(new Principles(100, 100, 100)));
    }

    @Test
    void costHoldsOnlyFedPrinciples() {
        assertEquals(new Principles(10, 0, 4), IRON.cost(new Principles(50, 1, 4)));
        assertEquals(new Principles(10, 2, 4), IRON.cost(new Principles(50, 2, 4)));
    }

    @Test
    void principlesOutsideTheFeedNeverCount() {
        VeinFeed saltOnly = new VeinFeed(Map.of(Principle.SALT, new Entry(5, 1.0)));
        Principles mercury = new Principles(0, 0, 1000);
        assertFalse(saltOnly.fed(Principle.MERCURY, mercury));
        assertEquals(1, saltOnly.factor(mercury));
        assertEquals(Principles.NONE, saltOnly.cost(mercury));
        assertFalse(saltOnly.eats(Principle.MERCURY));
    }

    @Test
    void amountsListEveryPrincipleOfTheFeed() {
        assertEquals(new Principles(10, 2, 4), IRON.amounts());
    }

    @Test
    void badEntriesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Entry(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Entry(1, -0.5));
    }
}
