package io.github.dirnei.mc.servus.foundation.data;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Data Cable: links to other cables and shows an arm toward blocks that
 * expose the production or stock capabilities. It holds and moves nothing.
 */
public class DataCableBlock extends ConnectableBlock {

    public static final MapCodec<DataCableBlock> CODEC = simpleCodec(DataCableBlock::new);

    public DataCableBlock(Properties properties) {
        super(properties, 2);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataCableBlockEntity(pos, state);
    }

    /**
     * Cables always link. Members are judged on the server only; elsewhere the
     * current flag is kept, so the state comes from the server.
     */
    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (neighborState.getBlock() instanceof DataCableBlock) {
            return true;
        }
        if (level instanceof Level world && !world.isClientSide()) {
            return isMember(world, neighborPos, direction.getOpposite());
        }
        BlockState own = level.getBlockState(neighborPos.relative(direction.getOpposite()));
        return own.getBlock() instanceof DataCableBlock && own.getValue(property(direction));
    }

    /** Whether the block at {@code pos} exposes a data capability on {@code side}. */
    public static boolean isMember(Level level, BlockPos pos, Direction side) {
        return level.getCapability(ServusFoundationCapabilities.PRODUCTION, pos, side) != null
                || level.getCapability(ServusFoundationCapabilities.STOCK_PROVIDER, pos, side) != null
                || level.getCapability(ServusFoundationCapabilities.STOCK_SINK, pos, side) != null;
    }

    /** A neighbour's block entity may have changed without a shape update: recompute the arms. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide()) {
            return;
        }
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            BlockPos other = pos.relative(direction);
            updated = updated.setValue(property(direction), connectsTo(level, other, level.getBlockState(other), direction));
        }
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    /** Only an empty hand reaches the summary; any item keeps its own use, so cables can be placed against cables. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand shows the network summary in the action bar. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            DataNetworks.networkAt(server, pos).ifPresent(network -> player.displayClientMessage(summary(network), true));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static Component summary(DataNetwork network) {
        return Component.translatable("message.servus_lib.data_cable.summary", network.cables(),
                network.dataMembers(DataRole.PRODUCER).size(), network.dataMembers(DataRole.STOCK).size());
    }
}
