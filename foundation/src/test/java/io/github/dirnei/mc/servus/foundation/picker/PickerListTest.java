package io.github.dirnei.mc.servus.foundation.picker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PickerListTest {

    private static final PickerList.Item NONE = new PickerList.Item(PickerList.NONE_ID, "None, harvest only");
    private static final PickerList.Item WHEAT = new PickerList.Item("minecraft:wheat_seeds", "Wheat Seeds");
    private static final PickerList.Item CARROT = new PickerList.Item("minecraft:carrot", "Carrot");
    private static final PickerList.Item BEET = new PickerList.Item("minecraft:beetroot_seeds", "beetroot Seeds");
    private static final PickerList.Item RICE = new PickerList.Item("farmersdelight:rice", "Rice");

    @Test
    void sortsByNameIgnoringCaseWithNoneFirst() {
        assertEquals(List.of(NONE, BEET, CARROT, RICE, WHEAT), PickerList.sorted(List.of(WHEAT, RICE, NONE, CARROT, BEET)));
    }

    @Test
    void equalNamesSortById() {
        PickerList.Item a = new PickerList.Item("a:rice", "Rice");
        PickerList.Item b = new PickerList.Item("b:rice", "Rice");
        assertEquals(List.of(a, b), PickerList.sorted(List.of(b, a)));
    }

    @Test
    void blankQueryKeepsAll() {
        List<PickerList.Item> all = List.of(NONE, CARROT, WHEAT);
        assertEquals(all, PickerList.filter(all, ""));
        assertEquals(all, PickerList.filter(all, "   "));
        assertEquals(all, PickerList.filter(all, null));
    }

    @Test
    void searchesNamesAndIds() {
        List<PickerList.Item> all = List.of(NONE, BEET, CARROT, RICE, WHEAT);
        assertEquals(List.of(NONE, CARROT), PickerList.filter(all, "CARR"));
        assertEquals(List.of(NONE, RICE), PickerList.filter(all, "farmersdelight"));
        assertEquals(List.of(NONE, BEET, WHEAT), PickerList.filter(all, " seeds "));
    }

    @Test
    void noneStaysWhenNothingMatches() {
        assertEquals(List.of(NONE), PickerList.filter(List.of(NONE, CARROT, WHEAT), "melon"));
    }
}
