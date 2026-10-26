package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.block.SideConfigData;
import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/** The station's buffer and the player inventory; the state line comes along as a payload. */
public class InboundStationMenu extends AbstractContainerMenu implements LockedMenu, StationNameMenu {

    public static final int BUFFER_Y = 18;
    public static final int STATUS_Y = 40;
    public static final int INVENTORY_Y = 58;
    private static final int COLUMNS = 9;

    private final InboundStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int bufferSlots;
    private final int inventoryStart;
    private final MenuLocks locks;
    private final PayloadSync<StationStatePayload> sync = new PayloadSync<>(5, false, this::state);

    public InboundStationMenu(int containerId, Inventory playerInventory, InboundStationBlockEntity station) {
        super(ServusShuttlesMenus.INBOUND_STATION.get(), containerId);
        this.station = station;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        this.bufferSlots = station.buffer().getSlots();
        for (int slot = 0; slot < bufferSlots; slot++) {
            addSlot(new ResourceHandlerSlot(station.buffer(), station.buffer()::set, slot, 8 + (slot % COLUMNS) * 18, BUFFER_Y + (slot / COLUMNS) * 18));
        }
        inventoryStart = slots.size();
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, INVENTORY_Y);
        addDataSlots(new SideConfigData(station.sideConfig()));
        locks = MenuLocks.of(slots);
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

    public SideMode sideMode(RelativeSide side) {
        return station.sideConfig().mode(side);
    }

    @Override
    public void rename(String typed) {
        if (stillValid(player)) {
            station.rename(typed);
        }
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
        QuickMoves.settle(slot, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.INBOUND_STATION.get());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        sync.tick(player);
        locks.tick(player, containerId, station::network);
    }

    @Override
    public MenuLocks locks() {
        return locks;
    }

    /** The station's state as the client should see it. */
    public StationStatePayload state() {
        String blockedReason = station.blockedReason() == null ? "" : station.blockedReason().name();
        String offline = station.offlineReason();
        return new StationStatePayload(offline.isEmpty(), offline, blockedReason, station.blockedStack().copy(), station.blockedZone(),
                station.isEmpty());
    }
}
