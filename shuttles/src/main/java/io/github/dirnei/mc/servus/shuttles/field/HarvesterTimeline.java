package io.github.dirnei.mc.servus.shuttles.field;

import java.util.List;

/**
 * Where a Lumber shuttle's harvester is in its motion: each felling part is
 * split into stages, each a share of the part. Unfolding opens the doors,
 * folds the walls down and raises the crane; sawing reaches out, moves in,
 * grips and cuts; toppling lets go, waits while the tree falls and breaks
 * apart and swings home; folding is unfolding backwards. A shuttle that is
 * unfolded between two trees stands at the end of unfolding. Pure Java.
 */
public final class HarvesterTimeline {

    /** The percent of the topple part the fall and the break apart take; the head swings home in the rest. */
    public static final int FALL_PERCENT = 80;

    /** The felling parts, as the shuttle's view names them. */
    public enum Part { UNFOLD, READY, SAW, TOPPLE, FOLD }

    /** The stages, in the order they run. */
    public enum Stage { DOORS, WALLS, RISE, REACH, MOVE_IN, GRIP, CUT, LET_GO, WAIT, HOME }

    /** A stage and how far into it, from 0 to 1. */
    public record Moment(Stage stage, double progress) {
    }

    /** A stage's share of its part, from {@code from} to {@code to}. */
    public record Span(Stage stage, double from, double to) {
    }

    private static final List<Span> UNFOLD = List.of(
            new Span(Stage.DOORS, 0, 0.3), new Span(Stage.WALLS, 0.3, 0.6), new Span(Stage.RISE, 0.6, 1));
    private static final List<Span> SAW = List.of(
            new Span(Stage.REACH, 0, 0.35), new Span(Stage.MOVE_IN, 0.35, 0.5), new Span(Stage.GRIP, 0.5, 0.62),
            new Span(Stage.CUT, 0.62, 1));
    private static final List<Span> TOPPLE = List.of(
            new Span(Stage.LET_GO, 0, 0.1), new Span(Stage.WAIT, 0.1, FALL_PERCENT / 100.0),
            new Span(Stage.HOME, FALL_PERCENT / 100.0, 1));

    private HarvesterTimeline() {
    }

    /** The stages of a part with their shares; folding runs the unfold stages backwards. */
    public static List<Span> spans(Part part) {
        return switch (part) {
            case UNFOLD, READY, FOLD -> UNFOLD;
            case SAW -> SAW;
            case TOPPLE -> TOPPLE;
        };
    }

    /** The moment at the part's progress, from 0 to 1. */
    public static Moment of(Part part, double progress) {
        double p = Math.clamp(progress, 0, 1);
        if (part == Part.READY) {
            p = 1;
        } else if (part == Part.FOLD) {
            p = 1 - p;
        }
        for (Span span : spans(part)) {
            if (p < span.to()) {
                return new Moment(span.stage(), (p - span.from()) / (span.to() - span.from()));
            }
        }
        List<Span> spans = spans(part);
        return new Moment(spans.getLast().stage(), 1);
    }
}
