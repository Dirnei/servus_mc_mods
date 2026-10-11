package io.github.dirnei.mc.servus.storage.shuttle.module;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A work bench for shuttles. Like the crafting table it keeps nothing: the
 * GUI fits modules into the shuttle the player brings and hands it back.
 */
public class ShuttleWorkshopBlock extends Block {

    public static final MapCodec<ShuttleWorkshopBlock> CODEC = simpleCodec(ShuttleWorkshopBlock::new);
    private static final Component TITLE = Component.translatable("container.servus_storage.shuttle_workshop");

    public ShuttleWorkshopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((containerId, inventory, player) ->
                new ShuttleWorkshopMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
