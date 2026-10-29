package io.github.dirnei.mc.servus.shuttles.zone;

/**
 * A team zone as bays and the warehouse see it: its id, name, colour and filter.
 *
 * @param id     never {@link ZoneList#NONE}
 * @param name   the name as typed, trimmed
 * @param colour a dye colour index from 0 to 15
 * @param filter what the zone takes; empty means anything
 */
public record ZoneDefinition(int id, String name, int colour, ZoneFilter filter) {

    /** The key zones are grouped by: the id as text. */
    public String key() {
        return Integer.toString(id);
    }

    public static ZoneDefinition of(ZoneList.Zone<ZoneFilter> zone) {
        return new ZoneDefinition(zone.id(), zone.name(), zone.colour(), zone.filter());
    }
}
