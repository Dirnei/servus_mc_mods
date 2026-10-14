package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.field.SupplyBuffer;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;

/**
 * The state of one supply run beyond what every field run has: what it still
 * picks for each part of the head's buffer and the sources it tried. The
 * amounts are pure ({@code SupplyPlan}).
 */
final class SupplyRun extends FieldRun {

    /** The item picked for each planned part. */
    final Map<SupplyBuffer.Part, ItemKey> keys = new EnumMap<>(SupplyBuffer.Part.class);
    /** Items still to pick for each planned part. */
    final Map<SupplyBuffer.Part, Integer> remaining = new EnumMap<>(SupplyBuffer.Part.class);
    /** The fertilizer reserve the run keeps in storage, fixed when it planned. */
    int reserve;
    /** The picking is over, whatever it brought. */
    boolean fetched;
    /** The stop at the head is over. */
    boolean unloaded;
    /** Storage blocks the run already took from or drove to. */
    final Set<BlockPos> sourcesTried = new HashSet<>();
    /** The storage block the shuttle drives to. */
    BlockPos source;

    SupplyRun(BlockPos head, int slots) {
        super(head, slots);
    }

    /** The reserve storage keeps of the part's item. */
    int reserveOf(SupplyBuffer.Part part) {
        return part == SupplyBuffer.Part.FERTILIZER ? reserve : 0;
    }
}
