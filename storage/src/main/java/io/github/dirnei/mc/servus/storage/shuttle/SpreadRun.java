package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import net.minecraft.core.BlockPos;

/**
 * The state of one Spreader's fertilize run beyond what every field run has:
 * the fertilizer it doses with, the doses per plot, and the fertilizer it
 * still takes from the Field Head's buffer before it drives to the plots.
 * The amounts are pure ({@code FertilizePlan}).
 */
final class SpreadRun extends FieldRun {

    final ItemKey fertilizer;
    final int doses;
    /** Fertilizer still to take from the head's buffer; -1 until the run planned it. */
    int toFetch = -1;
    /** The fetch is over, whatever it brought. */
    boolean fetched;

    SpreadRun(BlockPos head, int slots, ItemKey fertilizer, int doses) {
        super(head, slots);
        this.fertilizer = fertilizer;
        this.doses = Math.max(1, doses);
    }
}
