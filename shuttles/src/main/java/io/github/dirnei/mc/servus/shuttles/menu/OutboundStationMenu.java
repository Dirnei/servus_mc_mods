package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.naming.MachineNameMenu;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.block.SideConfigData;
import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.logistics.menu.GhostSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.outbound.ExportListPayload;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Keep row (ghost slots with amounts, some holding exports), buffer row and
 * the player inventory; the state line and the export list come along as payloads.
 */
public class OutboundStationMenu extends AbstractContainerMenu implements LockedMenu, MachineNameMenu {

    public static final int COLUMNS = 9;
    public static final int KEEP_Y = 30;
    public static final int BUFFER_Y = 58;
    public static final int STATUS_Y = 78;
    public static final int INVENTORY_Y = 98;
    public static final int HOTBAR_Y = INVENTORY_Y + PlayerInventorySlots.HOTBAR_OFFSET;
    public static final int IMAGE_HEIGHT = HOTBAR_Y + 18 + 6;

    private final OutboundStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final MenuLocks locks;
    private final int keepSlots;
    private final int bufferStart;
    private final int inventoryStart;
    private final PayloadSync<StationStatePayload> sync = new PayloadSync<>(5, false, this::state);
    private final PayloadSync<ExportListPayload> exportSync = new PayloadSync<>(5, true, this::exports, ExportListPayload::signature);
    /** The exports as the server last sent them, on the client. */
    private ExportListPayload exports = ExportListPayload.EMPTY;

    public OutboundStationMenu(int containerId, Inventory playerInventory, OutboundStationBlockEntity station) {
        super(ServusShuttlesMenus.OUTBOUND_STATION.get(), containerId);
        this.station = station;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        this.keepSlots = station.keep().getSlots();
        for (int slot = 0; slot < keepSlots; slot++) {
            addSlot(new GhostSlot(station::keep, slot, 8 + (slot % COLUMNS) * 18, KEEP_Y + (slot / COLUMNS) * 18));
        }
        bufferStart = slots.size();
        for (int slot = 0; slot < station.buffer().getSlots(); slot++) {
            addSlot(new ResourceHandlerSlot(station.buffer(), station.buffer()::set, slot, 8 + (slot % COLUMNS) * 18, BUFFER_Y + (slot / COLUMNS) * 18));
        }
        inventoryStart = slots.size();
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
        locks = MenuLocks.of(slots);
        addDataSlots(new SideConfigData(station.sideConfig()));
    }

    public static OutboundStationMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof OutboundStationBlockEntity station) {
            return new OutboundStationMenu(containerId, playerInventory, station);
        }
        throw new IllegalStateException("No outbound station at " + pos);
    }

    public OutboundStationBlockEntity station() {
        return station;
    }

    public int keepSlots() {
        return keepSlots;
    }

    public int bufferSlots() {
        return inventoryStart - bufferStart;
    }

    public int inventoryStart() {
        return inventoryStart;
    }

    /** Sets a keep slot and sends the state on the next tick. Server side. */
    public void setKeep(int slot, ItemStack item, int amount) {
        station.setKeep(slot, item, amount);
        sync.force();
    }

    public SideMode sideMode(RelativeSide side) {
        return station.sideConfig().mode(side);
    }

    @Override
    public Optional<NamedMachine> namedMachine() {
        return Optional.of(station);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        RelativeSide side = SideConfigMenus.sideOf(id);
        if (side != null) {
            if (!player.level().isClientSide()) {
                station.cycleSide(side);
            }
            return true;
        }
        return false;
    }

    /**
     * Keep slots take the carried item with its count on a left click and
     * clear on a right click. A slot holding an export ignores left clicks
     * and releases the export on a right click with an empty hand.
     */
    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < keepSlots) {
            if (!player.level().isClientSide()) {
                ItemStack carried = getCarried();
                if (station.isExportSlot(slotId)) {
                    if (button == 1 && carried.isEmpty() && station.releaseExport(slotId)) {
                        exportSync.force();
                    }
                } else {
                    station.setKeep(slotId, button == 1 ? ItemStack.EMPTY : carried, carried.getCount());
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Server side: claims the item's export for an empty keep slot, from the picker. */
    public void claim(int slot, ItemStack item) {
        if (stillValid(player) && slot >= 0 && slot < keepSlots && !item.isEmpty()) {
            station.claimExport(ItemKey.of(item), slot);
            exportSync.force();
        }
    }

    /** Client side: the export list the server sent. */
    public void acceptExports(ExportListPayload payload) {
        exports = payload;
    }

    /** The exports of this station and those it may claim, as the client last heard them. */
    public ExportListPayload exportList() {
        return exports;
    }

    /** The exports of this station and those of its warehouse it may claim, unassigned ones first. Server side. */
    public ExportListPayload exports() {
        Optional<WarehouseNetwork> network = station.network().filter(found -> found.status() == WarehouseNetwork.Status.ONLINE);
        if (network.isEmpty()) {
            return ExportListPayload.EMPTY;
        }
        BlockPos pos = station.getBlockPos();
        List<ExportListPayload.Slot> slots = new ArrayList<>();
        network.get().exportSlotsAt(pos).forEach((slot, export) ->
                slots.add(new ExportListPayload.Slot(slot, export.key().stack(1), export.threshold(), export.state())));
        List<ExportListPayload.Offer> free = new ArrayList<>();
        List<ExportListPayload.Offer> held = new ArrayList<>();
        for (WarehouseNetwork.ExportOffer offer : network.get().exportOffers()) {
            if (offer.holder().isEmpty()) {
                free.add(new ExportListPayload.Offer(offer.key().stack(1), offer.threshold(), Component.empty()));
            } else if (!offer.holder().get().equals(pos)) {
                held.add(new ExportListPayload.Offer(offer.key().stack(1), offer.threshold(), network.get().stationLabel(offer.holder().get())));
            }
        }
        free.addAll(held);
        return new ExportListPayload(slots, free);
    }

    /** Shift click moves between the buffer and the player inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < bufferStart || index >= slots.size()) {
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
                : moveItemStackTo(stack, bufferStart, inventoryStart, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.OUTBOUND_STATION.get());
    }

    @Override
    public MenuLocks locks() {
        return locks;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        locks.tick(player, containerId, station::network);
        sync.tick(player);
        exportSync.tick(player);
    }

    /** The station's state as the client should see it: the shortage rides in the blocked fields. */
    public StationStatePayload state() {
        String offline = station.offlineReason();
        return new StationStatePayload(offline.isEmpty(), offline, station.shortReason(), station.shortStack().copy(), "", station.isEmpty());
    }
}
