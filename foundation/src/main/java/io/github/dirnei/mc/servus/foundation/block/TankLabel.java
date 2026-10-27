package io.github.dirnei.mc.servus.foundation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The name and colour the side panel, its tooltips, the action bar and the markers show for one
 * tank of a Fluids layer.
 *
 * @param color opaque ARGB
 */
public record TankLabel(Component name, int color) {

    /** The grey of a tank that is empty and set to nothing. */
    public static final int NEUTRAL = 0xFF9A9A9A;

    /** "Tank 2" in the neutral grey, for the tank with index 1. */
    public static TankLabel numbered(int tank) {
        return new TankLabel(Component.translatable("gui.servus_foundation.fluid_side.tank", tank + 1), NEUTRAL);
    }

    /** The fluid's name in its map colour, or {@link #numbered} for no fluid. */
    public static TankLabel of(Fluid fluid, int tank) {
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return numbered(tank);
        }
        int rgb = fluid.defaultFluidState().createLegacyBlock().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
        return new TankLabel(fluid.getFluidType().getDescription(), rgb == 0 ? NEUTRAL : 0xFF000000 | rgb);
    }
}
