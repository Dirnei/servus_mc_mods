package io.github.dirnei.mc.servus.shuttles.zone;

import io.github.dirnei.mc.servus.shuttles.client.ClientZones;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Resolves a zone id on either side: the saved zone data on the server, the synced copy on the client. */
public final class Zones {

    private Zones() {
    }

    /** The zone with that id as seen from {@code level}, null for no zone, a deleted zone or no level. */
    public static ZoneDefinition definition(Level level, int id) {
        if (id == ZoneList.NONE || level == null) {
            return null;
        }
        if (level instanceof ServerLevel server) {
            return WarehouseZoneData.get(server.getServer()).definition(id);
        }
        return ClientZones.definition(id);
    }
}
