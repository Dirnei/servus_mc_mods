package io.github.dirnei.mc.servus.foundation.machine.anchor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The chunks each team holds loaded, for the per team cap: one entry per
 * active Ender Anchor or woken warehouse site, each with the chunks it holds.
 * A team's load is the union of its entries' chunks ({@link TeamLoad}). An
 * entry key is {@code "dimension@blockpos"}, a chunk {@code "dimension@chunk"}
 * with the chunk packed like {@code ChunkPos#pack}.
 */
public final class EnderAnchorData extends SavedData {

    /** One loader and its chunks, the saved form. */
    private record LoaderEntry(String key, List<String> chunks) {
        static final Codec<LoaderEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("Key").forGetter(LoaderEntry::key),
                Codec.STRING.listOf().optionalFieldOf("Chunks", List.of()).forGetter(LoaderEntry::chunks))
                .apply(i, LoaderEntry::new));
    }

    /** One team and its loaders, the saved form. Saves from before chunk lists keep their keys in {@code Loaders}. */
    private record TeamEntry(UUID team, List<String> legacy, List<LoaderEntry> entries) {
        static final Codec<TeamEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("Team").forGetter(TeamEntry::team),
                Codec.STRING.listOf().optionalFieldOf("Loaders", List.of()).forGetter(TeamEntry::legacy),
                LoaderEntry.CODEC.listOf().optionalFieldOf("Entries", List.of()).forGetter(TeamEntry::entries))
                .apply(i, TeamEntry::new));
    }

    private static final Codec<EnderAnchorData> CODEC = RecordCodecBuilder.create(i -> i.group(
            TeamEntry.CODEC.listOf().optionalFieldOf("Teams", List.of()).forGetter(EnderAnchorData::teamEntries))
            .apply(i, EnderAnchorData::fromEntries));

    private static final SavedDataType<EnderAnchorData> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("servus_ender_anchors"), EnderAnchorData::new, CODEC);

    private final Map<UUID, Map<String, Set<String>>> teams = new HashMap<>();

    public static EnderAnchorData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The entry key of the loader at that position. */
    public static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.asLong();
    }

    /** The chunk keys of packed chunk positions in one dimension. */
    public static Set<String> chunks(ResourceKey<Level> dimension, Collection<Long> packed) {
        Set<String> result = new LinkedHashSet<>();
        for (long chunk : packed) {
            result.add(dimension.identifier() + "@" + chunk);
        }
        return result;
    }

    private Map<String, Set<String>> entries(UUID team) {
        return teams.getOrDefault(team, Map.of());
    }

    /** How many distinct chunks the team holds loaded. */
    public int load(UUID team) {
        return TeamLoad.load(entries(team));
    }

    /** The team's load if the entry {@code key} held {@code chunks} instead of what it holds now. */
    public int loadWith(UUID team, String key, Collection<String> chunks) {
        return TeamLoad.loadWith(entries(team), key, chunks);
    }

    /** Whether the entry {@code key} may hold {@code chunks}, its own old chunks not counted. */
    public boolean fits(UUID team, String key, Collection<String> chunks, int cap) {
        return TeamLoad.fits(entries(team), key, chunks, cap);
    }

    public boolean isRegistered(UUID team, String key) {
        return entries(team).containsKey(key);
    }

    public boolean isRegistered(UUID team, ServerLevel level, BlockPos pos) {
        return isRegistered(team, key(level, pos));
    }

    /** The chunks the entry holds, empty when it is not registered. */
    public Set<String> chunksOf(UUID team, String key) {
        return entries(team).getOrDefault(key, Set.of());
    }

    /** Registers the entry with these chunks, replacing what it held before. */
    public void register(UUID team, String key, Collection<String> chunks) {
        Set<String> wanted = new LinkedHashSet<>(chunks);
        Map<String, Set<String>> entries = teams.computeIfAbsent(team, t -> new LinkedHashMap<>());
        if (!wanted.equals(entries.get(key))) {
            entries.put(key, wanted);
            setDirty();
        }
    }

    public void unregister(UUID team, String key) {
        Map<String, Set<String>> entries = teams.get(team);
        if (entries != null && entries.remove(key) != null) {
            if (entries.isEmpty()) {
                teams.remove(team);
            }
            setDirty();
        }
    }

    public void unregister(UUID team, ServerLevel level, BlockPos pos) {
        unregister(team, key(level, pos));
    }

    private static EnderAnchorData fromEntries(List<TeamEntry> saved) {
        EnderAnchorData data = new EnderAnchorData();
        for (TeamEntry team : saved) {
            Map<String, Set<String>> entries = data.teams.computeIfAbsent(team.team(), t -> new LinkedHashMap<>());
            for (String key : team.legacy()) {
                entries.put(key, new LinkedHashSet<>());
            }
            for (LoaderEntry entry : team.entries()) {
                entries.put(entry.key(), new LinkedHashSet<>(entry.chunks()));
            }
        }
        return data;
    }

    private List<TeamEntry> teamEntries() {
        return teams.entrySet().stream()
                .map(team -> new TeamEntry(team.getKey(), List.of(), team.getValue().entrySet().stream()
                        .map(entry -> new LoaderEntry(entry.getKey(), List.copyOf(entry.getValue())))
                        .toList()))
                .toList();
    }
}
