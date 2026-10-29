package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankShape;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The Aquarium Lid as an item. A click on the top of a Fluid Tank's top layer, not sneaking, lays a lid over every
 * column of the tank when every spot is free and the stack holds one per column (always in creative); otherwise it
 * places one slab.
 */
public class AquariumLidItem extends BlockItem {

    public AquariumLidItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.getLevel().isClientSide() && placeLayer(context)) {
            return InteractionResult.CONSUME;
        }
        return super.place(context);
    }

    private boolean placeLayer(BlockPlaceContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || player.isSecondaryUseActive() || context.getClickedFace() != Direction.UP) {
            return false;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos().below();
        if (!(level.getBlockEntity(clicked) instanceof FluidTankBlockEntity tank)) {
            return false;
        }
        TankNetwork network = tank.network().orElse(null);
        if (network == null) {
            return false;
        }
        TankShape box = network.boxAt(Conversions.toNodePos(clicked));
        if (clicked.getY() != box.maxY()) {
            return false;
        }
        int needed = box.blocksPerLayer();
        if (!player.getAbilities().instabuild && stack.getCount() < needed) {
            return false;
        }
        BlockState state = getBlock().defaultBlockState();
        List<BlockPlaceContext> spots = new ArrayList<>(needed);
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                BlockPos pos = new BlockPos(x, box.maxY() + 1, z);
                BlockPlaceContext spot = BlockPlaceContext.at(context, pos, Direction.UP);
                // A taken spot moves the context to the block above it, so the spot must stay where it was asked.
                if (!spot.getClickedPos().equals(pos) || !spot.canPlace() || !level.isUnobstructed(state, pos, CollisionContext.of(player))) {
                    return false;
                }
                spots.add(spot);
            }
        }
        for (BlockPlaceContext spot : spots) {
            super.place(spot);
        }
        return true;
    }
}
