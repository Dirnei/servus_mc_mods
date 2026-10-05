package io.github.dirnei.mc.servus.logistics.duct;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a utility duct carrying this channel, or adds the channel to an
 * existing duct that lacks it.
 */
public class ChannelItem extends BlockItem {

    private final Channel channel;

    public ChannelItem(Block block, Channel channel, Properties properties) {
        super(block, properties);
        this.channel = channel;
    }

    public Channel channel() {
        return channel;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).getBlock() instanceof UtilityDuctBlock) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
                if (!duct.addChannel(channel)) {
                    return InteractionResult.FAIL;
                }
                Player player = context.getPlayer();
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useOn(context);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!super.placeBlock(context, state)) {
            return false;
        }
        Level level = context.getLevel();
        if (!level.isClientSide() && level.getBlockEntity(context.getClickedPos()) instanceof UtilityDuctBlockEntity duct) {
            duct.addChannel(channel);
        }
        return true;
    }

    /** Each channel item has its own name instead of the shared block name. */
    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }
}
