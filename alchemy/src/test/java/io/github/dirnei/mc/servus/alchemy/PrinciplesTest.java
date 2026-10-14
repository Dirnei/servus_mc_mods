package io.github.dirnei.mc.servus.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PrinciplesTest {

    @Test
    void sumAndSubtract() {
        Principles a = new Principles(100, 0, 10);
        Principles b = new Principles(50, 20, 20);
        assertEquals(new Principles(150, 20, 30), a.plus(b));
        assertEquals(new Principles(50, 0, 0), new Principles(150, 20, 30).minus(new Principles(100, 20, 30)));
    }

    @Test
    void subtractingTooMuchIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Principles(10, 0, 0).minus(new Principles(20, 0, 0)));
    }

    @Test
    void negativeAmountsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Principles(-1, 0, 0));
    }

    @Test
    void coversNeeds() {
        Principles tanks = new Principles(250, 40, 0);
        assertTrue(tanks.covers(new Principles(250, 0, 0)));
        assertFalse(tanks.covers(new Principles(100, 50, 0)), "too little Sulfur");
        assertTrue(tanks.covers(Principles.NONE));
    }

    @Test
    void fitsIntoCapacity() {
        Principles tanks = new Principles(3950, 0, 4000);
        assertTrue(tanks.fits(new Principles(50, 100, 0), 4000));
        assertFalse(tanks.fits(new Principles(100, 0, 0), 4000), "Salt would overflow");
        assertFalse(tanks.fits(new Principles(0, 0, 1), 4000), "Mercury is full");
    }

    @Test
    void getAndWithByPrinciple() {
        Principles p = new Principles(1, 2, 3);
        assertEquals(2, p.get(Principle.SULFUR));
        assertEquals(new Principles(1, 2, 9), p.with(Principle.MERCURY, 9));
        assertEquals(6, p.total());
        assertTrue(Principles.NONE.isEmpty());
    }
}
