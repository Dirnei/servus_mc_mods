package io.github.dirnei.mc.servus.storage.rail;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One segment of a rail elevator shaft. Segments stack vertically and take
 * rails on their four sides; the network joins anything touching them.
 */
public class ElevatorBlock extends ConnectableBlock {

    public static final MapCodec<ElevatorBlock> CODEC = simpleCodec(ElevatorBlock::new);
    private static final VoxelShape PILLAR = Block.box(4, 0, 4, 12, 16, 12);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(5, 0, 0, 11, 1, 5),
            Direction.SOUTH, Block.box(5, 0, 11, 11, 1, 16),
            Direction.WEST, Block.box(0, 0, 5, 5, 1, 11),
            Direction.EAST, Block.box(11, 0, 5, 16, 1, 11));

    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public ElevatorBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (direction.getAxis().isVertical()) {
            return neighborState.getBlock() instanceof ElevatorBlock;
        }
        return neighborState.getBlock() instanceof RailBlock;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, s -> {
            VoxelShape shape = PILLAR;
            for (Map.Entry<Direction, VoxelShape> arm : ARMS.entrySet()) {
                if (s.getValue(property(arm.getKey()))) {
                    shape = Shapes.or(shape, arm.getValue());
                }
            }
            return shape;
        });
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorBlockEntity(pos, state);
    }
}
