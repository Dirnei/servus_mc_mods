package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.dock.MachineDockBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/** The dock's feed row, its collect row and the player inventory; the state line comes along as a payload. */
public class MachineDockMenu extends AbstractContainerMenu {

    public static final int FEED_Y = 18;
    public static final int COLLECT_Y = 40;
    public static final int INVENTORY_Y = 84;
    public static final int HOTBAR_Y = 142;
    private static final int COLUMNS = 9;
    private static final int SYNC_INTERVAL = 5;

    private final MachineDockBlockEntity dock;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int feedSlots;
    private final int collectStart;
    private final int inventoryStart;
    private int syncTimer;
    private int lastSignature = Integer.MIN_VALUE;

    public MachineDockMenu(int containerId, Inventory playerInventory, MachineDockBlockEntity dock) {
        super(ServusStorageMenus.MACHINE_DOCK.get(), containerId);
        this.dock = dock;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(dock.getLevel(), dock.getBlockPos());
        this.feedSlots = dock.feed().getSlots();
        for (int slot = 0; slot < feedSlots; slot++) {
            addSlot(new SlotItemHandler(dock.feed(), slot, 8 + (slot % COLUMNS) * 18, FEED_Y + (slot / COLUMNS) * 18));
        }
        collectStart = slots.size();
        for (int slot = 0; slot < dock.buffer().getSlots(); slot++) {
            addSlot(new SlotItemHandler(dock.buffer(), slot, 8 + (slot % COLUMNS) * 18, COLLECT_Y + (slot / COLUMNS) * 18));
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
    }

    public static MachineDockMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof MachineDockBlockEntity dock) {
            return new MachineDockMenu(containerId, playerInventory, dock);
        }
        throw new IllegalStateException("No machine dock at " + pos);
    }

    public MachineDockBlockEntity dock() {
        return dock;
    }

    public int inventoryStart() {
        return inventoryStart;
    }

    /** Shift click: inventory stacks go to the feed buffer, either dock row goes back to the inventory. */
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
        boolean moved = index < inventoryStart
                ? moveItemStackTo(stack, inventoryStart, slots.size(), true)
                : moveItemStackTo(stack, 0, feedSlots, false);
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
        return stillValid(access, player, ServusStorageBlocks.MACHINE_DOCK.get());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (++syncTimer < SYNC_INTERVAL) {
            return;
        }
        syncTimer = 0;
        StationStatePayload payload = InboundStationMenu.stateOf(dock);
        int signature = payload.hashCode();
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }
}
