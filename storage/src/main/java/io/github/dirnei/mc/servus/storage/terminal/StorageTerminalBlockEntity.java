package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.foundation.data.DataCarrier;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.order.ProductionLedger;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The pickup desk on the data line: opens the stock GUI of the warehouses it
 * reaches, keeps the output slots packets land in, and remembers the open
 * requests made here. Requests are saved with the terminal; their ids come
 * from the {@link ProductionLedger}, so they never repeat.
 */
public class StorageTerminalBlockEntity extends BlockEntity implements WoodHolder, MenuProvider, DataLink {

    /** Carries the data network to touching data nodes, like a piece of cable. */
    private final DataCarrier carrier = new DataCarrier(this);

    private final WoodSlot woodSlot = new WoodSlot(this);

    /** How a request is doing, as the GUI shows it. */
    public enum RequestState { PICKING, SENDING, WAITING_ROOM, PRODUCING, MISSING, WAKING }

    /** A shipment a Packing Station runs for a request, by its ledger id, and how many items were asked of it. */
    public record Tracked(BlockPos packingStation, long shipmentId, int asked) {
    }

    /** One open request: what was asked, what arrived, what cannot come. */
    public static final class Request {
        public final int id;
        public final ItemKey key;
        public final int requested;
        public int delivered;
        public int missing;
        /** Open shipments for this request. */
        public final List<Tracked> shipments = new ArrayList<>();
        /** Master of the Crafting CPU holding this request's work order, null without one. */
        public BlockPos orderCpu;
        /** The key of the other site a transfer comes from, null for a request of this site. */
        public String remote;
        /** Whether the other site was asked once; a transfer ends with its shipments after that. */
        public boolean shipped;
        /** Retries a just woken site gets before its missing stock counts; not saved. */
        public int tries;

        Request(int id, ItemKey key, int requested) {
            this.id = id;
            this.key = key;
            this.requested = requested;
        }

        /** Whether everything that can come has come. */
        public boolean settled() {
            return delivered + missing >= requested;
        }
    }

    /** Output slots shown in one row: at most {@link TerminalLayout.Storage#ROW_SLOTS}. */
    private final ItemStackHandler output = new ItemStackHandler(Math.max(1,
            Math.min(TerminalLayout.Storage.ROW_SLOTS, StorageStatsManager.INSTANCE.terminalOutputSlots()))) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Items handed in; stored into the warehouse on the next tick, leftovers retried. */
    private final ItemStackHandler input = new ItemStackHandler(TerminalLayout.Storage.ROW_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            storeSoon = true;
        }
    };
    private final IItemHandler packets = new RoutingHandler();
    /** The running move of each input slot, or null. */
    private final Delivery[] inputMoves = new Delivery[TerminalLayout.Storage.ROW_SLOTS];
    private final IItemHandler[] inputViews = new IItemHandler[TerminalLayout.Storage.ROW_SLOTS];
    private boolean storeSoon;
    private String inputRefusal;
    private final List<Request> requests = new ArrayList<>();
    private int ticks;
    private boolean idsSeen;

    public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.STORAGE_TERMINAL.get(), pos, state);
        for (int i = 0; i < inputViews.length; i++) {
            inputViews[i] = new InputSlotView(i);
        }
    }

    /** The slots deliveries land in. */
    public ItemStackHandler output() {
        return output;
    }

    /** The slots the player and automation hand items in through. */
    public ItemStackHandler input() {
        return input;
    }

    /** What automation and packets see: items a request waits for go to the output, the rest to the input. */
    public IItemHandler packetHandler() {
        return packets;
    }

    /** How many of an item sit in the output slots. */
    public int outputCount(Item item) {
        int count = 0;
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // Requests

    public List<Request> requests() {
        return Collections.unmodifiableList(requests);
    }

    public Request request(int id) {
        for (Request request : requests) {
            if (request.id == id) {
                return request;
            }
        }
        return null;
    }

    public Request newRequest(ServerLevel level, ItemKey key, int requested) {
        seeIds(level);
        Request request = new Request(ProductionLedger.get(level).nextRequestId(), key, requested);
        requests.add(request);
        setChanged();
        return request;
    }

    public void removeRequest(int id) {
        Request request = request(id);
        if (request != null && request.remote != null) {
            RemoteTransfers.release(worldPosition, id);
        }
        if (requests.removeIf(entry -> entry.id == id)) {
            setChanged();
        }
    }

    /** Keeps the ledger's request counter above the ids of loaded requests. */
    void seeIds(ServerLevel level) {
        if (idsSeen) {
            return;
        }
        idsSeen = true;
        ProductionLedger ledger = ProductionLedger.get(level);
        for (Request request : requests) {
            ledger.seenRequestId(request.id);
        }
    }

    /** Whether an open request still waits for this item. */
    public boolean waitsFor(ItemKey key) {
        for (Request request : requests) {
            if (request.remote == null && request.key.equals(key) && request.delivered < request.requested) {
                return true;
            }
        }
        return false;
    }

    /** Gives arrived items to the open requests of that item, oldest first. */
    private void credit(ItemKey key, int count) {
        for (Request request : requests) {
            if (count <= 0) {
                return;
            }
            if (request.remote == null && request.key.equals(key) && request.delivered < request.requested) {
                int share = Math.min(count, request.requested - request.delivered);
                request.delivered += share;
                count -= share;
            }
        }
    }

    // Ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, StorageTerminalBlockEntity terminal) {
        if (level instanceof ServerLevel server) {
            TerminalRequests.tick(server, terminal, ++terminal.ticks);
            if (terminal.storeSoon || terminal.ticks % 20 == 0) {
                terminal.storeInput(server);
            }
        }
    }

    /**
     * Moves the input slots over the data line to the nearest Packing Station,
     * which puts them away. One move per slot takes items out of the slot only
     * as fast as the station converts them and has room in its To storage
     * buffer, so the row shrinks over time and keeps what the station cannot
     * take yet. A slot whose move still runs is left alone.
     */
    public void storeInput(ServerLevel server) {
        storeSoon = false;
        boolean any = false;
        for (int i = 0; i < input.getSlots() && !any; i++) {
            any = !input.getStackInSlot(i).isEmpty();
        }
        if (!any) {
            inputRefusal = null;
            return;
        }
        Optional<BlockPos> point = nearestPackingStation(server);
        if (point.isEmpty()) {
            inputRefusal = "input_no_packing_station";
            return;
        }
        inputRefusal = null;
        for (int i = 0; i < input.getSlots(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty() || inputMoves[i] != null && !inputMoves[i].ended()) {
                continue;
            }
            inputMoves[i] = DataPackets.move(server, worldPosition, point.get(), stack.copyWithCount(1), stack.getCount(),
                    inputViews[i]).orElse(null);
        }
    }

    /**
     * One input slot as a delivery source: items can be taken out (and given
     * back) only by the move, and only while this terminal exists.
     */
    private final class InputSlotView implements IItemHandler {

        private final int slot;

        private InputSlotView(int slot) {
            this.slot = slot;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return isRemoved() ? ItemStack.EMPTY : input.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
            return isRemoved() ? stack : input.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int index, int amount, boolean simulate) {
            return isRemoved() ? ItemStack.EMPTY : input.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int index) {
            return input.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int index, ItemStack stack) {
            return input.isItemValid(slot, stack);
        }
    }

    /** Puts a stack into the input row, which stores it; returns what does not fit. */
    public ItemStack queueInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemStack rest = ItemHandlerHelper.insertItemStacked(input, stack.copy(), false);
        storeSoon = true;
        return rest;
    }

    /** Why the input waits, a message key without its prefix, or null. */
    public String inputRefusal() {
        return inputRefusal;
    }

    /** The Packing Station of an online warehouse on this data line nearest to the terminal. */
    private Optional<BlockPos> nearestPackingStation(ServerLevel server) {
        Optional<DataNetwork> line = DataNetworks.networkAt(server, worldPosition);
        if (line.isEmpty()) {
            return Optional.empty();
        }
        BlockPos best = null;
        for (DataMember member : line.get().dataMembers()) {
            BlockPos pos = member.pos();
            if (!server.isLoaded(pos) || !(server.getBlockEntity(pos) instanceof PackingStationBlockEntity packingStation)) {
                continue;
            }
            boolean online = packingStation.network().filter(warehouse -> warehouse.status() == WarehouseNetwork.Status.ONLINE).isPresent();
            if (!online) {
                continue;
            }
            if (best == null || closer(pos, best)) {
                best = pos;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean closer(BlockPos candidate, BlockPos best) {
        double a = candidate.distSqr(worldPosition);
        double b = best.distSqr(worldPosition);
        return a < b || (a == b && candidate.asLong() < best.asLong());
    }

    /** Lights or darkens the lamp. */
    public void setOnline(boolean online) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(StorageNodeBlocks.ONLINE) && state.getValue(StorageNodeBlocks.ONLINE) != online) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ONLINE, online), Block.UPDATE_CLIENTS);
        }
    }

    /** Drops every stack in the output and input slots at the terminal. */
    public void dropOutput(Level level, BlockPos pos) {
        for (ItemStackHandler handler : List.of(output, input)) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                }
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
        return new StorageTerminalMenu(containerId, playerInventory, this);
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        woodSlot.save(tag);
        tag.put("Output", output.serializeNBT(registries));
        tag.put("Input", input.serializeNBT(registries));
        ListTag list = new ListTag();
        for (Request request : requests) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Id", request.id);
            entry.put("Item", request.key.stack(1).save(registries));
            entry.putInt("Requested", request.requested);
            entry.putInt("Delivered", request.delivered);
            entry.putInt("Missing", request.missing);
            if (request.orderCpu != null) {
                entry.put("OrderCpu", NbtUtils.writeBlockPos(request.orderCpu));
            }
            if (request.remote != null) {
                entry.putString("Remote", request.remote);
                entry.putBoolean("Shipped", request.shipped);
            }
            ListTag shipments = new ListTag();
            for (Tracked tracked : request.shipments) {
                CompoundTag shipment = new CompoundTag();
                shipment.put("PackingStation", NbtUtils.writeBlockPos(tracked.packingStation()));
                shipment.putLong("Id", tracked.shipmentId());
                shipment.putInt("Asked", tracked.asked());
                shipments.add(shipment);
            }
            entry.put("Shipments", shipments);
            list.add(entry);
        }
        tag.put("Requests", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        woodSlot.load(tag);
        if (tag.contains("Output")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Output"));
            for (int i = 0; i < output.getSlots(); i++) {
                output.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        if (tag.contains("Input")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Input"));
            for (int i = 0; i < input.getSlots(); i++) {
                input.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        requests.clear();
        idsSeen = false;
        for (Tag raw : tag.getList("Requests", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ItemStack item = ItemStack.parseOptional(registries, entry.getCompound("Item"));
            if (item.isEmpty()) {
                continue;
            }
            Request request = new Request(entry.getInt("Id"), ItemKey.of(item), entry.getInt("Requested"));
            request.delivered = entry.getInt("Delivered");
            request.missing = entry.getInt("Missing");
            request.orderCpu = NbtUtils.readBlockPos(entry, "OrderCpu").orElse(null);
            request.remote = entry.contains("Remote") ? entry.getString("Remote") : null;
            request.shipped = entry.getBoolean("Shipped");
            for (Tag rawShipment : entry.getList("Shipments", Tag.TAG_COMPOUND)) {
                CompoundTag shipment = (CompoundTag) rawShipment;
                NbtUtils.readBlockPos(shipment, "PackingStation").ifPresent(point -> request.shipments.add(
                        new Tracked(point, shipment.getLong("Id"), shipment.getInt("Asked"))));
            }
            requests.add(request);
        }
    }

    /**
     * What automation and packets see: the output slots first, then the input
     * slots. An item an open request waits for may only go into the output,
     * where it is credited; every other item only into the input, which
     * stores it. Nothing can be taken out.
     */
    private final class RoutingHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return output.getSlots() + input.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < output.getSlots() ? output.getStackInSlot(slot) : input.getStackInSlot(slot - output.getSlots());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }
            boolean wanted = waitsFor(ItemKey.of(stack));
            if (slot < output.getSlots()) {
                if (!wanted) {
                    return stack;
                }
                ItemStack left = output.insertItem(slot, stack, simulate);
                int landed = stack.getCount() - left.getCount();
                if (!simulate && landed > 0) {
                    credit(ItemKey.of(stack), landed);
                }
                return left;
            }
            return wanted ? stack : input.insertItem(slot - output.getSlots(), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return (slot < output.getSlots()) == waitsFor(ItemKey.of(stack));
        }
    }

    // Wood: the planks the block was crafted from, shown as its boards

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        WoodSlot.removeFromTag(tag);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }

    // Data carrier

    @Override
    public void onLoad() {
        super.onLoad();
        carrier.join();
    }

    @Override
    public void setRemoved() {
        carrier.leave();
        // Sites held awake for this terminal's transfers may sleep; a loaded terminal takes the holds again.
        for (Request request : requests) {
            if (request.remote != null) {
                RemoteTransfers.release(worldPosition, request.id);
            }
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        carrier.leave();
    }
}
