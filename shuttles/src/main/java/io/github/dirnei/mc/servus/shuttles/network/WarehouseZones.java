package io.github.dirnei.mc.servus.shuttles.network;

import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;

/**
 * The zone ids the stock blocks of one warehouse point at, and the order in
 * which they take a stack. Names and filters are resolved from the team zones
 * at each use, so an edited zone applies at once.
 */
final class WarehouseZones {

    /** The zone id of each zoned stock block; unzoned blocks are absent. */
    private final Map<BlockPos, Integer> zoneOf = new LinkedHashMap<>();
    private final IntFunction<ZoneDefinition> resolver;

    /** @param resolver the zone of an id, null for a deleted zone */
    WarehouseZones(IntFunction<ZoneDefinition> resolver) {
        this.resolver = resolver;
    }

    /** Sets the zone id of a block; {@link ZoneList#NONE} clears it. */
    void set(BlockPos pos, int id) {
        if (id == ZoneList.NONE) {
            zoneOf.remove(pos);
        } else {
            zoneOf.put(pos, id);
        }
    }

    /** The zone of a block, or null when it has none or its zone was deleted. */
    ZoneDefinition zoneOf(BlockPos pos) {
        Integer id = zoneOf.get(pos);
        return id == null ? null : resolver.apply(id);
    }

    void remove(BlockPos pos) {
        zoneOf.remove(pos);
    }

    void absorb(WarehouseZones other) {
        zoneOf.putAll(other.zoneOf);
    }

    void clear() {
        zoneOf.clear();
    }

    /** Every zoned block with its live zone; blocks of deleted zones are left out. */
    private Map<BlockPos, ZoneDefinition> resolved() {
        Map<BlockPos, ZoneDefinition> zones = new LinkedHashMap<>();
        for (BlockPos pos : zoneOf.keySet()) {
            ZoneDefinition zone = zoneOf(pos);
            if (zone != null) {
                zones.put(pos, zone);
            }
        }
        return zones;
    }

    /** Tag ids of an item, as the pure plan sees them. */
    private static Set<String> tagsOf(ItemKey key) {
        return key.stack(1).typeHolder().tags().map(tag -> tag.location().toString()).collect(Collectors.toSet());
    }

    /** The zoned sources that take this key, by exact item and by tag. */
    PutAwayPlan.Tiers<BlockPos> tiers(ItemKey key, Set<BlockPos> sources) {
        Map<BlockPos, PutAwayPlan.Filter<ItemKey>> filtered = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, ZoneDefinition> entry : resolved().entrySet()) {
            if (!entry.getValue().filter().isEmpty() && sources.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue().filter().toPure());
            }
        }
        return PutAwayPlan.tiers(key, filtered, WarehouseZones::tagsOf);
    }

    /** The zone name of the first block that matched, or null when none did. */
    String matchedZone(PutAwayPlan.Tiers<BlockPos> tiers) {
        String exact = firstZoneName(tiers.exact());
        return exact != null ? exact : firstZoneName(tiers.byTag());
    }

    private String firstZoneName(Set<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            ZoneDefinition zone = zoneOf(pos);
            if (zone != null) {
                return zone.name();
            }
        }
        return null;
    }

    /**
     * The target lists in order: exact matches, tag matches, then general
     * storage, which is every source without a filter; holders first within each.
     */
    List<List<BlockPos>> targets(PutAwayPlan.Tiers<BlockPos> tiers, Set<BlockPos> sources, List<BlockPos> holders) {
        Set<BlockPos> general = new LinkedHashSet<>();
        for (BlockPos pos : sources) {
            ZoneDefinition zone = zoneOf(pos);
            if (zone == null || zone.filter().isEmpty()) {
                general.add(pos);
            }
        }
        return PutAwayPlan.targets(tiers, general, holders);
    }

    /** Stock blocks per live zone name, sorted by name. */
    Map<String, Integer> blockCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        for (ZoneDefinition zone : resolved().values()) {
            counts.merge(zone.name(), 1, Integer::sum);
        }
        return counts;
    }

    /** Stock blocks per live zone id. */
    Map<Integer, Integer> blockCountsById() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (ZoneDefinition zone : resolved().values()) {
            counts.merge(zone.id(), 1, Integer::sum);
        }
        return counts;
    }

    /** Every live zone of the warehouse, sorted by name. */
    List<WarehouseNetwork.ZoneSummary> summaries() {
        Map<Integer, List<BlockPos>> blocks = new LinkedHashMap<>();
        Map<Integer, ZoneDefinition> zones = new LinkedHashMap<>();
        resolved().forEach((pos, zone) -> {
            blocks.computeIfAbsent(zone.id(), id -> new ArrayList<>()).add(pos);
            zones.put(zone.id(), zone);
        });
        List<WarehouseNetwork.ZoneSummary> list = new ArrayList<>();
        zones.forEach((id, zone) -> list.add(new WarehouseNetwork.ZoneSummary(zone.key(), zone.name(), blocks.get(id).size(),
                zone.filter().itemNames(), zone.filter().tags())));
        list.sort(Comparator.comparing((WarehouseNetwork.ZoneSummary summary) -> summary.display().toLowerCase(Locale.ROOT)));
        return list;
    }
}
