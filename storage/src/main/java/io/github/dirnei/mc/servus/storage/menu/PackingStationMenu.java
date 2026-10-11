package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStatusPayload;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import io.github.dirnei.mc.servus.foundation.upgrade.Upgrades;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Packing Station's two buffers, To storage and To network, as read-only
 * rows above the player inventory, then a row of upgrade slots, the only
 * slots players can change; the status line and the rate come along as a
 * payload.
 */
public class PackingStationMenu extends AbstractContainerMenu {

    public static final int COLUMNS = 9;
    public static final int FIRST_LABEL_Y = 18;
    private static final int SYNC_INTERVAL = 5;

    /** A slot that shows a system buffer and refuses every click. */
    public static final class ReadOnlySlot extends ResourceHandlerSlot {

        public ReadOnlySlot(ItemView handler, int index, int x, int y) {
            super(handler, index, x, y);
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
    private int syncTimer;
    private int lastSignature = Integer.MIN_VALUE;

    /** Server side: the station's own buffers. */
    public PackingStationMenu(int containerId, Inventory playerInventory, PackingStationBlockEntity point) {
        this(containerId, playerInventory, point, point.buffer(), point.outgoing(), point.upgrades());
    }

    private PackingStationMenu(int containerId, Inventory playerInventory, PackingStationBlockEntity point,
                               ItemView toStorage, ItemView toNetwork, ItemView upgrades) {
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
        imageHeight = inventoryY + 3 * 18 + 4 + 18 + 6;
        addRow(toStorage, toStorageY);
        addRow(toNetwork, toNetworkY);
        for (int slot = 0; slot < upgradeSlots; slot++) {
            addSlot(new ResourceHandlerSlot(upgrades, upgrades::set, slot, 8 + slot * 18, upgradesY));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, hotbarY()));
        }
    }

    private void addRow(ItemView handler, int y) {
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
        return inventoryY + 3 * 18 + 4;
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
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.PACKING_STATION.get())
                || stillValid(access, player, ServusStorageBlocks.WIRELESS_PACKING_STATION.get());
    }

    // Sync

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer) || point == null) {
            return;
        }
        if (++syncTimer < SYNC_INTERVAL) {
            return;
        }
        syncTimer = 0;
        PackingStatusPayload payload = point.status();
        int signature = payload.hashCode();
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }
}
