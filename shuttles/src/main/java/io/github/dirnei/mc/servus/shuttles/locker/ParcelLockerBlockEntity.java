package io.github.dirnei.mc.servus.shuttles.locker;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.naming.MachineNameSlot;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.shuttles.lock.LockHolder;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotFill;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.menu.ParcelLockerMenu;
import io.github.dirnei.mc.servus.shuttles.network.DeliveryTarget;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.PutAwaySource;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMachineKinds;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The dock of a Parcel Locker and everything the locker keeps: the boxes as
 * one set of slots, box after box in {@link LockerLayout} order, and the
 * orders that hold boxes. Shuttles deliver an order's picks into its own
 * boxes only; nothing is put into a box by hand, by pipe or by any other
 * delivery. A ready parcel the warehouse sends back exposes its boxes to
 * put-away. A box of a delivered order is free again once it is empty.
 * Server thread only, apart from the client copy a menu reads.
 */
public class ParcelLockerBlockEntity extends StorageNodeBlockEntity implements DeliveryTarget, PutAwaySource, NamedMachine, LockHolder,
        MenuProvider {

    /** What a player sees of a box in the locker GUI. */
    public enum BoxView { FREE, MINE, TAKEN }

    private final LockerLayout.Capacities capacities = ShuttlesStatsManager.INSTANCE.lockerCapacities();
    private final List<LockerLayout.Range> ranges = capacities.ranges();
    private final LockedSlots boxes = new LockedSlots(capacities.total(), LockedSlots.Mode.COUNTED) {
        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return actingJob() == 0 ? 0 : super.insert(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            freeEmptyBoxes();
        }

        @Override
        protected void onLocksChanged() {
            setChanged();
            freeEmptyBoxes();
        }
    };
    private final List<LockerOrder> orders = new ArrayList<>();
    private final MachineNameSlot nameSlot = new MachineNameSlot(this, ServusShuttlesMachineKinds.PARCEL_LOCKER);
    private boolean dropItem = true;
    private PutAwayPlan.Reason blockedReason;
    private ItemStack blockedStack = ItemStack.EMPTY;

    public ParcelLockerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.PARCEL_LOCKER.get(), pos, state);
    }

    public LockerLayout.Capacities capacities() {
        return capacities;
    }

    /** Every box's slots, box after box. */
    public LockedSlots boxes() {
        return boxes;
    }

    /** The slots of one box. */
    public LockerLayout.Range range(int box) {
        return ranges.get(box);
    }

    public List<LockerOrder> orders() {
        return List.copyOf(orders);
    }

    public Optional<LockerOrder> order(int id) {
        return orders.stream().filter(order -> order.id() == id).findFirst();
    }

    /** The order holding the box, if any. */
    public Optional<LockerOrder> orderOfBox(int box) {
        return orders.stream().filter(order -> order.holds(box)).findFirst();
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(ParcelLockerBlock.FACING) ? state.getValue(ParcelLockerBlock.FACING) : Direction.NORTH;
    }

    /** The rail in front of the dock, where shuttles stop. */
    public BlockPos railPos() {
        return worldPosition.relative(facing());
    }

    @Override
    public NodeKind kind() {
        return NodeKind.STATION;
    }

    /** Only the dock's front takes a rail. */
    @Override
    public boolean acceptsRail(Side side) {
        return side == Conversions.toSide(facing());
    }

    @Override
    public MachineNameSlot nameSlot() {
        return nameSlot;
    }

    @Override
    public LockedSlots deliveries() {
        return boxes;
    }

    @Override
    public List<LockedSlots> lockedSlots() {
        return List.of(boxes);
    }

    /** The free boxes with how many stacks each holds. */
    public List<BoxAssignment.Free> freeBoxes() {
        List<BoxAssignment.Free> free = new ArrayList<>();
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            if (orderOfBox(box.index()).isEmpty()) {
                free.add(new BoxAssignment.Free(box.index(), capacities.of(box.size())));
            }
        }
        return free;
    }

    /** How many free boxes of the size there are. */
    public int freeCount(LockerLayout.Size size) {
        return (int) freeBoxes().stream().filter(free -> LockerLayout.BOXES.get(free.box()).size() == size).count();
    }

    /** Reserves the boxes for a new order of the player and lights their lamps. */
    public LockerOrder reserve(int orderId, UUID owner, String ownerName, List<Integer> chosen) {
        LockerOrder order = new LockerOrder(orderId, owner, ownerName, chosen);
        orders.add(order);
        changed();
        return order;
    }

    /** Ends an order: its boxes are free at once, whatever they still hold stays in them until taken or broken. */
    public void cancel(int orderId) {
        if (orders.removeIf(order -> order.id() == orderId)) {
            changed();
        }
    }

    /**
     * Locks room in the order's boxes for a pick of the order: on top of the
     * same item first, then in empty slots; picks of one item may share a
     * slot. Picks of no order of this locker, or of an order that is no
     * longer waiting for deliveries, find no room.
     */
    @Override
    public int lockDelivery(int jobId, int requestId, ItemKey key, int amount) {
        Optional<LockerOrder> order = order(requestId).filter(found -> found.state() == LockerReturns.State.RESERVED);
        if (order.isEmpty()) {
            return 0;
        }
        List<Integer> slots = slotsOf(order.get());
        List<SlotFill.Slot<ItemKey>> states = new ArrayList<>(slots.size());
        for (int slot : slots) {
            states.add(reservedView(slot));
        }
        int max = key.stack(1).getMaxStackSize();
        int locked = 0;
        for (SlotFill.Part part : SlotFill.in(states, key, amount, max)) {
            boxes.lock(slots.get(part.slot()), new SlotLock(SlotLock.Kind.IN, key, part.count(), jobId));
            locked += part.count();
        }
        return locked;
    }

    /** A slot as an order's deliveries see it: what it holds and what IN locks already bring count as its item. */
    private SlotFill.Slot<ItemKey> reservedView(int slot) {
        ItemStack stack = boxes.getStackInSlot(slot);
        ItemKey key = stack.isEmpty() ? null : ItemKey.of(stack);
        int count = stack.getCount();
        for (SlotLock lock : boxes.locksOf(slot)) {
            if (lock.kind() == SlotLock.Kind.IN) {
                key = key == null ? lock.key() : key;
                count += lock.amount();
            }
        }
        return new SlotFill.Slot<>(key, count, boxes.getSlotLimit(slot), false);
    }

    /** Every slot of the order's boxes. */
    public List<Integer> slotsOf(LockerOrder order) {
        List<Integer> slots = new ArrayList<>();
        for (int box : order.boxes()) {
            LockerLayout.Range range = ranges.get(box);
            for (int slot = range.first(); slot < range.first() + range.count(); slot++) {
                slots.add(slot);
            }
        }
        return slots;
    }

    /** Whether a delivery to the order is still on its way: one of its slots holds an IN lock. */
    public boolean awaitsDelivery(LockerOrder order) {
        return slotsOf(order).stream().anyMatch(boxes::hasIn);
    }

    /** Whether any of the order's boxes holds an item. */
    public boolean holdsItems(LockerOrder order) {
        return slotsOf(order).stream().anyMatch(slot -> !boxes.getStackInSlot(slot).isEmpty());
    }

    /** The items an order's boxes hold, one stack per item with its total count. */
    public List<ItemStack> contents(LockerOrder order) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot : slotsOf(order)) {
            ItemStack stack = boxes.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Optional<ItemStack> same = stacks.stream().filter(other -> ItemStack.isSameItemSameComponents(other, stack)).findFirst();
            if (same.isPresent()) {
                same.get().grow(stack.getCount());
            } else {
                stacks.add(stack.copy());
            }
        }
        return stacks;
    }

    /** The parcel is in: its lamps turn green, and boxes nothing arrived in are free again. */
    public void markReady(LockerOrder order, long now) {
        order.ready(now);
        changed();
        freeEmptyBoxes();
    }

    /** The parcel waited too long: its boxes join the put-away buffer. */
    public void markReturning(LockerOrder order) {
        order.returning();
        changed();
    }

    /** Only the slots of parcels going back to the racks are put away. */
    @Override
    public boolean drains(int slot) {
        int box = capacities.boxOf(slot);
        return box >= 0 && orderOfBox(box).map(order -> order.state() == LockerReturns.State.RETURNING).orElse(false);
    }

    @Override
    public LockedSlots putAwayBuffer() {
        return boxes;
    }

    @Override
    public void setBlocked(PutAwayPlan.Reason reason, ItemKey key, String zoneDisplay) {
        blockedReason = reason;
        blockedStack = key.stack(1);
    }

    @Override
    public void clearBlocked() {
        blockedReason = null;
        blockedStack = ItemStack.EMPTY;
    }

    /** Why a returning parcel's item finds no rack, or null when nothing is stuck. */
    public PutAwayPlan.Reason blockedReason() {
        return blockedReason;
    }

    public ItemStack blockedStack() {
        return blockedStack;
    }

    /**
     * Frees the boxes of delivered and returning orders whose slots are empty
     * and hold no lock, and ends orders left without a box.
     */
    private void freeEmptyBoxes() {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean any = false;
        for (LockerOrder order : List.copyOf(orders)) {
            if (order.state() == LockerReturns.State.RESERVED) {
                continue;
            }
            for (int box : order.boxes()) {
                if (boxEmpty(box)) {
                    order.release(box);
                    any = true;
                }
            }
            if (order.boxes().isEmpty()) {
                orders.remove(order);
            }
        }
        if (any) {
            changed();
        }
    }

    private boolean boxEmpty(int box) {
        LockerLayout.Range range = ranges.get(box);
        for (int slot = range.first(); slot < range.first() + range.count(); slot++) {
            if (!boxes.getStackInSlot(slot).isEmpty() || boxes.isLocked(slot)) {
                return false;
            }
        }
        return true;
    }

    /** What the player sees of every box: free, theirs or someone else's. */
    public List<BoxView> views(UUID player) {
        List<BoxView> views = new ArrayList<>();
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            views.add(orderOfBox(box.index()).map(order -> order.owner().equals(player) ? BoxView.MINE : BoxView.TAKEN).orElse(BoxView.FREE));
        }
        return views;
    }

    /** The lamp a box shows. */
    public LockerLamp lamp(int box) {
        return orderOfBox(box).map(order -> LockerLamp.of(order.state())).orElse(LockerLamp.OFF);
    }

    /** The dock's lamp: orange while shuttles bring orders. */
    public LockerLamp dockLamp() {
        return orders.stream().anyMatch(order -> order.state() == LockerReturns.State.RESERVED) ? LockerLamp.RESERVED : LockerLamp.OFF;
    }

    /** Saves and turns every part's lamps to the boxes' states. */
    private void changed() {
        setChanged();
        if (level == null || level.isClientSide()) {
            return;
        }
        Direction facing = facing();
        for (LockerPart part : LockerPart.values()) {
            BlockPos pos = part.at(worldPosition, facing);
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ParcelLockerBlock) || state.getValue(ParcelLockerBlock.PART) != part) {
                continue;
            }
            List<LockerLayout.Box> lamps = part.lampBoxes();
            LockerLamp top = part == LockerPart.HEAD_DOCK ? dockLamp() : lamps.isEmpty() ? LockerLamp.OFF : lamp(lamps.get(0).index());
            LockerLamp bottom = lamps.size() < 2 ? LockerLamp.OFF : lamp(lamps.get(1).index());
            BlockState lit = state.setValue(ParcelLockerBlock.LAMP_TOP, top).setValue(ParcelLockerBlock.LAMP_BOTTOM, bottom);
            if (lit != state) {
                level.setBlock(pos, lit, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    /** Removing the locker drops no locker item: a creative player broke it. The boxes' items still drop. */
    public void dropNoItem() {
        dropItem = false;
    }

    /** Writes what the GUI needs: the dock's position and what the opening player sees of every box. */
    public void writeOpening(RegistryFriendlyByteBuf buf, UUID player) {
        buf.writeBlockPos(worldPosition);
        for (BoxView view : views(player)) {
            buf.writeEnum(view);
        }
    }

    @Override
    public Component getDisplayName() {
        return label();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ParcelLockerMenu(containerId, playerInventory, this, views(player.getUUID()));
    }

    /**
     * Breaking the locker drops the item and every box's items; picks still
     * queued for its orders are dropped and their stock unlocked, loads on
     * the way go back to the racks when they find no locker.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) {
            return;
        }
        Optional<WarehouseNetwork> network = network();
        for (LockerOrder order : orders) {
            network.ifPresent(found -> found.dropQueuedFor(order.id()));
        }
        orders.clear();
        boxes.dropAll(level, pos);
        if (dropItem) {
            dropItem = false;
            Block.popResource(level, pos, new ItemStack(state.getBlock()));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        tag.put("Items", boxes.serializeNBT(registries));
        tag.put("Locks", boxes.saveLocks(registries));
        ListTag list = new ListTag();
        orders.forEach(order -> list.add(order.save()));
        tag.put("Orders", list);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        orders.clear();
        if (tag.contains("Items")) {
            boxes.fillFrom(registries, tag.getCompoundOrEmpty("Items"));
            boxes.loadLocks(registries, tag.getListOrEmpty("Locks"));
        }
        ListTag list = tag.getListOrEmpty("Orders");
        for (int i = 0; i < list.size(); i++) {
            LockerOrder.load(list.getCompoundOrEmpty(i)).ifPresent(orders::add);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
