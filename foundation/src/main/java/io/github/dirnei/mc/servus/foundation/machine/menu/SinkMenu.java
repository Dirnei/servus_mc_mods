package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBatch;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkCapacity;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkRates;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkStatsManager;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.upgrade.CapacityUpgradeSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The Sink GUI: the buffer rows with a tank beside each, the Capacity Upgrade
 * slots on a side tab and the player inventory. The player puts items in and
 * takes them back by hand. The chimney height, the rates, the open rows, the
 * tanks, the time to the next batch and whether an upgrade may come out are
 * synced as data slots, amounts in two 16 bit halves. Always belongs to the
 * controller of a formed Sink.
 */
public class SinkMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176;
    /** The top left item position of the buffer; rows are 18 apart. */
    public static final int BUFFER_X = 8;
    public static final int BUFFER_Y = 18;
    public static final int TANK_X = 138;
    public static final int TANK_WIDTH = 30;
    public static final int TANK_HEIGHT = 18;
    /** The batch bar under the buffer, as a box of {@link #BAR_WIDTH} by 4. */
    public static final int BAR_X = 7;
    public static final int BAR_Y = 92;
    public static final int BAR_WIDTH = 162;
    public static final int RATES_Y = 100;
    public static final int MESSAGE_Y = 110;
    public static final int INVENTORY_Y = 120;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET + 22;
    /** The upgrade tab right of the panel, and its first slot. */
    public static final int TAB_X = 172;
    public static final int TAB_Y = 18;
    public static final int TAB_WIDTH = 30;
    public static final int UPGRADE_X = 180;
    public static final int UPGRADE_Y = 26;
    /** Ticks the GUI names a refused upgrade after the refusal. */
    private static final int REFUSED_TICKS = 100;

    private static final int DATA_HEIGHT = 0;
    private static final int DATA_ITEMS = 1;
    private static final int DATA_FLUID = 2;
    private static final int DATA_ENERGY = 3;
    private static final int DATA_OPEN_ROWS = 4;
    private static final int DATA_TO_BATCH = 5;
    private static final int DATA_REMOVABLE = 6;
    private static final int DATA_CAPACITY = 7;
    /** Per tank from here: the fluid id and the amount in two halves. */
    private static final int DATA_TANKS = 9;
    private static final int DATA_PER_TANK = 3;

    private final SinkBlockEntity sink;
    private final ContainerLevelAccess access;
    private final Player player;
    private final ContainerData data;
    private final SinkCapacity capacity;
    private final ItemSlots items;
    private final int bufferSlots;
    private final int upgradeSlots;
    private long refusedAt = Long.MIN_VALUE / 2;

    /** Server side: the controller's own slots. */
    public SinkMenu(int containerId, Inventory playerInventory, SinkBlockEntity sink) {
        this(containerId, playerInventory, ContainerLevelAccess.create(sink.getLevel(), sink.getBlockPos()), sink,
                sink.capacity(), sink.items(), sink.upgrades(), serverData(sink));
    }

    private SinkMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, SinkBlockEntity sink,
                     SinkCapacity capacity, ItemSlots items, ItemSlots upgrades, ContainerData data) {
        super(MachineContent.SINK_MENU.get(), containerId);
        this.sink = sink;
        this.access = access;
        this.player = playerInventory.player;
        this.capacity = capacity;
        this.items = items;
        this.data = data;
        this.bufferSlots = items.getSlots();
        this.upgradeSlots = upgrades.getSlots();
        for (int slot = 0; slot < bufferSlots; slot++) {
            int row = capacity.rowOf(slot);
            int column = slot - row * capacity.slotsPerRow();
            addSlot(new BufferSlot(items, slot, BUFFER_X + column * 18, BUFFER_Y + row * 18));
        }
        for (int slot = 0; slot < upgradeSlots; slot++) {
            addSlot(new UpgradeSlot(upgrades, slot, UPGRADE_X, UPGRADE_Y + slot * 18));
        }
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Client side: the server writes the controller's position and the buffer layout. */
    public static SinkMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        SinkCapacity capacity = new SinkCapacity(buf.readVarInt(), buf.readVarInt());
        return new SinkMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos), null,
                capacity, new ItemSlots(capacity.maxSlots()), new CapacityUpgradeSlots(capacity.upgradeSlots(), null),
                new SimpleContainerData(dataCount(capacity)));
    }

    /** What the server writes for {@link #fromNetwork}. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, SinkBlockEntity sink) {
        buf.writeBlockPos(sink.getBlockPos());
        buf.writeVarInt(sink.capacity().slotsPerRow());
        buf.writeVarInt(sink.capacity().upgradeSlots());
    }

    private static int dataCount(SinkCapacity capacity) {
        return DATA_TANKS + capacity.maxRows() * DATA_PER_TANK;
    }

    private static ContainerData serverData(SinkBlockEntity sink) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                SinkRates rates = SinkStatsManager.INSTANCE.stats().rates();
                int height = sink.chimneyHeight();
                return switch (index) {
                    case DATA_HEIGHT -> height;
                    case DATA_ITEMS -> rates.itemsDisplay(height);
                    case DATA_FLUID -> rates.fluidDisplay(height);
                    case DATA_ENERGY -> rates.energyDisplay(height);
                    case DATA_OPEN_ROWS -> sink.openRows();
                    case DATA_TO_BATCH -> sink.ticksToBatch();
                    case DATA_REMOVABLE -> sink.canRemoveUpgrade() ? 1 : 0;
                    case DATA_CAPACITY, DATA_CAPACITY + 1 -> half(sink.tankCapacity(), index - DATA_CAPACITY);
                    default -> tankData(sink, index - DATA_TANKS);
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return dataCount(sink.capacity());
            }
        };
    }

    private static int tankData(SinkBlockEntity sink, int offset) {
        FluidStack fluid = sink.tank(offset / DATA_PER_TANK);
        int field = offset % DATA_PER_TANK;
        return field == 0 ? BuiltInRegistries.FLUID.getId(fluid.getFluid()) : half(fluid.getAmount(), field - 1);
    }

    /** The low (0) or high (1) 16 bits of a value; data slots travel as shorts. */
    private static int half(int value, int which) {
        return which == 0 ? value & 0xFFFF : value >>> 16;
    }

    private int whole(int index) {
        return (data.get(index + 1) & 0xFFFF) << 16 | data.get(index) & 0xFFFF;
    }

    /** A buffer slot: hidden and closed while its row is locked. */
    private final class BufferSlot extends ResourceHandlerSlot {

        BufferSlot(ItemSlots items, int slot, int x, int y) {
            super(items, items::set, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return isSlotOpen(getContainerSlot());
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && super.mayPlace(stack);
        }
    }

    /** An upgrade slot that keeps its upgrade while the last open row or tank holds anything. */
    private final class UpgradeSlot extends ResourceHandlerSlot {

        UpgradeSlot(ItemSlots upgrades, int slot, int x, int y) {
            super(upgrades, upgrades::set, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            if (!hasItem() || canRemoveUpgrade()) {
                return true;
            }
            refusedAt = player.level().getGameTime();
            return false;
        }
    }

    public SinkCapacity capacity() {
        return capacity;
    }

    public int bufferSlots() {
        return bufferSlots;
    }

    public int upgradeSlots() {
        return upgradeSlots;
    }

    public int chimneyHeight() {
        return data.get(DATA_HEIGHT);
    }

    /** Items per second, or -1 for Unlimited. */
    public int itemRate() {
        return data.get(DATA_ITEMS);
    }

    /** mB per second, or -1 for Unlimited. */
    public int fluidRate() {
        return data.get(DATA_FLUID);
    }

    /** FE per tick, or -1 for Unlimited. */
    public int energyRate() {
        return data.get(DATA_ENERGY);
    }

    public int openRows() {
        return data.get(DATA_OPEN_ROWS);
    }

    public int maxRows() {
        return capacity.maxRows();
    }

    public boolean isSlotOpen(int slot) {
        return capacity.rowOf(slot) < openRows();
    }

    /** Ticks until the next batch, or {@link SinkBlockEntity#NO_BATCH} while the buffer is empty. */
    public int ticksToBatch() {
        return data.get(DATA_TO_BATCH);
    }

    public boolean canRemoveUpgrade() {
        return data.get(DATA_REMOVABLE) != 0;
    }

    /** Whether the Sink is active, read from its block state. */
    public boolean active() {
        return access.evaluate((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            return state.hasProperty(SinkBlock.ACTIVE) && state.getValue(SinkBlock.ACTIVE);
        }, false);
    }

    /** Whether an upgrade was refused within the last few seconds. */
    public boolean upgradeJustRefused() {
        return player.level().getGameTime() - refusedAt < REFUSED_TICKS;
    }

    public int tankCapacity() {
        return whole(DATA_CAPACITY);
    }

    /** What a tank holds, as synced. */
    public FluidStack fluid(int tank) {
        int base = DATA_TANKS + tank * DATA_PER_TANK;
        Fluid fluid = BuiltInRegistries.FLUID.byId(data.get(base));
        int amount = whole(base + 1);
        return fluid == Fluids.EMPTY || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    /** Which buffer slots the next batch takes from, at least in part. */
    public boolean[] nextBatchSlots() {
        int[] counts = new int[bufferSlots];
        for (int slot = 0; slot < bufferSlots; slot++) {
            counts[slot] = items.getStackInSlot(slot).getCount();
        }
        int rate = itemRate();
        double budget = rate < 0 ? SinkRates.UNLIMITED : rate * (SinkRates.BATCH_TICKS / 20.0);
        int[] taken = SinkBatch.plan(counts, budget);
        boolean[] marked = new boolean[bufferSlots];
        for (int slot = 0; slot < bufferSlots; slot++) {
            marked[slot] = taken[slot] > 0;
        }
        return marked;
    }

    /** Valid while the player is near and, on the server, the Sink is still formed. */
    @Override
    public boolean stillValid(Player player) {
        return (sink == null || SinkBlock.isFormed(sink.getBlockState()))
                && stillValid(access, player, MachineContent.SINK.get());
    }

    /**
     * Shift click: buffer and upgrades go to the inventory; from the inventory
     * Capacity Upgrades go only to the upgrade slots, anything else into the
     * open buffer slots.
     */
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
        int inventoryStart = bufferSlots + upgradeSlots;
        if (index < inventoryStart) {
            if (!moveItemStackTo(stack, inventoryStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean upgrade = stack.is(ServusFoundationItems.CAPACITY_UPGRADE.get());
            if (!moveItemStackTo(stack, upgrade ? bufferSlots : 0, upgrade ? inventoryStart : bufferSlots, false)) {
                return ItemStack.EMPTY;
            }
        }
        QuickMoves.settle(slot, stack);
        return copy;
    }
}
