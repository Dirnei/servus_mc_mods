package io.github.dirnei.mc.servus.foundation.logistics.channel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Whether a channel id exists: on the server from the saved channels, on the client from the synced copy. */
@FunctionalInterface
public interface ChannelLookup {

    boolean exists(int id);

    /** The lookup for the side {@code level} lives on. */
    static ChannelLookup of(Level level) {
        if (level instanceof ServerLevel server) {
            ChannelList list = DuctChannelData.get(server.getServer()).list();
            return list::exists;
        }
        return id -> id == ChannelList.DEFAULT_ID;
    }
}
