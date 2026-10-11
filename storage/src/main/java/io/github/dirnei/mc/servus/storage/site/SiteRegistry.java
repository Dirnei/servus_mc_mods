package io.github.dirnei.mc.servus.storage.site;

import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Every site of the world, kept while it sleeps: name, team, chunks, upgrades and last known stock. */
public final class SiteRegistry extends SavedData {

    private static final String NAME = "servus_storage_sites";
    private static final Factory<SiteRegistry> FACTORY = new Factory<>(SiteRegistry::new, SiteRegistry::load, null);

    private final Map<String, SiteRecord> sites = new LinkedHashMap<>();

    public static SiteRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public static SiteRegistry of(ServerLevel level) {
        return get(level.getServer());
    }

    public Optional<SiteRecord> site(ResourceKey<Level> dimension, BlockPos pos) {
        return Optional.ofNullable(sites.get(SiteRecord.key(dimension, pos)));
    }

    public Optional<SiteRecord> site(String key) {
        return Optional.ofNullable(sites.get(key));
    }

    /** Every site, sorted by name. */
    public List<SiteRecord> all() {
        List<SiteRecord> list = new ArrayList<>(sites.values());
        list.sort(Comparator.comparing(SiteRecord::name, String.CASE_INSENSITIVE_ORDER).thenComparing((SiteRecord site) -> site.key()));
        return list;
    }

    /** The sites of one team in one dimension, sorted by name. */
    public List<SiteRecord> ofTeam(UUID team, ResourceKey<Level> dimension) {
        return all().stream().filter(site -> team.equals(site.team) && site.dimension().equals(dimension)).toList();
    }

    /** Writes name, team, span and upgrades of a loaded controller into its record. */
    public SiteRecord update(WarehouseControllerBlockEntity controller) {
        ResourceKey<Level> dimension = controller.getLevel().dimension();
        BlockPos pos = controller.getBlockPos();
        SiteRecord record = sites.computeIfAbsent(SiteRecord.key(dimension, pos), key -> {
            setDirty();
            return new SiteRecord(dimension, pos);
        });
        String name = controller.displayName();
        UUID team = controller.team().orElse(null);
        Set<Long> span = new LinkedHashSet<>(controller.span());
        int upgrades = controller.upgrades().count();
        if (!name.equals(record.name) || !Objects.equals(team, record.team) || !span.equals(record.span) || upgrades != record.upgrades) {
            record.name = name;
            record.team = team;
            record.span.clear();
            record.span.addAll(span);
            record.upgrades = upgrades;
            setDirty();
        }
        return record;
    }

    /** The stock a whole, online site reports; written only when it changed. */
    public void updateStock(WarehouseControllerBlockEntity controller, List<Map.Entry<ItemKey, Long>> entries, long gameTime) {
        SiteRecord record = update(controller);
        Map<ItemKey, Long> stock = new LinkedHashMap<>();
        entries.forEach(entry -> stock.put(entry.getKey(), entry.getValue()));
        record.lastSeen = gameTime;
        if (!stock.equals(record.stock)) {
            record.stock.clear();
            record.stock.putAll(stock);
            setDirty();
        }
    }

    /** The teams with a Wireless Packing Station at the controller's site; written only when they changed. */
    public void updateWireless(WarehouseControllerBlockEntity controller, Set<UUID> teams) {
        SiteRecord record = update(controller);
        if (!record.wireless.equals(teams)) {
            record.wireless.clear();
            record.wireless.addAll(teams);
            setDirty();
        }
    }

    /**
     * Writes a record without a controller, for GameTests and tools: a site
     * as it is remembered while it sleeps.
     */
    public SiteRecord remember(ResourceKey<Level> dimension, BlockPos pos, String name, UUID team, Set<Long> span, int upgrades,
                               Map<ItemKey, Long> stock, Set<UUID> wireless) {
        SiteRecord record = new SiteRecord(dimension, pos);
        record.name = name;
        record.team = team;
        record.span.addAll(span);
        record.upgrades = upgrades;
        record.stock.putAll(stock);
        record.wireless.addAll(wireless);
        sites.put(record.key(), record);
        setDirty();
        return record;
    }

    public void remove(ResourceKey<Level> dimension, BlockPos pos) {
        if (sites.remove(SiteRecord.key(dimension, pos)) != null) {
            setDirty();
        }
    }

    private static SiteRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        SiteRegistry registry = new SiteRegistry();
        ListTag list = tag.getList("Sites", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            SiteRecord record = SiteRecord.load(list.getCompound(i), registries);
            if (record != null) {
                registry.sites.put(record.key(), record);
            }
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        sites.values().forEach(record -> list.add(record.save(registries)));
        tag.put("Sites", list);
        return tag;
    }
}
