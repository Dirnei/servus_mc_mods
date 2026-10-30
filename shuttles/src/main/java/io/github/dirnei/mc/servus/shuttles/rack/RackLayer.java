package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.foundation.placement.PlacementAssist;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * A whole layer of racks over one shelf: an unsneaked click on the top of
 * a rack with a rack item of its kind places one rack on every bay of the
 * clicked shelf, and a sneak click that walks up a column does the same on
 * top of the column, when every spot above is free and the held stack has an
 * item per bay (or the player is in creative). Each bay is placed as a click
 * on the top of the bay below, so it takes that bay's facing and joins.
 */
public final class RackLayer {

    private RackLayer() {
    }

    /**
     * Places the layer and returns true, or returns false and places nothing.
     * Server side only; the client predicts one rack.
     *
     * @param state   the state a rack of this kind places with, for the room check
     * @param shelf the bays of the shelf a rack at a position belongs to; empty when no rack of this kind stands there
     * @param placeOne places one rack for a context
     */
    public static boolean place(BlockPlaceContext context, BlockState state,
                                BiFunction<BlockGetter, BlockPos, List<BlockPos>> shelf,
                                Function<BlockPlaceContext, InteractionResult> placeOne) {
        Player player = context.getPlayer();
        if (player == null || player.isSecondaryUseActive() || context.getClickedFace() != Direction.UP
                || context.replacingClickedOnBlock()) {
            return false;
        }
        return layer(context, player, state, shelf, placeOne);
    }

    /**
     * A sneak click whose assist walks up a column: the layer goes over the
     * shelf of the column's top rack instead of one rack on that column.
     * Returns false and places nothing when the walk does not go up or the
     * layer does not fit, so the assist places one rack. Server side only.
     */
    public static boolean placeUpColumn(BlockPlaceContext context, PlacementAssist assist, BlockState state,
                                        BiFunction<BlockGetter, BlockPos, List<BlockPos>> shelf,
                                        Function<BlockPlaceContext, InteractionResult> placeOne) {
        Player player = context.getPlayer();
        if (player == null || !PlacementAssists.applies(context, assist)) {
            return false;
        }
        Optional<PlacementAssists.Target> target = PlacementAssists.target(context, assist);
        if (target.isEmpty() || target.get().direction() != Direction.UP) {
            return false;
        }
        BlockPos top = target.get().pos().below();
        return layer(BlockPlaceContext.at(context, top, Direction.UP), player, state, shelf, placeOne);
    }

    private static boolean layer(BlockPlaceContext context, Player player, BlockState state,
                                 BiFunction<BlockGetter, BlockPos, List<BlockPos>> shelf,
                                 Function<BlockPlaceContext, InteractionResult> placeOne) {
        Level level = context.getLevel();
        List<BlockPos> bays = shelf.apply(level, context.getClickedPos().below());
        ItemStack stack = context.getItemInHand();
        if (bays.size() < 2 || !player.getAbilities().instabuild && stack.getCount() < bays.size()) {
            return false;
        }
        List<BlockPlaceContext> spots = new ArrayList<>(bays.size());
        for (BlockPos bay : bays) {
            BlockPlaceContext spot = BlockPlaceContext.at(context, bay, Direction.UP);
            if (!spot.canPlace() || !level.isUnobstructed(state, spot.getClickedPos(), CollisionContext.of(player))) {
                return false;
            }
            spots.add(spot);
        }
        for (BlockPlaceContext spot : spots) {
            placeOne.apply(spot);
        }
        return true;
    }
}
