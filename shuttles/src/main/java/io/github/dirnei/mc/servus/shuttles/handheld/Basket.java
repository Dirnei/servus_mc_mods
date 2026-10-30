package io.github.dirnei.mc.servus.shuttles.handheld;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The handheld's basket: items with amounts, one entry per key, at most
 * {@link #MAX_ENTRIES}. Immutable; every edit returns the new basket. Pure Java.
 *
 * @param entries the entries in the order they were added
 */
public record Basket<K>(List<Entry<K>> entries) {

    /** How many different items the basket holds at most; a limit of the GUI. */
    public static final int MAX_ENTRIES = 9;

    /** One item of the basket and how many of it are wanted, at least 1. */
    public record Entry<K>(K key, int amount) {
    }

    public Basket {
        entries = List.copyOf(entries);
    }

    public static <K> Basket<K> empty() {
        return new Basket<>(List.of());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public boolean full() {
        return entries.size() >= MAX_ENTRIES;
    }

    /** Whether {@link #add} would take the key: it is in the basket already or there is a free entry. */
    public boolean canAdd(K key) {
        return indexOf(key) >= 0 || !full();
    }

    /** How many of the key the basket holds, 0 when none. */
    public int amount(K key) {
        int index = indexOf(key);
        return index < 0 ? 0 : entries.get(index).amount();
    }

    /**
     * Adds {@code amount} of the key, raising its entry when it has one. A
     * new key in a full basket, or an amount below 1, leaves the basket as it is.
     */
    public Basket<K> add(K key, int amount) {
        if (amount < 1 || !canAdd(key)) {
            return this;
        }
        int index = indexOf(key);
        if (index < 0) {
            List<Entry<K>> next = new ArrayList<>(entries);
            next.add(new Entry<>(key, amount));
            return new Basket<>(next);
        }
        return replace(index, new Entry<>(key, saturatedSum(entries.get(index).amount(), amount)));
    }

    /** Sets the amount of an entry already in the basket; below 1 removes it. A key not in the basket changes nothing. */
    public Basket<K> set(K key, int amount) {
        int index = indexOf(key);
        if (index < 0) {
            return this;
        }
        return amount < 1 ? remove(key) : replace(index, new Entry<>(key, amount));
    }

    public Basket<K> remove(K key) {
        int index = indexOf(key);
        if (index < 0) {
            return this;
        }
        List<Entry<K>> next = new ArrayList<>(entries);
        next.remove(index);
        return new Basket<>(next);
    }

    public Basket<K> clear() {
        return empty();
    }

    private Basket<K> replace(int index, Entry<K> entry) {
        List<Entry<K>> next = new ArrayList<>(entries);
        next.set(index, entry);
        return new Basket<>(next);
    }

    private int indexOf(K key) {
        for (int i = 0; i < entries.size(); i++) {
            if (Objects.equals(entries.get(i).key(), key)) {
                return i;
            }
        }
        return -1;
    }

    private static int saturatedSum(int a, int b) {
        return (int) Math.min(Integer.MAX_VALUE, (long) a + b);
    }
}
