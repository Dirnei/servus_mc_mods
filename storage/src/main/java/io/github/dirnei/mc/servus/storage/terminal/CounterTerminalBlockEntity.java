package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.DeliveryTarget;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseEvent;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The pickup desk: opens the stock GUI, keeps the output slots deliveries
 * land in, and remembers the open requests made here. Requests are not
 * persisted; a reload clears them.
 */
public class CounterTerminalBlockEntity extends StorageNodeBlockEntity implements MenuProvider, DeliveryTarget {

    /** How a request is doing, as the GUI shows it. */
    public enum RequestState { WAITING_SHUTTLE, PICKING, WAITING_ROOM, PRODUCING, MISSING }

    /** One open request: what was asked, what arrived, what cannot come. */
    public static final class Request {
        public final int id;
        public final ItemKey key;
        public final int requested;
        public int delivered;
        public int missing;
        /** A delivery did not fit into the output slots and waits. */
        public boolean waitingRoom;

        Request(int id, ItemKey key, int requested, int missing) {
            this.id = id;
            this.key = key;
            this.requested = requested;
            this.missing = missing;
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
            network().ifPresent(network -> network.post(new WarehouseEvent.OutputChanged(worldPosition)));
        }
    };
    private final List<Request> requests = new ArrayList<>();
    private int nextRequestId = 1;

    public CounterTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.COUNTER_TERMINAL.get(), pos, state);
    }

    /** The slots deliveries land in. Never a capability: not stock, not hopper reachable. */
    public ItemStackHandler output() {
        return output;
    }

    @Override
    public ItemStackHandler deliveries() {
        return output;
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

    public Request newRequest(ItemKey key, int requested, int missing) {
        Request request = new Request(nextRequestId++, key, requested, missing);
        requests.add(request);
        return request;
    }

    public void removeRequest(int id) {
        requests.removeIf(request -> request.id == id);
    }

    /** Drops requests that were fully delivered and have nothing in flight. Requests with missing stock stay until cancelled. */
    public void prune(java.util.function.Predicate<Request> inFlight) {
        for (Iterator<Request> it = requests.iterator(); it.hasNext(); ) {
            Request request = it.next();
            if (request.delivered >= request.requested && !request.waitingRoom && !inFlight.test(request)) {
                it.remove();
            }
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
}
