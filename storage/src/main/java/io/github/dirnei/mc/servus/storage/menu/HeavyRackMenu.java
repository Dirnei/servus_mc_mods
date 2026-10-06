package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Chest style menu: the rack's slots in rows of nine above the player inventory. */
public class HeavyRackMenu extends AbstractContainerMenu {

    private static final int COLUMNS = 9;

    private final HeavyRackBlockEntity rack;
    private final ContainerLevelAccess access;
    private final int rackSlots;

    public HeavyRackMenu(int containerId, Inventory playerInventory, HeavyRackBlockEntity rack) {
        super(ServusStorageMenus.HEAVY_RACK.get(), containerId);
        this.rack = rack;
        this.access = ContainerLevelAccess.create(rack.getLevel(), rack.getBlockPos());
        this.rackSlots = rack.items().getSlots();
        int rows = rows();
        for (int slot = 0; slot < rackSlots; slot++) {
            addSlot(new SlotItemHandler(rack.items(), slot, 8 + (slot % COLUMNS) * 18, 18 + (slot / COLUMNS) * 18));
        }
        int inventoryY = 18 + rows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, inventoryY + 58));
        }
    }

    public static HeavyRackMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof HeavyRackBlockEntity rack) {
            return new HeavyRackMenu(containerId, playerInventory, rack);
        }
        throw new IllegalStateException("No heavy duty rack at " + pos);
    }

    public HeavyRackBlockEntity rack() {
        return rack;
    }

    /** Rows of rack slots, at least one. */
    public int rows() {
        return Math.max(1, (rackSlots + COLUMNS - 1) / COLUMNS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < rackSlots
                ? moveItemStackTo(stack, rackSlots, slots.size(), true)
                : moveItemStackTo(stack, 0, rackSlots, false);
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
        return stillValid(access, player, ServusStorageBlocks.HEAVY_RACK.get());
    }
}
