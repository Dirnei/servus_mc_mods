package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import java.util.Optional;
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

/** The station's buffer and the player inventory; the state line comes along as a payload. */
public class InboundStationMenu extends AbstractContainerMenu {

    public static final int BUFFER_Y = 18;
    public static final int SIDES_Y = 40;
    public static final int STATUS_Y = 58;
    public static final int INVENTORY_Y = 80;
    public static final int HOTBAR_Y = 138;
    private static final int COLUMNS = 9;
    private static final int SYNC_INTERVAL = 5;

    private final InboundStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int bufferSlots;
    private final int inventoryStart;
    private int syncTimer;
    private int lastSignature = Integer.MIN_VALUE;

    public InboundStationMenu(int containerId, Inventory playerInventory, InboundStationBlockEntity station) {
        super(ServusStorageMenus.INBOUND_STATION.get(), containerId);
        this.station = station;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        this.bufferSlots = station.buffer().getSlots();
        for (int slot = 0; slot < bufferSlots; slot++) {
            addSlot(new SlotItemHandler(station.buffer(), slot, 8 + (slot % COLUMNS) * 18, BUFFER_Y + (slot / COLUMNS) * 18));
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

    public static InboundStationMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof InboundStationBlockEntity station) {
            return new InboundStationMenu(containerId, playerInventory, station);
        }
        throw new IllegalStateException("No inbound station at " + pos);
    }

    public InboundStationBlockEntity station() {
        return station;
    }

    /** Index of the first player inventory slot. */
    public int inventoryStart() {
        return inventoryStart;
    }

    /** Cycles a side's mode from the GUI. */
    public void cycleSide(int direction) {
        if (direction >= 0 && direction < net.minecraft.core.Direction.values().length) {
            station.cycleSide(net.minecraft.core.Direction.values()[direction]);
        }
        lastSignature = Integer.MIN_VALUE;
        syncTimer = SYNC_INTERVAL;
    }

    /** Shift click moves between the buffer and the player inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < bufferSlots
                ? moveItemStackTo(stack, inventoryStart, slots.size(), true)
                : moveItemStackTo(stack, 0, bufferSlots, false);
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
        return stillValid(access, player, ServusStorageBlocks.INBOUND_STATION.get());
    }

    // Sync

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
        StationStatePayload payload = state();
        int signature = payload.hashCode();
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    /** The station's state as the client should see it. */
    public StationStatePayload state() {
        return stateOf(station);
    }

    /** The state line of any station like block entity, docks included. */
    public static StationStatePayload stateOf(InboundStationBlockEntity station) {
        Optional<WarehouseNetwork> network = station.network();
        String blockedReason = station.blockedReason() == null ? "" : station.blockedReason().name();
        ItemStack blockedStack = station.blockedStack().copy();
        String offline = network.isEmpty() ? "not_connected" : switch (network.get().status()) {
            case NO_CONTROLLER -> "no_controller";
            case MULTIPLE_CONTROLLERS -> "multiple_controllers";
            case ONLINE -> "";
        };
        return new StationStatePayload(offline.isEmpty(), offline, blockedReason, blockedStack, station.blockedZone(), station.isEmpty(),
                station.sides().symbols());
    }
}
