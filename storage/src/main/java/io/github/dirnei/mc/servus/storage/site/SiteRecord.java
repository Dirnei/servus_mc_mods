package io.github.dirnei.mc.servus.storage.site;

import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** What the world remembers of one site, also while it sleeps. */
public final class SiteRecord {

    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    String name = "";
    UUID team;
    final Set<Long> span = new LinkedHashSet<>();
    int upgrades;
    final Map<ItemKey, Long> stock = new LinkedHashMap<>();
    /** Teams with a Wireless Packing Station at the site. */
    final Set<UUID> wireless = new LinkedHashSet<>();
    long lastSeen;

    public SiteRecord(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos.immutable();
    }

    /** The key of a site: its dimension and its controller position. */
    public static String key(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "@" + pos.asLong();
    }

    public String key() {
        return key(dimension, pos);
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    /** The controller's position. */
    public BlockPos pos() {
        return pos;
    }

    /** The name to show. */
    public String name() {
        return name;
    }

    public Optional<UUID> team() {
        return Optional.ofNullable(team);
    }

    public Set<Long> span() {
        return Collections.unmodifiableSet(span);
    }

    public int upgrades() {
        return upgrades;
    }

    /** Chunks the upgrades cover with the current numbers. */
    public int capacity() {
        return SiteCapacity.capacity(upgrades, StorageStatsManager.INSTANCE.chunksPerUpgrade());
    }

    public SiteCapacity.State capacityState() {
        return SiteCapacity.state(span.size(), upgrades, StorageStatsManager.INSTANCE.chunksPerUpgrade());
    }

    /** The stock the site last reported while it was online and whole. */
    public Map<ItemKey, Long> stock() {
        return Collections.unmodifiableMap(stock);
    }

    /** Whether a Wireless Packing Station of that team stands at the site. */
    public boolean hasWireless(UUID team) {
        return team != null && wireless.contains(team);
    }

    public Set<UUID> wireless() {
        return Collections.unmodifiableSet(wireless);
    }

    /** Game time of the last stock report. */
    public long lastSeen() {
        return lastSeen;
    }

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dim", dimension.location().toString());
        tag.putLong("Pos", pos.asLong());
        tag.putString("Name", name);
        if (team != null) {
            tag.putUUID("Team", team);
        }
        tag.putLongArray("Span", span.stream().mapToLong(Long::longValue).toArray());
        tag.putInt("Upgrades", upgrades);
        tag.putLong("Seen", lastSeen);
        ListTag items = new ListTag();
        stock.forEach((key, count) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("Item", key.stack(1).save(registries));
            entry.putLong("Count", count);
            items.add(entry);
        });
        tag.put("Stock", items);
        ListTag teams = new ListTag();
        wireless.forEach(id -> teams.add(net.minecraft.nbt.NbtUtils.createUUID(id)));
        tag.put("Wireless", teams);
        return tag;
    }

    static SiteRecord load(CompoundTag tag, HolderLookup.Provider registries) {
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString("Dim"));
        if (dim == null) {
            return null;
        }
        SiteRecord record = new SiteRecord(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(tag.getLong("Pos")));
        record.name = tag.getString("Name");
        record.team = tag.hasUUID("Team") ? tag.getUUID("Team") : null;
        for (long key : tag.getLongArray("Span")) {
            record.span.add(key);
        }
        record.upgrades = tag.getInt("Upgrades");
        record.lastSeen = tag.getLong("Seen");
        for (Tag raw : tag.getList("Wireless", Tag.TAG_INT_ARRAY)) {
            record.wireless.add(net.minecraft.nbt.NbtUtils.loadUUID(raw));
        }
        ListTag items = tag.getList("Stock", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Item"));
            if (!stack.isEmpty()) {
                record.stock.merge(ItemKey.of(stack), entry.getLong("Count"), Long::sum);
            }
        }
        return record;
    }
}
