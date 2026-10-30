package io.github.dirnei.mc.servus.alchemy.venter;

import io.github.dirnei.mc.servus.alchemy.PlayerItems;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Bed Hatch in the middle of the Venter Equinus' front wall: the Humus is taken out here. */
public class BedHatchBlock extends VenterFrameBlock {

    private static final Map<Direction, VoxelShape> WALL = Shapes.rotateHorizontal(Block.box(0, 0, 0, 16, 12, 2.5));

    public BedHatchBlock(Properties properties) {
        super(properties);
    }

    /** A placed hatch turns its board toward the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape plainShape(BlockState state) {
        return WALL.get(state.getValue(FACING));
    }

    /** An empty hand takes the Humus. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            VenterStructure.formedCucurbit(level, pos, state).ifPresent(venter -> PlayerItems.give(player, venter.takeHatch()));
        }
        return InteractionResult.SUCCESS;
    }
}
