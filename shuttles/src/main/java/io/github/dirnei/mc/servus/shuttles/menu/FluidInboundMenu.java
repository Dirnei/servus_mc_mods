package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.block.TankLabel;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.FluidActivity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The Fluid Inbound's GUI: a gauge per tank, the Fluids side panel and the
 * player inventory. Tanks, sides and why a tank waits are synced as data
 * slots, amounts in two 16 bit halves; whether the warehouse is online comes
 * by {@link StationStatePayload}. Side panel clicks are the {@link SideConfigMenus} buttons.
 */
public class FluidInboundMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176;
    public static final int GAUGE_Y = 18;
    public static final int GAUGE_WIDTH = 18;
    public static final int GAUGE_HEIGHT = 52;
    public static final int GAUGE_GAP = 8;
    public static final int NUMBER_Y = GAUGE_Y + GAUGE_HEIGHT + 3;
    public static final int STATE_Y = NUMBER_Y + 12;
    public static final int INVENTORY_Y = STATE_Y + 30;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET + 18 + 6;

    private static final int MAX_TANKS = ShuttlesStatsManager.MAX_FLUID_STATION_TANKS;
    private static final int DATA_FLUID = 0;
    private static final int DATA_AMOUNT = DATA_FLUID + MAX_TANKS;
    private static final int DATA_CAPACITY = DATA_AMOUNT + 2 * MAX_TANKS;
    private static final int DATA_SIDES = DATA_CAPACITY + 2;
    /** 0 when nothing waits, {@link #WAITS_FOR_TANKER} without a Tanker, else the waiting fluid's id plus 2. */
    private static final int DATA_WAITING = DATA_SIDES + 6;
    private static final int DATA_COUNT = DATA_WAITING + 1;
    private static final int WAITS_FOR_TANKER = 1;

    private final ContainerLevelAccess access;
    private final Player player;
    private final ContainerData data;
    private final int tanks;
    private final @Nullable FluidStationBlockEntity station;
    private final PayloadSync<StationStatePayload> sync = new PayloadSync<>(5, true, this::state);

    /** Server side: the station's own tanks. */
    public FluidInboundMenu(int containerId, Inventory playerInventory, FluidStationBlockEntity station) {
        this(containerId, playerInventory, ContainerLevelAccess.create(station.getLevel(), station.getBlockPos()), station.tankCount(),
                serverData(station), station);
    }

    private FluidInboundMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, int tanks, ContainerData data,
                             @Nullable FluidStationBlockEntity station) {
        super(ServusShuttlesMenus.FLUID_INBOUND.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;
        this.data = data;
        this.tanks = tanks;
        this.station = station;
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Client side: the server writes the station's position and its tank count, see {@link FluidStationBlockEntity#writeOpening}. */
    public static FluidInboundMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int tanks = buf.readVarInt();
        return new FluidInboundMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos),
                tanks, new SimpleContainerData(DATA_COUNT), null);
    }

    private static ContainerData serverData(FluidStationBlockEntity station) {
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
                FluidActivity activity = station.fluidActivity();
                return activity.noRoom() != null ? BuiltInRegistries.FLUID.getId(activity.noRoom()) + 2
                        : activity.noTanker() ? WAITS_FOR_TANKER : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    /** The low (0) or high (1) 16 bits of a value; data slots travel as shorts. */
    private static int half(int value, int which) {
        return which == 0 ? value & 0xFFFF : value >>> 16;
    }

    private int whole(int index) {
        return (data.get(index + 1) & 0xFFFF) << 16 | data.get(index) & 0xFFFF;
    }

    public int tanks() {
        return tanks;
    }

    /** The left edge of a tank's gauge; the gauges are centred. */
    public int gaugeX(int tank) {
        int width = tanks * GAUGE_WIDTH + (tanks - 1) * GAUGE_GAP;
        return (WIDTH - width) / 2 + tank * (GAUGE_WIDTH + GAUGE_GAP);
    }

    public int hotbarY() {
        return INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET;
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

    /** The tank labels of the side panel, by what each tank holds. */
    public List<TankLabel> tankLabels() {
        List<TankLabel> labels = new ArrayList<>(tanks);
        for (int tank = 0; tank < tanks; tank++) {
            labels.add(TankLabel.of(fluid(tank).getFluid(), tank));
        }
        return labels;
    }

    /** Why a tank is red: no room for a fluid, no Tanker shuttle reaching the station; null while nothing waits. */
    public @Nullable Component waitingLine() {
        int waiting = data.get(DATA_WAITING);
        if (waiting == WAITS_FOR_TANKER) {
            return Component.translatable("gui.servus_shuttles.fluid_station.state.no_tanker");
        }
        if (waiting > WAITS_FOR_TANKER) {
            return Component.translatable("gui.servus_shuttles.fluid_station.state.no_room",
                    new FluidStack(BuiltInRegistries.FLUID.byId(waiting - 2), 1).getHoverName());
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.FLUID_INBOUND.get());
    }
}
