package io.github.dirnei.mc.servus.shuttles.network;

import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;

/** The zones the stock blocks of one warehouse carry, and the order in which they take a stack. */
final class WarehouseZones {

    /** The zone of each zoned stock block; unzoned blocks are absent. */
    private final Map<BlockPos, ZoneDefinition> zoneOf = new LinkedHashMap<>();

    /** Stamps the zone of a block; null clears it. */
    void set(BlockPos pos, ZoneDefinition definition) {
        if (definition == null) {
            zoneOf.remove(pos);
        } else {
            zoneOf.put(pos, definition);
        }
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

    /** Tag ids of an item, as the pure plan sees them. */
    private static Set<String> tagsOf(ItemKey key) {
        return key.stack(1).typeHolder().tags().map(tag -> tag.location().toString()).collect(Collectors.toSet());
    }

    /** The zoned sources that take this key, by exact item and by tag. */
    PutAwayPlan.Tiers<BlockPos> tiers(ItemKey key, Set<BlockPos> sources) {
        Map<BlockPos, PutAwayPlan.Filter<ItemKey>> filtered = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, ZoneDefinition> entry : zoneOf.entrySet()) {
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
            ZoneDefinition zone = zoneOf.get(pos);
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
            ZoneDefinition zone = zoneOf.get(pos);
            if (zone == null || zone.filter().isEmpty()) {
                general.add(pos);
            }
        }
        return PutAwayPlan.targets(tiers, general, holders);
    }

    /** Stock blocks per zone key, sorted by key. */
    Map<String, Integer> blockCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        for (ZoneDefinition zone : zoneOf.values()) {
            counts.merge(zone.key(), 1, Integer::sum);
        }
        return counts;
    }

    /** Every zone, sorted by key. */
    List<WarehouseNetwork.ZoneSummary> summaries() {
        List<WarehouseNetwork.ZoneSummary> list = new ArrayList<>();
        for (String key : blockCounts().keySet()) {
            list.add(summary(key));
        }
        return list;
    }

    private WarehouseNetwork.ZoneSummary summary(String key) {
        String display = key;
        int blocks = 0;
        Set<String> items = new LinkedHashSet<>();
        Set<String> tags = new LinkedHashSet<>();
        for (ZoneDefinition zone : zoneOf.values()) {
            if (!zone.key().equals(key)) {
                continue;
            }
            blocks++;
            display = zone.name();
            items.addAll(zone.filter().itemNames());
            tags.addAll(zone.filter().tags());
        }
        return new WarehouseNetwork.ZoneSummary(key, display, blocks, List.copyOf(items), List.copyOf(tags));
    }
}
