package io.github.dirnei.mc.servus.foundation.logistics.duct;

import io.github.dirnei.mc.servus.foundation.placement.PlacementAssist;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sneak placement for a line item: any duct starts a walk and the run is the
 * ducts carrying the line. On a straight run of the line the walk keeps that
 * axis; otherwise the direction is the clicked face.
 */
public final class DuctAssist implements PlacementAssist {

    private final Line line;

    public DuctAssist(Line line) {
        this.line = line;
    }

    @Override
    public boolean isStart(Level level, BlockPos pos, ItemStack stack) {
        return level.getBlockState(pos).getBlock() instanceof UtilityDuctBlock;
    }

    @Override
    public boolean isRun(Level level, BlockPos pos, ItemStack stack) {
        return level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && duct.has(line);
    }

    @Override
    public Direction direction(Level level, BlockHitResult hit, ItemStack stack) {
        int connections = 0;
        if (level.getBlockEntity(hit.getBlockPos()) instanceof UtilityDuctBlockEntity duct) {
            for (Direction side : Direction.values()) {
                if (duct.hasArm(line, side)) {
                    connections |= 1 << side.ordinal();
                }
            }
        }
        return PlacementAssists.alongRun(connections, hit).orElse(hit.getDirection());
    }
}
