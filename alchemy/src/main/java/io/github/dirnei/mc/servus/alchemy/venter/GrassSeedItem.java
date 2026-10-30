package io.github.dirnei.mc.servus.alchemy.venter;

import io.github.dirnei.mc.servus.alchemy.QuoteItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

/** The Grass Seed from the Athanor: used on Dirt that grass could live on, it turns the block into a Grass Block. */
public class GrassSeedItem extends QuoteItem {

    /** The level event that shows the bone meal sparkles on the clients. */
    private static final int GROWTH_PARTICLES_EVENT = 1505;

    public GrassSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.DIRT) || !grassCouldLive(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(GROWTH_PARTICLES_EVENT, pos, 5);
        }
        return InteractionResult.SUCCESS;
    }

    /** Whether a Grass Block at {@code pos} would stay alive, as vanilla grass decides it. */
    public static boolean grassCouldLive(BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(Blocks.SNOW) && above.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (above.getFluidState().isFull()) {
            return false;
        }
        return LightEngine.getLightBlockInto(Blocks.GRASS_BLOCK.defaultBlockState(), above, Direction.UP, above.getLightDampening()) < 15;
    }
}
