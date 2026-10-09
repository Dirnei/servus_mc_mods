package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctChannels;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The endpoint configuration of one duct side: per present line (Item Pipe,
 * Power Cable, Fluid Pipe) the mode, the channel and a remove button (the data
 * line only on or off and remove), for the
 * Item Pipe the side's filter as ghost slots and its priority, plus the
 * player inventory. Everything but the slots goes through button ids.
 */
public class UtilityDuctMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 8;
    public static final int FILTER_Y = 72;
    public static final int INVENTORY_Y = 122;
    public static final int HOTBAR_Y = 180;

    /** Mode button of a line: {@code BUTTON_CYCLE_MODE + line.ordinal()}. */
    public static final int BUTTON_CYCLE_MODE = 10;
    /** Remove button of a line: {@code BUTTON_REMOVE_LINE + line.ordinal()}. */
    public static final int BUTTON_REMOVE_LINE = 20;
    public static final int BUTTON_PRIORITY_DOWN = 30;
    public static final int BUTTON_PRIORITY_UP = 31;
    /** Channel button: {@code BUTTON_CHANNEL + line.ordinal() * DuctChannels.COUNT + channel}. */
    public static final int BUTTON_CHANNEL = 40;

    private static final int DATA_PRIORITY = 0;

    private final UtilityDuctBlockEntity duct;
    private final Direction side;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    /** The line whose page the screen shows; client side view state. */
    private Line page;

    public UtilityDuctMenu(int containerId, Inventory playerInventory, UtilityDuctBlockEntity duct, Direction side) {
        super(ServusLogisticsMenus.UTILITY_DUCT.get(), containerId);
        this.duct = duct;
        this.side = side;
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
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, SLOT_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, SLOT_X + column * 18, HOTBAR_Y));
        }
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

    /** The side this menu configures. */
    public Direction side() {
        return side;
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

    private IItemHandler filter() {
        return duct.filter(side);
    }

    @Override
    public void broadcastChanges() {
        data.set(DATA_PRIORITY, duct.priority(side));
        super.broadcastChanges();
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
        if (id == BUTTON_PRIORITY_DOWN || id == BUTTON_PRIORITY_UP) {
            duct.setPriority(side, duct.priority(side) + (id == BUTTON_PRIORITY_UP ? 1 : -1));
            data.set(DATA_PRIORITY, duct.priority(side));
            return true;
        }
        if (id >= BUTTON_CHANNEL && id < BUTTON_CHANNEL + Line.VALUES.length * DuctChannels.COUNT) {
            int offset = id - BUTTON_CHANNEL;
            Line line = Line.byOrdinal(offset / DuctChannels.COUNT);
            if (duct.has(line)) {
                duct.setChannel(line, side, offset % DuctChannels.COUNT);
            }
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < UtilityDuctBlockEntity.FILTER_SLOTS && slots.get(slotId) instanceof GhostSlot ghost) {
            if (ghost.isActive()) {
                ghost.setGhost(getCarried());
            }
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
                IItemHandler filter = filter();
                for (int i = 0; i < UtilityDuctBlockEntity.FILTER_SLOTS; i++) {
                    if (filter.getStackInSlot(i).isEmpty()) {
                        ((GhostSlot) slots.get(i)).setGhost(stack);
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
