package io.github.dirnei.mc.servus.shuttles.station;

import io.github.dirnei.mc.servus.foundation.data.packet.Grant;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketEndpoint;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.lock.LockHolder;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.PutAwaySource;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseEvent;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.packing.SlotGrant;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The inbound dock: an intake buffer that hoppers, ducts and players fill and
 * the warehouse drains into storage by its zone rules. Never indexed. A slot
 * whose put-away no shuttle has loaded yet takes more of its own item. As a
 * {@link PacketEndpoint} it answers an Item Pipe's offer with one empty slot
 * reserved for the grant; that slot is put away once the grant has ended.
 */
public class InboundStationBlockEntity extends StationBlockEntity implements LockHolder, PutAwaySource, PacketEndpoint {

    public static final String REFUSE_OFFLINE = "offline";
    public static final String REFUSE_NO_STORAGE = "no_storage";
    public static final String REFUSE_FULL = "to_storage_full";

    private final LockedSlots buffer = new LockedSlots(ShuttlesStatsManager.INSTANCE.inboundBufferSlots(), LockedSlots.Mode.TOP_UP) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            postIntake();
        }

        @Override
        protected void onLocksChanged() {
            setChanged();
            postIntake();
        }
    };

    /**
     * The outside view: inserts only, so nothing drains the dock, and no more
     * of an item than its rule's cap leaves room for. What the open
     * transactions already accepted counts towards the cap.
     */
    private final LegacyItemHandler intake = new LegacyItemHandler() {
        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return super.insert(index, resource, amount, transaction);
            }
            int pending = pendingInserts(resource);
            int room = Math.max(0, capRoom(ItemKey.of(resource.toStack()), pending + amount) - pending);
            if (room < amount) {
                reportCapRefused(ItemKey.of(resource.toStack()));
            }
            return room <= 0 ? 0 : super.insert(index, resource, room, transaction);
        }

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

    private final BufferGrants grants = new BufferGrants(buffer);
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
    public LockedSlots putAwayBuffer() {
        return buffer;
    }

    @Override
    public LockedSlots buffer() {
        return buffer;
    }

    @Override
    public List<LockedSlots> lockedSlots() {
        return List.of(buffer);
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

    /** The offer answering side a duct on that side sees: every side that is not Off. */
    public PacketEndpoint endpointFor(Direction side) {
        return side != null && sideMode(side) != SideMode.DISABLED ? this : null;
    }

    /**
     * Answers an offer into the buffer with the slots {@link #planGrant}
     * reserves, locked IN for the grant; refused while the warehouse is
     * offline, when no storage would take the item or nothing can be reserved.
     */
    @Override
    public Grant offer(ItemStack like, int amount) {
        Optional<WarehouseNetwork> network = network().filter(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE);
        if (!(level instanceof ServerLevel server) || network.isEmpty() || like.isEmpty() || amount < 1) {
            return Grant.refused(REFUSE_OFFLINE);
        }
        ItemKey key = ItemKey.of(like);
        if (network.get().putAwayTarget(key.stack(1), pos -> true).isEmpty()) {
            return Grant.refused(REFUSE_NO_STORAGE);
        }
        List<SlotGrant.Part> parts = planGrant(network.get(), key, amount);
        if (parts.isEmpty()) {
            return Grant.refused(REFUSE_FULL);
        }
        return grants.open(this, key, parts, network.get().nextJobId(), server.getGameTime());
    }

    /** The slots a grant reserves: one empty slot for at most a stack, no more than the item's cap leaves room for. */
    protected List<SlotGrant.Part> planGrant(WarehouseNetwork network, ItemKey key, int amount) {
        int stackSize = key.stack(1).getMaxStackSize();
        List<Integer> free = grants.freeSlots();
        if (free.isEmpty()) {
            return List.of();
        }
        return SlotGrant.one(free, stackSize, amount, network.capRoom(key, Math.min(amount, stackSize)));
    }

    @Override
    public ItemStack receive(Grant grant, ItemStack stack) {
        return grants.receive(grant, stack);
    }

    /** Frees the grant's slots, which the warehouse then puts away. */
    @Override
    public void close(Grant grant) {
        grants.close(grant);
    }

    /** Whether the lock with that id belongs to an open grant of this station. */
    public boolean holdsGrant(int lockId) {
        return grants.holds(lockId);
    }

    /** Whether the lock with that id belongs to an open grant an Item Pipe fills. */
    public boolean holdsPipeGrant(int lockId) {
        return grantsFromPipes() && grants.holds(lockId);
    }

    /** Whether the grants of this station come from Item Pipes. */
    protected boolean grantsFromPipes() {
        return true;
    }

    /** Empty buffer slots no lock holds. */
    protected List<Integer> freeBufferSlots() {
        return grants.freeSlots();
    }

    @Override
    public void setRemoved() {
        grants.closeAll();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        grants.closeAll();
        super.onChunkUnloaded();
    }

    /** Takes stacks from the neighbour on every Pull side while the buffer has room, every push interval. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, InboundStationBlockEntity station) {
        station.grants.tick(level.getGameTime());
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

    /**
     * Takes as much of the first stack the neighbour offers as the unlocked
     * slots and the item's cap take. Taking and storing happen in one transaction, so an item
     * the buffer does not take stays with the neighbour.
     */
    private void pullOneStack(ItemView neighbour) {
        for (int from = 0; from < neighbour.getSlots(); from++) {
            ItemStack offered = neighbour.extractItem(from, 64, true);
            if (offered.isEmpty()) {
                continue;
            }
            int bufferRoom = offered.getCount() - buffer.insertItemStacked(offered, true).getCount();
            ItemKey key = ItemKey.of(offered);
            int room = capRoom(key, bufferRoom);
            if (room < bufferRoom) {
                reportCapRefused(key);
            }
            if (room <= 0) {
                continue;
            }
            try (Transaction transaction = Transfer.open()) {
                ItemStack taken = neighbour.extractItem(from, room, false);
                if (!taken.isEmpty() && buffer.insertItemStacked(taken, false).isEmpty()) {
                    transaction.commit();
                    return;
                }
            }
        }
    }

    /** How many of the key the buffer holds and its open grants still expect, for the cap of the item's rule. */
    public int held(ItemKey key) {
        return countOf(buffer, key) + grants.left(key);
    }

    /** How many of the offered items the warehouse's cap for the item leaves room for; all while offline. */
    private int capRoom(ItemKey key, int offered) {
        return network().map(network -> network.capRoom(key, offered)).orElse(offered);
    }

    private void reportCapRefused(ItemKey key) {
        network().ifPresent(network -> network.post(new WarehouseEvent.CapRefused(key, worldPosition)));
    }

    /** Whether a put-away job holds the slot. */
    public boolean isClaimed(int slot) {
        return buffer.isLocked(slot);
    }

    /** The id of the job whose lock holds the slot, 0 when it is free. */
    public int claimedBy(int slot) {
        SlotLock lock = buffer.lockOf(slot);
        return lock == null ? 0 : lock.jobId();
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
    @Override
    public void setBlocked(PutAwayPlan.Reason reason, ItemKey key, String zoneDisplay) {
        blockedReason = reason;
        blockedStack = key.stack(1);
        blockedZone = zoneDisplay == null ? "" : zoneDisplay;
        setActivity(StorageNodeBlocks.Activity.BLOCKED);
    }

    /** Nothing is stuck: the lamp shows orange while stacks are being put away, green when the buffer is empty. */
    @Override
    public void clearBlocked() {
        blockedReason = null;
        blockedStack = ItemStack.EMPTY;
        blockedZone = "";
        setActivity(isEmpty() ? StorageNodeBlocks.Activity.IDLE : StorageNodeBlocks.Activity.WORKING);
    }

    @Override
    public StationCounters.Kind stationKind() {
        return StationCounters.Kind.INBOUND;
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
        tag.put("Locks", buffer.saveLocks(ValueIo.registries(this)));
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        loadStation(input, tag);
        grants.closeAll();
        if (tag.contains("Items")) {
            buffer.fillFrom(registries, tag.getCompoundOrEmpty("Items"));
            buffer.loadLocks(registries, tag.getListOrEmpty("Locks"));
        }
        grants.dropOrphanLocks();
    }
}
