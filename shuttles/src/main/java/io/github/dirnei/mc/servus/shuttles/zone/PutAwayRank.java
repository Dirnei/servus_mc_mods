package io.github.dirnei.mc.servus.shuttles.zone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orders the bays a put-away may go to. Within the zone tiers (exact item,
 * tag, general) the bays already holding the item come first, a bay with a
 * partial stack of it before the others, then the cheapest; every other bay
 * follows by most free slots, counted in buckets, then the cheapest. The
 * order in which bays joined only breaks exact ties. Pure Java.
 */
public final class PutAwayRank {

    private PutAwayRank() {
    }

    /**
     * One bay that might take the item.
     *
     * @param id        the bay
     * @param tier      0 exact item zone, 1 tag zone, 2 general storage
     * @param holds     whether it holds the item already
     * @param partial   whether an unlocked slot holds a partial stack of it
     * @param freeSlots unlocked empty slots
     * @param slots     all slots of the bay
     * @param room      how many of the item its unlocked slots take
     * @param cost      the travel cost to it, infinite when unreachable
     * @param joinOrder the order in which it joined the warehouse
     */
    public record Bay<S>(S id, int tier, boolean holds, boolean partial, int freeSlots, int slots, int room, double cost, int joinOrder) {
    }

    /** The bays with room in put-away order; {@code bucketStep} is the share of a bay's slots one bucket spans. */
    public static <S> List<Bay<S>> order(List<Bay<S>> bays, double bucketStep) {
        List<Bay<S>> ranked = new ArrayList<>();
        for (Bay<S> bay : bays) {
            if (bay.room() > 0) {
                ranked.add(bay);
            }
        }
        ranked.sort(Comparator.<Bay<S>>comparingInt(Bay::tier)
                .thenComparing(bay -> !bay.holds())
                .thenComparing(bay -> bay.holds() && !bay.partial())
                .thenComparingInt(bay -> bay.holds() ? 0 : -bucket(bay.freeSlots(), bay.slots(), bucketStep))
                .thenComparingDouble(Bay::cost)
                .thenComparingInt(Bay::joinOrder));
        return ranked;
    }

    /** The bucket of a bay's free slots: free slots divided by the slots one step spans, so more free slots give a higher bucket. */
    public static int bucket(int freeSlots, int slots, double bucketStep) {
        if (slots <= 0 || freeSlots <= 0) {
            return 0;
        }
        int size = Math.max(1, (int) Math.ceil(slots * Math.max(0.0001, Math.min(1.0, bucketStep))));
        return freeSlots / size;
    }
}
