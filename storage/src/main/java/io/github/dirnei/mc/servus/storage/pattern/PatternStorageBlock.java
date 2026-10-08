package io.github.dirnei.mc.servus.storage.pattern;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Keeps patterns on the data line; an empty hand shows how many. The patterns travel with the dropped block. */
public class PatternStorageBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<PatternStorageBlock> CODEC = simpleCodec(PatternStorageBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** How many of the nine front lights are lit, see {@link PatternLights}. */
    public static final IntegerProperty LIGHTS = IntegerProperty.create("lights", 0, PatternLights.LIGHTS);

    public PatternStorageBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIGHTS, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIGHTS);
    }

    /** A placed item may carry patterns: the lights follow them. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof PatternStorageBlockEntity storage) {
            storage.updateLights();
        }
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
        return new PatternStorageBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PatternStorageBlockEntity storage) {
            player.displayClientMessage(Component.translatable("message.servus_storage.pattern_storage.count",
                    storage.usedSlots().size(), storage.capacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The carrot turns the block; sneaking dismantles it, the item carrying the patterns. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!level.isClientSide()) {
            if (player != null && player.isShiftKeyDown()) {
                ItemStack drop = new ItemStack(this);
                if (level.getBlockEntity(pos) instanceof PatternStorageBlockEntity storage) {
                    drop.applyComponents(storage.collectComponents());
                }
                Block.popResource(level, pos, drop);
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
