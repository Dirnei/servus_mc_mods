package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * The chat report of the Data Potato on a Heavy Duty Rack: the zone, bays and
 * slots of its whole shelf (a slot a job locked counts as used) and the
 * items it holds most. Server thread only.
 */
public final class RackReport {

    private static final String KEY = "message.servus_shuttles.potato.rack_";

    private RackReport() {
    }

    public static List<Component> of(Level level, BlockPos pos) {
        Optional<ShelfStock> found = ShelfStock.of(level, pos, ShuttlesStatsManager.INSTANCE.rackPotatoTopItems());
        if (found.isEmpty()) {
            return List.of();
        }
        ShelfStock stock = found.get();
        Set<String> zones = new LinkedHashSet<>();
        for (HeavyRackBlockEntity bay : stock.bays()) {
            if (!bay.zone().isEmpty()) {
                zones.add(bay.zone());
            }
        }
        ShelfFill<ItemKey> fill = stock.fill();
        List<Component> report = new ArrayList<>();
        report.add(zones.isEmpty() ? Component.translatable(KEY + "no_zone") : Component.translatable(KEY + "zone", String.join(", ", zones)));
        report.add(Component.translatable(fill.full() ? KEY + "slots_full" : KEY + "slots", stock.bays().size(), fill.used(), fill.total()));
        if (fill.isEmpty()) {
            report.add(Component.translatable(KEY + "empty"));
        }
        for (ShelfFill.Entry<ItemKey> entry : fill.top()) {
            report.add(Component.translatable(KEY + "item", entry.key().stack(1).getHoverName(), entry.count()));
        }
        return report;
    }
}
