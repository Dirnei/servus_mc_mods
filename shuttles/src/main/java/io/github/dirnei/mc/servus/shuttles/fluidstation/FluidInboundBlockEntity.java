package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.TankRole;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidRole;
import io.github.dirnei.mc.servus.shuttles.menu.FluidInboundMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Fluid Inbound's tank: sides take fluid in on Input or Pull and give
 * none back, Input by default; the tank is a source of tanker put-away runs.
 */
public class FluidInboundBlockEntity extends FluidStationBlockEntity {

    public FluidInboundBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.FLUID_INBOUND.get(), pos, state, SideConfig.INBOUND_MODES, SideMode.INPUT, TankRole.FILL_ONLY);
    }

    @Override
    public FluidRole role() {
        return FluidRole.IMPORT;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidInboundMenu(containerId, playerInventory, this);
    }
}
