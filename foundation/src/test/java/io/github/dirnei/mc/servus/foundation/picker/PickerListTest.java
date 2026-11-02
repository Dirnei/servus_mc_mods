package io.github.dirnei.mc.servus.foundation.picker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.search.SearchFacts;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PickerListTest {

    private static final PickerList.Item NONE = new PickerList.Item(PickerList.NONE_ID, "None, harvest only");
    private static final PickerList.Item WHEAT = new PickerList.Item("minecraft:wheat_seeds", "Wheat Seeds");
    private static final PickerList.Item CARROT = new PickerList.Item("minecraft:carrot", "Carrot");
    private static final PickerList.Item BEET = new PickerList.Item("minecraft:beetroot_seeds", "beetroot Seeds");
    private static final PickerList.Item RICE = new PickerList.Item("farmersdelight:rice", "Rice");

    private static List<PickerList.Item> filter(List<PickerList.Item> items, String query) {
        Map<String, PickerList.Item> byId = items.stream().collect(Collectors.toMap(PickerList.Item::id, item -> item));
        return PickerList.filter(items, query, id -> {
            PickerList.Item item = byId.get(id);
            SearchFacts plain = PickerList.plainFacts(item);
            return new SearchFacts() {
                @Override
                public List<String> names() {
                    return plain.names();
                }

                @Override
                public List<String> mods() {
                    return List.of(id.substring(0, Math.max(0, id.indexOf(':'))));
                }
            };
        });
    }

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
        assertEquals(all, filter(all, ""));
        assertEquals(all, filter(all, "   "));
        assertEquals(all, filter(all, null));
    }

    @Test
    void searchesNamesAndIds() {
        List<PickerList.Item> all = List.of(NONE, BEET, CARROT, RICE, WHEAT);
        assertEquals(List.of(NONE, CARROT), filter(all, "CARR"));
        assertEquals(List.of(NONE, RICE), filter(all, "farmersdelight"));
        assertEquals(List.of(NONE, BEET, WHEAT), filter(all, " seeds "));
    }

    @Test
    void noneStaysWhenNothingMatches() {
        assertEquals(List.of(NONE), filter(List.of(NONE, CARROT, WHEAT), "melon"));
        assertEquals(List.of(NONE), filter(List.of(NONE, CARROT, WHEAT), "@farmersdelight"));
        assertEquals(List.of(NONE), filter(List.of(NONE, CARROT, WHEAT), "-@minecraft"));
    }

    @Test
    void modTermFiltersByMod() {
        List<PickerList.Item> all = List.of(NONE, BEET, CARROT, RICE, WHEAT);
        assertEquals(List.of(NONE, RICE), filter(all, "@farmersdelight"));
        assertEquals(List.of(NONE, BEET, CARROT, WHEAT), filter(all, "@minecraft"));
        assertEquals(List.of(NONE, BEET, WHEAT), filter(all, "@minecraft seeds"));
    }

    @Test
    void plainFactsMatchNameOrIdAndIdByAmpersand() {
        List<PickerList.Item> all = List.of(NONE, BEET, CARROT, RICE, WHEAT);
        assertEquals(List.of(NONE, BEET, WHEAT), PickerList.filter(all, "_seeds", id -> PickerList.plainFacts(itemOf(all, id))));
        assertEquals(List.of(NONE, RICE), PickerList.filter(all, "&farmersdelight:", id -> PickerList.plainFacts(itemOf(all, id))));
    }

    private static PickerList.Item itemOf(List<PickerList.Item> items, String id) {
        return items.stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void rankedPutsPickableFirstThenMostStockThenName() {
        List<PickerList.Item> all = List.of(NONE, BEET, CARROT, RICE, WHEAT);
        Map<String, Long> stock = Map.of(WHEAT.id(), 40L, CARROT.id(), 40L, RICE.id(), 900L);
        assertEquals(List.of(NONE, CARROT, WHEAT, BEET, RICE), PickerList.ranked(all, Set.of(RICE.id()), stock));
    }

    @Test
    void rankedWithoutStockKeepsNameOrderAndLocksLast() {
        List<PickerList.Item> all = List.of(NONE, BEET, CARROT, RICE, WHEAT);
        assertEquals(List.of(NONE, BEET, RICE, WHEAT, CARROT), PickerList.ranked(all, Set.of(CARROT.id()), null));
    }
}
