package io.github.dirnei.mc.servus.shuttles.station;

import io.github.dirnei.mc.servus.foundation.data.packet.Grant;
import io.github.dirnei.mc.servus.foundation.data.packet.GrantBook;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketEndpoint;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.packing.SlotGrant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * The grants a station's buffer answers offers with: each reserves buffer
 * slots with IN locks under the grant's ticket, takes items only through
 * them and frees them when the grant ends, so the warehouse puts the slots
 * away. Grants are not saved. Server thread only.
 */
public final class BufferGrants {

    /** Ticks between two sweeps of the grants. */
    private static final int SWEEP_TICKS = 20;

    private final LockedSlots slots;
    private final GrantBook book = new GrantBook();
    private final Map<Long, ItemKey> keys = new HashMap<>();
    private int sweepTimer;

    public BufferGrants(LockedSlots slots) {
        this.slots = slots;
    }

    /** Locks the planned slots IN for a new grant of the endpoint under {@code lockId}, made at game time {@code now}. */
    public Grant open(PacketEndpoint endpoint, ItemKey key, List<SlotGrant.Part> parts, int lockId, long now) {
        for (SlotGrant.Part part : parts) {
            slots.lock(part.slot(), new SlotLock(SlotLock.Kind.IN, key, part.count(), lockId));
        }
        Grant grant = Grant.of(endpoint, lockId, SlotGrant.total(parts), now);
        book.add(grant);
        keys.put(grant.ticket(), key);
        return grant;
    }

    /** Puts items of an open grant into its slots; returns what did not go in. */
    public ItemStack receive(Grant grant, ItemStack stack) {
        return book.holds(grant) ? slots.fillAs((int) grant.ticket(), stack) : stack;
    }

    /** Forgets the grant and frees its slots. */
    public void close(Grant grant) {
        book.remove(grant);
        keys.remove(grant.ticket());
        slots.release((int) grant.ticket());
    }

    /** Whether the lock with that id belongs to an open grant. */
    public boolean holds(int lockId) {
        return book.holds(lockId);
    }

    /** How many items of the key the open grants still expect. */
    public int left(ItemKey key) {
        int sum = 0;
        for (Grant grant : book.grants()) {
            if (key.equals(keys.get(grant.ticket()))) {
                sum += grant.left();
            }
        }
        return sum;
    }

    /** Empty slots no lock holds, the ones a grant may reserve. */
    public List<Integer> freeSlots() {
        List<Integer> free = new ArrayList<>();
        for (int slot = 0; slot < slots.getSlots(); slot++) {
            if (slots.getStackInSlot(slot).isEmpty() && !slots.isLocked(slot)) {
                free.add(slot);
            }
        }
        return free;
    }

    /** Sweeps the grants nobody uses any more every second; call every tick with the game time. */
    public void tick(long now) {
        if (++sweepTimer >= SWEEP_TICKS) {
            sweepTimer = 0;
            book.sweep(now);
        }
    }

    /** Ends every grant, for a buffer that goes away or loads again. */
    public void closeAll() {
        book.closeAll();
        keys.clear();
    }

    /** Releases IN locks no open grant holds, as after loading, so the items in their slots are put away. */
    public void dropOrphanLocks() {
        for (LockedSlots.Locked locked : slots.locked()) {
            if (locked.lock().kind() == SlotLock.Kind.IN && !book.holds(locked.lock().jobId())) {
                slots.release(locked.slot(), locked.lock().jobId());
            }
        }
    }
}
