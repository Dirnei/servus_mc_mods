package io.github.dirnei.mc.servus.foundation.adapter;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import io.github.dirnei.mc.servus.foundation.wood.Wood;

/**
 * The Machine Adapter: faces the machine it was placed against and makes it a
 * producer on the data line. An empty hand opens the upkeep GUI, the sneaking
 * carrot dismantles it.
 */
public class MachineAdapterBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<MachineAdapterBlock> CODEC = simpleCodec(MachineAdapterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MachineAdapterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Faces the block it was placed against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineAdapterBlockEntity(pos, state);
    }

    /** Any held item opens the GUI like a chest; sneaking with an item uses the item, so blocks can be placed against it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MachineAdapterBlockEntity adapter) {
            server.openMenu(adapter, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide()) {
            Block.popResource(level, pos, Wood.stamp(new ItemStack(this), level.getBlockEntity(pos)));
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
