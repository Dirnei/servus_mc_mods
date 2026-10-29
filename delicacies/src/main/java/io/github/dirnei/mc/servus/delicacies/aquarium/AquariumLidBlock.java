package io.github.dirnei.mc.servus.delicacies.aquarium;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Aquarium Lid: a copper slab 4 px high with a light panel underneath. Its full bottom face covers the tank's
 * glass top, which the tank model culls. {@link #LIT} while its aquarium has power.
 */
public class AquariumLidBlock extends AquariumPartBlock {

    public static final MapCodec<AquariumLidBlock> CODEC = simpleCodec(AquariumLidBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Height of the slab in pixels. */
    public static final int HEIGHT = 4;
    /** Light a lit lid gives. */
    public static final int LIGHT = 12;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, HEIGHT, 16);

    public AquariumLidBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(LIT) ? LIGHT : 0;
    }

    /** Sets {@link #LIT} of the lid at {@code pos} when it is a lid and differs. */
    public static void setLit(Level level, BlockPos pos, boolean lit) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AquariumLidBlock && state.getValue(LIT) != lit) {
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    BlockPos tankNeighbour(BlockPos pos) {
        return pos.below();
    }
}
