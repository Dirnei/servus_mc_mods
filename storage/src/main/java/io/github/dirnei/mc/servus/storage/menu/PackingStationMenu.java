package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import io.github.dirnei.mc.servus.foundation.upgrade.Upgrades;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStatusPayload;
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
 * The Packing Station's two buffers, To storage and To network, as read-only
 * rows, then a row of upgrade slots, the only slots players can change, and
 * the player inventory. The status line and the rate come along as a payload.
 */
public class PackingStationMenu extends AbstractContainerMenu {

    public static final int COLUMNS = 9;
    public static final int FIRST_LABEL_Y = 18;

    /** A slot that shows a system buffer and refuses every click. */
    public static final class ReadOnlySlot extends ResourceHandlerSlot {

        public ReadOnlySlot(ItemSlots handler, int index, int x, int y) {
            super(handler, handler::set, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private final PackingStationBlockEntity point;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int toStorageSlots;
    private final int toNetworkSlots;
    private final int upgradeSlots;
    private final int upgradesLabelY;
    private final int upgradesY;
    private final int toStorageY;
    private final int toNetworkLabelY;
    private final int toNetworkY;
    private final int statusY;
    private final int inventoryY;
    private final int imageHeight;
    private final PayloadSync<PackingStatusPayload> sync = new PayloadSync<>(5, false, this::status);

    /** Server side: the station's own buffers. */
    public PackingStationMenu(int containerId, Inventory playerInventory, PackingStationBlockEntity point) {
        this(containerId, playerInventory, point, point.buffer(), point.outgoing(), point.upgrades());
    }

    private PackingStationMenu(int containerId, Inventory playerInventory, PackingStationBlockEntity point,
                               ItemSlots toStorage, ItemSlots toNetwork, ItemSlots upgrades) {
        super(ServusStorageMenus.PACKING_STATION.get(), containerId);
        this.point = point;
        this.player = playerInventory.player;
        this.access = point == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(point.getLevel(), point.getBlockPos());
        this.toStorageSlots = toStorage.getSlots();
        this.toNetworkSlots = toNetwork.getSlots();
        this.upgradeSlots = upgrades.getSlots();
        toStorageY = FIRST_LABEL_Y + 10;
        toNetworkLabelY = toStorageY + rows(toStorageSlots) * 18 + 4;
        toNetworkY = toNetworkLabelY + 10;
        upgradesLabelY = toNetworkY + rows(toNetworkSlots) * 18 + 4;
        upgradesY = upgradesLabelY + 10;
        statusY = upgradesY + 18 + 4;
        inventoryY = statusY + 22;
        imageHeight = inventoryY + PlayerInventorySlots.HOTBAR_OFFSET + 18 + 6;
        addRow(toStorage, toStorageY);
        addRow(toNetwork, toNetworkY);
        for (int slot = 0; slot < upgradeSlots; slot++) {
            addSlot(new ResourceHandlerSlot(upgrades, upgrades::set, slot, 8 + slot * 18, upgradesY));
        }
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, inventoryY);
    }

    private void addRow(ItemSlots handler, int y) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            addSlot(new ReadOnlySlot(handler, slot, 8 + (slot % COLUMNS) * 18, y + (slot / COLUMNS) * 18));
        }
    }

    private static int rows(int slots) {
        return Math.max(1, (slots + COLUMNS - 1) / COLUMNS);
    }

    /** Client side: the server writes the station's position and both buffer sizes. */
    public static PackingStationMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int toStorage = buf.readVarInt();
        int toNetwork = buf.readVarInt();
        int upgrades = buf.readVarInt();
        PackingStationBlockEntity point = playerInventory.player.level().getBlockEntity(pos) instanceof PackingStationBlockEntity found
                ? found : null;
        return new PackingStationMenu(containerId, playerInventory, point, new ItemSlots(toStorage), new ItemSlots(toNetwork),
                new UpgradeSlots(upgrades, null));
    }

    /** What the server writes for {@link #fromNetwork}. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, PackingStationBlockEntity point) {
        buf.writeBlockPos(point.getBlockPos());
        buf.writeVarInt(point.buffer().getSlots());
        buf.writeVarInt(point.outgoing().getSlots());
        buf.writeVarInt(point.upgrades().getSlots());
    }

    public int toStorageSlots() {
        return toStorageSlots;
    }

    public int toNetworkSlots() {
        return toNetworkSlots;
    }

    public int toStorageY() {
        return toStorageY;
    }

    public int toNetworkLabelY() {
        return toNetworkLabelY;
    }

    public int toNetworkY() {
        return toNetworkY;
    }

    public int upgradeSlots() {
        return upgradeSlots;
    }

    public int upgradesLabelY() {
        return upgradesLabelY;
    }

    public int upgradesY() {
        return upgradesY;
    }

    /** Index of the first upgrade slot. */
    public int upgradesStart() {
        return toStorageSlots + toNetworkSlots;
    }

    public int statusY() {
        return statusY;
    }

    public int inventoryY() {
        return inventoryY;
    }

    public int hotbarY() {
        return inventoryY + PlayerInventorySlots.HOTBAR_OFFSET;
    }

    public int imageHeight() {
        return imageHeight;
    }

    /** Index of the first player inventory slot. */
    public int inventoryStart() {
        return toStorageSlots + toNetworkSlots + upgradeSlots;
    }

    /**
     * Shift click moves upgrades between the upgrade row and the player
     * inventory; the buffer rows are read-only and nothing else moves.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < upgradesStart() || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < inventoryStart()) {
            if (!moveItemStackTo(stack, inventoryStart(), slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!Upgrades.isUpgrade(stack) || !moveItemStackTo(stack, upgradesStart(), inventoryStart(), false)) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.PACKING_STATION.get());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (point != null) {
            sync.tick(player);
        }
    }

    private PackingStatusPayload status() {
        return point.status();
    }
}
