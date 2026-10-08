package io.github.dirnei.mc.servus.logistics.duct;

import io.github.dirnei.mc.servus.lib.placement.PlacementAssist;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sneak placement for a line item: any duct starts a walk, the run is the
 * ducts carrying the line and the direction is the clicked face.
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
    public Direction direction(BlockHitResult hit) {
        return hit.getDirection();
    }
}
