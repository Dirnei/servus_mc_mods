package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.storage.network.WarehouseEvent;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The inbound dock: an intake buffer that hoppers, ducts and players fill and
 * the warehouse drains into storage by its zone rules. Never indexed.
 */
public class InboundStationBlockEntity extends StorageNodeBlockEntity implements MenuProvider, SidedBlockEntity {

    private final ItemStackHandler buffer = new ItemStackHandler(StorageStatsManager.INSTANCE.inboundBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            if (getStackInSlot(slot).isEmpty()) {
                claimed[slot] = false;
            }
            setChanged();
            postIntake();
        }
    };

    /** The outside view: inserts only, so nothing drains the dock. */
    private final IItemHandler intake = new IItemHandler() {
        @Override
        public int getSlots() {
            return buffer.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return buffer.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return buffer.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return buffer.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return buffer.isItemValid(slot, stack);
        }
    };

    private final boolean[] claimed = new boolean[StorageStatsManager.INSTANCE.inboundBufferSlots()];
    private final SideConfig sides = SideConfig.inbound();
    private int pullTimer;
    private PutAwayPlan.Reason blockedReason;
    private ItemStack blockedStack = ItemStack.EMPTY;
    private String blockedZone = "";

    public InboundStationBlockEntity(BlockPos pos, BlockState state) {
        this(ServusStorageBlockEntities.INBOUND_STATION.get(), pos, state);
    }

    protected InboundStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // The front is where the player stands: never an item side.
        sides.lock(RelativeSide.FRONT, SideMode.DISABLED);
    }

    /** Re-evaluates which rails link to this node, for when its facing changed. */
    public void refreshRails() {
        refreshNetworkConnections();
    }

    @Override
    public NodeKind kind() {
        return NodeKind.STATION;
    }

    /** The buffer itself, for the warehouse and the GUI. */
    public ItemStackHandler buffer() {
        return buffer;
    }

    /** The insert only handler exposed as the item capability. */
    public IItemHandler intake() {
        return intake;
    }

    // Sides

    @Override
    public SideConfig sideConfig() {
        return sides;
    }

    @Override
    public Direction facing() {
        return getBlockState().hasProperty(InboundStationBlock.FACING) ? getBlockState().getValue(InboundStationBlock.FACING) : Direction.NORTH;
    }

    /** The item handler a neighbour on that side sees: the intake where insertion is allowed, nothing elsewhere. */
    public IItemHandler handlerFor(Direction side) {
        if (side == null || sideMode(side).allowsInsert()) {
            return intake;
        }
        return null;
    }

    @Override
    public SideMode cycleSide(RelativeSide side) {
        SideMode mode = sides.cycle(side);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        return mode;
    }

    /** Takes stacks from the neighbour on every Pull side while the buffer has room, every push interval. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, InboundStationBlockEntity station) {
        if (++station.pullTimer < StorageStatsManager.INSTANCE.pushInterval()) {
            return;
        }
        station.pullTimer = 0;
        for (Direction direction : Direction.values()) {
            if (!station.sideMode(direction).pulls()) {
                continue;
            }
            IItemHandler neighbour = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(direction), direction.getOpposite());
            if (neighbour == null) {
                continue;
            }
            for (int from = 0; from < neighbour.getSlots(); from++) {
                ItemStack offered = neighbour.extractItem(from, 64, true);
                if (offered.isEmpty()) {
                    continue;
                }
                int room = offered.getCount() - ItemHandlerHelper.insertItem(station.buffer, offered, true).getCount();
                if (room <= 0) {
                    continue;
                }
                ItemStack taken = neighbour.extractItem(from, room, false);
                if (!taken.isEmpty()) {
                    ItemHandlerHelper.insertItem(station.buffer, taken, false);
                    break;
                }
            }
        }
    }

    public boolean isEmpty() {
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // Claims: a slot a shuttle is on its way to pick up

    public boolean isClaimed(int slot) {
        return slot >= 0 && slot < claimed.length && claimed[slot];
    }

    public void claim(int slot) {
        if (slot >= 0 && slot < claimed.length) {
            claimed[slot] = true;
        }
    }

    public void release(int slot) {
        if (slot >= 0 && slot < claimed.length) {
            claimed[slot] = false;
        }
    }

    // Backed up state

    public PutAwayPlan.Reason blockedReason() {
        return blockedReason;
    }

    public ItemStack blockedStack() {
        return blockedStack;
    }

    public String blockedZone() {
        return blockedZone;
    }

    /** Records the first stack that has no room and turns the activity lamp red. */
    public void setBlocked(PutAwayPlan.Reason reason, ItemKey key, String zoneDisplay) {
        blockedReason = reason;
        blockedStack = key.stack(1);
        blockedZone = zoneDisplay == null ? "" : zoneDisplay;
        setActivity(StorageNodeBlocks.Activity.BLOCKED);
    }

    /** Nothing is stuck: the lamp shows orange while stacks are being put away, green when the buffer is empty. */
    public void clearBlocked() {
        blockedReason = null;
        blockedStack = ItemStack.EMPTY;
        blockedZone = "";
        setActivity(isEmpty() ? StorageNodeBlocks.Activity.IDLE : StorageNodeBlocks.Activity.WORKING);
    }

    private void setActivity(StorageNodeBlocks.Activity activity) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(StorageNodeBlocks.ACTIVITY) && state.getValue(StorageNodeBlocks.ACTIVITY) != activity) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ACTIVITY, activity), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new InboundStationMenu(containerId, playerInventory, this);
    }

    private void postIntake() {
        network().ifPresent(network -> network.post(new WarehouseEvent.Intake(worldPosition)));
    }

    /** Drops every stack in the buffer at the station. */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                buffer.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", buffer.serializeNBT(registries));
        tag.putByteArray("Sides", sides.toBytes());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putByteArray("Sides", sides.toBytes());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Arrays.fill(claimed, false);
        if (tag.contains("Items")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Items"));
            for (int i = 0; i < buffer.getSlots(); i++) {
                buffer.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        if (tag.contains("Sides")) {
            sides.fromBytes(tag.getByteArray("Sides"));
        }
    }
}
