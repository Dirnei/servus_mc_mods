package io.github.dirnei.mc.servus.manufacturing.research;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Research progress of every team, stored with the overworld. */
public final class ResearchSavedData extends SavedData {

    private static final String NAME = "servus_research";
    private static final Factory<ResearchSavedData> FACTORY =
            new Factory<>(ResearchSavedData::new, ResearchSavedData::load, null);

    private final Map<UUID, ResearchState> teams = new HashMap<>();

    public static ResearchSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public ResearchState forTeam(UUID team) {
        return teams.computeIfAbsent(team, id -> new ResearchState());
    }

    // NBT

    private static ResearchSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ResearchSavedData data = new ResearchSavedData();
        ListTag list = tag.getList("Teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID team = entry.getUUID("Team");
            Set<String> completed = new LinkedHashSet<>();
            for (Tag t : entry.getList("Completed", Tag.TAG_STRING)) {
                completed.add(t.getAsString());
            }
            String current = entry.contains("Current") ? entry.getString("Current") : null;
            Map<String, Map<String, Integer>> progress = new HashMap<>();
            CompoundTag progressTag = entry.getCompound("Progress");
            for (String tech : progressTag.getAllKeys()) {
                CompoundTag counts = progressTag.getCompound(tech);
                Map<String, Integer> items = new HashMap<>();
                for (String item : counts.getAllKeys()) {
                    items.put(item, counts.getInt(item));
                }
                progress.put(tech, items);
            }
            ResearchState state = new ResearchState();
            state.restore(completed, current, progress);
            data.teams.put(team, state);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        teams.forEach((team, state) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Team", team);
            ListTag completed = new ListTag();
            state.completed().forEach(id -> completed.add(StringTag.valueOf(id)));
            entry.put("Completed", completed);
            state.current().ifPresent(id -> entry.putString("Current", id));
            CompoundTag progress = new CompoundTag();
            state.progress().forEach((tech, counts) -> {
                CompoundTag countsTag = new CompoundTag();
                counts.forEach(countsTag::putInt);
                progress.put(tech, countsTag);
            });
            entry.put("Progress", progress);
            list.add(entry);
        });
        tag.put("Teams", list);
        return tag;
    }
}
