package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.Placed;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual.Turn;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** What an Athanor turns back when it falls back to a lower tier, and which Athanor a broken grown block belongs to. Pure Java. */
public final class RitualUndo {

    /** The tier an unformed Athanor counts as: below every ritual's. */
    public static final int UNFORMED = -1;

    /**
     * One block to turn back.
     *
     * @param grown what a ritual made of it; it turns back only while it still is that
     * @param back  what it becomes: the ritual's {@code from} in its first accepted state
     */
    public record Step(BlockAt grown, BlockAt back) {
    }

    /**
     * An Athanor a broken block may belong to.
     *
     * @param facing the Athanor's front
     * @param dx     x offset from the broken block to the Athanor
     * @param dy     y offset
     * @param dz     z offset
     */
    public record AthanorAt(Facing facing, int dx, int dy, int dz) {
    }

    private record Offset(int dx, int dy, int dz) {

        static Offset of(BlockAt block) {
            return new Offset(block.dx(), block.dy(), block.dz());
        }
    }

    private RitualUndo() {
    }

    /**
     * The steps that undo every ritual whose result lies above {@code tier} on an Athanor facing {@code facing}, the highest
     * ritual first and each ritual's turns in their order. Only blocks that still are what the ritual made turn back; a
     * later ritual sees what an earlier step turned back. {@code blockAt} reads what stands at an offset already turned
     * with the front.
     */
    public static List<Step> plan(List<AthanorRitual> rituals, int tier, Facing facing, Function<BlockAt, Placed> blockAt) {
        Map<Offset, Placed> turnedBack = new HashMap<>();
        List<Step> steps = new ArrayList<>();
        List<AthanorRitual> undone = rituals.stream().filter(ritual -> ritual.tier() >= tier)
                .sorted(Comparator.comparingInt(AthanorRitual::tier).reversed()).toList();
        for (AthanorRitual ritual : undone) {
            for (Turn turn : ritual.turns(facing)) {
                Offset offset = Offset.of(turn.to());
                Placed placed = turnedBack.containsKey(offset) ? turnedBack.get(offset) : blockAt.apply(turn.to());
                if (turn.to().matches(placed)) {
                    steps.add(new Step(turn.to(), turn.from()));
                    turnedBack.put(offset, after(placed, turn.from()));
                }
            }
        }
        return steps;
    }

    /** What stands after {@code placed} turned into {@code back}: its other properties stay when the block stays. */
    private static Placed after(Placed placed, BlockAt back) {
        Map<String, String> state = back.state();
        if (!placed.block().equals(back.block())) {
            return Placed.of(back.block(), state);
        }
        return new Placed() {
            @Override
            public String block() {
                return back.block();
            }

            @Override
            public String property(String name) {
                return state.containsKey(name) ? state.get(name) : placed.property(name);
            }
        };
    }

    /**
     * Where an Athanor may stand whose ritual made {@code broken}: for every turn of {@code rituals} whose result it is,
     * and every front, the offset from the broken block to that Athanor.
     */
    public static List<AthanorAt> athanorsOf(List<AthanorRitual> rituals, Placed broken) {
        Set<AthanorAt> found = new LinkedHashSet<>();
        for (Facing facing : Facing.values()) {
            for (AthanorRitual ritual : rituals) {
                for (Turn turn : ritual.turns(facing)) {
                    if (turn.to().matches(broken)) {
                        found.add(new AthanorAt(facing, -turn.to().dx(), -turn.to().dy(), -turn.to().dz()));
                    }
                }
            }
        }
        return List.copyOf(found);
    }
}
