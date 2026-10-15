package io.github.dirnei.mc.servus.shuttles.outbound;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.station.StationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The outbound dock of a warehouse. Right click opens its GUI; the carrot cycles the clicked side or dismantles it. */
public class OutboundStationBlock extends StationBlock {

    public static final MapCodec<OutboundStationBlock> CODEC = simpleCodec(OutboundStationBlock::new);

    public OutboundStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OutboundStationBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ServusShuttlesBlockEntities.OUTBOUND_STATION.get(), OutboundStationBlockEntity::serverTick);
    }
}
