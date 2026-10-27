package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/** The Fluid Inbound's GUI: a gauge per tank with its number below, the Fluids side panel and the player inventory. */
public class FluidInboundMenu extends FluidStationMenu {

    public static final int GAUGE_Y = 18;
    public static final int GAUGE_GAP = 8;
    public static final int NUMBER_Y = GAUGE_Y + GAUGE_HEIGHT + 3;
    public static final int STATE_Y = NUMBER_Y + 12;
    public static final int INVENTORY_Y = STATE_Y + 30;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET + 18 + 6;
    private static final int PITCH = GAUGE_WIDTH + GAUGE_GAP;

    /** Server side: the station's own tanks. */
    public FluidInboundMenu(int containerId, Inventory playerInventory, FluidStationBlockEntity station) {
        this(containerId, playerInventory, ContainerLevelAccess.create(station.getLevel(), station.getBlockPos()), station.tankCount(),
                serverData(station, 0, index -> 0), station);
    }

    private FluidInboundMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, int tanks, ContainerData data,
                             @Nullable FluidStationBlockEntity station) {
        super(ServusShuttlesMenus.FLUID_INBOUND.get(), containerId, playerInventory, access, tanks, data, station, PITCH, INVENTORY_Y);
    }

    /** Client side: the server writes the station's position and its tank count, see {@link FluidStationBlockEntity#writeOpening}. */
    public static FluidInboundMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int tanks = buf.readVarInt();
        return new FluidInboundMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos),
                tanks, new SimpleContainerData(DATA_EXTRA), null);
    }

    @Override
    protected String waitingFluidKey() {
        return "gui.servus_shuttles.fluid_station.state.no_room";
    }

    @Override
    protected Block block() {
        return ServusShuttlesBlocks.FLUID_INBOUND.get();
    }
}
