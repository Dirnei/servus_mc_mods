package io.github.dirnei.mc.servus.storage.access;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.wood.Wood;

/**
 * The Access Point: a station on the rails that is also a link member of the
 * data line. Right click shows the intake buffer; the carrot dismantles it
 * while sneaking. Its sides have no modes.
 */
public class AccessPointBlock extends InboundStationBlock {

    public static final MapCodec<AccessPointBlock> CODEC = simpleCodec(AccessPointBlock::new);

    public AccessPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AccessPointBlockEntity(pos, state);
    }

    /** Sneaking dismantles and drops both buffers; a plain click does nothing, there are no side modes. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof AccessPointBlockEntity point) {
                point.dropContents(level, pos);
            }
            Block.popResource(level, pos, Wood.stamp(new ItemStack(this), level.getBlockEntity(pos)));
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ServusStorageBlockEntities.ACCESS_POINT.get(), AccessPointBlockEntity::serverTick);
    }
}
