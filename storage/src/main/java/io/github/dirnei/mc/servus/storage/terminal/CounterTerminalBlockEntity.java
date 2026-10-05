package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Node that opens the stock GUI. */
public class CounterTerminalBlockEntity extends StorageNodeBlockEntity implements MenuProvider {

    public CounterTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.COUNTER_TERMINAL.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CounterTerminalMenu(containerId, playerInventory, this);
    }
}
