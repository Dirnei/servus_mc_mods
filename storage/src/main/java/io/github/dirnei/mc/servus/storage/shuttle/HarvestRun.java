package io.github.dirnei.mc.servus.storage.shuttle;

import net.minecraft.core.BlockPos;

/**
 * The state of one harvester's field run beyond what every field run has:
 * whether it plants at the rail it works, a full cargo, kept seeds and the
 * seeds it takes from the Field Head's buffer. The decisions are pure
 * ({@code HarvestStep}, {@code PlotWork}, {@code SeedPlan}).
 */
final class HarvestRun extends FieldRun {

    /** Whether the rail the harvester works now gets tilled and planted, fixed when it got there. */
    boolean stopPlants;
    /** The last harvest did not fit. */
    boolean cargoFull;
    /** The cargo filled up with seeds kept for planting: the next put-away keeps none. */
    boolean keepNothing;
    /** Something was done in the current work segment. */
    boolean worked;
    /** A work segment did nothing: the run has no more work it can do. */
    boolean idle;
    /** Seeds were tried from the head's buffer since the last work segment. */
    boolean fetchTried;
    /** The seeds the harvester takes from the head's buffer. */
    int seedAmount;

    HarvestRun(BlockPos head, int slots) {
        super(head, slots);
    }
}
