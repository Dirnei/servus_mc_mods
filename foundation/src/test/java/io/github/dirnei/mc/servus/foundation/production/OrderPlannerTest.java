package io.github.dirnei.mc.servus.foundation.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.production.OrderPlanner.Plan;
import io.github.dirnei.mc.servus.foundation.production.OrderPlanner.Recipe;
import io.github.dirnei.mc.servus.foundation.production.OrderPlanner.Step;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderPlannerTest {

    private static final Recipe<String> GEAR = new Recipe<>("gear", Map.of("ingot", 2), Map.of("gear", 1), 20);
    private static final Recipe<String> WIRE = new Recipe<>("wire", Map.of("copper", 1), Map.of("wire", 2), 10);
    private static final Recipe<String> PACK = new Recipe<>("pack", ordered("gear", 1, "wire", 2), Map.of("pack", 1), 100);
    private static final List<Recipe<String>> ALL = List.of(PACK, GEAR, WIRE);

    /** Map.of shuffles its order per JVM run; recipes in the game come as ordered lists, so the tests use ordered maps too. */
    private static Map<String, Integer> ordered(String keyA, int countA, String keyB, int countB) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(keyA, countA);
        map.put(keyB, countB);
        return map;
    }

    @Test
    void inStockNeedsNoSteps() {
        Plan<String> plan = OrderPlanner.plan("gear", 10, Map.of("gear", 12L), ALL, 8);
        assertTrue(plan.steps().isEmpty());
        assertEquals(Map.of("gear", 10), plan.reserved());
        assertTrue(plan.complete());
    }

    @Test
    void oneLevel() {
        Plan<String> plan = OrderPlanner.plan("gear", 3, Map.of("ingot", 10L), ALL, 8);
        assertEquals(List.of(new Step<>(GEAR, 3)), plan.steps());
        assertEquals(Map.of("ingot", 6), plan.reserved());
        assertTrue(plan.complete());
    }

    @Test
    void twoLevelsLeavesFirst() {
        Plan<String> plan = OrderPlanner.plan("pack", 1, Map.of("ingot", 10L, "copper", 10L), ALL, 8);
        assertEquals(List.of(new Step<>(GEAR, 1), new Step<>(WIRE, 1), new Step<>(PACK, 1)), plan.steps());
        assertEquals(Map.of("ingot", 2, "copper", 1), plan.reserved());
        assertTrue(plan.complete());
        assertEquals(130, plan.ticks());
    }

    @Test
    void surplusIsReused() {
        Plan<String> plan = OrderPlanner.plan("wire", 3, Map.of("copper", 10L), ALL, 8);
        assertEquals(List.of(new Step<>(WIRE, 2)), plan.steps());
        assertTrue(plan.complete());
        // Two packs need four wire: two batches of wire, no third.
        Plan<String> packs = OrderPlanner.plan("pack", 2, Map.of("ingot", 10L, "copper", 10L), ALL, 8);
        assertEquals(List.of(new Step<>(GEAR, 2), new Step<>(WIRE, 2), new Step<>(PACK, 2)), packs.steps());
    }

    @Test
    void missingRawMaterialIsReported() {
        Plan<String> plan = OrderPlanner.plan("gear", 1, Map.of(), ALL, 8);
        assertEquals(List.of(new Step<>(GEAR, 1)), plan.steps());
        assertEquals(Map.of("ingot", 2), plan.missing());
        assertFalse(plan.complete());
    }

    @Test
    void cycleStopsAtTheDepthLimit() {
        Recipe<String> a = new Recipe<>("a", Map.of("b", 1), Map.of("a", 1), 1);
        Recipe<String> b = new Recipe<>("b", Map.of("a", 1), Map.of("b", 1), 1);
        Plan<String> plan = OrderPlanner.plan("a", 1, Map.of(), List.of(a, b), 3);
        assertFalse(plan.complete());
        assertEquals(1, plan.missing().size());
        assertTrue(plan.steps().size() <= 3);
    }

    @Test
    void repeatedNeedsMergeIntoOneStep() {
        Recipe<String> both = new Recipe<>("both", Map.of("ingot", 1), ordered("gear", 1, "wire", 1), 5);
        Plan<String> plan = OrderPlanner.plan("pack", 1, Map.of("ingot", 10L), List.of(PACK, both), 8);
        // One gear and two wire from a recipe making one of each: two batches, one surplus gear.
        assertEquals(List.of(new Step<>(both, 2), new Step<>(PACK, 1)), plan.steps());
        assertEquals(Map.of("ingot", 2), plan.reserved());
        assertTrue(plan.complete());
    }

    private static Recipe<String> charcoal(String log) {
        return new Recipe<>(log, Map.of(log, 1), Map.of("charcoal", 1), 200);
    }

    private static final Recipe<String> ACACIA = charcoal("acacia_log");
    private static final Recipe<String> OAK = charcoal("oak_log");
    private static final Recipe<String> BIRCH = charcoal("birch_log");
    private static final List<Recipe<String>> CHARCOAL = List.of(ACACIA, OAK, BIRCH);

    @Test
    void stockedAlternativeIsUsed() {
        Plan<String> plan = OrderPlanner.plan("charcoal", 4, Map.of("oak_log", 10L), CHARCOAL, 8);
        assertEquals(List.of(new Step<>(OAK, 4)), plan.steps());
        assertEquals(Map.of("oak_log", 4), plan.reserved());
        assertTrue(plan.complete());
    }

    @Test
    void splitsAcrossAlternatives() {
        Plan<String> plan = OrderPlanner.plan("charcoal", 10, Map.of("oak_log", 6L, "birch_log", 4L), CHARCOAL, 8);
        assertEquals(List.of(new Step<>(OAK, 6), new Step<>(BIRCH, 4)), plan.steps());
        assertEquals(Map.of("oak_log", 6, "birch_log", 4), plan.reserved());
        assertTrue(plan.complete());
    }

    @Test
    void producesInputsOfAnAlternative() {
        Recipe<String> fromBamboo = new Recipe<>("bamboo_sticks", Map.of("bamboo", 2), Map.of("stick", 1), 10);
        Recipe<String> fromPlanks = new Recipe<>("plank_sticks", Map.of("planks", 2), Map.of("stick", 4), 10);
        Recipe<String> planks = new Recipe<>("planks", Map.of("log", 1), Map.of("planks", 4), 10);
        Plan<String> plan = OrderPlanner.plan("stick", 2, Map.of("log", 5L),
                List.of(fromBamboo, fromPlanks, planks), 8);
        assertEquals(List.of(new Step<>(planks, 1), new Step<>(fromPlanks, 1)), plan.steps());
        assertTrue(plan.complete());
    }

    @Test
    void missingFollowsTheBestAlternative() {
        Plan<String> plan = OrderPlanner.plan("charcoal", 4, Map.of("birch_log", 3L), CHARCOAL, 8);
        assertEquals(List.of(new Step<>(BIRCH, 4)), plan.steps());
        assertEquals(Map.of("birch_log", 1), plan.missing());
        assertFalse(plan.complete());
    }

    @Test
    void tieGoesByRecipeOrder() {
        Plan<String> plan = OrderPlanner.plan("charcoal", 2, Map.of("acacia_log", 5L, "oak_log", 5L), CHARCOAL, 8);
        assertEquals(List.of(new Step<>(ACACIA, 2)), plan.steps());
        Plan<String> none = OrderPlanner.plan("charcoal", 2, Map.of(), CHARCOAL, 8);
        assertEquals(Map.of("acacia_log", 2), none.missing());
    }

    @Test
    void stepsStayLeavesFirstWhenAnAlternativeIsTopped() {
        Recipe<String> fromPlanks = new Recipe<>("plank_sticks", Map.of("planks", 2), Map.of("stick", 4), 10);
        Recipe<String> fromBamboo = new Recipe<>("bamboo_sticks", Map.of("bamboo", 2), Map.of("stick", 1), 10);
        Recipe<String> planks = new Recipe<>("planks", Map.of("log", 1), Map.of("planks", 4), 10);
        Plan<String> plan = OrderPlanner.plan("stick", 8, Map.of("planks", 2L, "log", 1L),
                List.of(fromBamboo, fromPlanks, planks), 8);
        assertEquals(List.of(new Step<>(planks, 1), new Step<>(fromPlanks, 2)), plan.steps());
        assertTrue(plan.complete());
    }
}
