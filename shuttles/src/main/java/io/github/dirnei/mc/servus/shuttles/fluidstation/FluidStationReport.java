package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/** The chat report of the Data Potato on a fluid station: one line per tank. Server thread only. */
public final class FluidStationReport {

    private static final String KEY = "message.servus_shuttles.potato.fluid_station_";

    private FluidStationReport() {
    }

    public static List<Component> of(FluidStationBlockEntity station) {
        List<Component> report = new ArrayList<>();
        for (int index = 0; index < station.tankCount(); index++) {
            FluidSlot tank = station.tank(index);
            report.add(tank.isEmpty()
                    ? Component.translatable(KEY + "empty", index + 1, tank.getCapacity())
                    : Component.translatable(KEY + "tank", index + 1, tank.getFluid().getHoverName(), tank.getFluidAmount(), tank.getCapacity()));
        }
        return report;
    }
}
