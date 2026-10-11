package io.github.dirnei.mc.servus.foundation.adapter;

import io.github.dirnei.mc.servus.foundation.logistics.menu.GhostSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * The adapter's upkeep GUI: a ghost slot for the upkeep item, the level with
 * minus and plus buttons, the count in the machine, and the player inventory.
 */
public class AdapterMenu extends AbstractContainerMenu {

    public static final int GHOST_X = 8;
    public static final int GHOST_Y = 40;
    public static final int INVENTORY_Y = 84;
    public static final int HOTBAR_Y = 142;
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
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
        addDataSlots(data);
        refresh();
    }

    /** A one slot view of the adapter's upkeep item. */
    private final IItemHandlerModifiable ghost = new IItemHandlerModifiable() {
        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            adapter.setUpkeepItem(stack);
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return adapter.upkeepItem();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
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
