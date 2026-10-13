package io.github.dirnei.mc.servus.storage.shuttle.module.build;

import java.util.List;

/**
 * One step of a build by hand: its kind and how many bolts, wires or seams.
 * The count is clamped to the kind's range. Pure Java.
 */
public record BuildStep(StepKind kind, int count) {

    /** The steps of a build sheet that names none: about twenty seconds of work. */
    public static final List<BuildStep> DEFAULTS = List.of(
            new BuildStep(StepKind.BOLTS, 4),
            new BuildStep(StepKind.WIRES, 4),
            new BuildStep(StepKind.WELD, 1),
            new BuildStep(StepKind.BOLTS, 3));

    public BuildStep {
        count = kind.clamp(count);
    }
}
