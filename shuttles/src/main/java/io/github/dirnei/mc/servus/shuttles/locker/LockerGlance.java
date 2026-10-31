package io.github.dirnei.mc.servus.shuttles.locker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What a look at a Parcel Locker tells the viewer, as the server sends it to
 * a block info overlay: the looked at box with its orderer and state, or
 * from the head every taken box. Only the viewer's own boxes carry their
 * items; items of other players' boxes never leave the server.
 */
public final class LockerGlance {

    private static final String BOXES = "Boxes";

    /**
     * One box as the viewer sees it.
     *
     * @param owner the name of the player who ordered it, empty while free
     * @param state the order's state, null while free
     * @param items what it holds, one stack per item with its total, only for the viewer's own box
     */
    public record Line(LockerLayout.Box box, String owner, LockerReturns.@Nullable State state, List<ItemStack> items) {

        public boolean free() {
            return state == null;
        }
    }

    private LockerGlance() {
    }

    /**
     * The lines for a look at the part at {@code hitY} pixels up inside it:
     * the box there, or on the head every taken box.
     */
    public static List<Line> lines(ParcelLockerBlockEntity locker, LockerPart part, double hitY, UUID viewer) {
        LockerLayout.Box looked = part.box(hitY);
        if (looked != null) {
            return List.of(line(locker, looked, viewer));
        }
        List<Line> taken = new ArrayList<>();
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            if (locker.orderOfBox(box.index()).isPresent()) {
                taken.add(line(locker, box, viewer));
            }
        }
        return taken;
    }

    private static Line line(ParcelLockerBlockEntity locker, LockerLayout.Box box, UUID viewer) {
        Optional<LockerOrder> order = locker.orderOfBox(box.index());
        if (order.isEmpty()) {
            return new Line(box, "", null, List.of());
        }
        List<ItemStack> items = order.get().owner().equals(viewer) ? locker.contents(box.index()) : List.of();
        return new Line(box, order.get().ownerName(), order.get().state(), items);
    }

    /** The lines as a tag; a total past the stack limit survives. */
    public static CompoundTag write(List<Line> lines, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Line line : lines) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Box", line.box().index());
            entry.putString("Owner", line.owner());
            if (line.state() != null) {
                entry.putString("State", line.state().name());
            }
            ListTag items = new ListTag();
            line.items().forEach(stack -> items.add(ParcelLockerBlockEntity.shownStack(registries, stack)));
            entry.put("Items", items);
            list.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        tag.put(BOXES, list);
        return tag;
    }

    /** The lines from a tag {@link #write} made; unknown boxes are skipped. */
    public static List<Line> read(CompoundTag tag, HolderLookup.Provider registries) {
        List<Line> lines = new ArrayList<>();
        for (Tag element : tag.getListOrEmpty(BOXES)) {
            if (!(element instanceof CompoundTag entry)) {
                continue;
            }
            int index = entry.getIntOr("Box", -1);
            if (index < 0 || index >= LockerLayout.BOXES.size()) {
                continue;
            }
            List<ItemStack> items = new ArrayList<>();
            for (Tag item : entry.getListOrEmpty("Items")) {
                ItemStack stack = ParcelLockerBlockEntity.shownStack(registries, item);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
            lines.add(new Line(LockerLayout.BOXES.get(index), entry.getStringOr("Owner", ""), state(entry.getStringOr("State", "")),
                    List.copyOf(items)));
        }
        return lines;
    }

    private static LockerReturns.@Nullable State state(String name) {
        for (LockerReturns.State state : LockerReturns.State.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }
        return null;
    }
}
