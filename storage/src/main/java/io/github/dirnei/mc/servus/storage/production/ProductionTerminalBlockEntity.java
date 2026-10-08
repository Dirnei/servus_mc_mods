package io.github.dirnei.mc.servus.storage.production;

import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The production overview on the data line. Holds nothing; it only reads the CPUs and producers of its line. */
public class ProductionTerminalBlockEntity extends BlockEntity implements MenuProvider, DataLink {

    private static final int LAMP_TICKS = 20;

    private int ticks;

    public ProductionTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.PRODUCTION_TERMINAL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProductionTerminalBlockEntity terminal) {
        if (level instanceof ServerLevel server && ++terminal.ticks % LAMP_TICKS == 0) {
            terminal.setOnline(ProductionView.online(server, pos));
        }
    }

    /** Lights or darkens the lamp. */
    public void setOnline(boolean online) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(StorageNodeBlocks.ONLINE) && state.getValue(StorageNodeBlocks.ONLINE) != online) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ONLINE, online), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ProductionTerminalMenu(containerId, playerInventory, this);
    }
}
