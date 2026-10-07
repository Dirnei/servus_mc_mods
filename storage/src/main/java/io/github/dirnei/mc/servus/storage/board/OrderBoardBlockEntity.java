package io.github.dirnei.mc.servus.storage.board;

import io.github.dirnei.mc.servus.storage.menu.OrderBoardMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** The production overview: a view block that joins the warehouse like the terminal and holds nothing. */
public class OrderBoardBlockEntity extends StorageNodeBlockEntity implements MenuProvider {

    public OrderBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.ORDER_BOARD.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OrderBoardMenu(containerId, playerInventory, this);
    }
}
