package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The chat report of the Data Potato on a fluid station: one line for the
 * tank, on a Fluid Outbound followed by what it keeps and its restock level.
 * Server thread only.
 */
public final class FluidStationReport {

    private static final String KEY = "message.servus_shuttles.potato.fluid_station_";

    private FluidStationReport() {
    }

    public static List<Component> of(FluidStationBlockEntity station) {
        FluidSlot tank = station.tank();
        MutableComponent line = tank.isEmpty()
                ? Component.translatable(KEY + "empty", tank.getCapacity())
                : Component.translatable(KEY + "tank", tank.getFluid().getHoverName(), tank.getFluidAmount(), tank.getCapacity());
        if (station instanceof FluidOutboundBlockEntity outbound) {
            line.append(keeps(outbound.keep()));
        }
        return List.of(line);
    }

    private static Component keeps(FluidKeep<Fluid> keep) {
        Fluid fluid = keep.fluid();
        return fluid == null ? Component.translatable(KEY + "keeps_nothing")
                : Component.translatable(KEY + "keeps_restock", keep.amount(), new FluidStack(fluid, 1).getHoverName(), keep.restockAt());
    }
}
