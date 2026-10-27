package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidKeep;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidOutboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.SetFluidKeepPayload;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * The Fluid Outbound's GUI: per tank a keep cell, its gauge with a mark at
 * the keep amount, the amount and four step buttons; the Fluids side panel,
 * the state line and the player inventory. Keeps are synced as data slots
 * after the shared ones. A keep cell click comes by {@link SetFluidKeepPayload},
 * the steps are menu buttons {@link #keepButton}.
 */
public class FluidOutboundMenu extends FluidStationMenu {

    /** The keep steps in mB, in the order of the buttons: the upper row, then the lower row. */
    public static final int[] KEEP_STEPS = {-1000, 1000, -10000, 10000};
    public static final int CELL_SIZE = 18;
    public static final int CELL_Y = 17;
    public static final int GAUGE_Y = CELL_Y + CELL_SIZE + 2;
    public static final int AMOUNT_Y = GAUGE_Y + GAUGE_HEIGHT + 3;
    public static final int BUTTONS_Y = AMOUNT_Y + 11;
    public static final int BUTTON_HEIGHT = 11;
    public static final int STATE_Y = BUTTONS_Y + 2 * (BUTTON_HEIGHT + 1) + 3;
    public static final int INVENTORY_Y = STATE_Y + 22;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET + 18 + 6;
    private static final int MAX_PITCH = 44;

    private static final int DATA_KEEP_FLUID = DATA_EXTRA;
    private static final int DATA_KEEP_AMOUNT = DATA_KEEP_FLUID + MAX_TANKS;
    private static final int EXTRA_COUNT = 3 * MAX_TANKS;

    /** Server side: the station's own tanks and keeps. */
    public FluidOutboundMenu(int containerId, Inventory playerInventory, FluidOutboundBlockEntity station) {
        this(containerId, playerInventory, ContainerLevelAccess.create(station.getLevel(), station.getBlockPos()), station.tankCount(),
                serverData(station, EXTRA_COUNT, index -> keepData(station, index)), station);
    }

    private FluidOutboundMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, int tanks, ContainerData data,
                              @Nullable FluidStationBlockEntity station) {
        super(ServusShuttlesMenus.FLUID_OUTBOUND.get(), containerId, playerInventory, access, tanks, data, station, pitch(tanks), INVENTORY_Y);
    }

    /** Client side: the server writes the station's position and its tank count, see {@link FluidStationBlockEntity#writeOpening}. */
    public static FluidOutboundMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int tanks = buf.readVarInt();
        return new FluidOutboundMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos),
                tanks, new SimpleContainerData(DATA_EXTRA + EXTRA_COUNT), null);
    }

    /** The column width: as wide as fits, at most {@link #MAX_PITCH}. */
    private static int pitch(int tanks) {
        return Math.min(MAX_PITCH, (WIDTH - 16) / Math.max(1, tanks));
    }

    private static int keepData(FluidOutboundBlockEntity station, int index) {
        if (index < DATA_KEEP_AMOUNT) {
            Fluid fluid = station.keep(index - DATA_KEEP_FLUID).fluid();
            return fluid == null ? 0 : BuiltInRegistries.FLUID.getId(fluid) + 1;
        }
        int tank = (index - DATA_KEEP_AMOUNT) / 2;
        return half(station.keep(tank).amount(), (index - DATA_KEEP_AMOUNT) % 2);
    }

    /** What a tank keeps, as synced. */
    public FluidKeep<Fluid> keep(int tank) {
        int id = data(DATA_KEEP_FLUID + tank);
        return id <= 0 ? FluidKeep.none() : new FluidKeep<>(BuiltInRegistries.FLUID.byId(id - 1), whole(DATA_KEEP_AMOUNT + tank * 2));
    }

    /** A tank's label names what it holds, else what it keeps. */
    @Override
    protected Fluid labelFluid(int tank) {
        Fluid held = super.labelFluid(tank);
        Fluid kept = keep(tank).fluid();
        return held != Fluids.EMPTY || kept == null ? held : kept;
    }

    /** The left edge of a tank's keep cell, centred in its column. */
    public int cellX(int tank) {
        return columnX(tank) + (pitch() - CELL_SIZE) / 2;
    }

    /** The button id that steps a tank's keep by {@code KEEP_STEPS[step]}. */
    public static int keepButton(int tank, int step) {
        return tank * KEEP_STEPS.length + step;
    }

    @Override
    protected String waitingFluidKey() {
        return "gui.servus_shuttles.fluid_station.state.missing";
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (super.clickMenuButton(player, id)) {
            return true;
        }
        if (!(station() instanceof FluidOutboundBlockEntity outbound) || id < 0 || id >= tanks() * KEEP_STEPS.length) {
            return false;
        }
        outbound.stepKeep(id / KEEP_STEPS.length, KEEP_STEPS[id % KEEP_STEPS.length]);
        return true;
    }

    /**
     * Server: a keep cell was clicked. The fluid of the container on the
     * cursor becomes the tank's keep fluid, the container stays; an empty
     * cursor clears the keep, any other item does nothing.
     */
    public void setKeepFromCarried(int tank) {
        if (!(station() instanceof FluidOutboundBlockEntity outbound) || tank < 0 || tank >= outbound.tankCount()) {
            return;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            outbound.setKeepFluid(tank, null);
            return;
        }
        Optional<Fluid> fluid = Transfer.fluidOf(carried);
        fluid.ifPresent(found -> outbound.setKeepFluid(tank, found));
    }

    @Override
    protected Block block() {
        return ServusShuttlesBlocks.FLUID_OUTBOUND.get();
    }
}
