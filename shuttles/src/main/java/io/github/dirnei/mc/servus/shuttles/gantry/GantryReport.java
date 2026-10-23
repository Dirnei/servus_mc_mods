package io.github.dirnei.mc.servus.shuttles.gantry;

import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** The chat report of the Data Potato on a Fluid Gantry: its mode and each tank's fill line and lock. Server thread only. */
public final class GantryReport {

    private static final String KEY = "message.servus_shuttles.potato.gantry_";
    private static final int TANKS = 2;

    private GantryReport() {
    }

    public static List<Component> of(FluidGantryBlockEntity gantry) {
        List<Component> report = new ArrayList<>();
        report.add(Component.translatable(KEY + "mode",
                Component.translatable("gui.servus_shuttles.fluid_gantry.mode." + gantry.mode().getSerializedName())));
        for (int tank = 0; tank < TANKS; tank++) {
            Fluid lock = gantry.lock(tank);
            report.add(Component.translatable(KEY + "tank",
                    Component.translatable("gui.servus_shuttles.fluid_gantry.tank_" + tank),
                    FluidTankBlock.describe(gantry.tank(tank).getFluid(), gantry.tank(tank).getCapacity()),
                    lock == null ? Component.translatable(KEY + "any") : new FluidStack(lock, 1).getHoverName()));
        }
        return report;
    }
}
