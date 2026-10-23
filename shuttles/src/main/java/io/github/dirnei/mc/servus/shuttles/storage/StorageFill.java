package io.github.dirnei.mc.servus.shuttles.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How full a warehouse is: rack slots in use against all rack slots, per
 * zone, and the fluid in its Storage gantries against their capacity. Pure Java.
 */
public final class StorageFill {

    /** The key of the row that gathers the racks without a zone. */
    public static final String NO_ZONE = "";

    private StorageFill() {
    }

    /**
     * One rack bay.
     *
     * @param zoneKey  the key of its zone, {@link #NO_ZONE} without one
     * @param zoneName the zone's display name, empty without one
     * @param used     slots that hold items or are locked
     * @param total    all its slots
     */
    public record Bay(String zoneKey, String zoneName, int used, int total) {
    }

    /** One Storage gantry tank: mB held and mB it holds at most. */
    public record Tank(long amount, long capacity) {
    }

    /** Slots in use of a total, for the whole warehouse or one zone; {@link #NO_ZONE} keys the unzoned row. */
    public record Row(String key, String name, long used, long total) {

        public boolean full() {
            return total > 0 && used >= total;
        }

        /** The part in use, 0 for an empty total. */
        public double share() {
            return total <= 0 ? 0 : (double) used / total;
        }

        public boolean unzoned() {
            return NO_ZONE.equals(key);
        }
    }

    /**
     * The fill of one warehouse.
     *
     * @param items         slots in use and total over all bays
     * @param bays          how many rack bays count
     * @param zones         one row per zone and one for unzoned bays, full first, then the fullest
     * @param fluid         mB in the Storage gantries
     * @param fluidCapacity mB they hold at most
     * @param tanks         how many tanks count
     */
    public record Report(Row items, int bays, List<Row> zones, long fluid, long fluidCapacity, int tanks) {

        public static final Report EMPTY = new Report(new Row(NO_ZONE, "", 0, 0), 0, List.of(), 0, 0, 0);

        public boolean fluidFull() {
            return fluidCapacity > 0 && fluid >= fluidCapacity;
        }
    }

    /** Full rows first, then the larger share, then by name, the unzoned row after zones of the same share. */
    public static final Comparator<Row> ORDER = Comparator.comparing((Row row) -> !row.full())
            .thenComparing(Comparator.comparingDouble(Row::share).reversed())
            .thenComparing(Row::unzoned)
            .thenComparing(Row::name);

    /** Adds up the bays and tanks of one warehouse. */
    public static Report of(List<Bay> bays, List<Tank> tanks) {
        Map<String, long[]> perZone = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        long used = 0;
        long total = 0;
        for (Bay bay : bays) {
            used += bay.used();
            total += bay.total();
            long[] sums = perZone.computeIfAbsent(bay.zoneKey(), key -> new long[2]);
            sums[0] += bay.used();
            sums[1] += bay.total();
            names.put(bay.zoneKey(), bay.zoneName());
        }
        List<Row> zones = new ArrayList<>();
        perZone.forEach((key, sums) -> zones.add(new Row(key, NO_ZONE.equals(key) ? "" : names.get(key), sums[0], sums[1])));
        zones.sort(ORDER);
        long fluid = 0;
        long capacity = 0;
        for (Tank tank : tanks) {
            fluid += tank.amount();
            capacity += tank.capacity();
        }
        return new Report(new Row(NO_ZONE, "", used, total), bays.size(), List.copyOf(zones), fluid, capacity, tanks.size());
    }
}
