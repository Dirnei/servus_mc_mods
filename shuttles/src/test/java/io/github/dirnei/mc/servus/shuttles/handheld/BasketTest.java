package io.github.dirnei.mc.servus.shuttles.handheld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BasketTest {

    private static Basket<String> fullBasket() {
        Basket<String> basket = Basket.empty();
        for (int i = 0; i < Basket.MAX_ENTRIES; i++) {
            basket = basket.add("item" + i, 1);
        }
        return basket;
    }

    @Test
    void addKeepsTheOrder() {
        Basket<String> basket = Basket.<String>empty().add("cobblestone", 128).add("bread", 64);
        assertEquals(List.of(new Basket.Entry<>("cobblestone", 128), new Basket.Entry<>("bread", 64)), basket.entries());
    }

    @Test
    void addMergesTheSameKey() {
        Basket<String> basket = Basket.<String>empty().add("bread", 64).add("bread", 32);
        assertEquals(1, basket.size());
        assertEquals(96, basket.amount("bread"));
    }

    @Test
    void mergeSaturates() {
        Basket<String> basket = Basket.<String>empty().add("bread", Integer.MAX_VALUE).add("bread", 5);
        assertEquals(Integer.MAX_VALUE, basket.amount("bread"));
    }

    @Test
    void setChangesTheAmount() {
        Basket<String> basket = Basket.<String>empty().add("bread", 64).set("bread", 10);
        assertEquals(10, basket.amount("bread"));
    }

    @Test
    void setOfAMissingKeyChangesNothing() {
        Basket<String> basket = Basket.<String>empty().add("bread", 64);
        assertSame(basket, basket.set("stone", 10));
    }

    @Test
    void amountBelowOneRemoves() {
        Basket<String> basket = Basket.<String>empty().add("bread", 64).add("stone", 1).set("bread", 0);
        assertEquals(List.of(new Basket.Entry<>("stone", 1)), basket.entries());
    }

    @Test
    void addBelowOneChangesNothing() {
        Basket<String> basket = Basket.<String>empty().add("bread", 0);
        assertTrue(basket.isEmpty());
    }

    @Test
    void removeDropsTheEntry() {
        Basket<String> basket = Basket.<String>empty().add("bread", 64).add("stone", 1).remove("bread");
        assertEquals(List.of(new Basket.Entry<>("stone", 1)), basket.entries());
    }

    @Test
    void clearEmpties() {
        assertTrue(Basket.<String>empty().add("bread", 64).clear().isEmpty());
    }

    @Test
    void fullRefusesANewKey() {
        Basket<String> basket = fullBasket();
        assertTrue(basket.full());
        assertFalse(basket.canAdd("bread"));
        assertSame(basket, basket.add("bread", 1));
    }

    @Test
    void fullStillRaisesAnEntry() {
        Basket<String> basket = fullBasket().add("item0", 4);
        assertEquals(5, basket.amount("item0"));
        assertEquals(Basket.MAX_ENTRIES, basket.size());
    }
}
