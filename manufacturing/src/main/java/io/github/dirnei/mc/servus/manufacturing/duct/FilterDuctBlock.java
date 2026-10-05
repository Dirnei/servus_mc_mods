package io.github.dirnei.mc.servus.manufacturing.duct;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A duct whose neighbours only receive whitelisted items. Right click opens the list. */
public class FilterDuctBlock extends ItemDuctBlock {

    public static final MapCodec<FilterDuctBlock> CODEC = simpleCodec(FilterDuctBlock::new);

    public FilterDuctBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilterDuctBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FilterDuctBlockEntity filter) {
            player.openMenu(filter, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
