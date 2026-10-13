package io.github.dirnei.mc.servus.foundation.adapter;

import io.github.dirnei.mc.servus.foundation.logistics.menu.GhostSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;

/**
 * The adapter's upkeep GUI: a ghost slot for the upkeep item, the level with
 * minus and plus buttons, the count in the machine, and the player inventory.
 */
public class AdapterMenu extends AbstractContainerMenu {

    public static final int GHOST_X = 8;
    public static final int GHOST_Y = 40;
    public static final int INVENTORY_Y = 84;
    public static final int BUTTON_LEVEL_DOWN = 0;
    public static final int BUTTON_LEVEL_UP = 1;

    private static final int DATA_LEVEL = 0;
    private static final int DATA_COUNT = 1;
    private static final int DATA_PROFILE = 2;

    private final MachineAdapterBlockEntity adapter;
    private final ContainerLevelAccess access;
    private final ContainerData data = new SimpleContainerData(3);

    public AdapterMenu(int containerId, Inventory inventory, MachineAdapterBlockEntity adapter) {
        super(AdapterContent.MENU.get(), containerId);
        this.adapter = adapter;
        this.access = ContainerLevelAccess.create(adapter.getLevel(), adapter.getBlockPos());
        addSlot(new GhostSlot(() -> ghost, 0, GHOST_X, GHOST_Y));
        PlayerInventorySlots.add(this::addSlot, inventory, 8, INVENTORY_Y);
        addDataSlots(data);
        refresh();
    }

    /** A one slot view of the adapter's upkeep item. */
    private final ItemSlots ghost = new ItemSlots(1) {
        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            adapter.setUpkeepItem(stack);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return adapter.upkeepItem();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public static AdapterMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof MachineAdapterBlockEntity adapter) {
            return new AdapterMenu(containerId, inventory, adapter);
        }
        throw new IllegalStateException("No machine adapter at " + pos);
    }

    public MachineAdapterBlockEntity adapter() {
        return adapter;
    }

    public int level() {
        return data.get(DATA_LEVEL);
    }

    public int count() {
        return data.get(DATA_COUNT);
    }

    public boolean hasProfile() {
        return data.get(DATA_PROFILE) == 1;
    }

    private void refresh() {
        if (adapter.getLevel() != null && !adapter.getLevel().isClientSide()) {
            data.set(DATA_LEVEL, adapter.upkeepLevel());
            data.set(DATA_COUNT, adapter.upkeepCount());
            data.set(DATA_PROFILE, adapter.profile().isPresent() ? 1 : 0);
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_LEVEL_DOWN || id == BUTTON_LEVEL_UP) {
            adapter.setUpkeepLevel(adapter.upkeepLevel() + (id == BUTTON_LEVEL_UP ? 1 : -1));
            refresh();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId == 0 && slots.get(0) instanceof GhostSlot slot) {
            slot.setGhost(getCarried());
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Shift clicking an inventory item makes it the upkeep item. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index > 0 && index < slots.size()) {
            ItemStack stack = slots.get(index).getItem();
            if (!stack.isEmpty() && slots.get(0) instanceof GhostSlot slot) {
                slot.setGhost(stack);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, AdapterContent.BLOCK.get());
    }
}
