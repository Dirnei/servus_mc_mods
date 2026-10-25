package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import java.util.ArrayList;
import java.util.Comparator;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The patterns of a data line: every loaded Pattern Storage of the line,
 * lowest position first, slots in order. Server thread only.
 */
public final class PatternLibrary {

    /**
     * A pattern and where it is kept.
     *
     * @param variants the other items its cells may take; {@link PatternVariants#NONE} for Exact patterns
     */
    public record Entry(BlockPos storage, int slot, EncodedPattern pattern, PatternVariants variants) {

        /** A pattern kept nowhere, such as a one-off order's. */
        public static Entry unsaved(Level level, EncodedPattern pattern) {
            return new Entry(null, -1, pattern, pattern.any() ? PatternVariants.of(level, pattern) : PatternVariants.NONE);
        }

        private static Entry kept(PatternStorageBlockEntity storage, int slot, EncodedPattern pattern) {
            return new Entry(storage.getBlockPos(), slot, pattern, pattern.any() ? storage.variants(slot) : PatternVariants.NONE);
        }

        /** The pattern crafting from the accepted items the stock holds most of; the pattern itself when it is Exact. */
        public EncodedPattern variant(Level level, Map<ItemKey, ? extends Number> stock) {
            return variants.variant(level, pattern, stock);
        }
    }

    /** An empty pattern slot. */
    public record Free(BlockPos storage, int slot) {
    }

    private PatternLibrary() {
    }

    /** The Pattern Storages of the line, lowest position first. */
    public static List<PatternStorageBlockEntity> storages(ServerLevel level, DataNetwork network) {
        List<PatternStorageBlockEntity> list = new ArrayList<>();
        for (DataMember member : network.dataMembers(DataRole.LINK)) {
            if (level.isLoaded(member.pos()) && level.getBlockEntity(member.pos()) instanceof PatternStorageBlockEntity storage) {
                list.add(storage);
            }
        }
        list.sort(Comparator.comparingLong(storage -> storage.getBlockPos().asLong()));
        return list;
    }

    public static List<Entry> entries(ServerLevel level, DataNetwork network) {
        List<Entry> entries = new ArrayList<>();
        for (PatternStorageBlockEntity storage : storages(level, network)) {
            for (int slot : storage.usedSlots()) {
                int at = slot;
                storage.pattern(slot).ifPresent(pattern -> entries.add(Entry.kept(storage, at, pattern)));
            }
        }
        return entries;
    }

    /** How many patterns the line can hold. */
    public static int capacity(ServerLevel level, DataNetwork network) {
        int total = 0;
        for (PatternStorageBlockEntity storage : storages(level, network)) {
            total += storage.capacity();
        }
        return total;
    }

    /** The first free slot of the line. */
    public static Optional<Free> firstFree(ServerLevel level, DataNetwork network) {
        for (PatternStorageBlockEntity storage : storages(level, network)) {
            int slot = storage.firstFree();
            if (slot >= 0) {
                return Optional.of(new Free(storage.getBlockPos(), slot));
            }
        }
        return Optional.empty();
    }

    /** The pattern at a slot of a Pattern Storage of the line. */
    public static Optional<Entry> find(ServerLevel level, DataNetwork network, BlockPos storage, int slot) {
        for (PatternStorageBlockEntity candidate : storages(level, network)) {
            if (candidate.getBlockPos().equals(storage)) {
                return candidate.pattern(slot).map(pattern -> Entry.kept(candidate, slot, pattern));
            }
        }
        return Optional.empty();
    }

    /** Whether the line already has the same work, ignoring the slot {@code except} (may be null). */
    public static boolean contains(ServerLevel level, DataNetwork network, EncodedPattern pattern, Free except) {
        for (Entry entry : entries(level, network)) {
            if (except != null && entry.storage().equals(except.storage()) && entry.slot() == except.slot()) {
                continue;
            }
            if (entry.pattern().duplicates(pattern)) {
                return true;
            }
        }
        return false;
    }
}
