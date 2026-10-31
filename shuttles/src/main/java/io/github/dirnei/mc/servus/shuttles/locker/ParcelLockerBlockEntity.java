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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The dock of a Parcel Locker and everything the locker keeps: the boxes as
 * one set of slots, box after box in {@link LockerLayout} order, and the
 * orders that hold boxes. Shuttles deliver an order's picks into its own
 * boxes only; nothing is put into a box by hand, by pipe or by any other
 * delivery. A ready parcel the warehouse sends back exposes its boxes to
 * put-away. A box of a delivered order is free again once it is empty.
 * A Handheld scan opens the doors of the scanner's ready boxes for a time;
 * an emptied box closes. Clients get every box's state, owner, closing time
 * and the first items of open boxes in the update tag, which the renderer
 * and the client tick read. Server thread only, apart from that client copy
 * and the one a menu reads.
 */
public class ParcelLockerBlockEntity extends StorageNodeBlockEntity implements DeliveryTarget, PutAwaySource, NamedMachine, LockHolder,
        MenuProvider {

    private static final int SCREEN_TICKS = 60;
    private static final int SHOWN_STACKS = 3;
    private static final String VIEW = "View";
    private static final float DING_VOLUME = 0.7F;
    private static final float DING_PITCH = 1.6F;
    private static final float CLOSE_VOLUME = 0.3F;
    private static final float CLOSE_PITCH = 1.6F;

    /** Whose a box is for a player: free, theirs or someone else's. */
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
            int box = capacities.boxOf(slot);
            if (box >= 0 && isOpen(box)) {
                sync();
            }
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
    private final long[] openUntil = new long[LockerLayout.BOXES.size()];
    private long screenUntil;
    private long scanTime = Long.MIN_VALUE;
    private int greeted;
    private final Shown[] shown = new Shown[LockerLayout.BOXES.size()];
    private final LockerDoors doors = new LockerDoors(LockerLayout.BOXES.size());

    /**
     * What a client knows of one box.
     *
     * @param state     the state of the order holding it, null while free
     * @param owner     the order's owner, null while free
     * @param openUntil the game time its door closes, 0 when closed
     * @param stacks    the first items of an open box, empty otherwise
     */
    public record Shown(LockerReturns.@Nullable State state, @Nullable UUID owner, long openUntil, List<ItemStack> stacks) {

        static final Shown FREE = new Shown(null, null, 0, List.of());
    }

    public ParcelLockerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.PARCEL_LOCKER.get(), pos, state);
        Arrays.fill(shown, Shown.FREE);
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
        order(orderId).ifPresent(order -> {
            order.boxes().forEach(box -> openUntil[box] = 0);
            orders.remove(order);
            changed();
        });
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
        return merged(slotsOf(order));
    }

    /** The items a box holds, one stack per item with its total count. */
    public List<ItemStack> contents(int box) {
        LockerLayout.Range range = ranges.get(box);
        List<Integer> slots = new ArrayList<>();
        for (int slot = range.first(); slot < range.first() + range.count(); slot++) {
            slots.add(slot);
        }
        return merged(slots);
    }

    private List<ItemStack> merged(List<Integer> slots) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot : slots) {
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
        order.boxes().forEach(box -> openUntil[box] = 0);
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
                    openUntil[box] = 0;
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

    /** The lamp a box shows to that player. */
    public LockerLamp lamp(int box, UUID viewer) {
        return orderOfBox(box).map(order -> LockerLamp.of(order.state(), order.owner().equals(viewer))).orElse(LockerLamp.OFF);
    }

    /** Whether shuttles are bringing orders: the dock lamp blinks and the screen shows LOAD. */
    public boolean loading() {
        return orders.stream().anyMatch(order -> order.state() == LockerReturns.State.RESERVED);
    }

    /** Whether the box's door stands open on the server. */
    public boolean isOpen(int box) {
        return level != null && openUntil[box] > level.getGameTime();
    }

    /** The boxes of the player's ready parcels, in box order. */
    public List<Integer> readyBoxes(UUID player) {
        List<Integer> ready = new ArrayList<>();
        for (LockerOrder order : orders) {
            if (order.owner().equals(player) && order.state() == LockerReturns.State.READY) {
                ready.addAll(order.boxes());
            }
        }
        ready.sort(null);
        return ready;
    }

    /**
     * A Handheld scan: opens the doors of the player's ready boxes for the
     * time from data and greets them on the screen, or says there is no
     * parcel. Returns the boxes it opened.
     */
    public List<Integer> scan(Player player) {
        if (level == null || level.isClientSide()) {
            return List.of();
        }
        long now = level.getGameTime();
        List<Integer> ready = readyBoxes(player.getUUID());
        long until = now + ShuttlesStatsManager.INSTANCE.lockerDoorOpenTicks();
        ready.forEach(box -> openUntil[box] = until);
        greeted = ready.size();
        scanTime = now;
        show(ready.isEmpty() ? LockerScreen.NONE : LockerScreen.HELLO);
        sync();
        return ready;
    }

    /** What a click on a box did. */
    public enum Take { TAKEN, NOT_YOURS, CLOSED, FREE }

    /**
     * A click on a box: the owner's open box moves its items into the
     * player's inventory, what does not fit stays; an emptied box closes and
     * is free. Someone else's box stays closed and the screen says so.
     */
    public Take take(Player player, int box) {
        Optional<LockerOrder> order = orderOfBox(box);
        if (order.isEmpty()) {
            return Take.FREE;
        }
        if (!order.get().owner().equals(player.getUUID())) {
            show(LockerScreen.DENY);
            return Take.NOT_YOURS;
        }
        if (order.get().state() != LockerReturns.State.READY || !isOpen(box)) {
            return Take.CLOSED;
        }
        LockerLayout.Range range = ranges.get(box);
        for (int slot = range.first(); slot < range.first() + range.count(); slot++) {
            ItemStack stack = boxes.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack rest = stack.copy();
            player.getInventory().add(rest);
            if (rest.getCount() != stack.getCount()) {
                boxes.setStackInSlot(slot, rest.isEmpty() ? ItemStack.EMPTY : rest);
            }
        }
        sync();
        return Take.TAKEN;
    }

    private BlockPos screenPos() {
        return LockerPart.HEAD_SCREEN.at(worldPosition, facing());
    }

    /** Shows a passing state on the screen for a few seconds. */
    private void show(LockerScreen screen) {
        if (level == null || level.isClientSide()) {
            return;
        }
        screenUntil = level.getGameTime() + SCREEN_TICKS;
        setScreen(screen);
        level.scheduleTick(screenPos(), getBlockState().getBlock(), SCREEN_TICKS);
    }

    /** The screen's scheduled tick: a passing state whose time is up falls back to LOAD or IDLE. */
    public void refreshScreen() {
        if (level == null || level.isClientSide()) {
            return;
        }
        long left = screenUntil - level.getGameTime();
        if (left > 0) {
            level.scheduleTick(screenPos(), getBlockState().getBlock(), (int) left);
            return;
        }
        setScreen(loading() ? LockerScreen.LOAD : LockerScreen.IDLE);
    }

    /** What the screen shows now. */
    public LockerScreen screen() {
        BlockState state = level == null ? null : level.getBlockState(screenPos());
        return state != null && state.getBlock() instanceof ParcelLockerBlock ? state.getValue(ParcelLockerBlock.SCREEN) : LockerScreen.IDLE;
    }

    private void setScreen(LockerScreen screen) {
        setPart(LockerPart.HEAD_SCREEN, state -> state.setValue(ParcelLockerBlock.SCREEN, screen));
    }

    private void setPart(LockerPart part, UnaryOperator<BlockState> change) {
        BlockPos pos = part.at(worldPosition, facing());
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ParcelLockerBlock) || state.getValue(ParcelLockerBlock.PART) != part) {
            return;
        }
        BlockState changed = change.apply(state);
        if (changed != state) {
            level.setBlock(pos, changed, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** Saves, sets the dock lamp and the screen's LOAD to whether shuttles bring orders, and tells clients. */
    private void changed() {
        setChanged();
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean loading = loading();
        setPart(LockerPart.HEAD_DOCK, state -> state.setValue(ParcelLockerBlock.LOADING, loading));
        if (level.getGameTime() >= screenUntil) {
            setScreen(loading ? LockerScreen.LOAD : LockerScreen.IDLE);
        }
        sync();
    }

    /** Sends the boxes' states, owners, open doors and the items of open boxes to clients. */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Removing the locker drops no locker item: a creative player broke it. The boxes' items still drop. */
    public void dropNoItem() {
        dropItem = false;
    }

    /** What the GUI shows of every box to that player, in {@link LockerLayout#BOXES} order: read only. */
    public List<BoxInfo> infos(UUID player) {
        List<BoxView> views = views(player);
        List<BoxInfo> infos = new ArrayList<>();
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            int index = box.index();
            BoxView view = views.get(index);
            infos.add(orderOfBox(index)
                    .map(order -> new BoxInfo(view, order.ownerName(), order.state()))
                    .orElse(new BoxInfo(view, "", null)));
        }
        return infos;
    }

    /** Writes what the GUI needs: the dock's position and what the opening player sees of every box. */
    public void writeOpening(RegistryFriendlyByteBuf buf, UUID player) {
        buf.writeBlockPos(worldPosition);
        for (BoxInfo info : infos(player)) {
            info.write(buf);
        }
    }

    @Override
    public Component getDisplayName() {
        return label();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ParcelLockerMenu(containerId, this, infos(player.getUUID()));
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
        tag.putLongArray("OpenUntil", openUntil);
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
        long[] open = tag.getLongArray("OpenUntil").orElse(new long[0]);
        Arrays.fill(openUntil, 0);
        System.arraycopy(open, 0, openUntil, 0, Math.min(open.length, openUntil.length));
        tag.getCompound(VIEW).ifPresent(view -> readView(registries, view));
    }

    /**
     * The client copy of the boxes: per box the state and owner of its order,
     * when its door closes and the first items of an open box; and the last
     * scan with how many boxes it opened.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag view = new CompoundTag();
        ListTag list = new ListTag();
        for (LockerLayout.Box box : LockerLayout.BOXES) {
            CompoundTag entry = new CompoundTag();
            int index = box.index();
            orderOfBox(index).ifPresent(order -> {
                entry.putString("State", order.state().name());
                entry.store("Owner", UUIDUtil.CODEC, order.owner());
            });
            entry.putLong("OpenUntil", openUntil[index]);
            if (isOpen(index)) {
                ListTag stacks = new ListTag();
                contents(index).stream().limit(SHOWN_STACKS).forEach(stack -> stacks.add(shownStack(registries, stack)));
                entry.put("Stacks", stacks);
            }
            list.add(entry);
        }
        view.put("Boxes", list);
        view.putLong("ScanTime", scanTime);
        view.putInt("Greeted", greeted);
        tag.put(VIEW, view);
        return tag;
    }

    /** A merged stack for the client: the item saved as one, its total beside it, since a total can pass the stack limit. */
    static CompoundTag shownStack(HolderLookup.Provider registries, ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.put("Item", ValueIo.saveStack(registries, stack.copyWithCount(1)));
        tag.putInt("Count", stack.getCount());
        return tag;
    }

    static ItemStack shownStack(HolderLookup.Provider registries, Tag tag) {
        if (!(tag instanceof CompoundTag compound)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = compound.get("Item") == null ? ItemStack.EMPTY : ValueIo.parseStack(registries, compound.get("Item"));
        stack.setCount(compound.getIntOr("Count", 0));
        return stack;
    }

    private void readView(HolderLookup.Provider registries, CompoundTag view) {
        ListTag list = view.getListOrEmpty("Boxes");
        for (int box = 0; box < shown.length; box++) {
            CompoundTag entry = list.getCompoundOrEmpty(box);
            LockerReturns.State state = null;
            for (LockerReturns.State candidate : LockerReturns.State.values()) {
                if (candidate.name().equals(entry.getStringOr("State", ""))) {
                    state = candidate;
                }
            }
            List<ItemStack> stacks = new ArrayList<>();
            for (Tag stack : entry.getListOrEmpty("Stacks")) {
                ItemStack parsed = shownStack(registries, stack);
                if (!parsed.isEmpty()) {
                    stacks.add(parsed);
                }
            }
            shown[box] = new Shown(state, entry.read("Owner", UUIDUtil.CODEC).orElse(null), entry.getLongOr("OpenUntil", 0L), List.copyOf(stacks));
        }
        scanTime = view.getLongOr("ScanTime", Long.MIN_VALUE);
        greeted = view.getIntOr("Greeted", 0);
    }

    /** The client copy of a box. */
    public Shown shown(int box) {
        return shown[box];
    }

    /** The game time of the last scan, on the client. */
    public long scanTime() {
        return scanTime;
    }

    /** How many boxes the last scan opened. */
    public int greeted() {
        return greeted;
    }

    /** The doors' swing on the client. */
    public LockerDoors doors() {
        return doors;
    }

    /** Client tick: starts doors swinging as their open times say and plays a ding per opening door and a click per closing one. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, ParcelLockerBlockEntity locker) {
        long now = level.getGameTime();
        boolean[] open = new boolean[locker.shown.length];
        for (int box = 0; box < open.length; box++) {
            open[box] = locker.shown[box].openUntil() > now;
        }
        locker.doors.update(open, now);
        for (int started : locker.doors.started(now)) {
            boolean opening = started >= 0;
            BlockPos at = locker.boxPos(opening ? started : -1 - started);
            if (opening) {
                level.playLocalSound(at, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, DING_VOLUME, DING_PITCH, false);
            } else {
                level.playLocalSound(at, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, CLOSE_VOLUME, CLOSE_PITCH, false);
            }
        }
    }

    /** The block the box's bottom lies in. */
    public BlockPos boxPos(int box) {
        LockerLayout.Box at = LockerLayout.BOXES.get(box);
        return worldPosition.relative(LockerPart.right(facing()), at.column() - LockerLayout.HEAD_COLUMN).above(at.bottom() / LockerLayout.BLOCK);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
