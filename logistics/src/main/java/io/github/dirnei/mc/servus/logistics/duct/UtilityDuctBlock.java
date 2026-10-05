package io.github.dirnei.mc.servus.logistics.duct;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Utility duct block. Connections depend on the channels the block entity
 * holds and on the side modes. Right click opens the side configuration.
 */
public class UtilityDuctBlock extends ConnectableBlock {

    public static final MapCodec<UtilityDuctBlock> CODEC = simpleCodec(UtilityDuctBlock::new);

    public UtilityDuctBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UtilityDuctBlockEntity(pos, state);
    }

    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (neighborState.getBlock() instanceof UtilityDuctBlock) {
            return true;
        }
        BlockPos pos = neighborPos.relative(direction.getOpposite());
        if (!(level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) || !(level instanceof Level realLevel)) {
            return false;
        }
        if (duct.has(Channel.ITEM) && duct.mode(Channel.ITEM, direction) != SideMode.DISABLED
                && realLevel.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, direction.getOpposite()) != null) {
            return true;
        }
        return duct.has(Channel.ENERGY) && duct.mode(Channel.ENERGY, direction) != SideMode.DISABLED
                && realLevel.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite()) != null;
    }

    /** Recomputes the six connection flags, after channels or modes changed. */
    public void refreshConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof UtilityDuctBlock)) {
            return;
        }
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            updated = updated.setValue(property(direction), connectsTo(level, neighborPos, level.getBlockState(neighborPos), direction));
        }
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            player.openMenu(duct, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            for (Channel channel : duct.channels()) {
                Block.popResource(level, pos, new ItemStack(ServusLogisticsItems.channelItem(channel)));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Sneak wrench removes the duct; the channels drop through {@link #onRemove}. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            level.removeBlock(context.getClickedPos(), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && !duct.channels().isEmpty()) {
            return new ItemStack(ServusLogisticsItems.channelItem(duct.channels().iterator().next()));
        }
        return new ItemStack(ServusLogisticsItems.ITEM_CHANNEL.get());
    }
}
