package io.github.dirnei.mc.servus.storage.crate;

import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.zone.ZoneLabelItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crate with one, two or four compartments. Right click inserts into the
 * compartment aimed at, double right click inserts all matching, left click
 * takes (handled in the left click event so creative mode does not break it).
 */
public class CrateBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<CrateBlock> CODEC = simpleCodec(CrateBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private final int compartments;

    public CrateBlock(Properties properties) {
        this(properties, 1);
    }

    protected CrateBlock(Properties properties, int compartments) {
        super(properties);
        this.compartments = compartments;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(StorageNodeBlocks.ONLINE, false));
    }

    public int compartments() {
        return compartments;
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
        return new CrateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ServusStorageBlockEntities.CRATE.get(), CrateBlockEntity::serverTick);
    }

    /** Which compartment a hit on the block lands in; side faces mean the first one. */
    public static int compartmentAt(BlockState state, BlockPos pos, Direction face, Vec3 location) {
        if (!(state.getBlock() instanceof CrateBlock crate)) {
            return 0;
        }
        Vec3 local = location.subtract(Vec3.atLowerCornerOf(pos));
        boolean front = face == state.getValue(FACING);
        return CrateCompartments.index(crate.compartments, state.getValue(FACING).get2DDataValue(), local.x, local.y, local.z, front);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || stack.getItem() instanceof ZoneLabelItem) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int compartment = compartmentAt(state, pos, hitResult.getDirection(), hitResult.getLocation());
        if (hitResult.getDirection() != state.getValue(FACING)) {
            compartment = crate.preferredCompartment(stack);
        }
        if (compartment < 0 || !crate.matches(compartment, stack)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            boolean doubleClick = crate.registerRightClick(player, level, compartment);
            insertHeld(crate, compartment, player, hand, stack);
            if (doubleClick) {
                insertAllMatching(crate, compartment, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void insertHeld(CrateBlockEntity crate, int compartment, Player player, InteractionHand hand, ItemStack stack) {
        int inserted = crate.insert(compartment, stack, false);
        if (inserted > 0) {
            stack.shrink(inserted);
            player.setItemInHand(hand, stack);
        }
    }

    private static void insertAllMatching(CrateBlockEntity crate, int compartment, Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (stack.isEmpty() || crate.isEmpty(compartment) || !crate.matches(compartment, stack)) {
                continue;
            }
            int inserted = crate.insert(compartment, stack, false);
            if (inserted > 0) {
                stack.shrink(inserted);
                inventory.setItem(slot, stack);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        StorageNodeBlockEntity.neighborsChanged(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            for (int i = 0; i < crate.compartments(); i++) {
                while (!crate.isEmpty(i)) {
                    Block.popResource(level, pos, crate.extract(i, crate.type(i).getMaxStackSize(), false));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean dismantle = player != null && player.isShiftKeyDown();
        if (!level.isClientSide()) {
            if (dismantle) {
                ItemStack drop = new ItemStack(this);
                if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
                    BlockEntityItems.saveToItem(crate, drop, level.registryAccess());
                    // Empty the crate first so onRemove does not spill the contents we just saved.
                    crate.clear();
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
