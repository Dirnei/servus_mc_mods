package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.pattern.PatternItem;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Chest style menu for a Pattern Storage: its configured pattern slots in rows
 * of nine above the player inventory. The server sends the slot count, since
 * the data driven size is only known there.
 */
public class PatternStorageMenu extends AbstractContainerMenu {

    private static final int COLUMNS = 9;

    private final ContainerLevelAccess access;
    private final int patternSlots;

    public PatternStorageMenu(int containerId, Inventory playerInventory, PatternStorageBlockEntity storage) {
        this(containerId, playerInventory, storage.patterns(), storage.capacity(),
                ContainerLevelAccess.create(storage.getLevel(), storage.getBlockPos()));
    }

    private PatternStorageMenu(int containerId, Inventory playerInventory, IItemHandler handler, int count, ContainerLevelAccess access) {
        super(ServusStorageMenus.PATTERN_STORAGE.get(), containerId);
        this.access = access;
        this.patternSlots = count;
        for (int slot = 0; slot < count; slot++) {
            addSlot(new SlotItemHandler(handler, slot, 8 + (slot % COLUMNS) * 18, 18 + (slot / COLUMNS) * 18));
        }
        int inventoryY = 18 + rows() * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, inventoryY + 58));
        }
    }

    /** Client side: the slot count travels with the position; the slots mirror the server's. */
    public static PatternStorageMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readVarInt();
        ItemStackHandler mirror = new ItemStackHandler(count) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return PatternItem.encoded(stack).isPresent();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        return new PatternStorageMenu(containerId, playerInventory, mirror, count,
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public int rows() {
        return Math.max(1, (patternSlots + COLUMNS - 1) / COLUMNS);
    }

    public int patternSlots() {
        return patternSlots;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < patternSlots
                ? moveItemStackTo(stack, patternSlots, slots.size(), true)
                : PatternItem.encoded(stack).isPresent() && moveItemStackTo(stack, 0, patternSlots, false);
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
        return stillValid(access, player, ServusStorageBlocks.PATTERN_STORAGE.get());
    }
}
