package io.github.dirnei.mc.servus.storage.packing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The open wireless transfers on their way to each Packing Station, kept at
 * run time by the sending stations as their shipments open and end, and again
 * as they load. Only spreads transfers over receivers; a miss never loses items.
 * Server thread only.
 */
public final class WirelessIncoming {

    private static final Map<ResourceKey<Level>, Map<Long, Set<Long>>> INCOMING = new HashMap<>();

    private WirelessIncoming() {
    }

    /** A shipment with that ledger id is on its way to the station. */
    public static void add(ResourceKey<Level> dimension, BlockPos station, long shipment) {
        INCOMING.computeIfAbsent(dimension, key -> new HashMap<>()).computeIfAbsent(station.asLong(), key -> new HashSet<>()).add(shipment);
    }

    /** The shipment ended or its sender left. */
    public static void remove(ResourceKey<Level> dimension, BlockPos station, long shipment) {
        Map<Long, Set<Long>> stations = INCOMING.get(dimension);
        if (stations == null) {
            return;
        }
        Set<Long> ids = stations.get(station.asLong());
        if (ids != null && ids.remove(shipment) && ids.isEmpty()) {
            stations.remove(station.asLong());
        }
    }

    /** How many open transfers are on their way to the station. */
    public static int count(ResourceKey<Level> dimension, BlockPos station) {
        Map<Long, Set<Long>> stations = INCOMING.get(dimension);
        Set<Long> ids = stations == null ? null : stations.get(station.asLong());
        return ids == null ? 0 : ids.size();
    }

    /** Forgets everything, when the server stops. */
    public static void clear() {
        INCOMING.clear();
    }
}
