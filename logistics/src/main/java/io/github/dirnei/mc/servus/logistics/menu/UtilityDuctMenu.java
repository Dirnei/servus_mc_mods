package io.github.dirnei.mc.servus.logistics.menu;

import io.github.dirnei.mc.servus.logistics.duct.Channel;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsMenus;
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
 * Side configuration of one utility duct: the selected side's filter as ghost
 * slots, plus the player inventory. Everything else goes through button ids.
 */
public class UtilityDuctMenu extends AbstractContainerMenu {

    public static final int FILTER_Y = 64;
    public static final int INVENTORY_Y = 130;
    public static final int HOTBAR_Y = 188;

    public static final int BUTTON_SELECT_SIDE = 0;
    public static final int BUTTON_CYCLE_MODE = 10;
    public static final int BUTTON_REMOVE_CHANNEL = 20;
    public static final int BUTTON_PRIORITY_DOWN = 30;
    public static final int BUTTON_PRIORITY_UP = 31;

    private static final int DATA_SIDE = 0;
    private static final int DATA_PRIORITY = 1;

    private final UtilityDuctBlockEntity duct;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private Direction side = Direction.NORTH;

    public UtilityDuctMenu(int containerId, Inventory playerInventory, UtilityDuctBlockEntity duct) {
        super(ServusLogisticsMenus.UTILITY_DUCT.get(), containerId);
        this.duct = duct;
        this.access = ContainerLevelAccess.create(duct.getLevel(), duct.getBlockPos());
        this.data = new SimpleContainerData(2);
        addDataSlots(data);
        for (int i = 0; i < UtilityDuctBlockEntity.FILTER_SLOTS; i++) {
            addSlot(new GhostSlot(this::currentFilter, i, 8 + i * 18, FILTER_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
        pushData();
    }

    public static UtilityDuctMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct) {
            return new UtilityDuctMenu(containerId, playerInventory, duct);
        }
        throw new IllegalStateException("No utility duct at " + pos);
    }

    public UtilityDuctBlockEntity duct() {
        return duct;
    }

    /** The side being configured; the client reads the synced value. */
    public Direction side() {
        return Direction.values()[Math.floorMod(data.get(DATA_SIDE), Direction.values().length)];
    }

    public int priority() {
        return data.get(DATA_PRIORITY);
    }

    private IItemHandler currentFilter() {
        return duct.filter(side());
    }

    private void pushData() {
        data.set(DATA_SIDE, side.ordinal());
        data.set(DATA_PRIORITY, duct.priority(side));
    }

    @Override
    public void broadcastChanges() {
        data.set(DATA_PRIORITY, duct.priority(side));
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SELECT_SIDE && id < BUTTON_SELECT_SIDE + Direction.values().length) {
            side = Direction.values()[id - BUTTON_SELECT_SIDE];
            pushData();
            return true;
        }
        if (id >= BUTTON_CYCLE_MODE && id < BUTTON_CYCLE_MODE + Channel.VALUES.length) {
            Channel channel = Channel.byOrdinal(id - BUTTON_CYCLE_MODE);
            if (duct.has(channel)) {
                duct.setMode(channel, side, UtilityDuctBlockEntity.cycle(duct.mode(channel, side)));
            }
            return true;
        }
        if (id >= BUTTON_REMOVE_CHANNEL && id < BUTTON_REMOVE_CHANNEL + Channel.VALUES.length) {
            Channel channel = Channel.byOrdinal(id - BUTTON_REMOVE_CHANNEL);
            if (!player.level().isClientSide() && duct.removeChannel(channel)) {
                ItemStack drop = new ItemStack(ServusLogisticsItems.channelItem(channel));
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
                if (duct.channels().isEmpty()) {
                    player.level().removeBlock(duct.getBlockPos(), false);
                }
            }
            return true;
        }
        if (id == BUTTON_PRIORITY_DOWN || id == BUTTON_PRIORITY_UP) {
            duct.setPriority(side, duct.priority(side) + (id == BUTTON_PRIORITY_UP ? 1 : -1));
            pushData();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < UtilityDuctBlockEntity.FILTER_SLOTS && slots.get(slotId) instanceof GhostSlot ghost) {
            ghost.setGhost(getCarried());
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
                IItemHandler filter = currentFilter();
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
