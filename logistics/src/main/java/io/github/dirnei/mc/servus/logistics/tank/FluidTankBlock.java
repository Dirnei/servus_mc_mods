package io.github.dirnei.mc.servus.logistics.tank;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A glass Fluid Tank block. The six boolean properties say whether the block
 * is joined to a block of the same formed tank on that side; the tank network
 * sets them, and the multipart model hides glass and frame toward joined
 * sides.
 */
public class FluidTankBlock extends BaseEntityBlock {

    public static final MapCodec<FluidTankBlock> CODEC = simpleCodec(FluidTankBlock::new);

    private static final Map<Direction, BooleanProperty> PROPERTIES = PipeBlock.PROPERTY_BY_DIRECTION;

    public FluidTankBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PROPERTIES.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public static BooleanProperty property(Direction direction) {
        return PROPERTIES.get(direction);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPERTIES.values().toArray(new BooleanProperty[0]));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }
}
