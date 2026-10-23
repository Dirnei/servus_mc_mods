package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.Placed;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Phases;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Sounds;
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
        return new Turn(BlockAt.of(dx, -1, dz, VAT, PLAIN), BlockAt.of(dx, -1, dz, VAT, GROWN));
    }

    private static final AthanorRitual CITRINITAS = new AthanorRitual("citrinitas", "minecraft:amethyst_block", 1, 0xB070FF, 0xFFB000,
            160, PHASES, Sounds.NONE, List.of(vat(0, 3), vat(-3, 0), vat(3, 0)));

    /** A world holding the blocks {@code placed} with their properties, every vat unformed. */
    private static Function<BlockAt, Placed> world(List<BlockAt> placed) {
        Map<String, Placed> world = new HashMap<>();
        for (BlockAt block : placed) {
            Map<String, String> properties = new HashMap<>(block.state());
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
        assertEquals(BlockAt.of(-3, -1, 0, VAT, GROWN), plan.turns().get(0).to(), "the vat behind an east facing Athanor is west of it");
        assertEquals(Outcome.MISSING, RitualPlan.check(CITRINITAS, 1, Facing.NORTH, world(plainVats(Facing.EAST))).outcome(),
                "vats placed for an east front do not all fit a north front");
    }

    @Test
    void firstMissingTurnIsNamed() {
        List<BlockAt> placed = new ArrayList<>(plainVats(Facing.NORTH));
        placed.remove(0);
        RitualPlan plan = RitualPlan.check(CITRINITAS, 1, Facing.NORTH, world(placed));
        assertEquals(Outcome.MISSING, plan.outcome());
        assertEquals(BlockAt.of(0, -1, 3, VAT, PLAIN), plan.missing().from(), "the vat behind the back tank is named");
        assertEquals(List.of(), plan.turns());

        placed.add(BlockAt.of(0, -1, 3, VAT, Map.of("crystal", "none", "formed", "true")));
        assertEquals(Outcome.MISSING, RitualPlan.check(CITRINITAS, 1, Facing.NORTH, world(placed)).outcome(), "a formed vat does not turn");
    }

    @Test
    void turnedBlocksCountAsInPlace() {
        List<BlockAt> placed = new ArrayList<>(plainVats(Facing.NORTH));
        placed.set(0, BlockAt.of(0, -1, 3, VAT, GROWN));
        RitualPlan plan = RitualPlan.check(CITRINITAS, 1, Facing.NORTH, world(placed));
        assertEquals(Outcome.READY, plan.outcome(), "a vat grown before stays in the ritual");
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
        AthanorRitual higher = new AthanorRitual("higher", "minecraft:amethyst_block", 2, 0, 0, 160, PHASES, Sounds.NONE, List.of());
        List<AthanorRitual> both = List.of(higher, CITRINITAS);
        assertSame(CITRINITAS, RitualPlan.choose(both, 1));
        assertSame(higher, RitualPlan.choose(both, 2));
        assertSame(CITRINITAS, RitualPlan.choose(both, 0), "the nearest above");
        assertSame(higher, RitualPlan.choose(both, 3), "the nearest below");
    }

    private static final String OBSIDIAN = "minecraft:obsidian";
    private static final String OBELISK = "servus_alchemy:rubedo_obelisk";
    private static final String AIR = "minecraft:air";

    /** Obsidian behind the left tank turns into an obelisk's lower half, the air above it into its upper half. */
    private static final AthanorRitual OBELISK_RITUAL = new AthanorRitual("rubedo", "minecraft:ghast_tear", 2, 0, 0, 160, PHASES,
            Sounds.NONE, List.of(
                    new Turn(new BlockAt(2, -1, 1, OBSIDIAN), BlockAt.of(2, -1, 1, OBELISK, Map.of("half", "lower"))),
                    new Turn(new BlockAt(2, 0, 1, AIR), BlockAt.of(2, 0, 1, OBELISK, Map.of("half", "upper")), false)));

    @Test
    void airAboveIsATurn() {
        Function<BlockAt, Placed> free = world(List.of(new BlockAt(2, -1, 1, OBSIDIAN), new BlockAt(2, 0, 1, AIR)));
        RitualPlan plan = RitualPlan.check(OBELISK_RITUAL, 2, Facing.NORTH, free);
        assertEquals(Outcome.READY, plan.outcome());
        assertEquals(BlockAt.of(2, 0, 1, OBELISK, Map.of("half", "upper")), plan.turns().get(1).to());
        assertFalse(plan.turns().get(1).stream(), "no stream flies to the space above");

        Function<BlockAt, Placed> blocked = world(List.of(new BlockAt(2, -1, 1, OBSIDIAN), new BlockAt(2, 0, 1, "minecraft:torch")));
        RitualPlan refused = RitualPlan.check(OBELISK_RITUAL, 2, Facing.NORTH, blocked);
        assertEquals(Outcome.MISSING, refused.outcome());
        assertEquals(new BlockAt(2, 0, 1, AIR), refused.missing().from(), "the space above is named");
    }

    @Test
    void orangeVatsTurnAgain() {
        Map<String, List<String>> citrineOrOrange = Map.of("crystal", List.of("citrine", "rubedo"));
        AthanorRitual rubedo = new AthanorRitual("rubedo", "minecraft:ghast_tear", 2, 0, 0, 160, PHASES, Sounds.NONE,
                List.of(new Turn(new BlockAt(0, -1, 3, VAT, citrineOrOrange), BlockAt.of(0, -1, 3, VAT, Map.of("crystal", "rubedo")), false)));
        for (String crystal : List.of("citrine", "rubedo")) {
            Function<BlockAt, Placed> vats = world(List.of(BlockAt.of(0, -1, 3, VAT, Map.of("crystal", crystal))));
            assertEquals(Outcome.READY, RitualPlan.check(rubedo, 2, Facing.NORTH, vats).outcome(), crystal + " turns");
        }
        Function<BlockAt, Placed> plain = world(List.of(BlockAt.of(0, -1, 3, VAT, Map.of("crystal", "none"))));
        assertEquals(Outcome.MISSING, RitualPlan.check(rubedo, 2, Facing.NORTH, plain).outcome(), "a plain vat does not");
    }

    @Test
    void phasesMustRiseWithinTheRitual() {
        assertThrows(IllegalArgumentException.class, () -> new AthanorRitual("bad", "x", 1, 0, 0, 100, new Phases(36, 30, 80, 90), Sounds.NONE, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new AthanorRitual("bad", "x", 1, 0, 0, 100, PHASES, Sounds.NONE, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Turn(new BlockAt(0, 0, 0, VAT), new BlockAt(1, 0, 0, VAT)));
    }
}
