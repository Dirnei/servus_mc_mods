package io.github.dirnei.mc.servus.foundation.machine.loader;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Which chunk loaders each team has registered, for the per team cap. */
public final class ChunkLoaderData extends SavedData {

    /** One team and its loaders, the saved form. */
    private record TeamEntry(UUID team, List<String> loaders) {
        static final Codec<TeamEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("Team").forGetter(TeamEntry::team),
                Codec.STRING.listOf().fieldOf("Loaders").forGetter(TeamEntry::loaders)).apply(i, TeamEntry::new));
    }

    private static final Codec<ChunkLoaderData> CODEC = RecordCodecBuilder.create(i -> i.group(
            TeamEntry.CODEC.listOf().optionalFieldOf("Teams", List.of()).forGetter(ChunkLoaderData::entries))
            .apply(i, ChunkLoaderData::fromEntries));

    private static final SavedDataType<ChunkLoaderData> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("servus_chunk_loaders"), ChunkLoaderData::new, CODEC);

    private final Map<UUID, Set<String>> loaders = new HashMap<>();

    public static ChunkLoaderData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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

    private static ChunkLoaderData fromEntries(List<TeamEntry> entries) {
        ChunkLoaderData data = new ChunkLoaderData();
        for (TeamEntry entry : entries) {
            data.loaders.put(entry.team(), new LinkedHashSet<>(entry.loaders()));
        }
        return data;
    }

    private List<TeamEntry> entries() {
        return loaders.entrySet().stream()
                .map(e -> new TeamEntry(e.getKey(), List.copyOf(e.getValue())))
                .toList();
    }
}
