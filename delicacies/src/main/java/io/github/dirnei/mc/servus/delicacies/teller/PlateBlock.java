package io.github.dirnei.mc.servus.delicacies.teller;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** An empty Plate: placed on a solid top, it holds nothing and does nothing. A finished meal leaves one behind. */
public class PlateBlock extends Block {

    public static final MapCodec<PlateBlock> CODEC = simpleCodec(PlateBlock::new);

    private static final VoxelShape SHAPE = PartShapes.shapeOf(MealParts.plateBoxes(), Direction.NORTH);

    public PlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** Only on a block with a solid top. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return restsOn(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    /** Whether a plate or a meal at {@code pos} has a solid top under it. */
    public static boolean restsOn(LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
