package io.github.dirnei.mc.servus.foundation.machine.pump;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A pump item that lands on the water surface the player looks at, like a lily
 * pad, instead of sinking to the pond floor. Without water in the line of sight
 * it places like any block item.
 *
 * <p>Follows the 1.21.1 {@code PlaceOnWaterBlockItem}: {@code Item.getPlayerPOVHitResult}
 * with {@code ClipContext.Fluid.SOURCE_ONLY}, then {@code BlockHitResult.withPosition}
 * one block up.
 */
public class PumpItem extends BlockItem {

    public PumpItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            BlockHitResult water = waterSurface(context.getLevel(), player);
            if (water != null && !water.getBlockPos().equals(context.getClickedPos())) {
                return super.useOn(new UseOnContext(player, context.getHand(), water));
            }
        }
        return super.useOn(context);
    }

    /** Clicking into the air over water that is out of block reach still finds the surface. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult water = waterSurface(level, player);
        if (water == null) {
            return super.use(level, player, hand);
        }
        InteractionResult result = super.useOn(new UseOnContext(player, hand, water));
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    /** The spot above the water source the player looks at, or null when something else is nearer. */
    private static BlockHitResult waterSurface(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) {
            return null;
        }
        return hit.withPosition(hit.getBlockPos().above());
    }
}
