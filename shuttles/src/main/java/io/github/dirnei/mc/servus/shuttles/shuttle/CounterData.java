package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** A world counter, one for every dimension, kept with the overworld: shuttle numbers and job ids. */
public final class CounterData extends SavedData {

    /** What a counter counts; each kind is saved on its own. */
    public enum Kind {
        SHUTTLE_NUMBERS("shuttle_numbers"),
        JOB_IDS("job_ids");

        private final String name;

        Kind(String name) {
            this.name = name;
        }
    }

    private static final Map<Kind, SavedDataType<CounterData>> TYPES = new EnumMap<>(Kind.class);

    static {
        for (Kind kind : Kind.values()) {
            TYPES.put(kind, ValueIo.tagSavedData(ServusShuttlesIds.MOD_ID + "_" + kind.name, CounterData::new,
                    (tag, registries) -> new CounterData(tag.getIntOr("Last", 0)), (data, tag, registries) -> data.save(tag)));
        }
    }

    private final ShuttleCounter counter;

    private CounterData() {
        this(0);
    }

    private CounterData(int last) {
        this.counter = new ShuttleCounter(last);
    }

    public static CounterData get(MinecraftServer server, Kind kind) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPES.get(kind));
    }

    /** Gives the next number and marks the counter for saving. */
    public int take() {
        setDirty();
        return counter.take();
    }

    /** The last number given in this world, 0 when none was. */
    public int last() {
        return counter.last();
    }

    private CompoundTag save(CompoundTag tag) {
        tag.putInt("Last", counter.last());
        return tag;
    }
}
