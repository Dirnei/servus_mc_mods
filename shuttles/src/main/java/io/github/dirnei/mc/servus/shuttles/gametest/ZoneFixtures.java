package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.zone.WarehouseZoneData;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Team zones and handhelds for GameTests. */
final class ZoneFixtures {

    private ZoneFixtures() {
    }

    /** Creates a zone for a team of its own, so tests running side by side never clash on names; returns its id. */
    static int zone(GameTestHelper helper, String name, ZoneFilter filter) {
        return zone(helper, UUID.randomUUID(), name, filter);
    }

    static int zone(GameTestHelper helper, UUID team, String name, ZoneFilter filter) {
        return WarehouseZoneData.get(helper.getLevel().getServer()).create(team, name, 0, filter).orElseThrow().id();
    }

    /** A handheld in ZONE mode with that zone id active; 0 is "No zone". */
    static ItemStack handheld(int activeZone) {
        ItemStack handheld = new ItemStack(ServusShuttlesItems.HANDHELD.get());
        handheld.set(ServusShuttlesComponents.HANDHELD_MODE.get(), HandheldMode.ZONE);
        handheld.set(ServusShuttlesComponents.HANDHELD_ZONE.get(), activeZone);
        return handheld;
    }

    /** A handheld with a fresh zone of that name and filter active. */
    static ItemStack handheld(GameTestHelper helper, String name, ZoneFilter filter) {
        return handheld(zone(helper, name, filter));
    }

    /** A handheld in the given mode. */
    static ItemStack handheld(HandheldMode mode) {
        ItemStack handheld = new ItemStack(ServusShuttlesItems.HANDHELD.get());
        handheld.set(ServusShuttlesComponents.HANDHELD_MODE.get(), mode);
        return handheld;
    }
}
