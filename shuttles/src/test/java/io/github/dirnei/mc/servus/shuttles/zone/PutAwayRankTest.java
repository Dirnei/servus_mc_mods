package io.github.dirnei.mc.servus.shuttles.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.zone.PutAwayRank.Bay;
import java.util.List;
import org.junit.jupiter.api.Test;

class PutAwayRankTest {

    private static final double QUARTERS = 0.25;

    private static Bay<String> bay(String id, int tier, boolean holds, boolean partial, int free, double cost, int order) {
        return new Bay<>(id, tier, holds, partial, free, 27, free > 0 || partial ? 64 : 0, cost, order);
    }

    private static List<String> ids(List<Bay<String>> bays) {
        return bays.stream().map(Bay::id).toList();
    }

    @Test
    void tierBeatsEverything() {
        List<Bay<String>> bays = List.of(bay("general", 2, true, true, 27, 1, 0), bay("exact", 0, false, false, 1, 99, 1));
        assertEquals(List.of("exact", "general"), ids(PutAwayRank.order(bays, QUARTERS)));
    }

    @Test
    void holderBeatsEmptierBay() {
        List<Bay<String>> bays = List.of(bay("empty", 2, false, false, 27, 1, 0), bay("holder", 2, true, false, 1, 50, 1));
        assertEquals(List.of("holder", "empty"), ids(PutAwayRank.order(bays, QUARTERS)));
    }

    @Test
    void partialStackBeatsCheaperHolder() {
        List<Bay<String>> bays = List.of(bay("near", 2, true, false, 5, 3, 0), bay("partial", 2, true, true, 5, 30, 1));
        assertEquals(List.of("partial", "near"), ids(PutAwayRank.order(bays, QUARTERS)));
    }

    @Test
    void bucketBeatsCost() {
        List<Bay<String>> bays = List.of(bay("nearFull", 2, false, false, 2, 1, 0), bay("farEmpty", 2, false, false, 20, 40, 1));
        assertEquals(List.of("farEmpty", "nearFull"), ids(PutAwayRank.order(bays, QUARTERS)));
    }

    @Test
    void costWithinABucket() {
        List<Bay<String>> bays = List.of(bay("far", 2, false, false, 27, 9, 0), bay("near", 2, false, false, 27, 4, 1));
        assertEquals(List.of("near", "far"), ids(PutAwayRank.order(bays, QUARTERS)));
    }

    @Test
    void joinOrderOnExactTies() {
        List<Bay<String>> bays = List.of(bay("second", 2, false, false, 27, 4, 1), bay("first", 2, false, false, 27, 4, 0));
        assertEquals(List.of("first", "second"), ids(PutAwayRank.order(bays, QUARTERS)));
    }

    @Test
    void baysWithoutRoomAreLeftOut() {
        List<Bay<String>> bays = List.of(bay("full", 2, true, false, 0, 1, 0), bay("free", 2, false, false, 3, 9, 1));
        assertEquals(List.of("free"), ids(PutAwayRank.order(bays, QUARTERS)));
    }

    @Test
    void quarterBucketsOfTwentySevenSlots() {
        assertEquals(0, PutAwayRank.bucket(6, 27, QUARTERS));
        assertEquals(1, PutAwayRank.bucket(7, 27, QUARTERS));
        assertEquals(1, PutAwayRank.bucket(13, 27, QUARTERS));
        assertEquals(2, PutAwayRank.bucket(14, 27, QUARTERS));
        assertEquals(3, PutAwayRank.bucket(27, 27, QUARTERS));
        assertEquals(0, PutAwayRank.bucket(0, 27, QUARTERS));
    }
}
