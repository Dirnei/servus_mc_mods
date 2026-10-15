package io.github.dirnei.mc.servus.alchemy.hint;

import io.github.dirnei.mc.servus.alchemy.hint.OldGnomeLedger.Entry;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnomeLedger.State;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Every team's old gnome, saved with the overworld. Callers mark it dirty through the methods that change it. */
public final class OldGnomeSavedData extends SavedData {

    private static final String NAME = "servus_old_gnome";
    private static final SavedDataType<OldGnomeSavedData> FACTORY = ValueIo.tagSavedData(NAME, OldGnomeSavedData::new,
            OldGnomeSavedData::load, (data, tag, registries) -> data.save(tag));

    private final OldGnomeLedger ledger = new OldGnomeLedger();

    public static OldGnomeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY);
    }

    /** The ledger to read; change it only through this class. */
    public OldGnomeLedger ledger() {
        return ledger;
    }

    public boolean wake(UUID team, long pos, String dimension) {
        return changed(ledger.wake(team, pos, dimension));
    }

    public boolean found(UUID team) {
        return changed(ledger.found(team));
    }

    public boolean lost(UUID team, long pos) {
        return changed(ledger.lost(team, pos));
    }

    private boolean changed(boolean changed) {
        if (changed) {
            setDirty();
        }
        return changed;
    }

    private static OldGnomeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        OldGnomeSavedData data = new OldGnomeSavedData();
        ListTag teams = tag.getListOrEmpty("Teams");
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag entry = teams.getCompoundOrEmpty(i);
            entry.read("Team", UUIDUtil.CODEC).ifPresent(team -> data.ledger.restore(team, new Entry(
                    State.valueOf(entry.getStringOr("State", State.NONE.name())), entry.getLongOr("Pos", 0),
                    entry.getStringOr("Dimension", ""))));
        }
        return data;
    }

    private CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        ledger.entries().forEach((team, entry) -> {
            CompoundTag saved = new CompoundTag();
            saved.store("Team", UUIDUtil.CODEC, team);
            saved.putString("State", entry.state().name());
            saved.putLong("Pos", entry.pos());
            saved.putString("Dimension", entry.dimension());
            list.add(saved);
        });
        tag.put("Teams", list);
        return tag;
    }
}
