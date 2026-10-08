package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.access.AccessPointBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
 * requests made here. Requests are not persisted; a reload clears them.
 */
public class CounterTerminalBlockEntity extends BlockEntity implements MenuProvider, DataLink {

    /** Request ids are unique across terminals, so pick jobs and work orders never mix them up. */
    private static final AtomicInteger NEXT_REQUEST = new AtomicInteger(1);

    /** How a request is doing, as the GUI shows it. */
    public enum RequestState { PICKING, SENDING, WAITING_ROOM, PRODUCING, MISSING }

    /** A shipment an Access Point runs for a request, and how many items were asked of it. */
    public record Tracked(BlockPos accessPoint, AccessPointBlockEntity.Shipment shipment, int asked) {
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

    public Request newRequest(ItemKey key, int requested) {
        Request request = new Request(NEXT_REQUEST.getAndIncrement(), key, requested);
        requests.add(request);
        return request;
    }

    public void removeRequest(int id) {
        requests.removeIf(request -> request.id == id);
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
