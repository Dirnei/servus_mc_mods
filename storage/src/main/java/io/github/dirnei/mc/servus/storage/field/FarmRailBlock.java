package io.github.dirnei.mc.servus.storage.field;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.rail.RailBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Warehouse Rail for fields: it is a rail in every way, and a connected run
 * of farm rails is one field that a Field Head waters and watches. Placing or
 * removing one tells the Field Heads around it.
 */
public class FarmRailBlock extends RailBlock {

    public static final MapCodec<FarmRailBlock> CODEC = simpleCodec(FarmRailBlock::new);

    public FarmRailBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            FieldHeads.railChanged(level, pos);
        }
    }

    /**
     * The rail's tick after a change (see {@link RailBlock}): a loose rail, one
     * placed since it last was in a field, joins the stage of the farm rails
     * beside it. Placement itself cannot do this, the block entity comes after.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        if (level.getBlockEntity(pos) instanceof RailBlockEntity rail && rail.corrosion() == FieldCorrosion.LOOSE
                && besideStagedRail(level, pos)) {
            FieldHeads.loosePlaced(level, pos);
        }
    }

    private static boolean besideStagedRail(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(direction);
            if (level.isLoaded(next) && level.getBlockEntity(next) instanceof RailBlockEntity rail
                    && level.getBlockState(next).getBlock() instanceof FarmRailBlock && rail.corrosion() != FieldCorrosion.LOOSE) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            FieldHeads.railChanged(level, pos);
        }
    }
}
