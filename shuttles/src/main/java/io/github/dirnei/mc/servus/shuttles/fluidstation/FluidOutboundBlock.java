package io.github.dirnei.mc.servus.shuttles.fluidstation;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/** The Fluid Outbound: tankers keep its buffer tank stocked from the warehouse's Fluid Racks, and its sides hand the fluid out. */
public class FluidOutboundBlock extends FluidStationBlock {

    public static final MapCodec<FluidOutboundBlock> CODEC = simpleCodec(FluidOutboundBlock::new);

    public FluidOutboundBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected FluidStationBlockEntity createStation(BlockPos pos, BlockState state) {
        return new FluidOutboundBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return createTickerHelper(type, ServusShuttlesBlockEntities.FLUID_OUTBOUND.get(), FluidStationBlockEntity::serverTick);
    }
}
