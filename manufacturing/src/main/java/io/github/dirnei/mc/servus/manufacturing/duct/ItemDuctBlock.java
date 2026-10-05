package io.github.dirnei.mc.servus.manufacturing.duct;

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
 * Simple item duct. Connects to other ducts and to any block exposing the
 * item handler capability. Items inserted into a duct go straight to an
 * accepting inventory on the same network.
 */
public class ItemDuctBlock extends ConnectableBlock {

    public static final MapCodec<ItemDuctBlock> CODEC = simpleCodec(ItemDuctBlock::new);

    public ItemDuctBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemDuctBlockEntity(pos, state);
    }

    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (neighborState.getBlock() instanceof ItemDuctBlock) {
            return true;
        }
        if (level instanceof Level realLevel) {
            return realLevel.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, direction.getOpposite()) != null;
        }
        return false;
    }
}
