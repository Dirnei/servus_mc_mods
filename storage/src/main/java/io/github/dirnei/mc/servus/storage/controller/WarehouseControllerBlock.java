package io.github.dirnei.mc.servus.storage.controller;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.lib.item.Wrenchable;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right click prints the warehouse summary. A Shuttle item adds a shuttle to
 * the warehouse, sneak right click with an empty hand takes one back out.
 */
public class WarehouseControllerBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<WarehouseControllerBlock> CODEC = simpleCodec(WarehouseControllerBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public WarehouseControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(StorageNodeBlocks.ONLINE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, StorageNodeBlocks.ONLINE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarehouseControllerBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!stack.is(ServusStorageItems.SHUTTLE.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WarehouseControllerBlockEntity controller) {
            if (controller.addShuttle()) {
                stack.shrink(1);
                player.setItemInHand(hand, stack);
                player.displayClientMessage(Component.literal("Shuttles: " + controller.shuttleCount()), true);
            } else {
                player.displayClientMessage(Component.literal("The warehouse cannot take more shuttles"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WarehouseControllerBlockEntity controller) {
            if (player.isShiftKeyDown()) {
                if (controller.takeShuttle()) {
                    ItemStack shuttle = new ItemStack(ServusStorageItems.SHUTTLE.get());
                    if (!player.getInventory().add(shuttle)) {
                        player.drop(shuttle, false);
                    }
                    player.displayClientMessage(Component.literal("Shuttles: " + controller.shuttleCount()), true);
                }
                return InteractionResult.sidedSuccess(false);
            }
            Component message = controller.network().map(network -> network.summary())
                    .orElse(Component.literal("Warehouse: not in a network"));
            player.displayClientMessage(message, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        StorageNodeBlockEntity.neighborsChanged(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WarehouseControllerBlockEntity controller) {
            dropShuttles(level, pos, controller);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void dropShuttles(Level level, BlockPos pos, WarehouseControllerBlockEntity controller) {
        int count = controller.shuttleCount();
        if (count > 0) {
            Block.popResource(level, pos, new ItemStack(ServusStorageItems.SHUTTLE.get(), count));
            controller.clearShuttles();
        }
    }

    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean dismantle = player != null && player.isShiftKeyDown();
        if (!level.isClientSide()) {
            if (dismantle) {
                Block.popResource(level, pos, new ItemStack(this));
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
