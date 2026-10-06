package io.github.dirnei.mc.servus.storage.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.General;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.Pool;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.Reason;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.Route;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.Rule;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.Zone;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PutAwayPlanTest {

    private static final Function<String, Set<String>> TAGS = item -> switch (item) {
        case "raw_iron", "raw_copper" -> Set.of("c:ores", "c:raw_materials");
        case "bread" -> Set.of("c:foods");
        default -> Set.of();
    };

    private static Rule<String> items(String... items) {
        return new Rule<>(Set.of(items), Set.of(), false);
    }

    private static Rule<String> tags(String... tags) {
        return new Rule<>(Set.of(), Set.of(tags), false);
    }

    private static Rule<String> overflow() {
        return new Rule<>(Set.of(), Set.of(), true);
    }

    private static final Rule<String> EMPTY = new Rule<>(Set.of(), Set.of(), false);

    @Test
    void exactItemBeatsTag() {
        Route route = PutAwayPlan.route("raw_iron", Map.of("ores", tags("c:ores"), "iron", items("raw_iron")), TAGS);
        assertEquals(List.<Pool>of(new Zone("iron")), route.pools());
        assertEquals("iron", route.matched());
    }

    @Test
    void twoTagMatchesPickTheAlphabeticallyFirst() {
        Route route = PutAwayPlan.route("raw_iron", Map.of("raw", tags("c:raw_materials"), "ores", tags("c:ores")), TAGS);
        assertEquals(List.<Pool>of(new Zone("ores")), route.pools());
    }

    @Test
    void noMatchGoesGeneralThenOverflow() {
        Route route = PutAwayPlan.route("bread", Map.of("ores", tags("c:ores"), "spare", overflow()), TAGS);
        assertEquals(List.of(General.INSTANCE, new Zone("spare")), route.pools());
        assertNull(route.matched());
        assertEquals(Reason.OVERFLOW_FULL, route.blocked());
    }

    @Test
    void matchGoesZoneThenOverflow() {
        Route route = PutAwayPlan.route("raw_copper", Map.of("ores", tags("c:ores"), "spare", overflow()), TAGS);
        assertEquals(List.of(new Zone("ores"), new Zone("spare")), route.pools());
        assertEquals(Reason.OVERFLOW_FULL, route.blocked());
    }

    @Test
    void overflowZoneIsNeverPrimaryWithoutItsOwnMatch() {
        Route route = PutAwayPlan.route("bread", Map.of("spare", overflow()), TAGS);
        assertEquals(List.of(General.INSTANCE, new Zone("spare")), route.pools());
    }

    @Test
    void overflowZoneWithMatchingFilterIsPrimaryAndListedOnce() {
        Rule<String> ores = new Rule<>(Set.of(), Set.of("c:ores"), true);
        Route route = PutAwayPlan.route("raw_iron", Map.of("ores", ores), TAGS);
        assertEquals(List.<Pool>of(new Zone("ores")), route.pools());
        assertEquals(Reason.ZONE_FULL_NO_OVERFLOW, route.blocked());
    }

    @Test
    void unfilteredZoneIsNotAPoolOfItsOwn() {
        Route route = PutAwayPlan.route("bread", Map.of("back", EMPTY), TAGS);
        assertEquals(List.<Pool>of(General.INSTANCE), route.pools());
        assertEquals(Reason.NO_STORAGE, route.blocked());
    }

    @Test
    void zoneFullWithoutOverflowReason() {
        Route route = PutAwayPlan.route("raw_iron", Map.of("ores", tags("c:ores")), TAGS);
        assertEquals(List.<Pool>of(new Zone("ores")), route.pools());
        assertEquals(Reason.ZONE_FULL_NO_OVERFLOW, route.blocked());
    }

    @Test
    void generalRuleFlags() {
        assertEquals(true, EMPTY.general());
        assertEquals(false, overflow().general());
        assertEquals(false, tags("c:ores").general());
    }
}
