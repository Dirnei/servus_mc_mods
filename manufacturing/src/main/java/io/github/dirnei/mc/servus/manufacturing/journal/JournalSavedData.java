package io.github.dirnei.mc.servus.manufacturing.journal;

import io.github.dirnei.mc.servus.manufacturing.journal.JournalLedger.TeamJournal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Every team's journal: task counts and finished chapters. */
public final class JournalSavedData extends SavedData {

    private static final String NAME = "servus_journal";
    private static final Factory<JournalSavedData> FACTORY = new Factory<>(JournalSavedData::new, JournalSavedData::load, null);

    private final Map<UUID, TeamJournal> teams = new HashMap<>();

    public static JournalSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public TeamJournal forTeam(UUID team) {
        return teams.computeIfAbsent(team, id -> new TeamJournal());
    }

    private static JournalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        JournalSavedData data = new JournalSavedData();
        ListTag teams = tag.getList("Teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag entry = teams.getCompound(i);
            TeamJournal journal = data.forTeam(entry.getUUID("Team"));
            CompoundTag counts = entry.getCompound("Counts");
            for (String key : counts.getAllKeys()) {
                String[] parts = key.split("/", 2);
                if (parts.length == 2) {
                    journal.setCount(parts[0], parts[1], counts.getInt(key));
                }
            }
            for (Tag finished : entry.getList("Finished", Tag.TAG_STRING)) {
                journal.markFinished(finished.getAsString());
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        teams.forEach((team, journal) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Team", team);
            CompoundTag counts = new CompoundTag();
            journal.counts().forEach(counts::putInt);
            entry.put("Counts", counts);
            ListTag finished = new ListTag();
            journal.finished().forEach(chapter -> finished.add(StringTag.valueOf(chapter)));
            entry.put("Finished", finished);
            list.add(entry);
        });
        tag.put("Teams", list);
        return tag;
    }
}
