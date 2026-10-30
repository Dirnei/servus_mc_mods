package io.github.dirnei.mc.servus.shuttles.network;

import java.util.List;

/**
 * What a relocation of items onto a rack shelf did.
 *
 * @param outcome whether shuttles started, or why nothing moved
 * @param items   per wanted item what moves, in the order asked; empty when the shelf was refused
 */
public record Relocation(Outcome outcome, List<Item> items) {

    /** Whether shuttles started, or why nothing moves. */
    public enum Outcome {
        /** Shuttles carry at least one load onto the shelf. */
        STARTED,
        /** The warehouse is offline. */
        OFFLINE,
        /** The shelf is not a Heavy Duty Rack shelf of this warehouse, or it is the source shelf. */
        NOT_A_TARGET,
        /** The shelf cannot take everything that would move, so nothing does. */
        NO_ROOM,
        /** Every item was refused by the zone filter or has no stock on the source shelf. */
        NOTHING_TO_MOVE
    }

    /**
     * One wanted item.
     *
     * @param asked   how many were wanted
     * @param moved   how many shuttles carry onto the shelf
     * @param refused whether the shelf's zone filter refused the item
     */
    public record Item(ItemKey key, int asked, int moved, boolean refused) {

        /** How many of an accepted item the warehouse could not move. */
        public int missing() {
            return refused ? 0 : Math.max(0, asked - moved);
        }
    }

    public Relocation {
        items = List.copyOf(items);
    }

    static Relocation refused(Outcome outcome) {
        return new Relocation(outcome, List.of());
    }

    public boolean started() {
        return outcome == Outcome.STARTED;
    }
}
