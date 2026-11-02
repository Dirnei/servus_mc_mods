package io.github.dirnei.mc.servus.shuttles.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The warehouse numbers of a world: W1, W2, ... by warehouse id. A number is
 * never given twice, also after its warehouse is gone, and an id keeps its
 * number for good.
 */
public final class WarehouseNumbers {

    /** Where a warehouse's controller stands, for numbering warehouses that had no number yet. */
    public record Place(String dimension, int x, int y, int z) {

        static final Comparator<Place> ORDER = Comparator.comparing(Place::dimension)
                .thenComparingInt(Place::x).thenComparingInt(Place::y).thenComparingInt(Place::z);
    }

    private final Map<UUID, Integer> byId = new LinkedHashMap<>();
    private int last;

    /**
     * The number of the warehouse id, given now when it has none. {@code held} is a number its
     * record already carries without an id (0 for none); it becomes the id's number.
     */
    public int numberOf(UUID id, int held) {
        Integer known = byId.get(id);
        if (known != null) {
            return known;
        }
        int number = held > 0 ? held : next();
        byId.put(id, number);
        return number;
    }

    /** The number of the id, 0 when it has none yet. */
    public int known(UUID id) {
        return byId.getOrDefault(id, 0);
    }

    /** A fresh number that belongs to no id yet. */
    public int next() {
        return ++last;
    }

    /** The highest number given so far. */
    public int last() {
        return last;
    }

    /**
     * Gives each place a fresh number in the order of dimension, then x, y and z.
     * Returns the numbers in the order of the given list.
     */
    public int[] numberInOrder(List<Place> places) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparing(places::get, Place.ORDER));
        int[] numbers = new int[places.size()];
        for (int index : order) {
            numbers[index] = next();
        }
        return numbers;
    }

    /** Every id with its number, for saving. */
    public Map<UUID, Integer> all() {
        return Collections.unmodifiableMap(byId);
    }

    /** Takes saved numbers; the counter never falls below a known number. */
    public void load(Map<UUID, Integer> numbers, int lastGiven) {
        byId.clear();
        byId.putAll(numbers);
        last = Math.max(lastGiven, numbers.values().stream().mapToInt(Integer::intValue).max().orElse(0));
    }

    /** Raises the counter to a number found on a record, so it is not given again. */
    public void reserve(int number) {
        last = Math.max(last, number);
    }
}
