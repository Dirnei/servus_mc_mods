package io.github.dirnei.mc.servus.shuttles.station;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.shuttles.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseEvent;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The inbound dock: an intake buffer that hoppers, ducts and players fill and
 * the warehouse drains into storage by its zone rules. Never indexed.
 */
public class InboundStationBlockEntity extends StationBlockEntity {

    private final ItemSlots buffer = new ItemSlots(ShuttlesStatsManager.INSTANCE.inboundBufferSlots()) {
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
    private final LegacyItemHandler intake = new LegacyItemHandler() {
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

    /** Slots a shuttle is on its way to pick up. */
    private final boolean[] claimed = new boolean[ShuttlesStatsManager.INSTANCE.inboundBufferSlots()];
    private int pullTimer;
    private PutAwayPlan.Reason blockedReason;
    private ItemStack blockedStack = ItemStack.EMPTY;
    private String blockedZone = "";

    public InboundStationBlockEntity(BlockPos pos, BlockState state) {
        this(ServusShuttlesBlockEntities.INBOUND_STATION.get(), pos, state);
    }

    protected InboundStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SideConfig.inbound());
    }

    @Override
    public ItemSlots buffer() {
        return buffer;
    }

    /** The insert only handler exposed as the item capability. */
    public LegacyItemHandler intake() {
        return intake;
    }

    /** The item handler a neighbour on that side sees: the insert only intake on every side that is not Off, so pipes connect to Input and Pull sides. */
    public ResourceHandler<ItemResource> handlerFor(Direction side) {
        if (side == null || sideMode(side) != SideMode.DISABLED) {
            return intake;
        }
        return null;
    }

    /** Takes stacks from the neighbour on every Pull side while the buffer has room, every push interval. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, InboundStationBlockEntity station) {
        if (++station.pullTimer < ShuttlesStatsManager.INSTANCE.pushInterval()) {
            return;
        }
        station.pullTimer = 0;
        for (Direction direction : Direction.values()) {
            if (!station.sideMode(direction).pulls()) {
                continue;
            }
            ItemView neighbour = ItemView.of(level.getCapability(Capabilities.Item.BLOCK, pos.relative(direction), direction.getOpposite()));
            if (neighbour != null) {
                station.pullOneStack(neighbour);
            }
        }
    }

    /** Takes as much of the first stack the neighbour offers as fits. */
    private void pullOneStack(ItemView neighbour) {
        for (int from = 0; from < neighbour.getSlots(); from++) {
            ItemStack offered = neighbour.extractItem(from, 64, true);
            if (offered.isEmpty()) {
                continue;
            }
            int room = offered.getCount() - buffer.insertItem(offered, true).getCount();
            if (room <= 0) {
                continue;
            }
            ItemStack taken = neighbour.extractItem(from, room, false);
            if (!taken.isEmpty()) {
                buffer.insertItem(taken, false);
                return;
            }
        }
    }

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

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new InboundStationMenu(containerId, playerInventory, this);
    }

    private void postIntake() {
        network().ifPresent(network -> network.post(new WarehouseEvent.Intake(worldPosition)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        saveStation(output, tag);
        tag.put("Items", buffer.serializeNBT(ValueIo.registries(this)));
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        loadStation(input, tag);
        Arrays.fill(claimed, false);
        if (tag.contains("Items")) {
            buffer.fillFrom(registries, tag.getCompoundOrEmpty("Items"));
        }
    }
}
