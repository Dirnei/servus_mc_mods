package io.github.dirnei.mc.servus.foundation.data.packet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Items from packets that could not be placed anywhere loaded yet. Each entry
 * remembers the positions it may drop at, and drops as soon as one of them is
 * loaded. Saved with the level, so nothing is voided.
 */
public final class PacketStash extends SavedData {

    private static final String NAME = "servus_foundation_packet_stash";
    private static final Factory<PacketStash> FACTORY = new Factory<>(PacketStash::new, PacketStash::load, null);

    private record Entry(ItemStack stack, List<BlockPos> places) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public static PacketStash of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public void add(ItemStack stack, List<BlockPos> places) {
        if (stack.isEmpty() || places.isEmpty()) {
            return;
        }
        entries.add(new Entry(stack.copy(), List.copyOf(places)));
        setDirty();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    /** Drops every entry that has a loaded place. */
    public void tick(ServerLevel level) {
        boolean changed = false;
        for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) {
            Entry entry = it.next();
            for (BlockPos place : entry.places()) {
                if (level.isLoaded(place)) {
                    Containers.dropItemStack(level, place.getX() + 0.5, place.getY() + 0.5, place.getZ() + 0.5, entry.stack());
                    it.remove();
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            setDirty();
        }
    }

    private static PacketStash load(CompoundTag tag, HolderLookup.Provider registries) {
        PacketStash stash = new PacketStash();
        for (Tag element : tag.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("stack"));
            List<BlockPos> places = new ArrayList<>();
            for (long place : entry.getLongArray("places")) {
                places.add(BlockPos.of(place));
            }
            if (!stack.isEmpty() && !places.isEmpty()) {
                stash.entries.add(new Entry(stack, places));
            }
        }
        return stash;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag saved = new CompoundTag();
            saved.put("stack", entry.stack().save(registries));
            saved.putLongArray("places", entry.places().stream().mapToLong(BlockPos::asLong).toArray());
            list.add(saved);
        }
        tag.put("entries", list);
        return tag;
    }
}
