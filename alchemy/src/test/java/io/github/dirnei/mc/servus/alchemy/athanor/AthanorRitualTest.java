package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.Placed;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Phases;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Turn;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualPlan;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualPlan.Outcome;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class AthanorRitualTest {

    private static final String VAT = "servus_alchemy:principle_vat";
    private static final Map<String, String> PLAIN = Map.of("crystal", "none", "formed", "false");
    private static final Map<String, String> GROWN = Map.of("crystal", "citrine");
    private static final Phases PHASES = new Phases(36, 60, 80, 150);

    private static Turn vat(int dx, int dz) {
        return new Turn(new BlockAt(dx, -1, dz, VAT, PLAIN), new BlockAt(dx, -1, dz, VAT, GROWN));
    }

    private static final AthanorRitual CITRINITAS = new AthanorRitual("citrinitas", "minecraft:amethyst_block", 1, 0xB070FF, 0xFFB000,
            160, PHASES, List.of(vat(0, 3), vat(-3, 0), vat(3, 0)));

    /** A world holding the blocks {@code placed} with their properties, every vat unformed. */
    private static Function<BlockAt, Placed> world(List<BlockAt> placed) {
        Map<String, Placed> world = new HashMap<>();
        for (BlockAt block : placed) {
            Map<String, String> properties = new HashMap<>(block.properties());
            properties.putIfAbsent("formed", "false");
            world.put(block.dx() + "," + block.dy() + "," + block.dz(), Placed.of(block.block(), properties));
        }
        return block -> world.get(block.dx() + "," + block.dy() + "," + block.dz());
    }

    private static List<BlockAt> plainVats(Facing facing) {
        return CITRINITAS.turns(facing).stream().map(Turn::from).toList();
    }

    @Test
    void turnsAreTurnedWithTheFront() {
        RitualPlan plan = RitualPlan.check(CITRINITAS, 1, Facing.EAST, world(plainVats(Facing.EAST)));
        assertEquals(Outcome.READY, plan.outcome());
        assertEquals(new BlockAt(-3, -1, 0, VAT, GROWN), plan.turns().get(0).to(), "the vat behind an east facing Athanor is west of it");
        assertEquals(Outcome.MISSING, RitualPlan.check(CITRINITAS, 1, Facing.NORTH, world(plainVats(Facing.EAST))).outcome(),
                "vats placed for an east front do not all fit a north front");
    }

    @Test
    void firstMissingTurnIsNamed() {
        List<BlockAt> placed = new ArrayList<>(plainVats(Facing.NORTH));
        placed.remove(0);
        RitualPlan plan = RitualPlan.check(CITRINITAS, 1, Facing.NORTH, world(placed));
        assertEquals(Outcome.MISSING, plan.outcome());
        assertEquals(new BlockAt(0, -1, 3, VAT, PLAIN), plan.missing().from(), "the vat behind the back tank is named");
        assertEquals(List.of(), plan.turns());

        placed.add(new BlockAt(0, -1, 3, VAT, GROWN));
        assertEquals(Outcome.MISSING, RitualPlan.check(CITRINITAS, 1, Facing.NORTH, world(placed)).outcome(), "a grown vat is not empty");
    }

    @Test
    void wrongTierRefuses() {
        Function<BlockAt, Placed> vats = world(plainVats(Facing.NORTH));
        assertEquals(Outcome.TIER_TOO_LOW, RitualPlan.check(CITRINITAS, 0, Facing.NORTH, vats).outcome());
        assertEquals(Outcome.TIER_TOO_HIGH, RitualPlan.check(CITRINITAS, 2, Facing.NORTH, vats).outcome());
        assertNull(RitualPlan.check(CITRINITAS, 0, Facing.NORTH, vats).missing());
    }

    @Test
    void theRitualOfTheAthanorsTierIsChosen() {
        AthanorRitual higher = new AthanorRitual("higher", "minecraft:amethyst_block", 2, 0, 0, 160, PHASES, List.of());
        List<AthanorRitual> both = List.of(higher, CITRINITAS);
        assertSame(CITRINITAS, RitualPlan.choose(both, 1));
        assertSame(higher, RitualPlan.choose(both, 2));
        assertSame(CITRINITAS, RitualPlan.choose(both, 0), "the nearest above");
        assertSame(higher, RitualPlan.choose(both, 3), "the nearest below");
    }

    @Test
    void phasesMustRiseWithinTheRitual() {
        assertThrows(IllegalArgumentException.class, () -> new AthanorRitual("bad", "x", 1, 0, 0, 100, new Phases(36, 30, 80, 90), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new AthanorRitual("bad", "x", 1, 0, 0, 100, PHASES, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Turn(new BlockAt(0, 0, 0, VAT), new BlockAt(1, 0, 0, VAT)));
    }
}
