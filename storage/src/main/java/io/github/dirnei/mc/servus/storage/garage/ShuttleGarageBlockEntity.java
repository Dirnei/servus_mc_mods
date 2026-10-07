package io.github.dirnei.mc.servus.storage.garage;

import io.github.dirnei.mc.servus.lib.graph.Side;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Keeps shuttles as items, one per slot, and sends them out through its
 * front onto the rail. The live shuttles are data on the network's fleet;
 * this only holds the items and tells the network how many there are.
 */
public class ShuttleGarageBlockEntity extends StorageNodeBlockEntity implements MenuProvider {

    private final ItemStackHandler shuttles = new ItemStackHandler(StorageStatsManager.INSTANCE.garageCapacity()) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ServusStorageItems.SHUTTLE.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            network().ifPresent(network -> network.garageChanged(ShuttleGarageBlockEntity.this));
        }
    };

    public ShuttleGarageBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.SHUTTLE_GARAGE.get(), pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.GARAGE;
    }

    /** Shuttles drive in and out through the front: the rail links there only. */
    @Override
    public boolean acceptsRail(Side side) {
        return side == Conversions.toSide(getBlockState().getValue(ShuttleGarageBlock.FACING));
    }

    /** The shuttle items, one slot per place. */
    public ItemStackHandler shuttles() {
        return shuttles;
    }

    public int capacity() {
        return shuttles.getSlots();
    }

    public int shuttleCount() {
        int count = 0;
        for (int i = 0; i < shuttles.getSlots(); i++) {
            if (!shuttles.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** How many of this garage's shuttles are out on a job right now. */
    public int outCount() {
        return network().map(network -> network.garageOut(worldPosition)).orElse(0);
    }

    /** Puts a shuttle into the first free slot. @return false when full */
    public boolean addShuttle() {
        for (int i = 0; i < shuttles.getSlots(); i++) {
            if (shuttles.getStackInSlot(i).isEmpty()) {
                shuttles.setStackInSlot(i, new ItemStack(ServusStorageItems.SHUTTLE.get()));
                return true;
            }
        }
        return false;
    }

    /** Takes the last shuttle item out. @return false when empty */
    public boolean takeShuttle() {
        for (int i = shuttles.getSlots() - 1; i >= 0; i--) {
            if (!shuttles.getStackInSlot(i).isEmpty()) {
                shuttles.setStackInSlot(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    /** Drops every shuttle item at the garage, for when the block is destroyed anyway. */
    public void dropShuttles(Level level, BlockPos pos) {
        for (int i = 0; i < shuttles.getSlots(); i++) {
            ItemStack stack = shuttles.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                shuttles.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ShuttleGarageMenu(containerId, playerInventory, this);
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ShuttleItems", shuttles.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ShuttleItems")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("ShuttleItems"));
            for (int i = 0; i < shuttles.getSlots(); i++) {
                shuttles.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        } else if (tag.contains("Shuttles")) {
            // Older saves kept a plain count.
            int count = Math.min(tag.getInt("Shuttles"), shuttles.getSlots());
            for (int i = 0; i < count; i++) {
                shuttles.setStackInSlot(i, new ItemStack(ServusStorageItems.SHUTTLE.get()));
            }
        }
    }
}
