package io.github.dirnei.mc.servus.shuttles.controller;

import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.shuttles.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.rail.RailFacings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import io.github.dirnei.mc.servus.foundation.team.Teams;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Right click with an empty hand opens the site GUI. */
public class WarehouseControllerBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<WarehouseControllerBlock> CODEC = simpleCodec(WarehouseControllerBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public WarehouseControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(StorageNodeBlocks.ONLINE, false)
                .setValue(StorageNodeBlocks.ACTIVITY, StorageNodeBlocks.Activity.IDLE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, StorageNodeBlocks.ONLINE, StorageNodeBlocks.ACTIVITY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, RailFacings.frontToRail(context, RailFacings.ANY_RAIL));
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof WarehouseControllerBlockEntity controller) {
            serverPlayer.openMenu(controller, buf -> WarehouseControllerMenu.writeOpening(buf, controller));
        }
        return InteractionResult.SUCCESS;
    }

    /** The placer's team owns the site; a span carried in the item would point at the old place. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WarehouseControllerBlockEntity controller) {
            controller.resetSpan();
            if (placer instanceof ServerPlayer player) {
                controller.setTeam(Teams.teamOf(player));
            }
        }
    }

    /** The carrot turns the block; sneaking dismantles it, the item keeping the name, the FE and the rules. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean dismantle = player != null && player.isShiftKeyDown();
        if (!level.isClientSide()) {
            if (dismantle) {
                ItemStack drop = new ItemStack(this);
                if (level.getBlockEntity(pos) instanceof WarehouseControllerBlockEntity controller) {
                    controller.dropUpgrades(level, pos);
                    BlockEntityItems.saveToItem(controller, drop, level.registryAccess());
                }
                Block.popResource(level, pos, drop);
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
