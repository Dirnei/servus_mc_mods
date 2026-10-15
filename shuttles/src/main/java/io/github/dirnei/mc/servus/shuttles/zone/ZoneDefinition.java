package io.github.dirnei.mc.servus.shuttles.zone;

import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * A zone as a label defines it and a block carries it: a display name and a filter.
 *
 * @param name   the name as typed, never blank
 * @param filter what the zone takes; empty means anything
 */
public record ZoneDefinition(String name, ZoneFilter filter) {

    public ZoneDefinition {
        name = ZoneNames.display(name);
    }

    /** The key zones are grouped by. */
    public String key() {
        return ZoneNames.key(name);
    }

    /** The zone a Zone Label carries, or null when the label has no name. */
    public static ZoneDefinition of(ItemStack label) {
        if (!label.has(DataComponents.CUSTOM_NAME)) {
            return null;
        }
        String name = ZoneNames.display(label.getHoverName().getString());
        if (name.isEmpty()) {
            return null;
        }
        return new ZoneDefinition(name, label.getOrDefault(ServusShuttlesComponents.ZONE_FILTER.get(), ZoneFilter.EMPTY));
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = filter.save(registries);
        tag.putString("Name", name);
        return tag;
    }

    /** Reads a zone, or null when the tag has no name. */
    public static ZoneDefinition load(CompoundTag tag, HolderLookup.Provider registries) {
        String name = ZoneNames.display(tag.getStringOr("Name", ""));
        if (name.isEmpty()) {
            return null;
        }
        return new ZoneDefinition(name, ZoneFilter.load(tag, registries));
    }
}
