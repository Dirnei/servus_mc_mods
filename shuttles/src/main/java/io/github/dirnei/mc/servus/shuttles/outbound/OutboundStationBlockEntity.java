package io.github.dirnei.mc.servus.shuttles.outbound;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.shuttles.lock.LockHolder;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.network.DeliveryTarget;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseEvent;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.station.StationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The outbound dock: the warehouse keeps the buffer stocked at the keep
 * amounts, Push sides hand the buffer to neighbours, Output sides let
 * neighbours take from it. The machine it serves stands behind it. Deliveries
 * for work orders land here as well. Keep slots are configuration and never drop.
 */
public class OutboundStationBlockEntity extends StationBlockEntity implements DeliveryTarget, LockHolder {

    public static final int KEEP_LIMIT = 4096;

    private final LockedSlots buffer = new LockedSlots(ShuttlesStatsManager.INSTANCE.inboundBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            network().ifPresent(network -> network.post(new WarehouseEvent.OutputChanged(worldPosition)));
        }

        @Override
        protected void onLocksChanged() {
            setChanged();
        }
    };

    /** Ghost stacks whose count is the amount to keep. */
    private final ItemSlots keep = new ItemSlots(ShuttlesStatsManager.INSTANCE.inboundBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return KEEP_LIMIT;
        }
    };

    /** The outside view on Output sides: takes only, never inserts. */
    private final LegacyItemHandler extractOnly = new LegacyItemHandler() {
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
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return buffer.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return buffer.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private int pushTimer;
    private ItemStack shortStack = ItemStack.EMPTY;
    private String shortReason = "";

    public OutboundStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.OUTBOUND_STATION.get(), pos, state, SideConfig.outbound());
        sideConfig().set(RelativeSide.BACK, SideMode.PUSH);
    }

    /** The block behind the station, the machine it serves when it is one. */
    public BlockPos machinePos() {
        return worldPosition.relative(facing().getOpposite());
    }

    @Override
    public LockedSlots buffer() {
        return buffer;
    }

    @Override
    public List<LockedSlots> lockedSlots() {
        return List.of(buffer);
    }

    public ItemSlots keep() {
        return keep;
    }

    @Override
    public LockedSlots deliveries() {
        return buffer;
    }

    /** The item handler a neighbour on that side sees: the extract only view on every side that is not Off, so pipes connect to Push and Output sides. */
    public ResourceHandler<ItemResource> handlerFor(Direction side) {
        if (side == null || sideMode(side) != SideMode.DISABLED) {
            return extractOnly;
        }
        return null;
    }

    /** How many of the key sit in the buffer. */
    public int bufferCount(ItemKey key) {
        return countOf(buffer, key);
    }

    /** Sets a keep slot; an empty item or an amount below one clears it. */
    public void setKeep(int slot, ItemStack item, int amount) {
        if (slot < 0 || slot >= keep.getSlots()) {
            return;
        }
        if (item.isEmpty() || amount < 1) {
            keep.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            keep.setStackInSlot(slot, item.copyWithCount(Math.min(KEEP_LIMIT, amount)));
        }
    }

    public ItemStack shortStack() {
        return shortStack;
    }

    public String shortReason() {
        return shortReason;
    }

    /** Records what the last check could not fill and sets the activity lamp. */
    public void setRestockState(ItemStack shortStack, String shortReason, boolean pending) {
        this.shortStack = shortStack.copy();
        this.shortReason = shortReason == null ? "" : shortReason;
        setActivity(!this.shortStack.isEmpty() ? StorageNodeBlocks.Activity.BLOCKED
                : pending ? StorageNodeBlocks.Activity.WORKING : StorageNodeBlocks.Activity.IDLE);
    }

    /** Pushes the buffer into every Push side's neighbour every push interval. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, OutboundStationBlockEntity station) {
        if (++station.pushTimer < ShuttlesStatsManager.INSTANCE.pushInterval()) {
            return;
        }
        station.pushTimer = 0;
        if (station.isEmpty()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (!station.sideMode(direction).pushes()) {
                continue;
            }
            ItemView neighbour = ItemView.of(level.getCapability(Capabilities.Item.BLOCK, pos.relative(direction), direction.getOpposite()));
            if (neighbour != null) {
                station.pushInto(neighbour);
            }
        }
    }

    private void pushInto(ItemView neighbour) {
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (stack.isEmpty() || buffer.isLocked(i)) {
                continue;
            }
            ItemStack left = neighbour.insertItem(stack.copy(), false);
            if (left.getCount() != stack.getCount()) {
                buffer.setStackInSlot(i, left);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OutboundStationMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        saveStation(output, tag);
        tag.put("Items", buffer.serializeNBT(registries));
        tag.put("Keep", keep.serializeNBT(registries));
        tag.put("Locks", buffer.saveLocks(registries));
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        loadStation(input, tag);
        if (tag.contains("Items")) {
            buffer.fillFrom(registries, tag.getCompoundOrEmpty("Items"), keepSized());
            buffer.loadLocks(registries, tag.getListOrEmpty("Locks"));
        }
        if (tag.contains("Keep")) {
            keep.fillFrom(registries, tag.getCompoundOrEmpty("Keep"), keepSized());
        }
    }

    /** Slots that read back keep amounts above a stack. */
    private static ItemSlots keepSized() {
        return new ItemSlots() {
            @Override
            public int getSlotLimit(int slot) {
                return KEEP_LIMIT;
            }
        };
    }
}
