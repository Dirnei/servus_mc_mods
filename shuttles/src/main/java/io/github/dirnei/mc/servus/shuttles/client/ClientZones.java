package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneListPayload;

/**
 * The client's copy of the warehouse zones, replaced whenever the server
 * sends the list. No client imports, so block entities may resolve their
 * zone through it on the client side.
 */
public final class ClientZones {

    private static volatile ZoneList<ZoneFilter> list = new ZoneList<>();

    private ClientZones() {
    }

    public static ZoneList<ZoneFilter> list() {
        return list;
    }

    /** The zone with that id, null for no zone or a deleted one. */
    public static ZoneDefinition definition(int id) {
        ZoneList.Zone<ZoneFilter> zone = list.get(id);
        return zone == null ? null : ZoneDefinition.of(zone);
    }

    public static void update(ZoneListPayload payload) {
        list = payload.toList();
    }
}
