package io.github.dirnei.mc.servus.lib.placement;

import io.github.dirnei.mc.servus.lib.config.ServusLibConfig;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Minecraft side of the placement assist. Server and client run the same
 * walk; the server's result is the one that places.
 */
public final class PlacementAssists {

    /** Where an assisted click places and which way it walked. */
    public record Target(BlockPos pos, Direction direction) {

        /** A place context as if the player clicked the last block of the run towards the target. */
        public BlockPlaceContext context(BlockPlaceContext original) {
            return BlockPlaceContext.at(original, pos.relative(direction.getOpposite()), direction);
        }
    }

    private PlacementAssists() {
    }

    /** Whether the click is a sneak click on a block that starts a walk. */
    public static boolean applies(BlockPlaceContext context, PlacementAssist assist) {
        Player player = context.getPlayer();
        return player != null && player.isSecondaryUseActive()
                && assist.isStart(context.getLevel(), hitPos(context), context.getItemInHand());
    }

    /**
     * The target of an assisted click, or empty when the run ends at a block
     * that cannot be replaced or runs past the reach. Only meaningful when
     * {@link #applies} holds.
     */
    public static Optional<Target> target(BlockPlaceContext context, PlacementAssist assist) {
        Level level = context.getLevel();
        BlockHitResult hit = hitResult(context);
        Direction direction = assist.direction(hit);
        BlockPos start = hit.getBlockPos();
        Player player = context.getPlayer();
        Optional<RunWalk.Cell> cell = RunWalk.walk(cell(start), cell(direction),
                c -> assist.isRun(level, pos(c), context.getItemInHand()),
                c -> {
                    BlockPos pos = pos(c);
                    Target target = new Target(pos, direction);
                    return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                            && (player == null || player.mayUseItemAt(pos, direction, context.getItemInHand()))
                            && level.getBlockState(pos).canBeReplaced(target.context(context));
                },
                ServusLibConfig.placementAssistReach());
        return cell.map(c -> new Target(pos(c), direction));
    }

    /**
     * Places for an assisted item: unchanged through {@code place} without a
     * sneak click on a start block, at the end of the run with one, and not at
     * all when the run has no target.
     */
    public static InteractionResult place(BlockPlaceContext context, PlacementAssist assist,
                                          Function<BlockPlaceContext, InteractionResult> place) {
        if (!applies(context, assist)) {
            return place.apply(context);
        }
        return target(context, assist)
                .map(target -> place.apply(target.context(context)))
                .orElse(InteractionResult.FAIL);
    }

    /** The clicked block, not the spot in front of it that a place context reports. */
    private static BlockPos hitPos(BlockPlaceContext context) {
        return hitResult(context).getBlockPos();
    }

    private static BlockHitResult hitResult(BlockPlaceContext context) {
        return new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.replacingClickedOnBlock() ? context.getClickedPos()
                        : context.getClickedPos().relative(context.getClickedFace().getOpposite()),
                context.isInside());
    }

    private static RunWalk.Cell cell(BlockPos pos) {
        return new RunWalk.Cell(pos.getX(), pos.getY(), pos.getZ());
    }

    private static RunWalk.Cell cell(Direction direction) {
        return new RunWalk.Cell(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static BlockPos pos(RunWalk.Cell cell) {
        return new BlockPos(cell.x(), cell.y(), cell.z());
    }
}
