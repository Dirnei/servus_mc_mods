package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Chest style menu for a whole rack section: the slots of every bay, bay
 * after bay from left to right, in rows of nine above the player inventory.
 * Each bay keeps storing its own slots.
 */
public class HeavyRackMenu extends AbstractContainerMenu {

    private static final int COLUMNS = 9;

    private final Level level;
    private final BlockPos clicked;
    private final ContainerLevelAccess access;
    /** The section's bays from left to right. */
    private final List<BlockPos> bays;
    /** The block entity of each bay when the menu opened; entries may be null on the client. */
    private final List<HeavyRackBlockEntity> racks;
    private final int rackSlots;

    private HeavyRackMenu(int containerId, Inventory playerInventory, BlockPos clicked, List<BlockPos> bays,
                          List<HeavyRackBlockEntity> racks, List<ItemSlots> handlers) {
        super(ServusStorageMenus.HEAVY_RACK.get(), containerId);
        this.level = playerInventory.player.level();
        this.clicked = clicked;
        this.access = ContainerLevelAccess.create(level, clicked);
        this.bays = List.copyOf(bays);
        this.racks = new ArrayList<>(racks);
        int index = 0;
        for (ItemSlots handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++, index++) {
                addSlot(new ResourceHandlerSlot(handler, handler::set, slot, 8 + (index % COLUMNS) * 18, 18 + (index / COLUMNS) * 18));
            }
        }
        this.rackSlots = index;
        PlayerInventorySlots.add(this::addSlot, playerInventory, 8, 18 + rows() * 18 + 14);
    }

    /** Server side: the menu of the section the clicked bay belongs to. */
    public static HeavyRackMenu forSection(int containerId, Inventory playerInventory, HeavyRackBlockEntity clickedRack) {
        Level level = clickedRack.getLevel();
        BlockPos clicked = clickedRack.getBlockPos();
        List<BlockPos> bays = new ArrayList<>();
        List<HeavyRackBlockEntity> racks = new ArrayList<>();
        List<ItemSlots> handlers = new ArrayList<>();
        for (BlockPos bay : sectionOf(level, clicked)) {
            if (level.getBlockEntity(bay) instanceof HeavyRackBlockEntity rack) {
                bays.add(bay);
                racks.add(rack);
                handlers.add(rack.items());
            }
        }
        if (bays.isEmpty()) {
            bays.add(clicked);
            racks.add(clickedRack);
            handlers.add(clickedRack.items());
        }
        return new HeavyRackMenu(containerId, playerInventory, clicked, bays, racks, handlers);
    }

    private static List<BlockPos> sectionOf(Level level, BlockPos clicked) {
        return level == null ? List.of(clicked) : HeavyRackBlock.section(level, clicked);
    }

    /**
     * Writes what the client needs to build the same layout: the clicked bay,
     * then each bay of the section with its slot count.
     */
    public static void writeSection(RegistryFriendlyByteBuf buf, Level level, BlockPos clicked) {
        buf.writeBlockPos(clicked);
        List<BlockPos> bays = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        for (BlockPos bay : sectionOf(level, clicked)) {
            if (level.getBlockEntity(bay) instanceof HeavyRackBlockEntity rack) {
                bays.add(bay);
                sizes.add(rack.items().getSlots());
            }
        }
        if (bays.isEmpty() && level.getBlockEntity(clicked) instanceof HeavyRackBlockEntity rack) {
            bays.add(clicked);
            sizes.add(rack.items().getSlots());
        }
        buf.writeVarInt(bays.size());
        for (int i = 0; i < bays.size(); i++) {
            buf.writeBlockPos(bays.get(i));
            buf.writeVarInt(sizes.get(i));
        }
    }

    /**
     * Client side: rebuilds the server's layout. A bay the client cannot find,
     * or whose slot count differs, gets an empty stand in of the server's size;
     * container sync fills the slots either way.
     */
    public static HeavyRackMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        Level level = playerInventory.player.level();
        BlockPos clicked = buf.readBlockPos();
        int count = buf.readVarInt();
        List<BlockPos> bays = new ArrayList<>(count);
        List<HeavyRackBlockEntity> racks = new ArrayList<>(count);
        List<ItemSlots> handlers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos bay = buf.readBlockPos();
            int size = buf.readVarInt();
            bays.add(bay);
            if (level.getBlockEntity(bay) instanceof HeavyRackBlockEntity rack && rack.items().getSlots() == size) {
                racks.add(rack);
                handlers.add(rack.items());
            } else {
                racks.add(null);
                handlers.add(new ItemSlots(size));
            }
        }
        return new HeavyRackMenu(containerId, playerInventory, clicked, bays, racks, handlers);
    }

    /** The section's bays from left to right as the menu shows them. */
    public List<BlockPos> bays() {
        return bays;
    }

    /** Slots of all bays together; the player inventory follows them. */
    public int rackSlots() {
        return rackSlots;
    }

    /** Rows of rack slots, at least one. */
    public int rows() {
        return Math.max(1, (rackSlots + COLUMNS - 1) / COLUMNS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < rackSlots
                ? moveItemStackTo(stack, rackSlots, slots.size(), true)
                : moveItemStackTo(stack, 0, rackSlots, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        return original;
    }

    /**
     * Valid while the player is in range of the clicked bay, every bay is still
     * the same rack and the section has not changed.
     */
    @Override
    public boolean stillValid(Player player) {
        if (!stillValid(access, player, ServusStorageBlocks.HEAVY_RACK.get())) {
            return false;
        }
        for (int i = 0; i < bays.size(); i++) {
            HeavyRackBlockEntity rack = racks.get(i);
            if (rack == null || rack.isRemoved() || level.getBlockEntity(bays.get(i)) != rack) {
                return false;
            }
        }
        return HeavyRackBlock.section(level, clicked).equals(bays);
    }
}
