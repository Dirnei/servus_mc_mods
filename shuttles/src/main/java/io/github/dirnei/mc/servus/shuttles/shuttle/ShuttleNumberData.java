package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The world's shuttle number counter, one for every dimension, kept with the overworld. */
public final class ShuttleNumberData extends SavedData {

    private static final String NAME = ServusShuttlesIds.MOD_ID + "_shuttle_numbers";
    private static final SavedDataType<ShuttleNumberData> FACTORY = ValueIo.tagSavedData(NAME, ShuttleNumberData::new,
            (tag, registries) -> new ShuttleNumberData(tag.getIntOr("Last", 0)), (data, tag, registries) -> data.save(tag));

    private final ShuttleCounter counter;

    private ShuttleNumberData() {
        this(0);
    }

    private ShuttleNumberData(int last) {
        this.counter = new ShuttleCounter(last);
    }

    public static ShuttleNumberData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY);
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
