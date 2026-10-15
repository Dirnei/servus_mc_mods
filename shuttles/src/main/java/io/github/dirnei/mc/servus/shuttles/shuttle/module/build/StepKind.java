package io.github.dirnei.mc.servus.shuttles.shuttle.module.build;

import java.util.Locale;
import java.util.Optional;

/**
 * The kinds of step a build by hand is made of, each with the range its
 * count is clamped to. Pure Java.
 */
public enum StepKind {
    /** Click glowing spots in order; the count is the number of bolts. */
    BOLTS(1, 8),
    /** Connect contacts of the same colour; the count is the number of wires, one per duct colour. */
    WIRES(1, 8),
    /** Drag along a seam; the count is the number of seams. */
    WELD(1, 3);

    private final int min;
    private final int max;

    StepKind(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int clamp(int count) {
        return Math.max(min, Math.min(max, count));
    }

    /** The name used in {@code shuttle.json} and in lang keys. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The kind with that id, or empty for an unknown one. */
    public static Optional<StepKind> byId(String id) {
        for (StepKind kind : values()) {
            if (kind.id().equals(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
