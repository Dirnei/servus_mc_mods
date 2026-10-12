package io.github.dirnei.mc.servus.storage.site;

import net.minecraft.world.level.saveddata.SavedDataType;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Every site of the world, kept while it sleeps: name, team, chunks, upgrades and last known stock. */
public final class SiteRegistry extends SavedData {

    private static final String NAME = "servus_storage_sites";
    private static final SavedDataType<SiteRegistry> FACTORY = ValueIo.tagSavedData(NAME, SiteRegistry::new, SiteRegistry::load,
            (s, tag, registries) -> s.save(tag, registries));

    private final Map<String, SiteRecord> sites = new LinkedHashMap<>();

    public static SiteRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY);
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

    /**
     * Whether the controller of the site holds a Wireless Upgrade, is powered
     * for wireless, and whether the site has a Packing Station; written only
     * when one of them changed.
     */
    public void updateWireless(WarehouseControllerBlockEntity controller, boolean upgrade, boolean powered, boolean packing) {
        SiteRecord record = update(controller);
        if (record.upgrade != upgrade || record.powered != powered || record.packing != packing) {
            record.upgrade = upgrade;
            record.powered = powered;
            record.packing = packing;
            setDirty();
        }
    }

    /** Keeps the FE the controller holds now, for the wake of the sleeping site; written only when it changed. */
    public void updateEnergy(WarehouseControllerBlockEntity controller) {
        SiteRecord record = update(controller);
        int energy = controller.energyStored();
        if (record.energy != energy) {
            record.energy = energy;
            setDirty();
        }
    }

    /**
     * As {@link #remember(ResourceKey, BlockPos, String, UUID, Set, int, Map, boolean, boolean, boolean)},
     * with the FE its controller held when it went to sleep.
     */
    public SiteRecord remember(ResourceKey<Level> dimension, BlockPos pos, String name, UUID team, Set<Long> span, int upgrades,
                               Map<ItemKey, Long> stock, boolean upgrade, boolean powered, boolean packing, int energy) {
        SiteRecord record = remember(dimension, pos, name, team, span, upgrades, stock, upgrade, powered, packing);
        record.energy = Math.max(0, energy);
        setDirty();
        return record;
    }

    /**
     * Writes a record without a controller, for GameTests and tools: a site
     * as it is remembered while it sleeps.
     */
    public SiteRecord remember(ResourceKey<Level> dimension, BlockPos pos, String name, UUID team, Set<Long> span, int upgrades,
                               Map<ItemKey, Long> stock, boolean upgrade, boolean powered, boolean packing) {
        SiteRecord record = new SiteRecord(dimension, pos);
        record.name = name;
        record.team = team;
        record.span.addAll(span);
        record.upgrades = upgrades;
        record.stock.putAll(stock);
        record.upgrade = upgrade;
        record.powered = powered;
        record.packing = packing;
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
        ListTag list = tag.getListOrEmpty("Sites");
        for (int i = 0; i < list.size(); i++) {
            SiteRecord record = SiteRecord.load(list.getCompoundOrEmpty(i), registries);
            if (record != null) {
                registry.sites.put(record.key(), record);
            }
        }
        return registry;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        sites.values().forEach(record -> list.add(record.save(registries)));
        tag.put("Sites", list);
        return tag;
    }
}
