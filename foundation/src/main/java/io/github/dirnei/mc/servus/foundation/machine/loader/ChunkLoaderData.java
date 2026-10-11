package io.github.dirnei.mc.servus.foundation.machine.loader;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Which chunk loaders each team has registered, for the per team cap. */
public final class ChunkLoaderData extends SavedData {

    private static final String NAME = "servus_chunk_loaders";
    private static final Factory<ChunkLoaderData> FACTORY = new Factory<>(ChunkLoaderData::new, ChunkLoaderData::load, null);

    private final Map<UUID, Set<String>> loaders = new HashMap<>();

    public static ChunkLoaderData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.asLong();
    }

    public int countFor(UUID team) {
        Set<String> set = loaders.get(team);
        return set == null ? 0 : set.size();
    }

    public boolean isRegistered(UUID team, ServerLevel level, BlockPos pos) {
        Set<String> set = loaders.get(team);
        return set != null && set.contains(key(level, pos));
    }

    public void register(UUID team, ServerLevel level, BlockPos pos) {
        if (loaders.computeIfAbsent(team, t -> new LinkedHashSet<>()).add(key(level, pos))) {
            setDirty();
        }
    }

    public void unregister(UUID team, ServerLevel level, BlockPos pos) {
        Set<String> set = loaders.get(team);
        if (set != null && set.remove(key(level, pos))) {
            if (set.isEmpty()) {
                loaders.remove(team);
            }
            setDirty();
        }
    }

    private static ChunkLoaderData load(CompoundTag tag, HolderLookup.Provider registries) {
        ChunkLoaderData data = new ChunkLoaderData();
        ListTag teams = tag.getList("Teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag entry = teams.getCompound(i);
            Set<String> set = new LinkedHashSet<>();
            for (Tag t : entry.getList("Loaders", Tag.TAG_STRING)) {
                set.add(t.getAsString());
            }
            data.loaders.put(entry.getUUID("Team"), set);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag teams = new ListTag();
        loaders.forEach((team, set) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Team", team);
            ListTag list = new ListTag();
            set.forEach(key -> list.add(StringTag.valueOf(key)));
            entry.put("Loaders", list);
            teams.add(entry);
        });
        tag.put("Teams", teams);
        return tag;
    }
}
