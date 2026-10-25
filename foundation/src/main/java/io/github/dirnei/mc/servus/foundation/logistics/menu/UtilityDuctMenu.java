package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsMenus;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
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
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The endpoint configuration of a duct, one side at a time: the side can be
 * switched to any side with a block other than a duct beside it. Per present
 * line (Item Pipe, Power Cable, Fluid Pipe) the mode, the Control, the channel
 * and a remove button (the data line only on or off and remove), for the
 * Item Pipe the side's filter as ghost slots and its priority, plus the
 * player inventory. Everything but the slots and the channel goes through
 * button ids; a filter slot is also set by item id through
 * {@link SetDuctFilterPayload}, and channels go through {@link ChannelRequests}.
 * While open, the server sends the player the side's cable channels
 * ({@link CableChannelsPayload}) once a second when they changed.
 */
public class UtilityDuctMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 8;
    public static final int FILTER_Y = 106;
    public static final int INVENTORY_Y = 156;

    /** Mode button of a line: {@code BUTTON_CYCLE_MODE + line.ordinal()}. */
    public static final int BUTTON_CYCLE_MODE = 10;
    /** Remove button of a line: {@code BUTTON_REMOVE_LINE + line.ordinal()}. */
    public static final int BUTTON_REMOVE_LINE = 20;
    public static final int BUTTON_PRIORITY_DOWN = 30;
    public static final int BUTTON_PRIORITY_UP = 31;
    /** Side picker button: {@code BUTTON_SELECT_SIDE + direction.ordinal()}. */
    public static final int BUTTON_SELECT_SIDE = 40;
    /** Control button of a line: {@code BUTTON_CYCLE_CONTROL + line.ordinal()}. */
    public static final int BUTTON_CYCLE_CONTROL = 50;

    private static final int DATA_PRIORITY = 0;
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
        this.data = new SimpleContainerData(1);
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
        data.set(DATA_PRIORITY, duct.priority(side));
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
            data.set(DATA_PRIORITY, duct.priority(side));
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

    private ItemSlots filter() {
        return duct.filter(side);
    }

    @Override
    public void broadcastChanges() {
        data.set(DATA_PRIORITY, duct.priority(side));
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
        if (id >= BUTTON_CYCLE_MODE && id < BUTTON_CYCLE_MODE + Line.VALUES.length) {
            Line line = Line.byOrdinal(id - BUTTON_CYCLE_MODE);
            if (duct.has(line)) {
                SideMode current = duct.mode(line, side);
                SideMode next = line == Line.DATA
                        ? (current == SideMode.DISABLED ? SideMode.BOTH : SideMode.DISABLED)
                        : UtilityDuctBlockEntity.cycle(current);
                duct.setMode(line, side, next);
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
        if (id >= BUTTON_CYCLE_CONTROL && id < BUTTON_CYCLE_CONTROL + Line.VALUES.length) {
            Line line = Line.byOrdinal(id - BUTTON_CYCLE_CONTROL);
            if (line != Line.DATA && duct.has(line)) {
                duct.setControl(line, side, duct.control(line, side).next());
            }
            return true;
        }
        if (id == BUTTON_PRIORITY_DOWN || id == BUTTON_PRIORITY_UP) {
            duct.setPriority(side, duct.priority(side) + (id == BUTTON_PRIORITY_UP ? 1 : -1));
            data.set(DATA_PRIORITY, duct.priority(side));
            return true;
        }
        return false;
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
