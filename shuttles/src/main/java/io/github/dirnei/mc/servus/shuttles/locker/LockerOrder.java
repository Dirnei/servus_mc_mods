package io.github.dirnei.mc.servus.shuttles.locker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;

/**
 * One order of a Parcel Locker: the boxes it holds, the player who sent it
 * and where it stands. Its id tags the shuttle picks that bring it. A box
 * leaves the order once it is empty after delivery; the order ends with its
 * last box.
 */
public final class LockerOrder {

    private final int id;
    private final UUID owner;
    private final String ownerName;
    private final List<Integer> boxes;
    private LockerReturns.State state;
    private long readySince;

    public LockerOrder(int id, UUID owner, String ownerName, List<Integer> boxes) {
        this(id, owner, ownerName, boxes, LockerReturns.State.RESERVED, 0);
    }

    private LockerOrder(int id, UUID owner, String ownerName, List<Integer> boxes, LockerReturns.State state, long readySince) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.boxes = new ArrayList<>(boxes);
        this.state = state;
        this.readySince = readySince;
    }

    public int id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    /** The owner's name when the order was sent, for players who see it while the owner is away. */
    public String ownerName() {
        return ownerName;
    }

    /** The boxes it still holds, as indices into {@link LockerLayout#BOXES}. */
    public List<Integer> boxes() {
        return List.copyOf(boxes);
    }

    public LockerReturns.State state() {
        return state;
    }

    /** The game time the parcel became ready; 0 while reserved. */
    public long readySince() {
        return readySince;
    }

    public boolean holds(int box) {
        return boxes.contains(box);
    }

    void release(int box) {
        boxes.remove(Integer.valueOf(box));
    }

    void ready(long now) {
        state = LockerReturns.State.READY;
        readySince = now;
    }

    void returning() {
        state = LockerReturns.State.RETURNING;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.store("Owner", UUIDUtil.CODEC, owner);
        tag.putString("OwnerName", ownerName);
        tag.putIntArray("Boxes", boxes.stream().mapToInt(Integer::intValue).toArray());
        tag.putString("State", state.name());
        tag.putLong("ReadySince", readySince);
        return tag;
    }

    static Optional<LockerOrder> load(CompoundTag tag) {
        Optional<UUID> owner = tag.read("Owner", UUIDUtil.CODEC);
        int id = tag.getIntOr("Id", 0);
        if (owner.isEmpty() || id == 0) {
            return Optional.empty();
        }
        List<Integer> boxes = new ArrayList<>();
        if (tag.get("Boxes") instanceof IntArrayTag array) {
            for (int box : array.getAsIntArray()) {
                if (box >= 0 && box < LockerLayout.BOXES.size()) {
                    boxes.add(box);
                }
            }
        }
        LockerReturns.State state = LockerReturns.State.RESERVED;
        for (LockerReturns.State candidate : LockerReturns.State.values()) {
            if (candidate.name().equals(tag.getStringOr("State", ""))) {
                state = candidate;
            }
        }
        return Optional.of(new LockerOrder(id, owner.get(), tag.getStringOr("OwnerName", ""), boxes, state, tag.getLongOr("ReadySince", 0L)));
    }
}
