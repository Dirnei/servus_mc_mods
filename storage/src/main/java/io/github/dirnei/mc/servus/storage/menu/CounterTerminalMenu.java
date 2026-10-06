package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.index.WarehouseIndex;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/** The terminal's output slots, the player inventory and the warehouse behind the terminal. */
public class CounterTerminalMenu extends AbstractContainerMenu {

    public static final int OUTPUT_Y = 154;
    public static final int INVENTORY_Y = 176;
    public static final int HOTBAR_Y = 234;
    private static final int COLUMNS = 9;
    private static final int SYNC_INTERVAL = 10;
    private static final int MAX_ENTRIES = 4096;

    private final CounterTerminalBlockEntity terminal;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int outputSlots;
    private final int inventoryStart;
    private String message = "";
    private int syncTimer;
    private long lastSignature = Long.MIN_VALUE;

    public CounterTerminalMenu(int containerId, Inventory playerInventory, CounterTerminalBlockEntity terminal) {
        super(ServusStorageMenus.COUNTER_TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
        this.outputSlots = terminal.output().getSlots();
        for (int slot = 0; slot < outputSlots; slot++) {
            addSlot(new SlotItemHandler(terminal.output(), slot, 8 + (slot % COLUMNS) * 18, OUTPUT_Y + (slot / COLUMNS) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        inventoryStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
    }

    public static CounterTerminalMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CounterTerminalBlockEntity terminal) {
            return new CounterTerminalMenu(containerId, playerInventory, terminal);
        }
        throw new IllegalStateException("No counter terminal at " + pos);
    }

    public CounterTerminalBlockEntity terminal() {
        return terminal;
    }

    /** Index of the first player inventory slot. */
    public int inventoryStart() {
        return inventoryStart;
    }

    private Optional<WarehouseNetwork> online() {
        return terminal.network().filter(network -> network.status() == WarehouseNetwork.Status.ONLINE);
    }

    // Operations (server side)

    /** Requests this many of the key; deliveries land in the output slots. */
    public void request(ItemKey key, int amount) {
        Optional<WarehouseNetwork> network = online();
        if (network.isEmpty()) {
            message = "offline";
        } else {
            message = network.get().request(terminal, key, Math.max(1, amount));
        }
        forceSync();
    }

    public void cancel(int requestId) {
        terminal.network().ifPresent(network -> network.cancelRequest(terminal, requestId));
        forceSync();
    }

    /** Stores the carried stack. */
    public void pushCarried() {
        Optional<WarehouseNetwork> network = online();
        if (network.isEmpty() || getCarried().isEmpty()) {
            return;
        }
        setCarried(network.get().insert(getCarried()));
        forceSync();
    }

    private void forceSync() {
        lastSignature = Long.MIN_VALUE;
        syncTimer = SYNC_INTERVAL;
    }

    /** Shift click: an output slot goes to the inventory, an inventory slot is stored in the warehouse. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (index < outputSlots) {
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, inventoryStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return original;
        }
        Optional<WarehouseNetwork> network = online();
        if (network.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack left = network.get().insert(stack);
        slot.set(left);
        slot.setChanged();
        forceSync();
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.COUNTER_TERMINAL.get());
    }

    // Stock sync

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (++syncTimer < SYNC_INTERVAL) {
            return;
        }
        syncTimer = 0;
        StockSyncPayload payload = stock();
        long signature = signature(payload);
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    /** The current stock, requests and message as the client should see them. */
    public StockSyncPayload stock() {
        Optional<WarehouseNetwork> network = terminal.network();
        if (network.isEmpty()) {
            return new StockSyncPayload(false, "not_connected", List.of(), List.of(), message);
        }
        WarehouseNetwork warehouse = network.get();
        return switch (warehouse.status()) {
            case NO_CONTROLLER -> new StockSyncPayload(false, "no_controller", List.of(), List.of(), message);
            case MULTIPLE_CONTROLLERS -> new StockSyncPayload(false, "multiple_controllers", List.of(), List.of(), message);
            case ONLINE -> new StockSyncPayload(true, "", entries(warehouse.index()), warehouse.requestRows(terminal), message);
        };
    }

    private static List<StockEntry> entries(WarehouseIndex<BlockPos, ItemKey> index) {
        List<StockEntry> list = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> entry : index.entries()) {
            if (list.size() >= MAX_ENTRIES) {
                break;
            }
            list.add(new StockEntry(entry.getKey().stack(1), entry.getValue()));
        }
        return list;
    }

    private static long signature(StockSyncPayload payload) {
        long hash = payload.online() ? 1 : 0;
        hash = hash * 31 + payload.reason().hashCode();
        hash = hash * 31 + payload.message().hashCode();
        for (StockEntry entry : payload.entries()) {
            hash = hash * 31 + entry.stack().getItem().hashCode();
            hash = hash * 31 + entry.count();
        }
        for (RequestRow row : payload.requests()) {
            hash = hash * 31 + row.id();
            hash = hash * 31 + row.delivered();
            hash = hash * 31 + row.missing();
            hash = hash * 31 + row.state().hashCode();
        }
        return hash;
    }
}
