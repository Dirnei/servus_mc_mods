package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import io.github.dirnei.mc.servus.storage.terminal.TerminalCraftables;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Counter;
import io.github.dirnei.mc.servus.storage.terminal.TerminalRequests;
import io.github.dirnei.mc.servus.storage.terminal.TerminalWarehouses;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/** The terminal's output slots, the player inventory and the warehouses the terminal reaches over its data line. */
public class CounterTerminalMenu extends AbstractContainerMenu {

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
    private long firstSyncTick = -1;

    public CounterTerminalMenu(int containerId, Inventory playerInventory, CounterTerminalBlockEntity terminal) {
        super(ServusStorageMenus.COUNTER_TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
        this.outputSlots = terminal.output().getSlots();
        for (int slot = 0; slot < outputSlots; slot++) {
            addSlot(new SlotItemHandler(terminal.output(), slot, Counter.outputX(slot), Counter.outputY(slot)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        inventoryStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, Counter.INVENTORY.x() + column * 18,
                        Counter.INVENTORY.y() + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, Counter.HOTBAR.x() + column * 18, Counter.HOTBAR.y()));
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

    /** The first reachable warehouse, for rules and pushes. */
    private Optional<WarehouseNetwork> online() {
        return terminal.getLevel() instanceof ServerLevel server ? TerminalRequests.first(server, terminal.getBlockPos()) : Optional.empty();
    }

    // Operations (server side)

    /** Requests this many of the key; deliveries land in the output slots. */
    public void request(ItemKey key, int amount) {
        message = terminal.getLevel() instanceof ServerLevel server
                ? TerminalRequests.request(server, terminal, key, Math.max(1, amount)) : "offline";
        forceSync();
    }

    /** What crafting this many of the key would take, craft and miss, as the craft dialog shows it. */
    public CraftPreviewPayload preview(ItemKey key, int amount) {
        int wanted = Math.max(1, amount);
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            return new CraftPreviewPayload(key.stack(1), wanted, List.of(), List.of(), List.of(), "offline");
        }
        OrderBook.Preview preview = TerminalCraftables.preview(server, terminal.getBlockPos(), key, wanted);
        return new CraftPreviewPayload(key.stack(1), wanted, rows(preview.fromStock()), rows(preview.toCraft()),
                rows(preview.missing()), preview.refusal());
    }

    private static List<StockEntry> rows(Map<ItemKey, Integer> counts) {
        List<StockEntry> list = new ArrayList<>();
        counts.forEach((key, count) -> list.add(new StockEntry(key.stack(1), count)));
        return list;
    }

    /** Crafts this many of the key on a CPU of the line; the results land in the output slots. */
    public void craft(ItemKey key, int amount) {
        message = terminal.getLevel() instanceof ServerLevel server
                ? TerminalRequests.craft(server, terminal, key, Math.max(1, amount)) : "offline";
        forceSync();
    }

    /** Sets the stock rule of an item; a negative minimum removes it. */
    public void setRule(ItemKey key, int min, int max) {
        online().ifPresent(network -> {
            if (min < 0) {
                network.removeStockRule(key);
            } else {
                network.setStockRule(key, min, max);
            }
        });
        forceSync();
    }

    public void cancel(int requestId) {
        if (terminal.getLevel() instanceof ServerLevel server) {
            TerminalRequests.cancel(server, terminal, requestId);
        }
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

    /**
     * Called by {@code setSynchronizer} when the game opens the screen: the
     * content goes out at once instead of with the next periodic check.
     */
    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();
        sendNow();
    }

    /** A resync sends the full content too. */
    @Override
    public void broadcastFullState() {
        super.broadcastFullState();
        sendNow();
    }

    private void sendNow() {
        if (player instanceof ServerPlayer serverPlayer) {
            StockSyncPayload payload = stock();
            lastSignature = signature(payload);
            syncTimer = 0;
            if (firstSyncTick < 0) {
                firstSyncTick = serverPlayer.serverLevel().getGameTime();
            }
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    /** The game time the content was first sent, -1 before that. */
    public long firstSyncTick() {
        return firstSyncTick;
    }

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
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            return new StockSyncPayload(false, "no_warehouse", List.of(), List.of(), message, List.of(), List.of());
        }
        List<WarehouseNetwork> warehouses = TerminalWarehouses.reachable(server, terminal.getBlockPos());
        List<RequestRow> requests = TerminalRequests.rows(server, terminal, warehouses);
        if (warehouses.isEmpty()) {
            return new StockSyncPayload(false, "no_warehouse", List.of(), requests, message, List.of(), List.of());
        }
        return new StockSyncPayload(true, "", entries(TerminalWarehouses.stock(warehouses)), requests, message,
                warehouses.get(0).ruleRows(), TerminalCraftables.of(server, terminal.getBlockPos(), MAX_ENTRIES));
    }

    private static List<StockEntry> entries(List<Map.Entry<ItemKey, Long>> stock) {
        List<StockEntry> list = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> entry : stock) {
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
        for (io.github.dirnei.mc.servus.storage.terminal.RuleRow rule : payload.rules()) {
            hash = hash * 31 + rule.stack().getItem().hashCode();
            hash = hash * 31 + rule.min();
            hash = hash * 31 + rule.max();
            hash = hash * 31 + rule.state().hashCode();
        }
        for (ItemStack craftable : payload.craftables()) {
            hash = hash * 31 + craftable.getItem().hashCode();
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
