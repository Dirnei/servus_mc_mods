package io.github.dirnei.mc.servus.shuttles.shuttle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * The state of one supply run beyond what every field run has: the racks it
 * locked items in and still drives to, in order, and where the stop at the
 * head stands. The amounts are pure ({@code SupplyPlan}).
 */
final class SupplyRun extends FieldRun {

    /** The racks the run locked items in and has not taken them from yet, in the order it visits them. */
    final List<BlockPos> sources = new ArrayList<>();
    /** The picking is over, whatever it brought. */
    boolean fetched;
    /** The stop at the head is over. */
    boolean unloaded;
    /** The rack the shuttle drives to. */
    BlockPos source;

    SupplyRun(BlockPos head, int slots) {
        super(head, slots);
    }
}
