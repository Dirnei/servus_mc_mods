package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.block.SideConfigData;
import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/** Keep row (ghost slots with amounts), buffer row and the player inventory; the state line comes along as a payload. */
public class OutboundStationMenu extends AbstractContainerMenu {

    public static final int KEEP_Y = 18;
    public static final int BUFFER_Y = 44;
    public static final int STATUS_Y = 66;
    public static final int INVENTORY_Y = 84;
    private static final int COLUMNS = 9;

    private final OutboundStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int keepSlots;
    private final int bufferStart;
    private final int inventoryStart;
    private final PayloadSync<StationStatePayload> sync = new PayloadSync<>(5, false, this::state);

    public OutboundStationMenu(int containerId, Inventory playerInventory, OutboundStationBlockEntity station) {
        super(ServusStorageMenus.OUTBOUND_STATION.get(), containerId);
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

    /** Keep slots take the carried item with its count on a left click and clear on a right click. */
    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < keepSlots) {
            if (!player.level().isClientSide()) {
                ItemStack carried = getCarried();
                station.setKeep(slotId, button == 1 ? ItemStack.EMPTY : carried, carried.getCount());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
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
        return stillValid(access, player, ServusStorageBlocks.OUTBOUND_STATION.get());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        sync.tick(player);
    }

    /** The station's state as the client should see it: the shortage rides in the blocked fields. */
    public StationStatePayload state() {
        String offline = InboundStationMenu.offlineReason(station.network());
        return new StationStatePayload(offline.isEmpty(), offline, station.shortReason(), station.shortStack().copy(), "", station.isEmpty());
    }
}
