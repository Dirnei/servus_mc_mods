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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Packing Station's two buffers, To storage and To network, as read-only
 * rows above the player inventory; the status line comes along as a payload.
 * Players can look but neither take nor put items.
 */
public class PackingStationMenu extends AbstractContainerMenu {

    public static final int COLUMNS = 9;
    public static final int FIRST_LABEL_Y = 18;
    private static final int SYNC_INTERVAL = 5;

    /** A slot that shows a system buffer and refuses every click. */
    public static final class ReadOnlySlot extends SlotItemHandler {

        public ReadOnlySlot(IItemHandler handler, int index, int x, int y) {
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
        this(containerId, playerInventory, point, point.buffer(), point.outgoing());
    }

    private PackingStationMenu(int containerId, Inventory playerInventory, PackingStationBlockEntity point,
                               IItemHandler toStorage, IItemHandler toNetwork) {
        super(ServusStorageMenus.PACKING_STATION.get(), containerId);
        this.point = point;
        this.player = playerInventory.player;
        this.access = point == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(point.getLevel(), point.getBlockPos());
        this.toStorageSlots = toStorage.getSlots();
        this.toNetworkSlots = toNetwork.getSlots();
        toStorageY = FIRST_LABEL_Y + 10;
        toNetworkLabelY = toStorageY + rows(toStorageSlots) * 18 + 4;
        toNetworkY = toNetworkLabelY + 10;
        statusY = toNetworkY + rows(toNetworkSlots) * 18 + 4;
        inventoryY = statusY + 22;
        imageHeight = inventoryY + 3 * 18 + 4 + 18 + 6;
        addRow(toStorage, toStorageY);
        addRow(toNetwork, toNetworkY);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, hotbarY()));
        }
    }

    private void addRow(IItemHandler handler, int y) {
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
        PackingStationBlockEntity point = playerInventory.player.level().getBlockEntity(pos) instanceof PackingStationBlockEntity found
                ? found : null;
        return new PackingStationMenu(containerId, playerInventory, point, new ItemStackHandler(toStorage), new ItemStackHandler(toNetwork));
    }

    /** What the server writes for {@link #fromNetwork}. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, PackingStationBlockEntity point) {
        buf.writeBlockPos(point.getBlockPos());
        buf.writeVarInt(point.buffer().getSlots());
        buf.writeVarInt(point.outgoing().getSlots());
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
        return toStorageSlots + toNetworkSlots;
    }

    /** Shift click moves nothing: the rows are read-only and nothing goes into them. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.PACKING_STATION.get());
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
