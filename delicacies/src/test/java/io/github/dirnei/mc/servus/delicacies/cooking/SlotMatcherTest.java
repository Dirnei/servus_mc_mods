package io.github.dirnei.mc.servus.delicacies.cooking;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class SlotMatcherTest {

    private static final Predicate<String> EMPTY = String::isEmpty;

    private static Predicate<String> startsWith(String prefix) {
        return slot -> slot.startsWith(prefix);
    }

    private static Optional<int[]> match(List<Predicate<String>> wants, String... slots) {
        return SlotMatcher.match(wants, List.of(slots), EMPTY);
    }

    @Test
    void oneRequirementFindsItsSlot() {
        assertArrayEquals(new int[] {2}, match(List.of(startsWith("egg")), "", "", "egg").orElseThrow());
    }

    @Test
    void twoRequirementsNeedDistinctSlots() {
        assertTrue(match(List.of(startsWith("potato"), startsWith("potato")), "potato", "", "", "").isEmpty());
        assertArrayEquals(new int[] {0, 1}, match(List.of(startsWith("potato"), startsWith("onion")), "potato", "onion").orElseThrow());
    }

    @Test
    void backtrackingFindsTheOtherAssignment() {
        Predicate<String> anyVegetable = slot -> slot.startsWith("onion") || slot.startsWith("potato");
        assertArrayEquals(new int[] {1, 0}, match(List.of(anyVegetable, startsWith("onion")), "onion", "potato").orElseThrow());
    }

    @Test
    void aLeftoverItemFails() {
        assertTrue(match(List.of(startsWith("egg")), "egg", "cobblestone").isEmpty());
    }

    @Test
    void emptySlotsAreIgnored() {
        assertArrayEquals(new int[] {3}, match(List.of(startsWith("egg")), "", "", "", "egg").orElseThrow());
    }

    @Test
    void tooFewItemsFail() {
        assertTrue(match(List.of(startsWith("potato"), startsWith("onion")), "potato", "", "", "").isEmpty());
        assertTrue(match(List.of(startsWith("egg")), "", "", "", "").isEmpty());
    }
}
