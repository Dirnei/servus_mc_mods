package io.github.dirnei.mc.servus.lib.item;

import io.github.dirnei.mc.servus.lib.config.ServusLibConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The Configuration Carrot, the item that fills the wrench role in Servus.
 * Blocks implementing {@link Wrenchable} get the click; everything else with
 * a facing property is rotated.
 */
public class ConfigurationCarrotItem extends Item {

    public ConfigurationCarrotItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof Wrenchable wrenchable) {
            return wrenchable.onWrench(context, state);
        }
        if (!ServusLibConfig.wrenchRotatesBlocks()) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        DirectionProperty property = facingProperty(state);
        if (property == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(property), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static DirectionProperty facingProperty(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return BlockStateProperties.HORIZONTAL_FACING;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return BlockStateProperties.FACING;
        }
        return null;
    }
}
