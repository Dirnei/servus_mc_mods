package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.Placed;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Phases;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Sounds;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Turn;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualUndo;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualUndo.AthanorAt;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualUndo.Step;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class RitualUndoTest {

    private static final String VAT = "servus_alchemy:principle_vat";
    private static final String OBSIDIAN = "minecraft:obsidian";
    private static final String OBELISK = "servus_alchemy:rubedo_obelisk";
    private static final Phases PHASES = new Phases(36, 60, 80, 150);

    private static final AthanorRitual CITRINITAS = ritual("citrinitas", 1, List.of(
            new Turn(BlockAt.of(0, -1, 3, VAT, Map.of("crystal", "none", "formed", "false")), BlockAt.of(0, -1, 3, VAT, Map.of("crystal", "citrine")))));

    private static final AthanorRitual RUBEDO = ritual("rubedo", 2, List.of(
            new Turn(new BlockAt(1, -1, 2, OBSIDIAN), BlockAt.of(1, -1, 2, OBELISK, Map.of("half", "lower"))),
            new Turn(new BlockAt(0, -1, 3, VAT, Map.of("crystal", List.of("citrine", "rubedo"))), BlockAt.of(0, -1, 3, VAT, Map.of("crystal", "rubedo")))));

    private static AthanorRitual ritual(String name, int tier, List<Turn> turns) {
        return new AthanorRitual(name, "minecraft:stick", tier, 0, 0, 160, PHASES, Sounds.NONE, turns);
    }

    /** A Rubedo build: orange vat behind, obelisk at its pillar cell. */
    private static Function<BlockAt, Placed> rubedoWorld() {
        Map<String, Placed> world = new HashMap<>();
        world.put("0,-1,3", Placed.of(VAT, Map.of("crystal", "rubedo", "formed", "false", "facing", "north")));
        world.put("1,-1,2", Placed.of(OBELISK, Map.of("half", "lower")));
        return block -> world.get(block.dx() + "," + block.dy() + "," + block.dz());
    }

    @Test
    void undoGoesHighestFirst() {
        List<Step> steps = RitualUndo.plan(List.of(CITRINITAS, RUBEDO), 1, Facing.NORTH, rubedoWorld());
        assertEquals(List.of(OBSIDIAN, VAT, VAT), steps.stream().map(step -> step.back().block()).toList(),
                "Rubedo turns back first, then Citrinitas sees the citrine it left");
        assertEquals("citrine", steps.get(1).back().state().get("crystal"));
        assertEquals("none", steps.get(2).back().state().get("crystal"));
    }

    @Test
    void undoUsesTheFirstFromState() {
        List<Step> steps = RitualUndo.plan(List.of(CITRINITAS, RUBEDO), 2, Facing.NORTH, rubedoWorld());
        assertEquals(2, steps.size(), "only Rubedo is undone at Citrinitas");
        assertEquals(Map.of("crystal", "citrine"), steps.get(1).back().state(), "an orange vat turns back to the first accepted state");
    }

    @Test
    void nothingUndoneWhenTheTierHolds() {
        assertTrue(RitualUndo.plan(List.of(CITRINITAS, RUBEDO), 3, Facing.NORTH, rubedoWorld()).isEmpty(), "Rubedo holds");
        Map<String, Placed> citrinitas = Map.of("0,-1,3", Placed.of(VAT, Map.of("crystal", "citrine", "formed", "false")),
                "1,-1,2", Placed.of(OBSIDIAN, Map.of()));
        assertTrue(RitualUndo.plan(List.of(CITRINITAS, RUBEDO), 2, Facing.NORTH,
                block -> citrinitas.get(block.dx() + "," + block.dy() + "," + block.dz())).isEmpty(), "nothing of Rubedo stands");
    }

    @Test
    void unformedUndoesEveryRitual() {
        List<Step> steps = RitualUndo.plan(List.of(CITRINITAS, RUBEDO), RitualUndo.UNFORMED, Facing.NORTH, rubedoWorld());
        assertEquals(3, steps.size());
    }

    @Test
    void athanorIsFoundFromAGrownBlock() {
        List<AthanorAt> found = RitualUndo.athanorsOf(List.of(CITRINITAS, RUBEDO), Placed.of(OBELISK, Map.of("half", "lower")));
        assertEquals(4, found.size(), "one Athanor place per front");
        assertTrue(found.contains(new AthanorAt(Facing.NORTH, -1, 1, -2)), "a north Athanor stands back from its obelisk");
        assertTrue(RitualUndo.athanorsOf(List.of(CITRINITAS, RUBEDO), Placed.of(OBSIDIAN, Map.of())).isEmpty(), "plain blocks belong to none");
    }
}
