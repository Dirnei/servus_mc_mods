package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.Principles;
import java.util.EnumMap;
import java.util.Map;

/**
 * What the gnome of a ripe vein eats: per principle, the mB one dug item uses and the bonus it gives while fed. Feeding
 * is never needed; each fed principle only speeds him up. Pure Java.
 *
 * @param entries the feed per principle; principles not named are not eaten
 */
public record VeinFeed(Map<Principle, Entry> entries) {

    /** A vein whose gnome eats nothing. */
    public static final VeinFeed NONE = new VeinFeed(Map.of());

    /**
     * One principle of the feed.
     *
     * @param amount mB one dug item uses, at least 1
     * @param bonus  what this principle adds to the speed factor while fed, at least 0
     */
    public record Entry(int amount, double bonus) {

        public Entry {
            if (amount < 1 || bonus < 0) {
                throw new IllegalArgumentException("Feed amount must be at least 1 and bonus at least 0");
            }
        }
    }

    public VeinFeed {
        entries = entries.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(entries));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Whether the feed names {@code principle}. */
    public boolean eats(Principle principle) {
        return entries.containsKey(principle);
    }

    /** The mB of each principle one dug item uses when all are fed. */
    public Principles amounts() {
        Principles amounts = Principles.NONE;
        for (Map.Entry<Principle, Entry> entry : entries.entrySet()) {
            amounts = amounts.with(entry.getKey(), entry.getValue().amount());
        }
        return amounts;
    }

    /** Whether {@code principle} is in the feed and {@code held} has at least its amount. */
    public boolean fed(Principle principle, Principles held) {
        Entry entry = entries.get(principle);
        return entry != null && held.get(principle) >= entry.amount();
    }

    /** How many times faster the gnome digs with {@code held}: 1 plus the bonuses of the fed principles. */
    public double factor(Principles held) {
        double factor = 1;
        for (Map.Entry<Principle, Entry> entry : entries.entrySet()) {
            if (fed(entry.getKey(), held)) {
                factor += entry.getValue().bonus();
            }
        }
        return factor;
    }

    /** What one dug item uses with {@code held}: the amount of every fed principle, nothing of the others. */
    public Principles cost(Principles held) {
        Principles cost = Principles.NONE;
        for (Map.Entry<Principle, Entry> entry : entries.entrySet()) {
            if (fed(entry.getKey(), held)) {
                cost = cost.with(entry.getKey(), entry.getValue().amount());
            }
        }
        return cost;
    }
}
