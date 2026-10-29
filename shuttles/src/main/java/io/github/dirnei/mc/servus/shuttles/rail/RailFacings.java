package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** Minecraft side of {@link RailFacing}: reads the rails around a placement. */
public final class RailFacings {

    /** Warehouse Rails and Farm Rails. */
    public static final Predicate<Block> ANY_RAIL = block -> block instanceof RailBlock;

    private RailFacings() {
    }

    /** The direction towards the rail the placed block turns to, or empty when it faces the player. */
    public static Optional<Direction> railSide(BlockPlaceContext context, Predicate<Block> isRail) {
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isSecondaryUseActive();
        return RailFacing.railSide(clickedRail(context, isRail), railsBeside(context, isRail),
                Conversions.toSide(context.getHorizontalDirection()), sneaking).map(Conversions::toDirection);
    }

    /** The facing of a block whose front docks to the rail: towards the rail, else towards the player. */
    public static Direction frontToRail(BlockPlaceContext context, Predicate<Block> isRail) {
        return railSide(context, isRail).orElse(context.getHorizontalDirection().getOpposite());
    }

    /** The side towards the rail whose side face the player clicked to place the block. */
    private static Optional<Side> clickedRail(BlockPlaceContext context, Predicate<Block> isRail) {
        Direction towardsRail = context.getClickedFace().getOpposite();
        if (!towardsRail.getAxis().isHorizontal()) {
            return Optional.empty();
        }
        BlockPos clicked = context.getClickedPos().relative(towardsRail);
        return isRail.test(context.getLevel().getBlockState(clicked).getBlock())
                ? Optional.of(Conversions.toSide(towardsRail))
                : Optional.empty();
    }

    private static Set<Side> railsBeside(BlockPlaceContext context, Predicate<Block> isRail) {
        Set<Side> sides = EnumSet.noneOf(Side.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isRail.test(context.getLevel().getBlockState(context.getClickedPos().relative(direction)).getBlock())) {
                sides.add(Conversions.toSide(direction));
            }
        }
        return sides;
    }
}
