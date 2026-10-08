package io.github.dirnei.mc.servus.storage.access;

import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.network.DeliveryTarget;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseEvent;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The warehouse's door to the data line. Packets that arrive land in the
 * inherited intake buffer and are put away like at an Inbound Station; the
 * outgoing buffer receives shuttle and instant picks for shipments and sends
 * them as packets, one delivery at a time.
 */
public class AccessPointBlockEntity extends InboundStationBlockEntity implements DeliveryTarget, DataLink {

    private static final AtomicInteger NEXT_SHIPMENT = new AtomicInteger(1);

    /** Items on their way to one data member. */
    public static final class Shipment {
        private final int id;
        private final BlockPos target;
        private final ItemKey key;
        private final int planned;
        private int delivered;
        private boolean ended;

        Shipment(int id, BlockPos target, ItemKey key, int planned, int delivered) {
            this.id = id;
            this.target = target;
            this.key = key;
            this.planned = planned;
            this.delivered = delivered;
        }

        public int id() {
            return id;
        }

        public BlockPos target() {
            return target;
        }

        public ItemKey key() {
            return key;
        }

        public int planned() {
            return planned;
        }

        public int delivered() {
            return delivered;
        }

        public boolean ended() {
            return ended;
        }

        int missing() {
            return Math.max(0, planned - delivered);
        }
    }

    private final ItemStackHandler outgoing = new ItemStackHandler(StorageStatsManager.INSTANCE.inboundBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            network().ifPresent(network -> network.post(new WarehouseEvent.OutputChanged(worldPosition)));
        }
    };

    private final List<Shipment> shipments = new ArrayList<>();
    private Delivery running;
    private Shipment runningFor;
    private int sendTimer;

    public AccessPointBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.ACCESS_POINT.get(), pos, state);
    }

    @Override
    public ItemStackHandler deliveries() {
        return outgoing;
    }

    public ItemStackHandler outgoing() {
        return outgoing;
    }

    /** Packets may arrive through any face, the front included. */
    @Override
    public IItemHandler handlerFor(Direction side) {
        return intake();
    }

    /** Open shipments, oldest first. */
    public List<Shipment> shipments() {
        return List.copyOf(shipments);
    }

    /** Registers a shipment of {@code planned} items; picks arrive in the outgoing buffer afterwards. */
    public Shipment addShipment(BlockPos target, ItemKey key, int planned) {
        Shipment shipment = new Shipment(NEXT_SHIPMENT.getAndIncrement(), target.immutable(), key, planned, 0);
        shipments.add(shipment);
        setChanged();
        return shipment;
    }

    public boolean sending() {
        return running != null && !running.ended();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AccessPointBlockEntity point) {
        InboundStationBlockEntity.serverTick(level, pos, state, point);
        if (level instanceof ServerLevel server && ++point.sendTimer >= StorageStatsManager.INSTANCE.pushInterval()) {
            point.sendTimer = 0;
            point.ship(server);
        }
    }

    /** Starts the next delivery when none runs, and hands items nobody waits for to the intake. */
    private void ship(ServerLevel server) {
        if (sending()) {
            return;
        }
        shipments.removeIf(Shipment::ended);
        for (Shipment shipment : shipments) {
            ItemStack stack = takeOutgoing(shipment.key, shipment.missing());
            if (stack.isEmpty()) {
                continue;
            }
            int count = stack.getCount();
            Optional<Delivery> started = DataPackets.send(server, worldPosition, shipment.target, stack);
            if (started.isEmpty() || started.get().ended() && started.get().state() == Delivery.State.FAILED) {
                // Refused at once: the stack is still ours, and this shipment cannot be served.
                ItemHandlerHelper.insertItemStacked(outgoing, stack, false);
                shipment.ended = true;
                continue;
            }
            running = started.get();
            runningFor = shipment;
            running.onEnd(delivery -> finished(shipment, delivery, count));
            setActivity(true);
            return;
        }
        returnOrphans();
        setActivity(false);
    }

    private void finished(Shipment shipment, Delivery delivery, int sent) {
        shipment.delivered += delivery.delivered();
        if (delivery.state() == Delivery.State.FAILED || shipment.missing() == 0) {
            shipment.ended = true;
        }
        if (runningFor == shipment) {
            running = null;
            runningFor = null;
        }
        setChanged();
    }

    /** Moves items in the outgoing buffer that no open shipment wants into the intake, to be put away. */
    private void returnOrphans() {
        for (int slot = 0; slot < outgoing.getSlots(); slot++) {
            ItemStack stack = outgoing.getStackInSlot(slot);
            if (stack.isEmpty() || wanted(ItemKey.of(stack))) {
                continue;
            }
            ItemStack left = ItemHandlerHelper.insertItemStacked(buffer(), stack.copy(), false);
            outgoing.setStackInSlot(slot, left);
        }
    }

    private boolean wanted(ItemKey key) {
        for (Shipment shipment : shipments) {
            if (!shipment.ended && shipment.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack takeOutgoing(ItemKey key, int max) {
        ItemStack taken = ItemStack.EMPTY;
        for (int slot = 0; slot < outgoing.getSlots() && taken.getCount() < max; slot++) {
            ItemStack stack = outgoing.getStackInSlot(slot);
            if (stack.isEmpty() || !ItemKey.of(stack).equals(key)) {
                continue;
            }
            ItemStack got = outgoing.extractItem(slot, max - taken.getCount(), false);
            if (taken.isEmpty()) {
                taken = got;
            } else {
                taken.grow(got.getCount());
            }
        }
        return taken;
    }

    private void setActivity(boolean working) {
        if (level == null || level.isClientSide() || !isEmpty()) {
            return;
        }
        if (working) {
            clearBlockedWorking();
        } else {
            clearBlocked();
        }
    }

    /** The orange lamp while packets leave, even when the intake is empty. */
    private void clearBlockedWorking() {
        BlockState state = getBlockState();
        if (state.hasProperty(StorageNodeBlocks.ACTIVITY)) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ACTIVITY,
                    StorageNodeBlocks.Activity.WORKING), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        for (int i = 0; i < outgoing.getSlots(); i++) {
            ItemStack stack = outgoing.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                outgoing.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Outgoing", outgoing.serializeNBT(registries));
        ListTag list = new ListTag();
        for (Shipment shipment : shipments) {
            if (shipment.ended) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.put("Target", NbtUtils.writeBlockPos(shipment.target));
            entry.put("Item", shipment.key.stack(1).save(registries));
            entry.putInt("Planned", shipment.planned);
            entry.putInt("Delivered", shipment.delivered);
            list.add(entry);
        }
        tag.put("Shipments", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Outgoing")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Outgoing"));
            for (int i = 0; i < outgoing.getSlots(); i++) {
                outgoing.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        shipments.clear();
        for (Tag raw : tag.getList("Shipments", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            Optional<BlockPos> target = NbtUtils.readBlockPos(entry, "Target");
            ItemStack item = ItemStack.parseOptional(registries, entry.getCompound("Item"));
            if (target.isPresent() && !item.isEmpty()) {
                shipments.add(new Shipment(NEXT_SHIPMENT.getAndIncrement(), target.get(), ItemKey.of(item),
                        entry.getInt("Planned"), entry.getInt("Delivered")));
            }
        }
    }
}
