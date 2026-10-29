package io.github.dirnei.mc.servus.delicacies.aquarium;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The Aquarium Floor: a copper cabinet under the tank. The master of an aquarium is one of these; it takes FE. */
public class AquariumFloorBlock extends AquariumPartBlock {

    public static final MapCodec<AquariumFloorBlock> CODEC = simpleCodec(AquariumFloorBlock::new);

    public AquariumFloorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ServusDelicaciesBlockEntities.AQUARIUM_PART.get(), (tickLevel, pos, tickState, part) -> part.serverTick());
    }

    @Override
    BlockPos tankNeighbour(BlockPos pos) {
        return pos.above();
    }
}
