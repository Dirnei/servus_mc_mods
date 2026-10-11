package io.github.dirnei.mc.servus.foundation.data.packet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedDataType;
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

    private record Entry(ItemStack stack, List<BlockPos> places) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(Entry::stack),
                BlockPos.CODEC.listOf().fieldOf("places").forGetter(Entry::places)).apply(i, Entry::new));
    }

    private static final Codec<PacketStash> CODEC = Entry.CODEC.listOf().optionalFieldOf("entries", List.of())
            .xmap(PacketStash::fromEntries, stash -> List.copyOf(stash.entries)).codec();

    private static final SavedDataType<PacketStash> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("servus_foundation_packet_stash"), PacketStash::new, CODEC);

    private final List<Entry> entries = new ArrayList<>();

    public static PacketStash of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static PacketStash fromEntries(List<Entry> saved) {
        PacketStash stash = new PacketStash();
        for (Entry entry : saved) {
            if (!entry.stack().isEmpty() && !entry.places().isEmpty()) {
                stash.entries.add(new Entry(entry.stack(), new ArrayList<>(entry.places())));
            }
        }
        return stash;
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
}
