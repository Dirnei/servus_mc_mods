package io.github.dirnei.mc.servus.manufacturing.cable;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Simple power cable. Connects to other cables and to any block exposing the
 * energy capability. Carries no buffer; the cable network moves the energy.
 */
public class PowerCableBlock extends ConnectableBlock {

    public static final MapCodec<PowerCableBlock> CODEC = simpleCodec(PowerCableBlock::new);

    public PowerCableBlock(Properties properties) {
        super(properties, 3);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerCableBlockEntity(pos, state);
    }

    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (neighborState.getBlock() instanceof PowerCableBlock) {
            return true;
        }
        if (level instanceof Level realLevel) {
            return realLevel.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite()) != null;
        }
        return false;
    }
}
