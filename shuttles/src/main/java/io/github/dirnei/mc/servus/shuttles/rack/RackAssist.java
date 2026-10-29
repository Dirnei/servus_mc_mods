package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.foundation.placement.PlacementAssist;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sneak placement for racks: the run is the racks of one kind with the
 * clicked rack's facing. A click on a side face walks along the row that way,
 * a click on the top or bottom walks up or down the column, and a click on
 * the front or back walks towards the edge of the face nearest to the hit:
 * along the row for the left and right edges, up or down the column for the
 * top and bottom edges.
 */
public final class RackAssist implements PlacementAssist {

    public static final RackAssist HEAVY = new RackAssist(block -> block instanceof HeavyRackBlock);
    public static final RackAssist FLUID = new RackAssist(block -> block instanceof FluidRackBlock);

    private final Predicate<Block> kind;

    private RackAssist(Predicate<Block> kind) {
        this.kind = kind;
    }

    @Override
    public boolean isRun(Level level, BlockPos pos, ItemStack stack) {
        return kind.test(level.getBlockState(pos).getBlock());
    }

    @Override
    public boolean isRun(Level level, BlockPos start, BlockPos pos, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        return kind.test(state.getBlock()) && facing(state) == facing(level.getBlockState(start));
    }

    @Override
    public Direction direction(Level level, BlockHitResult hit, ItemStack stack) {
        Direction.Axis row = facing(level.getBlockState(hit.getBlockPos())).getClockWise().getAxis();
        Direction face = hit.getDirection();
        if (face.getAxis() == row || face.getAxis() == Direction.Axis.Y) {
            return face;
        }
        double along = hit.getLocation().get(row) - hit.getBlockPos().get(row);
        double up = hit.getLocation().y - hit.getBlockPos().getY();
        double toRowEdge = Math.min(along, 1 - along);
        if (Math.min(up, 1 - up) < toRowEdge) {
            return up < 0.5 ? Direction.DOWN : Direction.UP;
        }
        return Direction.fromAxisAndDirection(row, along < 0.5 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
    }

    private static Direction facing(BlockState state) {
        return state.getValue(HorizontalDirectionalBlock.FACING);
    }
}
