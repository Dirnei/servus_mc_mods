package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.shuttles.rail.RailFacings;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The facing of a newly placed rack: that of a rack of the same kind whose
 * face was clicked, else that of the rack of the same kind directly below,
 * else its front turned to a rail (see {@link RailFacings}), or to the player.
 */
public final class RackFacing {

    private RackFacing() {
    }

    /** @param kind whether a block is a rack of the placed kind */
    public static Direction of(BlockPlaceContext context, Predicate<Block> kind) {
        Level level = context.getLevel();
        if (!context.replacingClickedOnBlock()) {
            BlockState clicked = level.getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
            if (kind.test(clicked.getBlock())) {
                return clicked.getValue(HorizontalDirectionalBlock.FACING);
            }
        }
        BlockState below = level.getBlockState(context.getClickedPos().below());
        if (kind.test(below.getBlock())) {
            return below.getValue(HorizontalDirectionalBlock.FACING);
        }
        return RailFacings.frontToRail(context, RailFacings.ANY_RAIL);
    }
}
