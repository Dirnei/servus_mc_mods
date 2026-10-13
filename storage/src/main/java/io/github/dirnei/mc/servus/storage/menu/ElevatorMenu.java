package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.LiftUpgradeSlots;
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
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The Speed upgrades of an elevator shaft, opened from any of its segments but
 * always on the lowest one, where they are kept; then the player inventory.
 */
public class ElevatorMenu extends AbstractContainerMenu {

    public static final int UPGRADES_Y = 30;
    public static final int INVENTORY_Y = 74;
    public static final int IMAGE_HEIGHT = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET + 18 + 6;
    private static final int WIDTH = 176;

    private final ContainerLevelAccess access;
    private final int upgradeSlots;

    /** Server side: the lowest segment's own slots. */
    public ElevatorMenu(int containerId, Inventory playerInventory, ElevatorBlockEntity bottom) {
        this(containerId, playerInventory, ContainerLevelAccess.create(bottom.getLevel(), bottom.getBlockPos()), bottom.upgrades());
    }

    private ElevatorMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, ItemSlots upgrades) {
        super(ServusStorageMenus.ELEVATOR.get(), containerId);
        this.access = access;
        this.upgradeSlots = upgrades.getSlots();
        for (int slot = 0; slot < upgradeSlots; slot++) {
            addSlot(new ResourceHandlerSlot(upgrades, upgrades::set, slot, upgradeX(slot), UPGRADES_Y));
        }
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
    }

    /** Client side: the server writes the lowest segment's position and the slot count. */
    public static ElevatorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos bottom = buf.readBlockPos();
        int slots = buf.readVarInt();
        return new ElevatorMenu(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), bottom),
                new LiftUpgradeSlots(slots, null));
    }

    /** What the server writes for {@link #fromNetwork}. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, ElevatorBlockEntity bottom) {
        buf.writeBlockPos(bottom.getBlockPos());
        buf.writeVarInt(bottom.upgrades().getSlots());
    }

    public int upgradeSlots() {
        return upgradeSlots;
    }

    /** The upgrade row is centred. */
    public int upgradeX(int slot) {
        return (WIDTH - upgradeSlots * 18) / 2 + 1 + slot * 18;
    }

    public int hotbarY() {
        return INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET;
    }

    /** Shift click moves Speed upgrades between the upgrade row and the player inventory. */
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
        if (index < upgradeSlots) {
            if (!moveItemStackTo(stack, upgradeSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(ServusFoundationItems.SPEED_UPGRADE.get()) || !moveItemStackTo(stack, 0, upgradeSlots, false)) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.ELEVATOR.get());
    }
}
