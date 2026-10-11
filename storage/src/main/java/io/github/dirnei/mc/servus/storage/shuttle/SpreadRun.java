package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;

/**
 * The state of one Spreader's fertilize run beyond what every field run has:
 * the fertilizer it doses with, the doses per plot, and the fertilizer it
 * still picks from storage before it drives to the field. The amounts are
 * pure ({@code FertilizePlan}).
 */
final class SpreadRun extends FieldRun {

    final ItemKey fertilizer;
    final int doses;
    /** Fertilizer still to pick from storage; -1 until the run planned it. */
    int toFetch = -1;
    /** The fetch is over, whatever it brought. */
    boolean fetched;
    /** Storage blocks the run already took fertilizer from or drove to. */
    final Set<BlockPos> sourcesTried = new HashSet<>();
    /** The storage block the Spreader drives to for fertilizer. */
    BlockPos source;

    SpreadRun(BlockPos head, int slots, ItemKey fertilizer, int doses) {
        super(head, slots);
        this.fertilizer = fertilizer;
        this.doses = Math.max(1, doses);
    }
}
