package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ItemQueryTest {

    @Test
    void emptyQueryMatchesEverything() {
        assertTrue(ItemQuery.of("  ").matches("Glass", "minecraft", "Minecraft"));
    }

    @Test
    void everyWordIsPartOfTheNameIgnoringCase() {
        assertTrue(ItemQuery.of("ice").matches("Packed Ice", "minecraft", "Minecraft"));
        assertTrue(ItemQuery.of("PACK ice").matches("Packed Ice", "minecraft", "Minecraft"));
        assertFalse(ItemQuery.of("blue ice").matches("Packed Ice", "minecraft", "Minecraft"));
    }

    @Test
    void atWordFiltersByNamespaceOrModName() {
        ItemQuery query = ItemQuery.of("@notes book");
        assertTrue(query.matches("Notebook", "servus_notes", "Servus: Notes"));
        assertFalse(query.matches("Book", "minecraft", "Minecraft"));
        assertTrue(ItemQuery.of("@servus:").matches("Notebook", "servus_notes", "Servus: Notes"));
        assertTrue(ItemQuery.of("@").matches("Glass", "minecraft", "Minecraft"));
    }
}
