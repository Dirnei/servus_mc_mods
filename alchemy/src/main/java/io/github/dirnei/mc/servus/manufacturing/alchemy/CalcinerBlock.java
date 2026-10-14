package io.github.dirnei.mc.servus.manufacturing.alchemy;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.manufacturing.journal.ParacelsusJournalItem;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The furnace mouth of the Calciner structure and its controller: it holds the work, the tanks and the owner. */
public class CalcinerBlock extends MachineBlock {

    public static final MapCodec<CalcinerBlock> CODEC = simpleCodec(CalcinerBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public CalcinerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusManufacturingBlockEntities.CALCINER.get();
    }

    /**
     * The Journal forms the structure; a formed Calciner takes calcinable matter by hand.
     * There is no GUI.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ParacelsusJournalItem) {
            if (!level.isClientSide()) {
                CalcinerStructure.tryForm(level, pos, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(FORMED) && level.getBlockEntity(pos) instanceof CalcinerBlockEntity calciner && calciner.calcinable(stack)) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, calciner.insertInput(stack));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Sneaking with an empty hand takes the matter back out of a formed Calciner. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown() || !state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CalcinerBlockEntity calciner) {
            PlayerItems.give(player, calciner.takeInput());
        }
        return InteractionResult.SUCCESS;
    }

    /** The carrot never turns, configures or dismantles the Calciner. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        return InteractionResult.PASS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        CalcinerStructure.broken(level, state, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
