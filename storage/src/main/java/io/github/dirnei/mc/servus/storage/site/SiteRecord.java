package io.github.dirnei.mc.servus.storage.site;

import net.minecraft.core.UUIDUtil;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
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
import net.minecraft.resources.Identifier;
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
    /** Whether the site's controller holds a Wireless Upgrade. */
    boolean upgrade;
    /** Whether the site's controller holds the FE of one item at the smallest wireless cost. */
    boolean powered;
    /** Whether the site has a Packing Station on its rails. */
    boolean packing;
    long lastSeen;

    public SiteRecord(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos.immutable();
    }

    /** The key of a site: its dimension and its controller position. */
    public static String key(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.identifier() + "@" + pos.asLong();
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

    /** Whether the site's controller holds a Wireless Upgrade. */
    public boolean hasUpgrade() {
        return upgrade;
    }

    /** Whether the site's controller is powered for wireless; kept while the site sleeps. */
    public boolean powered() {
        return powered;
    }

    /** Whether the site has a Packing Station on its rails. */
    public boolean hasPacking() {
        return packing;
    }

    /** Whether the site's controller is wireless capable: it holds the upgrade and is powered. */
    public boolean canTrade() {
        return upgrade && powered;
    }

    /** Game time of the last stock report. */
    public long lastSeen() {
        return lastSeen;
    }

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dim", dimension.identifier().toString());
        tag.putLong("Pos", pos.asLong());
        tag.putString("Name", name);
        if (team != null) {
            tag.store("Team", UUIDUtil.CODEC, team);
        }
        tag.putLongArray("Span", span.stream().mapToLong(Long::longValue).toArray());
        tag.putInt("Upgrades", upgrades);
        tag.putLong("Seen", lastSeen);
        ListTag items = new ListTag();
        stock.forEach((key, count) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("Item", ValueIo.saveStack(registries, key.stack(1)));
            entry.putLong("Count", count);
            items.add(entry);
        });
        tag.put("Stock", items);
        tag.putBoolean("Wireless", upgrade);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Packing", packing);
        return tag;
    }

    static SiteRecord load(CompoundTag tag, HolderLookup.Provider registries) {
        Identifier dim = Identifier.tryParse(tag.getStringOr("Dim", ""));
        if (dim == null) {
            return null;
        }
        SiteRecord record = new SiteRecord(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(tag.getLongOr("Pos", 0L)));
        record.name = tag.getStringOr("Name", "");
        record.team = tag.read("Team", UUIDUtil.CODEC).isPresent() ? tag.read("Team", UUIDUtil.CODEC).orElseThrow() : null;
        for (long key : tag.getLongArray("Span").orElse(new long[0])) {
            record.span.add(key);
        }
        record.upgrades = tag.getIntOr("Upgrades", 0);
        record.lastSeen = tag.getLongOr("Seen", 0L);
        record.upgrade = tag.getBooleanOr("Wireless", false);
        record.powered = tag.getBooleanOr("Powered", false);
        record.packing = tag.getBooleanOr("Packing", false);
        ListTag items = tag.getListOrEmpty("Stock");
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompoundOrEmpty(i);
            ItemStack stack = ValueIo.parseStack(registries, entry.getCompoundOrEmpty("Item"));
            if (!stack.isEmpty()) {
                record.stock.merge(ItemKey.of(stack), entry.getLongOr("Count", 0L), Long::sum);
            }
        }
        return record;
    }
}
