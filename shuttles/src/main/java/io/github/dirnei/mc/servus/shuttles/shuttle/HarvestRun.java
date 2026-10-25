package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.shuttles.field.FieldKind;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * The state of one field run of a Harvester or a Lumber shuttle beyond what
 * every field run has: whether it plants at the rail it works, a full cargo,
 * kept seeds or saplings and those it takes from the Field Head's buffer.
 * The decisions are pure ({@code HarvestStep}, {@code PlotWork},
 * {@code TreePlotWork}, {@code SeedPlan}).
 */
final class HarvestRun extends FieldRun {

    /** What the field grew when the run started; the run works only a field of that kind. */
    final FieldKind kind;
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
    /** Whether the field harvests; false while the head's harvest limit is reached. Read from the head at the start and at each rail. */
    boolean harvest = true;
    /** Everything harvested in this run by item id, for the head to learn its produce from at the end. */
    final Map<Identifier, Long> tally = new HashMap<>();

    HarvestRun(BlockPos head, int slots, FieldKind kind) {
        super(head, slots);
        this.kind = kind;
    }
}
