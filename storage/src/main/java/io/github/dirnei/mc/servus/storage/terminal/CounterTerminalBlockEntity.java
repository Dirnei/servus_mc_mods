package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.order.ProductionLedger;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The pickup desk on the data line: opens the stock GUI of the warehouses it
 * reaches, keeps the output slots packets land in, and remembers the open
 * requests made here. Requests are saved with the terminal; their ids come
 * from the {@link ProductionLedger}, so they never repeat.
 */
public class CounterTerminalBlockEntity extends BlockEntity implements MenuProvider, DataLink {

    /** How a request is doing, as the GUI shows it. */
    public enum RequestState { PICKING, SENDING, WAITING_ROOM, PRODUCING, MISSING }

    /** A shipment an Access Point runs for a request, by its ledger id, and how many items were asked of it. */
    public record Tracked(BlockPos accessPoint, long shipmentId, int asked) {
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

    private final ItemStackHandler output = new ItemStackHandler(StorageStatsManager.INSTANCE.terminalOutputSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler packets = new OutputInsertHandler();
    private final List<Request> requests = new ArrayList<>();
    private int ticks;
    private boolean idsSeen;

    public CounterTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.COUNTER_TERMINAL.get(), pos, state);
    }

    /** The slots deliveries land in. */
    public ItemStackHandler output() {
        return output;
    }

    /** What automation and packets see: insertion into the output slots only. */
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
        if (requests.removeIf(request -> request.id == id)) {
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

    /** Gives arrived items to the open requests of that item, oldest first. */
    private void credit(ItemKey key, int count) {
        for (Request request : requests) {
            if (count <= 0) {
                return;
            }
            if (request.key.equals(key) && request.delivered < request.requested) {
                int share = Math.min(count, request.requested - request.delivered);
                request.delivered += share;
                count -= share;
            }
        }
    }

    // Ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, CounterTerminalBlockEntity terminal) {
        if (level instanceof ServerLevel server) {
            TerminalRequests.tick(server, terminal, ++terminal.ticks);
        }
    }

    /** Lights or darkens the lamp. */
    public void setOnline(boolean online) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(StorageNodeBlocks.ONLINE) && state.getValue(StorageNodeBlocks.ONLINE) != online) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ONLINE, online), Block.UPDATE_CLIENTS);
        }
    }

    /** Drops every stack in the output slots at the terminal. */
    public void dropOutput(Level level, BlockPos pos) {
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                output.setStackInSlot(i, ItemStack.EMPTY);
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
        return new CounterTerminalMenu(containerId, playerInventory, this);
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Output", output.serializeNBT(registries));
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
            ListTag shipments = new ListTag();
            for (Tracked tracked : request.shipments) {
                CompoundTag shipment = new CompoundTag();
                shipment.put("AccessPoint", NbtUtils.writeBlockPos(tracked.accessPoint()));
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
        if (tag.contains("Output")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Output"));
            for (int i = 0; i < output.getSlots(); i++) {
                output.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
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
            for (Tag rawShipment : entry.getList("Shipments", Tag.TAG_COMPOUND)) {
                CompoundTag shipment = (CompoundTag) rawShipment;
                NbtUtils.readBlockPos(shipment, "AccessPoint").ifPresent(point -> request.shipments.add(
                        new Tracked(point, shipment.getLong("Id"), shipment.getInt("Asked"))));
            }
            requests.add(request);
        }
    }

    /** Insertion into the output slots, crediting requests; nothing can be taken out. */
    private final class OutputInsertHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return output.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return output.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack left = output.insertItem(slot, stack, simulate);
            int landed = stack.getCount() - left.getCount();
            if (!simulate && landed > 0) {
                credit(ItemKey.of(stack), landed);
            }
            return left;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return output.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return output.isItemValid(slot, stack);
        }
    }
}
