package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.shuttles.lock.SlotFill;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * What a Heavy Duty Rack shelf holds over all its bays: slots in use (a slot
 * a job locked counts as used), all slots and the items held most. Shared by
 * the Data Potato report and the handheld's INFO. Server thread only.
 *
 * @param bays the shelf's bays from left to right
 * @param fill the tally over all their slots
 */
public record ShelfStock(List<HeavyRackBlockEntity> bays, ShelfFill<ItemKey> fill) {

    /** The stock of the shelf at {@code pos}, listing up to {@code topLimit} items; empty when no rack stands there. */
    public static Optional<ShelfStock> of(BlockGetter level, BlockPos pos, int topLimit) {
        List<HeavyRackBlockEntity> bays = new ArrayList<>();
        for (BlockPos bay : HeavyRackBlock.shelf(level, pos)) {
            if (level.getBlockEntity(bay) instanceof HeavyRackBlockEntity rack) {
                bays.add(rack);
            }
        }
        if (bays.isEmpty()) {
            return Optional.empty();
        }
        List<SlotFill.Slot<ItemKey>> slots = new ArrayList<>();
        for (HeavyRackBlockEntity bay : bays) {
            slots.addAll(bay.items().states());
        }
        return Optional.of(new ShelfStock(List.copyOf(bays), ShelfFill.of(slots, topLimit)));
    }
}
