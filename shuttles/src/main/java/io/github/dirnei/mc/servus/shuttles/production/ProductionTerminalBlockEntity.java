package io.github.dirnei.mc.servus.shuttles.production;

import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.shuttles.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The production overview on the data line. Holds nothing; it only reads the CPUs and producers of its line. */
public class ProductionTerminalBlockEntity extends DataLineBlockEntity implements MenuProvider, DataLink {

    public ProductionTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.PRODUCTION_TERMINAL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProductionTerminalBlockEntity terminal) {
        if (level instanceof ServerLevel server) {
            terminal.tickOnlineLamp(server, state);
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
