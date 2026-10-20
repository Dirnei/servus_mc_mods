package io.github.dirnei.mc.servus.foundation.machine.mill;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Mill: heavy iron base, steel housing, an iron grinding wheel with a
 * brass hub that turns while it mills, a steel hopper with a brass rim and a
 * chute out of the front. Grinds only while a crank or motor drives it.
 */
public class MillBlock extends MachineBlock {

    public static final MapCodec<MillBlock> CODEC = simpleCodec(MillBlock::new);

    /** The shape is the same for every facing, since only the chute's tip, which stays out of it, is not symmetric. */
    private static final VoxelShape SHAPE = PartShapes.shapeOf(MillParts.shapeParts(), Direction.NORTH);

    public MillBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return MachineContent.MILL_ENTITY.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The server runs the machine; the client turns the wheel while it mills. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return super.getTicker(level, state, type);
        }
        return createTickerHelper(type, MachineContent.MILL_ENTITY.get(),
                (tickLevel, pos, tickState, mill) -> mill.wheel().tick(tickState.getValue(LIT)));
    }
}
