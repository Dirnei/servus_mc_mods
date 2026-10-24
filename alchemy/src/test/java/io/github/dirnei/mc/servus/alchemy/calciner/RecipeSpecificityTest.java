package io.github.dirnei.mc.servus.alchemy.calciner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.calciner.RecipeSpecificity.Candidate;
import io.github.dirnei.mc.servus.alchemy.calciner.RecipeSpecificity.Rank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RecipeSpecificityTest {

    private static Candidate<String> tag(String id, int items) {
        return new Candidate<>(id, new Rank(true, items, id));
    }

    private static Candidate<String> item(String id, int items) {
        return new Candidate<>(id, new Rank(false, items, id));
    }

    @Test
    void itemBeatsTag() {
        assertEquals(Optional.of("oak_log"), RecipeSpecificity.best(List.of(tag("logs", 1), item("oak_log", 1))));
    }

    @Test
    void itemListBeatsSmallerTag() {
        assertEquals(Optional.of("two_items"), RecipeSpecificity.best(List.of(tag("one_tag", 1), item("two_items", 2))));
    }

    @Test
    void subsetTagBeatsItsSuperset() {
        assertEquals(Optional.of("nether_wart"), RecipeSpecificity.best(List.of(tag("crops", 10), tag("nether_wart", 1))));
    }

    @Test
    void equalCountsFallToTheId() {
        assertEquals(Optional.of("a_stems"), RecipeSpecificity.best(List.of(tag("b_stems", 4), tag("a_stems", 4))));
    }

    @Test
    void orderOfTheCandidatesNeverMatters() {
        List<Candidate<String>> candidates = new ArrayList<>(List.of(
                tag("logs", 40), tag("crimson_stems", 4), tag("warped_stems", 4), item("crimson_stem", 1), tag("everything", 900)));
        Random random = new Random(7);
        for (int i = 0; i < 20; i++) {
            Collections.shuffle(candidates, random);
            assertEquals(Optional.of("crimson_stem"), RecipeSpecificity.best(candidates));
        }
    }

    @Test
    void noCandidateGivesNothing() {
        assertTrue(RecipeSpecificity.best(List.<Candidate<String>>of()).isEmpty());
    }
}
