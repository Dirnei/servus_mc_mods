package io.github.dirnei.mc.servus.alchemy.coagulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SeedIndexTest {

    private static final SeedIndex.Entry<String> SALT = new SeedIndex.Entry<>("servus_alchemy:coagulation/salt_crystal",
            List.of("servus_alchemy:kandis", "servus_alchemy:salt_crystal"), "salt");
    private static final SeedIndex.Entry<String> FLINT = new SeedIndex.Entry<>("servus_alchemy:coagulation/flint",
            List.of("minecraft:flint"), "flint");
    private static final SeedIndex.Entry<String> SUGAR_CUBE = new SeedIndex.Entry<>("mypack:coagulation/sugar_cube",
            List.of("servus_alchemy:kandis", "minecraft:sugar"), "sugar_cube");

    @Test
    void everySeedItemFindsItsRecipe() {
        SeedIndex<String> index = SeedIndex.build(List.of(SALT, FLINT));
        assertEquals(Optional.of("salt"), index.lookup("servus_alchemy:kandis"));
        assertEquals(Optional.of("salt"), index.lookup("servus_alchemy:salt_crystal"));
        assertEquals(Optional.of("flint"), index.lookup("minecraft:flint"));
        assertTrue(index.conflicts().isEmpty());
    }

    @Test
    void anUnknownItemSeedsNothing() {
        assertEquals(Optional.empty(), SeedIndex.build(List.of(SALT)).lookup("minecraft:dirt"));
    }

    @Test
    void aSharedSeedStaysWithTheFirstRecipeIdInAnyOrder() {
        for (List<SeedIndex.Entry<String>> order : List.of(List.of(SALT, SUGAR_CUBE), List.of(SUGAR_CUBE, SALT))) {
            SeedIndex<String> index = SeedIndex.build(order);
            assertEquals(Optional.of("sugar_cube"), index.lookup("servus_alchemy:kandis"),
                    "mypack sorts before servus_alchemy and keeps the seed");
            assertEquals(Optional.of("sugar_cube"), index.lookup("minecraft:sugar"));
            assertEquals(List.of("servus_alchemy:salt_crystal"), index.seedsOf(SALT.recipeId()));
            assertEquals(List.of(new SeedIndex.Conflict("servus_alchemy:kandis", SUGAR_CUBE.recipeId(), SALT.recipeId())),
                    index.conflicts());
        }
    }

    @Test
    void aRecipeThatLostEverySeedCannotBeGrown() {
        SeedIndex.Entry<String> copy = new SeedIndex.Entry<>("servus_alchemy:gametest/duplicate_seed", List.of("servus_alchemy:kandis"), "copy");
        SeedIndex<String> index = SeedIndex.build(List.of(copy, SALT));
        assertEquals(List.of(), index.seedsOf(copy.recipeId()));
        assertEquals(Optional.of("salt"), index.lookup("servus_alchemy:kandis"));
        assertEquals(1, index.conflicts().size());
    }

    @Test
    void seedsKeepTheirOrder() {
        assertEquals(List.of("servus_alchemy:kandis", "servus_alchemy:salt_crystal"), SeedIndex.build(List.of(SALT)).seedsOf(SALT.recipeId()));
    }
}
