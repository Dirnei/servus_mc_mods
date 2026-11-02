package io.github.dirnei.mc.servus.shuttles.site;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.world.level.saveddata.SavedDataType;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
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
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Every site of the world, kept while it sleeps: name, team, chunks, upgrades and last known stock. */
public final class SiteRegistry extends SavedData {

    private static final String NAME = ServusShuttlesIds.MOD_ID + "_sites";
    private static final SavedDataType<SiteRegistry> FACTORY = ValueIo.tagSavedData(NAME, SiteRegistry::new, SiteRegistry::load,
            (s, tag, registries) -> s.save(tag, registries));

    private final Map<String, SiteRecord> sites = new LinkedHashMap<>();
    private final WarehouseNumbers numbers = new WarehouseNumbers();

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

    /** The site of the warehouse with that number. */
    public Optional<SiteRecord> byNumber(int number) {
        return sites.values().stream().filter(site -> site.number == number).findFirst();
    }

    /** The number of the controller's warehouse, recording the site first when it has no record yet. */
    public int number(WarehouseControllerBlockEntity controller) {
        return update(controller).number;
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
        UUID id = controller.warehouseId();
        if (!id.equals(record.warehouse)) {
            record.number = numbers.numberOf(id, record.warehouse == null ? record.number : 0);
            record.warehouse = id;
            setDirty();
        }
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
        record.number = numbers.next();
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
        Map<UUID, Integer> known = new LinkedHashMap<>();
        ListTag numbered = tag.getListOrEmpty("Numbers");
        for (int i = 0; i < numbered.size(); i++) {
            CompoundTag entry = numbered.getCompoundOrEmpty(i);
            entry.read("Id", UUIDUtil.CODEC).ifPresent(id -> known.put(id, entry.getIntOr("Number", 0)));
        }
        registry.numbers.load(known, tag.getIntOr("LastNumber", 0));
        registry.sites.values().forEach(record -> registry.numbers.reserve(record.number));
        registry.numberOldRecords();
        return registry;
    }

    /** Gives the records of a save from before warehouse numbers a number each, in position order. */
    private void numberOldRecords() {
        List<SiteRecord> unnumbered = sites.values().stream().filter(record -> record.number <= 0).toList();
        if (unnumbered.isEmpty()) {
            return;
        }
        List<WarehouseNumbers.Place> places = unnumbered.stream().map(record -> new WarehouseNumbers.Place(
                record.dimension().identifier().toString(), record.pos().getX(), record.pos().getY(), record.pos().getZ())).toList();
        int[] given = numbers.numberInOrder(places);
        for (int i = 0; i < given.length; i++) {
            SiteRecord record = unnumbered.get(i);
            record.number = given[i];
            if (record.warehouse != null) {
                numbers.numberOf(record.warehouse, record.number);
            }
        }
        setDirty();
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        sites.values().forEach(record -> list.add(record.save(registries)));
        tag.put("Sites", list);
        ListTag numbered = new ListTag();
        numbers.all().forEach((id, number) -> {
            CompoundTag entry = new CompoundTag();
            entry.store("Id", UUIDUtil.CODEC, id);
            entry.putInt("Number", number);
            numbered.add(entry);
        });
        tag.put("Numbers", numbered);
        tag.putInt("LastNumber", numbers.last());
        return tag;
    }
}
