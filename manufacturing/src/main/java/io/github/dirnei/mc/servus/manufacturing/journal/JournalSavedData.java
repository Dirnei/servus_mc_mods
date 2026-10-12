package io.github.dirnei.mc.servus.manufacturing.journal;

import net.minecraft.world.level.saveddata.SavedDataType;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.core.UUIDUtil;
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
    private static final SavedDataType<JournalSavedData> FACTORY = ValueIo.tagSavedData(NAME, JournalSavedData::new, JournalSavedData::load,
            (j, tag, registries) -> j.save(tag, registries));

    private final Map<UUID, TeamJournal> teams = new HashMap<>();

    public static JournalSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY);
    }

    public TeamJournal forTeam(UUID team) {
        return teams.computeIfAbsent(team, id -> new TeamJournal());
    }

    private static JournalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        JournalSavedData data = new JournalSavedData();
        ListTag teams = tag.getListOrEmpty("Teams");
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag entry = teams.getCompoundOrEmpty(i);
            TeamJournal journal = data.forTeam(entry.read("Team", UUIDUtil.CODEC).orElseThrow());
            CompoundTag counts = entry.getCompoundOrEmpty("Counts");
            for (String key : counts.keySet()) {
                String[] parts = key.split("/", 2);
                if (parts.length == 2) {
                    journal.setCount(parts[0], parts[1], counts.getIntOr(key, 0));
                }
            }
            for (Tag finished : entry.getListOrEmpty("Finished")) {
                journal.markFinished(finished.asString().orElse(""));
            }
        }
        return data;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        teams.forEach((team, journal) -> {
            CompoundTag entry = new CompoundTag();
            entry.store("Team", UUIDUtil.CODEC, team);
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
