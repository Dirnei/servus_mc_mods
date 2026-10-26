package io.github.dirnei.mc.servus.shuttles.station;

import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** The chat report of the Data Potato on a station, after its label: its site and whether it is online. Server thread only. */
public final class StationReport {

    private static final String KEY = "message.servus_shuttles.potato.station_";

    private StationReport() {
    }

    public static List<Component> of(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof StationBlockEntity station)) {
            return List.of();
        }
        List<Component> report = new ArrayList<>();
        String offline = station.offlineReason();
        if (!offline.isEmpty()) {
            report.add(Component.translatable(KEY + "offline", Component.translatable("gui.servus_shuttles.terminal." + offline)));
            return report;
        }
        station.network().flatMap(WarehouseNetwork::controller)
                .ifPresent(controller -> report.add(Component.translatable(KEY + "site", controller.displayName())));
        report.add(Component.translatable(KEY + "online"));
        return report;
    }
}
