package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * The Fluid Outbound's GUI: the keep fluid picker, the gauge with marks at
 * the keep amount and the restock level, the keep amount and the restock
 * level each with four step buttons; the Fluids side panel, the state line
 * and the player inventory. The keep is synced as data slots after the shared
 * ones. A pick comes by {@link SetFluidKeepPayload}, the steps are menu
 * buttons {@link #keepButton} and {@link #restockButton}.
 */
public class FluidOutboundMenu extends FluidStationMenu {

    /** The keep and restock steps in mB, in the order of the buttons: the upper row, then the lower row. */
    public static final int[] KEEP_STEPS = {-1000, 1000, -10000, 10000};
    public static final int PICKER_WIDTH = 110;
    public static final int PICKER_HEIGHT = 18;
    public static final int PICKER_X = (WIDTH - PICKER_WIDTH) / 2;
    public static final int PICKER_Y = 17;
    public static final int GAUGE_Y = PICKER_Y + PICKER_HEIGHT + 2;
    public static final int AMOUNT_Y = GAUGE_Y + GAUGE_HEIGHT + 3;
    public static final int BUTTONS_Y = AMOUNT_Y + 11;
    public static final int BUTTON_HEIGHT = 11;
    public static final int RESTOCK_Y = BUTTONS_Y + 2 * (BUTTON_HEIGHT + 1) + 2;
    public static final int RESTOCK_BUTTONS_Y = RESTOCK_Y + 11;
    public static final int STATE_Y = RESTOCK_BUTTONS_Y + 2 * (BUTTON_HEIGHT + 1) + 3;
    public static final int INVENTORY_Y = STATE_Y + 22;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET + 18 + 6;
    private static final int PITCH = 44;

    private static final int DATA_KEEP_FLUID = DATA_EXTRA;
    private static final int DATA_KEEP_AMOUNT = DATA_KEEP_FLUID + 1;
    private static final int DATA_RESTOCK_AT = DATA_KEEP_AMOUNT + 2;
    private static final int EXTRA_COUNT = 5;

    /** Server side: the station's own tank and keep. */
    public FluidOutboundMenu(int containerId, Inventory playerInventory, FluidOutboundBlockEntity station) {
        this(containerId, playerInventory, ContainerLevelAccess.create(station.getLevel(), station.getBlockPos()),
                serverData(station, EXTRA_COUNT, index -> keepData(station, index)), station);
    }

    private FluidOutboundMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, ContainerData data,
                              @Nullable FluidStationBlockEntity station) {
        super(ServusShuttlesMenus.FLUID_OUTBOUND.get(), containerId, playerInventory, access, data, station, PITCH, INVENTORY_Y);
    }

    /** Client side: the server writes the station's position, see {@link FluidStationBlockEntity#writeOpening}. */
    public static FluidOutboundMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new FluidOutboundMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos),
                new SimpleContainerData(DATA_EXTRA + EXTRA_COUNT), null);
    }

    private static int keepData(FluidOutboundBlockEntity station, int index) {
        if (index == DATA_KEEP_FLUID) {
            Fluid fluid = station.keep().fluid();
            return fluid == null ? 0 : BuiltInRegistries.FLUID.getId(fluid) + 1;
        }
        if (index >= DATA_RESTOCK_AT) {
            return half(station.keep().restockAt(), index - DATA_RESTOCK_AT);
        }
        return half(station.keep().amount(), index - DATA_KEEP_AMOUNT);
    }

    /** What the tank keeps, as synced. */
    public FluidKeep<Fluid> keep() {
        int id = data(DATA_KEEP_FLUID);
        return id <= 0 ? FluidKeep.none() : new FluidKeep<>(BuiltInRegistries.FLUID.byId(id - 1), whole(DATA_KEEP_AMOUNT),
                whole(DATA_RESTOCK_AT));
    }

    /** The tank's label names what it holds, else what it keeps. */
    @Override
    protected Fluid labelFluid() {
        Fluid held = super.labelFluid();
        Fluid kept = keep().fluid();
        return held != Fluids.EMPTY || kept == null ? held : kept;
    }

    /** The button id that steps the keep by {@code KEEP_STEPS[step]}. */
    public static int keepButton(int step) {
        return step;
    }

    /** The button id that steps the restock level by {@code KEEP_STEPS[step]}. */
    public static int restockButton(int step) {
        return KEEP_STEPS.length + step;
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
        if (!(station() instanceof FluidOutboundBlockEntity outbound) || id < 0 || id >= 2 * KEEP_STEPS.length) {
            return false;
        }
        if (id < KEEP_STEPS.length) {
            outbound.stepKeep(KEEP_STEPS[id]);
        } else {
            outbound.stepRestock(KEEP_STEPS[id - KEEP_STEPS.length]);
        }
        return true;
    }

    /**
     * Server: the player picked a keep fluid. Empty clears the keep; an id
     * that names no registered source fluid changes nothing.
     *
     * @return whether the keep was set or cleared
     */
    public boolean pickKeep(Optional<Identifier> id) {
        if (!(station() instanceof FluidOutboundBlockEntity outbound)) {
            return false;
        }
        if (id.isEmpty()) {
            outbound.setKeepFluid(null);
            return true;
        }
        Optional<Fluid> fluid = keepableFluid(id.get());
        fluid.ifPresent(outbound::setKeepFluid);
        return fluid.isPresent();
    }

    /** The registered source fluid with that id, the only fluids a station can keep. */
    public static Optional<Fluid> keepableFluid(Identifier id) {
        return BuiltInRegistries.FLUID.getOptional(id)
                .filter(fluid -> fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState()));
    }

    @Override
    protected Block block() {
        return ServusShuttlesBlocks.FLUID_OUTBOUND.get();
    }
}
