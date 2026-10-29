package io.github.dirnei.mc.servus.shuttles.station;

import io.github.dirnei.mc.servus.foundation.block.SideConfigWrench;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.DataPotatoItem;
import io.github.dirnei.mc.servus.foundation.item.PotatoInspectable;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.rail.RailFacings;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A warehouse station block: turns its front to a rail when placed (see
 * {@link RailFacings}), else to the player, shows whether it
 * is online and what it is doing, leaves the carrot click to the wrench and
 * reports its label to the Data Potato.
 */
public abstract class StationBlock extends BaseEntityBlock implements Wrenchable, PotatoInspectable {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    protected StationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(StorageNodeBlocks.ONLINE, false).setValue(StorageNodeBlocks.ACTIVITY, StorageNodeBlocks.Activity.IDLE));
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof DataPotatoItem) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        return StationReport.of(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
            player.openMenu(station, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** The carrot cycles the clicked side; sneaking dismantles, drops the buffer and keeps the configuration on the item. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof StationBlockEntity station) {
            return SideConfigWrench.use(context, this, station, () -> station.dropContents(level, pos));
        }
        return InteractionResult.SUCCESS;
    }
}
