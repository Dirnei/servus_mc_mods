package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.block.TankLabel;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.FluidActivity;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The GUI shared by the fluid stations: a gauge per tank in a centred row of
 * columns, the Fluids side panel, a state line and the player inventory.
 * Tanks, sides and why the station waits are synced as data slots, amounts
 * in two 16 bit halves, followed by the data slots a station adds; whether
 * the warehouse is online comes by {@link StationStatePayload}. Side panel
 * clicks are the {@link SideConfigMenus} buttons.
 */
public abstract class FluidStationMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176;
    public static final int GAUGE_WIDTH = 18;
    public static final int GAUGE_HEIGHT = 52;

    protected static final int MAX_TANKS = ShuttlesStatsManager.MAX_FLUID_STATION_TANKS;
    private static final int DATA_FLUID = 0;
    private static final int DATA_AMOUNT = DATA_FLUID + MAX_TANKS;
    private static final int DATA_CAPACITY = DATA_AMOUNT + 2 * MAX_TANKS;
    private static final int DATA_SIDES = DATA_CAPACITY + 2;
    /** 0 when nothing waits, {@link #WAITS_FOR_TANKER} without a Tanker, else the waiting fluid's id plus 2. */
    private static final int DATA_WAITING = DATA_SIDES + 6;
    /** The first data slot a station adds. */
    protected static final int DATA_EXTRA = DATA_WAITING + 1;
    private static final int WAITS_FOR_TANKER = 1;

    private final ContainerLevelAccess access;
    private final Player player;
    private final ContainerData data;
    private final int tanks;
    private final int pitch;
    private final int inventoryY;
    private final @Nullable FluidStationBlockEntity station;
    private final PayloadSync<StationStatePayload> sync = new PayloadSync<>(5, true, this::state);

    /**
     * @param pitch      the width of one tank's column
     * @param inventoryY the top of the player inventory
     */
    protected FluidStationMenu(MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access, int tanks,
                               ContainerData data, @Nullable FluidStationBlockEntity station, int pitch, int inventoryY) {
        super(type, containerId);
        this.access = access;
        this.player = playerInventory.player;
        this.data = data;
        this.tanks = tanks;
        this.station = station;
        this.pitch = pitch;
        this.inventoryY = inventoryY;
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, inventoryY);
        addDataSlots(data);
    }

    /**
     * The server's data slots for a station: the shared ones, then {@code
     * extraCount} slots answered by {@code extra} from {@link #DATA_EXTRA} on.
     */
    protected static ContainerData serverData(FluidStationBlockEntity station, int extraCount, IntUnaryOperator extra) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index < DATA_AMOUNT) {
                    int tank = index - DATA_FLUID;
                    return tank < station.tankCount() ? BuiltInRegistries.FLUID.getId(station.tank(tank).getFluid().getFluid()) : 0;
                }
                if (index < DATA_CAPACITY) {
                    int tank = (index - DATA_AMOUNT) / 2;
                    return tank < station.tankCount() ? half(station.tank(tank).getFluidAmount(), (index - DATA_AMOUNT) % 2) : 0;
                }
                if (index < DATA_SIDES) {
                    return half(station.tank(0).getCapacity(), index - DATA_CAPACITY);
                }
                if (index < DATA_WAITING) {
                    return station.fluidSides().packed(RelativeSide.byOrdinal(index - DATA_SIDES));
                }
                if (index == DATA_WAITING) {
                    FluidActivity activity = station.fluidActivity();
                    Fluid waiting = activity.noRoom() != null ? activity.noRoom() : activity.missing();
                    return waiting != null ? BuiltInRegistries.FLUID.getId(waiting) + 2 : activity.noTanker() ? WAITS_FOR_TANKER : 0;
                }
                return extra.applyAsInt(index);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_EXTRA + extraCount;
            }
        };
    }

    /** The low (0) or high (1) 16 bits of a value; data slots travel as shorts. */
    protected static int half(int value, int which) {
        return which == 0 ? value & 0xFFFF : value >>> 16;
    }

    /** A value synced by {@link #half} in the data slots at {@code index} and the one after. */
    protected int whole(int index) {
        return (data.get(index + 1) & 0xFFFF) << 16 | data.get(index) & 0xFFFF;
    }

    protected int data(int index) {
        return data.get(index);
    }

    /** The station, on the server only. */
    protected @Nullable FluidStationBlockEntity station() {
        return station;
    }

    protected Player player() {
        return player;
    }

    public int tanks() {
        return tanks;
    }

    /** The left edge of a tank's column; the columns are centred. */
    public int columnX(int tank) {
        return (WIDTH - tanks * pitch) / 2 + tank * pitch;
    }

    public int pitch() {
        return pitch;
    }

    /** The left edge of a tank's gauge, centred in its column. */
    public int gaugeX(int tank) {
        return columnX(tank) + (pitch - GAUGE_WIDTH) / 2;
    }

    public int inventoryY() {
        return inventoryY;
    }

    public int hotbarY() {
        return inventoryY + PlayerInventorySlots.HOTBAR_OFFSET;
    }

    /** What a tank holds, as synced. */
    public FluidStack fluid(int tank) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(data.get(DATA_FLUID + tank));
        int amount = whole(DATA_AMOUNT + tank * 2);
        return fluid == Fluids.EMPTY || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    /** The capacity of each tank, as synced. */
    public int capacity() {
        return whole(DATA_CAPACITY);
    }

    /** The packed Fluids side, as synced. */
    public int fluidSide(RelativeSide side) {
        return data.get(DATA_SIDES + side.ordinal());
    }

    /** The fluid a tank's label names: by default what it holds. */
    protected Fluid labelFluid(int tank) {
        return fluid(tank).getFluid();
    }

    /** The tank labels of the side panel. */
    public List<TankLabel> tankLabels() {
        List<TankLabel> labels = new ArrayList<>(tanks);
        for (int tank = 0; tank < tanks; tank++) {
            labels.add(TankLabel.of(labelFluid(tank), tank));
        }
        return labels;
    }

    /** The lang key of the state line naming the fluid the station waits for, with the fluid as its argument. */
    protected abstract String waitingFluidKey();

    /** Why the station is red: a fluid it waits for, no Tanker shuttle reaching it; null while nothing waits. */
    public @Nullable Component waitingLine() {
        int waiting = data.get(DATA_WAITING);
        if (waiting == WAITS_FOR_TANKER) {
            return Component.translatable("gui.servus_shuttles.fluid_station.state.no_tanker");
        }
        if (waiting > WAITS_FOR_TANKER) {
            return Component.translatable(waitingFluidKey(), new FluidStack(BuiltInRegistries.FLUID.byId(waiting - 2), 1).getHoverName());
        }
        return null;
    }

    /** The station's state as the client should see it: online, or why not. */
    public StationStatePayload state() {
        String offline = station == null ? "" : station.offlineReason();
        boolean idle = station == null || station.isEmpty();
        return new StationStatePayload(offline.isEmpty(), offline, "", ItemStack.EMPTY, "", idle);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return station != null && SideConfigMenus.click(station, id);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        sync.tick(player);
    }

    /** Shift click moves stacks between the inventory and the hotbar. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int hotbar = PlayerInventorySlots.COUNT - 9;
        boolean moved = index < hotbar ? moveItemStackTo(stack, hotbar, slots.size(), false) : moveItemStackTo(stack, 0, hotbar, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return copy;
    }

    /** The station block this GUI belongs to. */
    protected abstract Block block();

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block());
    }
}
