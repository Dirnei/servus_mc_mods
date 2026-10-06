package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.station.DispatchStationBlockEntity;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload.ZoneRow;
import java.util.ArrayList;
import java.util.List;
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

/** The station's buffer and the player inventory, plus a read only overview of the warehouse's zones. */
public class DispatchStationMenu extends AbstractContainerMenu {

    public static final int BUFFER_Y = 18;
    public static final int INVENTORY_Y = 118;
    public static final int HOTBAR_Y = 176;
    private static final int COLUMNS = 9;
    private static final int SYNC_INTERVAL = 5;

    private final DispatchStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int bufferSlots;
    private final int inventoryStart;
    private int syncTimer;
    private int lastSignature = Integer.MIN_VALUE;

    public DispatchStationMenu(int containerId, Inventory playerInventory, DispatchStationBlockEntity station) {
        super(ServusStorageMenus.DISPATCH_STATION.get(), containerId);
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

    public static DispatchStationMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof DispatchStationBlockEntity station) {
            return new DispatchStationMenu(containerId, playerInventory, station);
        }
        throw new IllegalStateException("No dispatch station at " + pos);
    }

    public DispatchStationBlockEntity station() {
        return station;
    }

    /** Index of the first player inventory slot. */
    public int inventoryStart() {
        return inventoryStart;
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
        return stillValid(access, player, ServusStorageBlocks.DISPATCH_STATION.get());
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
        ZoneRulesSyncPayload payload = state();
        int signature = payload.hashCode();
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    /** The zones and the station's state as the client should see them. */
    public ZoneRulesSyncPayload state() {
        Optional<WarehouseNetwork> network = station.network();
        String blockedReason = station.blockedReason() == null ? "" : station.blockedReason().name();
        ItemStack blockedStack = station.blockedStack().copy();
        if (network.isEmpty()) {
            return new ZoneRulesSyncPayload(false, "not_connected", List.of(), blockedReason, blockedStack, station.blockedZone(), station.isEmpty());
        }
        WarehouseNetwork warehouse = network.get();
        return switch (warehouse.status()) {
            case NO_CONTROLLER -> new ZoneRulesSyncPayload(false, "no_controller", List.of(), blockedReason, blockedStack, station.blockedZone(), station.isEmpty());
            case MULTIPLE_CONTROLLERS -> new ZoneRulesSyncPayload(false, "multiple_controllers", List.of(), blockedReason, blockedStack, station.blockedZone(), station.isEmpty());
            case ONLINE -> {
                List<ZoneRow> rows = new ArrayList<>();
                for (WarehouseNetwork.ZoneSummary zone : warehouse.zoneSummaries()) {
                    rows.add(new ZoneRow(zone.key(), zone.display(), zone.blocks(), zone.items(), zone.tags()));
                }
                yield new ZoneRulesSyncPayload(true, "", rows, blockedReason, blockedStack, station.blockedZone(), station.isEmpty());
            }
        };
    }
}
