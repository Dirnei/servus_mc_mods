package io.github.dirnei.mc.servus.storage.shuttle.module;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A work bench for shuttles. It keeps the shuttle and the supplies a player
 * leaves in it, installs upgrades and tools, and builds the next tier.
 */
public class ShuttleWorkshopBlock extends BaseEntityBlock {

    public static final MapCodec<ShuttleWorkshopBlock> CODEC = simpleCodec(ShuttleWorkshopBlock::new);

    public ShuttleWorkshopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShuttleWorkshopBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShuttleWorkshopBlockEntity workshop) {
            player.openMenu(workshop, buf -> ShuttleWorkshopMenu.writeOpening(buf, pos, StorageStatsManager.INSTANCE.shuttleTiers()));
        }
        return InteractionResult.SUCCESS;
    }

}
