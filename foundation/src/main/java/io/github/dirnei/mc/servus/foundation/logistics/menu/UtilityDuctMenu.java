package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.FluidFilter;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsMenus;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The endpoint configuration of a duct, one side at a time: the side can be
 * switched to any side with a block other than a duct beside it. Per present
 * line (Item Pipe, Power Cable, Fluid Pipe) the In, Out and Pull flags, the redstone setting, On/Off, the channel
 * and a remove button (the data line only on or off and remove), for the
 * Item Pipe the side's filter as ghost slots and its priority, for the Fluid
 * Pipe the side's fluid filter and fluid priority, plus the player inventory.
 * Everything but the slots and the channel goes through button ids; a filter
 * slot is also set by item id through {@link SetDuctFilterPayload}, a fluid
 * filter slot only by fluid id through {@link SetDuctFluidFilterPayload}, the
 * amount of either through {@link SetDuctFilterAmountPayload}, and channels go
 * through {@link ChannelRequests}.
 * While open, the server sends the player the side's cable channels
 * ({@link CableChannelsPayload}) once a second when they changed.
 */
public class UtilityDuctMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 8;
    public static final int FILTER_Y = 73;
    public static final int INVENTORY_Y = 122;

    /** In toggle of a line: {@code BUTTON_TOGGLE_IN + line.ordinal()}. */
    public static final int BUTTON_TOGGLE_IN = 10;
    /** Remove button of a line: {@code BUTTON_REMOVE_LINE + line.ordinal()}. */
    public static final int BUTTON_REMOVE_LINE = 20;
    public static final int BUTTON_PRIORITY_DOWN = 30;
    public static final int BUTTON_PRIORITY_UP = 31;
    public static final int BUTTON_FLUID_PRIORITY_DOWN = 32;
    public static final int BUTTON_FLUID_PRIORITY_UP = 33;
    /** Side picker button: {@code BUTTON_SELECT_SIDE + direction.ordinal()}. */
    public static final int BUTTON_SELECT_SIDE = 40;
    /** Redstone button of a line: {@code BUTTON_CYCLE_REDSTONE + line.ordinal()}. */
    public static final int BUTTON_CYCLE_REDSTONE = 50;
    /** Out toggle of a line: {@code BUTTON_TOGGLE_OUT + line.ordinal()}. */
    public static final int BUTTON_TOGGLE_OUT = 60;
    /** Pull toggle of a line: {@code BUTTON_TOGGLE_PULL + line.ordinal()}. */
    public static final int BUTTON_TOGGLE_PULL = 70;
    /** On/Off button of a line: {@code BUTTON_TOGGLE_ON + line.ordinal()}; on the data line it connects or disconnects the side. */
    public static final int BUTTON_TOGGLE_ON = 80;

    private static final int DATA_PRIORITY = 0;
    private static final int DATA_FLUID_PRIORITY = 1;
    private static final int CABLE_SYNC_TICKS = 20;

    private final UtilityDuctBlockEntity duct;
    private Direction side;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Player player;
    /** The line whose page the screen shows; client side view state. */
    private Line page;
    private int cableSyncCountdown;
    private CableChannelsPayload cableChannelsSent;

    public UtilityDuctMenu(int containerId, Inventory playerInventory, UtilityDuctBlockEntity duct, Direction side) {
        super(ServusLogisticsMenus.UTILITY_DUCT.get(), containerId);
        this.duct = duct;
        this.side = side;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(duct.getLevel(), duct.getBlockPos());
        this.data = new SimpleContainerData(2);
        addDataSlots(data);
        for (int i = 0; i < UtilityDuctBlockEntity.FILTER_SLOTS; i++) {
            addSlot(new GhostSlot(this::filter, i, SLOT_X + i * 18, FILTER_Y) {
                @Override
                public boolean isActive() {
                    return page == Line.ITEM;
                }
            });
        }
        PlayerInventorySlots.add(this::addSlot, playerInventory, SLOT_X, INVENTORY_Y);
        refreshPriorities();
    }

    public static UtilityDuctMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Direction side = buf.readEnum(Direction.class);
        if (playerInventory.player.level().getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            return new UtilityDuctMenu(containerId, playerInventory, duct, side);
        }
        throw new IllegalStateException("No utility duct at " + pos);
    }

    public UtilityDuctBlockEntity duct() {
        return duct;
    }

    /** The side this menu configures now. */
    public Direction side() {
        return side;
    }

    /**
     * Switches to another side when a block other than a duct is beside it.
     * The server sends the new side's priority and channels.
     *
     * @return whether the side is shown now
     */
    public boolean selectSide(Direction next) {
        if (!duct.isSelectable(next)) {
            return false;
        }
        if (next != side) {
            side = next;
            refreshPriorities();
            sendCableChannels(true);
        }
        return true;
    }

    /** The line whose page is shown, the first present line until a tab is clicked. */
    public Line page() {
        if (page == null || !duct.has(page)) {
            page = null;
            for (Line line : Line.VALUES) {
                if (duct.has(line)) {
                    page = line;
                    break;
                }
            }
        }
        return page;
    }

    public void setPage(Line line) {
        this.page = line;
    }

    public int priority() {
        return data.get(DATA_PRIORITY);
    }

    public int fluidPriority() {
        return data.get(DATA_FLUID_PRIORITY);
    }

    private void refreshPriorities() {
        data.set(DATA_PRIORITY, duct.priority(side));
        data.set(DATA_FLUID_PRIORITY, duct.fluidPriority(side));
    }

    private ItemSlots filter() {
        return duct.filter(side);
    }

    @Override
    public void broadcastChanges() {
        refreshPriorities();
        super.broadcastChanges();
        if (--cableSyncCountdown <= 0) {
            cableSyncCountdown = CABLE_SYNC_TICKS;
            sendCableChannels(false);
        }
    }

    /**
     * Server side: sends the player the side's cable channels, unless they
     * equal what was sent last and {@code force} is false. Players without a
     * connection, such as GameTest players, get nothing.
     */
    public void sendCableChannels(boolean force) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection == null) {
            return;
        }
        CableChannelsPayload payload = ChannelRequests.cableChannels(this, Teams.teamOf(serverPlayer), serverPlayer.level().getServer());
        if (force || !payload.equals(cableChannelsSent)) {
            cableChannelsSent = payload;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Line toggled = lineOf(id, BUTTON_TOGGLE_IN);
        if (toggled == null) {
            toggled = lineOf(id, BUTTON_TOGGLE_OUT);
        }
        if (toggled == null) {
            toggled = lineOf(id, BUTTON_TOGGLE_PULL);
        }
        if (toggled != null) {
            if (toggled != Line.DATA && duct.has(toggled)) {
                DuctMode mode = duct.mode(toggled, side);
                int base = id - toggled.ordinal();
                duct.setMode(toggled, side, base == BUTTON_TOGGLE_IN ? mode.toggleIn() : base == BUTTON_TOGGLE_OUT ? mode.toggleOut() : mode.togglePull());
            }
            return true;
        }
        Line switched = lineOf(id, BUTTON_TOGGLE_ON);
        if (switched != null) {
            if (switched == Line.DATA && duct.has(switched)) {
                duct.setMode(switched, side, duct.mode(switched, side).connects() ? DuctMode.NONE : DuctMode.IN_OUT);
            } else if (duct.has(switched)) {
                duct.setOn(switched, side, !duct.isOn(switched, side));
            }
            return true;
        }
        if (id >= BUTTON_REMOVE_LINE && id < BUTTON_REMOVE_LINE + Line.VALUES.length) {
            Line line = Line.byOrdinal(id - BUTTON_REMOVE_LINE);
            ItemStack drop = new ItemStack(duct.lineItem(line));
            if (!player.level().isClientSide() && duct.removeLine(line)) {
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
                if (duct.lines().isEmpty()) {
                    player.level().removeBlock(duct.getBlockPos(), false);
                }
            }
            return true;
        }
        if (id >= BUTTON_SELECT_SIDE && id < BUTTON_SELECT_SIDE + Direction.values().length) {
            return selectSide(Direction.values()[id - BUTTON_SELECT_SIDE]);
        }
        Line cycled = lineOf(id, BUTTON_CYCLE_REDSTONE);
        if (cycled != null) {
            if (cycled != Line.DATA && duct.has(cycled)) {
                duct.setRedstone(cycled, side, duct.redstone(cycled, side).next());
            }
            return true;
        }
        if (id == BUTTON_PRIORITY_DOWN || id == BUTTON_PRIORITY_UP) {
            duct.setPriority(side, duct.priority(side) + (id == BUTTON_PRIORITY_UP ? 1 : -1));
            refreshPriorities();
            return true;
        }
        if (id == BUTTON_FLUID_PRIORITY_DOWN || id == BUTTON_FLUID_PRIORITY_UP) {
            duct.setFluidPriority(side, duct.fluidPriority(side) + (id == BUTTON_FLUID_PRIORITY_UP ? 1 : -1));
            refreshPriorities();
            return true;
        }
        return false;
    }

    /** The line of a per line button id in the block starting at {@code base}, or null outside that block. */
    private static Line lineOf(int id, int base) {
        return id >= base && id < base + Line.VALUES.length ? Line.byOrdinal(id - base) : null;
    }

    /**
     * Sets a filter slot to one copy of the stack, or clears it with an empty
     * stack. Does nothing when the duct has no Item Pipe or the slot is out of
     * range; returns whether the slot was set.
     */
    public boolean setFilter(int slot, ItemStack stack) {
        if (!duct.has(Line.ITEM) || slot < 0 || slot >= UtilityDuctBlockEntity.FILTER_SLOTS) {
            return false;
        }
        ((GhostSlot) slots.get(slot)).setGhost(stack);
        return true;
    }

    /**
     * Server side: sets a filter slot of the endpoint GUI the player has open to
     * the item with that id; an empty id, an unknown id or air clears it.
     * Returns false and changes nothing when the player has no usable endpoint
     * GUI open, its duct has no Item Pipe or the slot is out of range.
     */
    public static boolean setFilterById(Player player, int slot, Optional<Identifier> item) {
        if (!(player.containerMenu instanceof UtilityDuctMenu menu) || !menu.stillValid(player)) {
            return false;
        }
        ItemStack stack = item.flatMap(BuiltInRegistries.ITEM::getOptional).map(ItemStack::new).orElse(ItemStack.EMPTY);
        return menu.setFilter(slot, stack);
    }

    /**
     * Sets a fluid filter slot to a fluid, or clears it with null. Does nothing
     * when the duct has no Fluid Pipe or the slot is out of range; returns
     * whether the slot was set.
     */
    public boolean setFluidFilter(int slot, Fluid fluid) {
        if (!duct.has(Line.FLUID) || slot < 0 || slot >= FluidFilter.SLOTS) {
            return false;
        }
        duct.setFluidFilter(side, slot, fluid);
        return true;
    }

    /**
     * Server side: sets a fluid filter slot of the endpoint GUI the player has
     * open to the fluid with that id; an empty id clears it. Returns false and
     * changes nothing when the player has no usable endpoint GUI open, its duct
     * has no Fluid Pipe, the slot is out of range or the id names no fluid.
     */
    public static boolean setFluidFilterById(Player player, int slot, Optional<Identifier> fluid) {
        if (!(player.containerMenu instanceof UtilityDuctMenu menu) || !menu.stillValid(player)) {
            return false;
        }
        if (fluid.isEmpty()) {
            return menu.setFluidFilter(slot, null);
        }
        Fluid found = BuiltInRegistries.FLUID.getOptional(fluid.get()).orElse(Fluids.EMPTY);
        return found != Fluids.EMPTY && menu.setFluidFilter(slot, found);
    }

    /**
     * Server side: sets the amount of a set item or fluid filter slot of the endpoint GUI the player has open;
     * 0 removes it, and the duct keeps it in range. Returns false and changes nothing when the player has no
     * usable endpoint GUI open, the line is neither the Item Pipe nor the Fluid Pipe or missing, or the slot is
     * out of range or empty.
     */
    public static boolean setFilterAmount(Player player, int lineOrdinal, int slot, int amount) {
        if (!(player.containerMenu instanceof UtilityDuctMenu menu) || !menu.stillValid(player)
                || lineOrdinal < 0 || lineOrdinal >= Line.VALUES.length) {
            return false;
        }
        Line line = Line.byOrdinal(lineOrdinal);
        UtilityDuctBlockEntity duct = menu.duct;
        if (!duct.has(line) || slot < 0 || slot >= FluidFilter.SLOTS) {
            return false;
        }
        if (line == Line.ITEM && !duct.filter(menu.side).getStackInSlot(slot).isEmpty()) {
            duct.setItemAmount(menu.side, slot, amount);
            return true;
        }
        if (line == Line.FLUID && duct.fluidFilter(menu.side).get(slot) != null) {
            duct.setFluidAmount(menu.side, slot, amount);
            return true;
        }
        return false;
    }

    /** A click on a filter slot: right click clears it, any other click copies the cursor (an empty cursor clears). */
    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < UtilityDuctBlockEntity.FILTER_SLOTS) {
            boolean rightClick = clickType == ContainerInput.PICKUP && button == 1;
            setFilter(slotId, rightClick ? ItemStack.EMPTY : getCarried());
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Shift clicking an inventory item adds it to the first free filter slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= UtilityDuctBlockEntity.FILTER_SLOTS && index < slots.size()) {
            ItemStack stack = slots.get(index).getItem();
            if (!stack.isEmpty()) {
                ItemSlots filter = filter();
                for (int i = 0; i < UtilityDuctBlockEntity.FILTER_SLOTS; i++) {
                    if (filter.getStackInSlot(i).isEmpty()) {
                        setFilter(i, stack);
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusLogisticsBlocks.UTILITY_DUCT.get());
    }
}
