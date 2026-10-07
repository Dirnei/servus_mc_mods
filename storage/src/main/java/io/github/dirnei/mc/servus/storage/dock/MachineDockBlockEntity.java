package io.github.dirnei.mc.servus.storage.dock;

import io.github.dirnei.mc.servus.lib.graph.Side;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.production.ProductionProvider;
import io.github.dirnei.mc.servus.lib.production.ServusLibCapabilities;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.MachineDockMenu;
import io.github.dirnei.mc.servus.storage.network.DeliveryTarget;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * An Inbound Station that also feeds the block it faces: the inherited buffer
 * collects what the machine pushes out, the feed buffer is pushed into the
 * machine's inventory. Deliveries from the warehouse land in the feed buffer.
 */
public class MachineDockBlockEntity extends InboundStationBlockEntity implements DeliveryTarget {

    private final ItemStackHandler feed = new ItemStackHandler(StorageStatsManager.INSTANCE.inboundBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int feedTimer;

    public MachineDockBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.MACHINE_DOCK.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MachineDockBlock.FACING);
    }

    /** The block this dock serves. */
    public BlockPos facingPos() {
        return worldPosition.relative(facing());
    }

    /** Rails never dock onto the front: the machine sits there. */
    @Override
    public boolean acceptsRail(Side side) {
        return side != Conversions.toSide(facing());
    }

    /** Items waiting to be pushed into the machine. */
    public ItemStackHandler feed() {
        return feed;
    }

    @Override
    public ItemStackHandler deliveries() {
        return feed;
    }

    public boolean feedEmpty() {
        for (int i = 0; i < feed.getSlots(); i++) {
            if (!feed.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The production view of the block in front, or null when it has none. */
    public ProductionProvider producer() {
        if (level == null) {
            return null;
        }
        return level.getCapability(ServusLibCapabilities.PRODUCTION, facingPos(), facing().getOpposite());
    }

    /** Pushes the feed buffer into the machine every feed interval. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineDockBlockEntity dock) {
        if (++dock.feedTimer < StorageStatsManager.INSTANCE.dockFeedInterval()) {
            return;
        }
        dock.feedTimer = 0;
        if (dock.feedEmpty()) {
            return;
        }
        IItemHandler machine = level.getCapability(Capabilities.ItemHandler.BLOCK, dock.facingPos(), dock.facing().getOpposite());
        if (machine == null) {
            return;
        }
        for (int i = 0; i < dock.feed.getSlots(); i++) {
            ItemStack stack = dock.feed.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack left = ItemHandlerHelper.insertItem(machine, stack.copy(), false);
            if (left.getCount() != stack.getCount()) {
                dock.feed.setStackInSlot(i, left);
            }
        }
    }

    /** Drops both buffers at the dock. */
    public void dropAll(Level level, BlockPos pos) {
        dropContents(level, pos);
        for (int i = 0; i < feed.getSlots(); i++) {
            ItemStack stack = feed.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                feed.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineDockMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Feed", feed.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Feed")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Feed"));
            for (int i = 0; i < feed.getSlots(); i++) {
                feed.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
    }
}
