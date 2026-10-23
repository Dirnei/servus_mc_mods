package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.List;

/**
 * A ritual of the Athanor: offering {@link #offering} to an Athanor of tier {@link #tier} turns the blocks of
 * {@link #turns} around it into others, in an animation of {@link #duration} ticks. Pure Java.
 *
 * @param name        the file name, such as {@code citrinitas}
 * @param offering    item id the player uses on the Athanor
 * @param tier        the tier the Athanor must have
 * @param color       RGB of the offering's glow, the flame during the ritual and the streams as they leave
 * @param resultColor RGB the streams turn into and the grown blocks glow in
 * @param duration    ticks from the offering to the turn
 * @param phases      when the parts of the animation start
 * @param turns       the blocks to turn, at offsets for an Athanor facing north
 */
public record AthanorRitual(String name, String offering, int tier, int color, int resultColor, int duration, Phases phases,
                            List<Turn> turns) {

    public AthanorRitual {
        if (!(0 < phases.burst() && phases.burst() < phases.fill() && phases.fill() < phases.grow()
                && phases.grow() < phases.settle() && phases.settle() < duration)) {
            throw new IllegalArgumentException("Ritual phases must rise and end before the ritual does: " + name);
        }
        turns = List.copyOf(turns);
    }

    /**
     * Ticks after the offering at which each part starts: the offering bursts into streams, the streams reach their blocks
     * and fill them, the blocks grow, and the final flash.
     */
    public record Phases(int burst, int fill, int grow, int settle) {
    }

    /** A block that must stand at {@code from}'s offset and that becomes {@code to} there. */
    public record Turn(BlockAt from, BlockAt to) {

        public Turn {
            if (from.dx() != to.dx() || from.dy() != to.dy() || from.dz() != to.dz()) {
                throw new IllegalArgumentException("A turn stays in its place: " + from + " to " + to);
            }
        }

        /** This turn for an Athanor facing {@code facing}. */
        public Turn turned(Facing facing) {
            return new Turn(AthanorTiers.turn(from, facing), AthanorTiers.turn(to, facing));
        }
    }

    /** The turns for an Athanor facing {@code facing}, in their order. */
    public List<Turn> turns(Facing facing) {
        return turns.stream().map(turn -> turn.turned(facing)).toList();
    }
}
