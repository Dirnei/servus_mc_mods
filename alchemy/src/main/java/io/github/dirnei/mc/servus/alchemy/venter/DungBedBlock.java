package io.github.dirnei.mc.servus.alchemy.venter;

import io.github.dirnei.mc.servus.alchemy.AlchemyDataMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A Dung Bed of the Venter Equinus: spruce and straw around the Cucurbit, where the Hay Bales are laid in. */
public class DungBedBlock extends VenterFrameBlock {

    private static final VoxelShape PLAIN_SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public DungBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape plainShape(BlockState state) {
        return PLAIN_SHAPE;
    }

    /** Bedding lays into the bed of a formed Venter Equinus, as much as it has room for. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!state.getValue(FORMED) || AlchemyDataMaps.warmth(stack) <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            VenterStructure.formedCucurbit(level, pos, state).ifPresent(venter -> player.setItemInHand(hand, venter.layBedding(stack)));
        }
        return InteractionResult.SUCCESS;
    }
}
