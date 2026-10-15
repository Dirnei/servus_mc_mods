package io.github.dirnei.mc.servus.foundation.logistics.tank;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import java.util.List;
import net.minecraft.network.chat.Component;

/** The chat report of the Data Potato on a Fluid Tank: the whole tank's fill line and its size in blocks. Server thread only. */
public final class TankReport {

    private TankReport() {
    }

    public static List<Component> of(FluidTankBlockEntity tank) {
        NodePos pos = Conversions.toNodePos(tank.getBlockPos());
        TankShape box = tank.network().map(network -> network.boxAt(pos)).orElseGet(() -> TankShape.single(pos));
        return List.of(FluidTankBlock.describe(tank.tankContent(), tank.tankCapacity()),
                Component.translatable("message.servus_foundation.potato.tank_size", box.width(), box.height(), box.depth()));
    }
}
