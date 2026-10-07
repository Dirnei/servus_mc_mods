package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * One slot per shuttle place above the player inventory. Slots whose shuttle
 * is out on a job cannot be emptied until it is back.
 */
public class ShuttleGarageMenu extends AbstractContainerMenu {

    public static final int SLOTS_Y = 30;
    public static final int INVENTORY_Y = 84;
    public static final int HOTBAR_Y = 142;
    private static final int COLUMNS = 9;

    private final ShuttleGarageBlockEntity garage;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int capacity;
    private final int inventoryStart;
    private final DataSlot out = DataSlot.standalone();

    public ShuttleGarageMenu(int containerId, Inventory playerInventory, ShuttleGarageBlockEntity garage) {
        super(ServusStorageMenus.SHUTTLE_GARAGE.get(), containerId);
        this.garage = garage;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(garage.getLevel(), garage.getBlockPos());
        this.capacity = garage.capacity();
        for (int slot = 0; slot < capacity; slot++) {
            int index = slot;
            addSlot(new SlotItemHandler(garage.shuttles(), slot, slotX(slot), SLOTS_Y) {
                @Override
                public boolean mayPickup(Player player) {
                    return !locked(index);
                }
            });
        }
        inventoryStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
        addDataSlot(out);
    }

    public static ShuttleGarageMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof ShuttleGarageBlockEntity garage) {
            return new ShuttleGarageMenu(containerId, playerInventory, garage);
        }
        throw new IllegalStateException("No shuttle garage at " + pos);
    }

    /** Where a shuttle slot sits: the row is centred. */
    public int slotX(int slot) {
        return 8 + (COLUMNS - capacity) * 9 + slot * 18;
    }

    public ShuttleGarageBlockEntity garage() {
        return garage;
    }

    public int capacity() {
        return capacity;
    }

    public int inventoryStart() {
        return inventoryStart;
    }

    /** Shuttles of this garage out on a job, as last synced. */
    public int out() {
        return out.get();
    }

    /** The first {@code out} filled slots stand for the shuttles that are away. */
    public boolean locked(int slot) {
        if (garage.shuttles().getStackInSlot(slot).isEmpty()) {
            return false;
        }
        int filledBefore = 0;
        for (int i = 0; i < slot; i++) {
            if (!garage.shuttles().getStackInSlot(i).isEmpty()) {
                filledBefore++;
            }
        }
        return filledBefore < out.get();
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            out.set(garage.outCount());
        }
        super.broadcastChanges();
    }

    /** Shift click: a Shuttle item in the inventory goes into a free place, a garage slot goes back to the inventory. */
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
        ItemStack original = stack.copy();
        boolean moved;
        if (index < capacity) {
            if (locked(index)) {
                return ItemStack.EMPTY;
            }
            moved = moveItemStackTo(stack, inventoryStart, slots.size(), true);
        } else {
            if (!stack.is(ServusStorageItems.SHUTTLE.get())) {
                return ItemStack.EMPTY;
            }
            moved = moveItemStackTo(stack, 0, capacity, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.SHUTTLE_GARAGE.get());
    }
}
