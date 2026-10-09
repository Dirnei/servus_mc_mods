package io.github.dirnei.mc.servus.storage.order;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Progress of production that outlives the block entities taking part:
 * shipments from Packing Stations and outputs moving back from producers, by
 * id, plus the counters for transfer and request ids. Owners keep only ids
 * and book them here, so a delivery that ends while its owner is unloaded,
 * or across a restart, is not lost. One per dimension, saved with it.
 */
public final class ProductionLedger extends SavedData {

    private static final String NAME = "servus_storage_production";
    /** How long an ended transfer stays readable. */
    static final long KEEP_ENDED_TICKS = 24000;

    /** Items asked to move somewhere, and how many arrived. */
    public static final class Transfer {
        private final long id;
        private int asked;
        private int delivered;
        private boolean ended;
        private long endedAt = -1;

        Transfer(long id, int asked) {
            this.id = id;
            this.asked = asked;
        }

        public long id() {
            return id;
        }

        public int asked() {
            return asked;
        }

        public int delivered() {
            return delivered;
        }

        public boolean ended() {
            return ended;
        }

        public int missing() {
            return Math.max(0, asked - delivered);
        }
    }

    private final Map<Long, Transfer> transfers = new HashMap<>();
    private long nextTransfer = 1;
    private int nextRequest = 1;
    private ServerLevel level;

    public ProductionLedger() {
    }

    public static ProductionLedger get(ServerLevel level) {
        ProductionLedger ledger = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ProductionLedger::new, ProductionLedger::load, null), NAME);
        ledger.level = level;
        return ledger;
    }

    /** A new transfer of {@code asked} items. */
    public Transfer open(int asked) {
        prune();
        Transfer transfer = new Transfer(nextTransfer++, asked);
        transfers.put(transfer.id, transfer);
        setDirty();
        return transfer;
    }

    /** The transfer with that id, null when unknown or pruned. */
    public Transfer transfer(long id) {
        return transfers.get(id);
    }

    /** {@code count} more items arrived. */
    public void delivered(Transfer transfer, int count) {
        if (count > 0) {
            transfer.delivered += count;
            setDirty();
        }
    }

    /** Fewer items will come than asked: lowers what is asked, never below what arrived. */
    public void shortfall(Transfer transfer, int count) {
        if (count > 0) {
            transfer.asked = Math.max(transfer.delivered, transfer.asked - count);
            setDirty();
        }
    }

    public void end(Transfer transfer) {
        if (!transfer.ended) {
            transfer.ended = true;
            transfer.endedAt = level == null ? 0 : level.getGameTime();
            setDirty();
        }
    }

    /** A request id never handed out before in this dimension. */
    public int nextRequestId() {
        setDirty();
        return nextRequest++;
    }

    /** Makes sure later request ids stay above an id already in use, after loading. */
    public void seenRequestId(int id) {
        if (id >= nextRequest) {
            nextRequest = id + 1;
            setDirty();
        }
    }

    private void prune() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Transfer> it = transfers.values().iterator(); it.hasNext(); ) {
            Transfer transfer = it.next();
            if (transfer.ended && now - transfer.endedAt > KEEP_ENDED_TICKS) {
                it.remove();
                setDirty();
            }
        }
    }

    // Persistence

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("NextTransfer", nextTransfer);
        tag.putInt("NextRequest", nextRequest);
        ListTag list = new ListTag();
        for (Transfer transfer : transfers.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Id", transfer.id);
            entry.putInt("Asked", transfer.asked);
            entry.putInt("Delivered", transfer.delivered);
            entry.putBoolean("Ended", transfer.ended);
            entry.putLong("EndedAt", transfer.endedAt);
            list.add(entry);
        }
        tag.put("Transfers", list);
        return tag;
    }

    private static ProductionLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        ProductionLedger ledger = new ProductionLedger();
        ledger.nextTransfer = Math.max(1, tag.getLong("NextTransfer"));
        ledger.nextRequest = Math.max(1, tag.getInt("NextRequest"));
        for (Tag raw : tag.getList("Transfers", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            Transfer transfer = new Transfer(entry.getLong("Id"), entry.getInt("Asked"));
            transfer.delivered = entry.getInt("Delivered");
            transfer.ended = entry.getBoolean("Ended");
            transfer.endedAt = entry.getLong("EndedAt");
            ledger.transfers.put(transfer.id, transfer);
        }
        return ledger;
    }
}
