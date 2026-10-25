package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Moment;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Part;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Stage;
import java.util.Optional;

/**
 * The sounds of a Lumber shuttle's harvester at a moment of its felling: the
 * chainsaw idles whenever the crane is out, from the moment it starts to rise
 * until it folds in, and each step of unfolding, cutting, letting go and
 * folding calls for one sound of its own. Pure Java.
 */
public final class HarvesterSound {

    /** A sound that plays once at a step of the felling. */
    public enum Cue {
        DOORS_OPEN(false), WALLS_DOWN(false), CRANE_UP(false), CUT(true), STOP(true), CRANE_DOWN(false), WALLS_UP(false), DOORS_SHUT(false);

        private final boolean chainsaw;

        Cue(boolean chainsaw) {
            this.chainsaw = chainsaw;
        }

        /** Whether it is the chainsaw's: it plays once per tree, and the idle gives way while it plays. */
        public boolean chainsaw() {
            return chainsaw;
        }
    }

    /** How far into their stage the walls touch the ground, and before its end the doors meet. */
    private static final double LANDING = 0.85;

    private HarvesterSound() {
    }

    /** Whether the chainsaw idles at this moment, unless one of its own cues is still playing. */
    public static boolean idles(Part part, Moment moment) {
        return switch (part) {
            case UNFOLD -> moment.stage() == Stage.RISE;
            case READY, TOPPLE -> true;
            case SAW -> moment.stage() != Stage.CUT;
            case FOLD -> false;
        };
    }

    /** The cue this moment calls for, if any; the caller plays each once. */
    public static Optional<Cue> cue(Part part, Moment moment) {
        Stage stage = moment.stage();
        Cue cue = switch (part) {
            case UNFOLD -> switch (stage) {
                case DOORS -> Cue.DOORS_OPEN;
                case WALLS -> moment.progress() >= LANDING ? Cue.WALLS_DOWN : null;
                case RISE -> Cue.CRANE_UP;
                default -> null;
            };
            case SAW -> stage == Stage.CUT ? Cue.CUT : null;
            case TOPPLE -> stage == Stage.LET_GO ? Cue.STOP : null;
            // folding runs the unfold stages backwards, each stage's progress from 1 down to 0
            case FOLD -> switch (stage) {
                case RISE -> Cue.CRANE_DOWN;
                case WALLS -> Cue.WALLS_UP;
                case DOORS -> moment.progress() <= 1 - LANDING ? Cue.DOORS_SHUT : null;
                default -> null;
            };
            case READY -> null;
        };
        return Optional.ofNullable(cue);
    }
}
