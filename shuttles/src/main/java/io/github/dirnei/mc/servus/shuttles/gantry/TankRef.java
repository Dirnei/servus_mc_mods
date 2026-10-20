package io.github.dirnei.mc.servus.shuttles.gantry;

import net.minecraft.core.BlockPos;

/** One tank of a Fluid Gantry: the master's position and the tank, 0 left and 1 right. */
public record TankRef(BlockPos gantry, int tank) {
}
