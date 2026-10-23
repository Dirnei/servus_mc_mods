package io.github.dirnei.mc.servus.alchemy.athanor;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.journal.ParacelsusJournalItem;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Athanor: items are laid on it, an empty hand takes the result, sneaking with an empty hand takes the item
 * back. It has no GUI. {@link #TIER} drives the flame colour.
 */
public class AthanorBlock extends MachineBlock {

    public static final MapCodec<AthanorBlock> CODEC = simpleCodec(AthanorBlock::new);
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 3);
    /** Whether the Athanor stands formed on its hearth; only then it crafts. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public AthanorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TIER, FORMED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusAlchemyBlockEntities.ATHANOR.get();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock()) && level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor) {
            athanor.checkTier();
        }
    }

    /** A neighbour changed: unform at once when the hearth is broken. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        // The update does not name the changed neighbour, so every update checks the hearth.
        if (!level.isClientSide() && state.getValue(FORMED)
                && level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor) {
            athanor.checkHearth();
        }
    }

    /** A formed Athanor stands as its tower: a player walks from a duct's pipe onto its plate. */
    private static final VoxelShape TOWER = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(1, 3, 1, 15, 17, 15),
            Block.box(2.5, 17, 2.5, 13.5, 19, 13.5), Block.box(4, 19, 4, 12, 21, 12), Block.box(5.5, 21, 5.5, 10.5, 22.5, 10.5),
            Block.box(3.5, 22.5, 3.5, 12.5, 23.5, 12.5));

    /** Formed, it is looked at two blocks tall, its own block and the space above where the item lies (see {@link AthanorCrownBlock}). */
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 32, 16);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? OUTLINE : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? TOWER : Shapes.block();
    }

    /** A broken Athanor gives its ducts back their straight look and takes its crown away. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.getValue(FORMED)) {
            AthanorBlockEntity.setDucts(level, pos, state.getValue(FACING), false);
            AthanorBlockEntity.setCrown(level, pos, false);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || !(level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (stack.getItem() instanceof ParacelsusJournalItem) {
            if (!level.isClientSide()) {
                athanor.tryForm(player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            athanor.checkTier();
        }
        if (athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).isEmpty() && athanor.isItemValid(AthanorBlockEntity.SLOT_INPUT, stack)) {
            if (!level.isClientSide()) {
                athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, stack.split(stack.getCount()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** An empty hand takes the result; sneaking takes the item back. Never opens a menu. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor && !level.isClientSide()) {
            athanor.checkTier();
            int slot = player.isShiftKeyDown() ? AthanorBlockEntity.SLOT_INPUT : AthanorBlockEntity.SLOT_OUTPUT;
            ItemStack taken = athanor.inventory().getStackInSlot(slot);
            if (!taken.isEmpty()) {
                athanor.inventory().setStackInSlot(slot, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(taken);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
