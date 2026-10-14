package io.github.dirnei.mc.servus.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PrincipleTextTest {

    @Test
    void namesAreCapitalised() {
        assertEquals("Salt", PrincipleText.name(Principle.SALT));
        assertEquals("Sulfur", PrincipleText.name(Principle.SULFUR));
        assertEquals("Mercury", PrincipleText.name(Principle.MERCURY));
    }

    @Test
    void lineListsNonZeroPrinciplesInOrder() {
        assertEquals("Cobblestone: 100 Salt, 10 Mercury", PrincipleText.line("Cobblestone", new Principles(100, 0, 10)));
    }

    @Test
    void singlePrincipleNamesOnlyThatOne() {
        assertEquals("Flint: 150 Salt", PrincipleText.line("Flint", new Principles(150, 0, 0)));
    }

    @Test
    void allThree() {
        assertEquals("100 Salt, 100 Sulfur, 250 Mercury", PrincipleText.amounts(new Principles(100, 100, 250)));
    }

    @Test
    void noneSaysNothing() {
        assertEquals("nothing", PrincipleText.amounts(Principles.NONE));
    }
}
