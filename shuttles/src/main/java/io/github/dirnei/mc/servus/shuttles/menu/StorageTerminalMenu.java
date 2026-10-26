package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.logistics.menu.GhostSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.QuickMoves;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.order.OrderBook;
import io.github.dirnei.mc.servus.shuttles.order.RuleThresholds;
import io.github.dirnei.mc.servus.shuttles.order.StockRule;
import io.github.dirnei.mc.servus.shuttles.pattern.CraftingGrid;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternEditing;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.site.SiteRegistry;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.LineWarehouse;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.RemoteTransfers;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestRow;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import io.github.dirnei.mc.servus.shuttles.terminal.SiteRow;
import io.github.dirnei.mc.servus.shuttles.terminal.StockEntry;
import io.github.dirnei.mc.servus.shuttles.terminal.StockSyncPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalCraftables;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Storage;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalRequests;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalWarehouses;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The terminal's output and input slots, the player inventory, the order
 * grid's nine ghost cells and the warehouses the terminal reaches over its
 * data line. Slot order: outputs, inputs, player inventory, ghost cells. The
 * ghost cells only take part while the order grid is open on the client; the
 * server always keeps them, and they vanish with the menu.
 */
public class StorageTerminalMenu extends AbstractContainerMenu {

    private static final int MAX_ENTRIES = 4096;

    private final StorageTerminalBlockEntity terminal;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int outputSlots;
    private final int inputStart;
    private final int inventoryStart;
    private final int gridStart;
    private final ItemSlots grid = new ItemSlots(EncodedPattern.GRID);
    private final PayloadSync<StockSyncPayload> sync = new PayloadSync<>(10, false, this::stock, StorageTerminalMenu::signature);
    private String message = "";
    /** The key of the other site the grid shows, empty for this site. */
    private String view = "";
    private long firstSyncTick = -1;
    /** Whether the order grid shows; only the client screen changes it. */
    private boolean orderOpen = true;

    public StorageTerminalMenu(int containerId, Inventory playerInventory, StorageTerminalBlockEntity terminal) {
        super(ServusShuttlesMenus.STORAGE_TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
        this.outputSlots = terminal.output().getSlots();
        for (int slot = 0; slot < outputSlots; slot++) {
            addSlot(new ResourceHandlerSlot(terminal.output(), terminal.output()::set, slot, Storage.outputX(slot), Storage.outputY(slot)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        inputStart = slots.size();
        for (int slot = 0; slot < terminal.input().getSlots(); slot++) {
            addSlot(new ResourceHandlerSlot(terminal.input(), terminal.input()::set, slot, Storage.inputX(slot), Storage.inputY(slot)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return inputOpen() && super.mayPlace(stack);
                }
            });
        }
        inventoryStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, Storage.INVENTORY.x() + column * 18,
                        Storage.INVENTORY.y() + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, Storage.HOTBAR.x() + column * 18, Storage.HOTBAR.y()));
        }
        gridStart = slots.size();
        for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
            addSlot(new GhostSlot(() -> grid, cell, Storage.orderX(cell), Storage.orderY(cell)) {
                @Override
                public boolean isActive() {
                    return orderOpen;
                }
            });
        }
    }

    public static StorageTerminalMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof StorageTerminalBlockEntity terminal) {
            return new StorageTerminalMenu(containerId, playerInventory, terminal);
        }
        throw new IllegalStateException("No storage terminal at " + pos);
    }

    public StorageTerminalBlockEntity terminal() {
        return terminal;
    }

    /** How many output slots there are; they come first. */
    public int outputSlots() {
        return outputSlots;
    }

    /** Index of the first input slot. */
    public int inputStart() {
        return inputStart;
    }

    /** Shows or hides the order grid's ghost cells; the client screen calls this. */
    public void setOrderOpen(boolean open) {
        orderOpen = open;
    }

    /** Index of the first player inventory slot. */
    public int inventoryStart() {
        return inventoryStart;
    }

    /** Index of the first ghost cell of the order grid. */
    public int gridStart() {
        return gridStart;
    }

    public boolean isGridSlot(int slotId) {
        return slotId >= gridStart && slotId < gridStart + EncodedPattern.GRID;
    }

    /** The ghost cells, for the result, the preview and the order. */
    public List<ItemStack> gridCells() {
        List<ItemStack> cells = new ArrayList<>(EncodedPattern.GRID);
        for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
            cells.add(grid.getStackInSlot(cell));
        }
        return cells;
    }

    /** Puts a single copy of the stack into a ghost cell, or clears it for an empty stack. */
    public void setGhost(int cell, ItemStack stack) {
        grid.setStackInSlot(cell, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    /** Ghost cells take a copy of the cursor and never hand anything out. */
    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (isGridSlot(slotId)) {
            if (!player.level().isClientSide()) {
                setGhost(slotId - gridStart, getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Whether the Input row takes items: only while this site is shown, since what is handed in is stored here. */
    public boolean inputOpen() {
        return view.isEmpty();
    }

    /** On the client: the site the server says is shown, so the Input row locks like on the server. */
    public void clientView(String shown) {
        if (player.level().isClientSide()) {
            view = shown == null ? "" : shown;
        }
    }

    /**
     * Recipe viewer transfer: sets each ghost cell to the stocked stack of an
     * accepted item the warehouse can give the most of, with its components,
     * else to the first accepted item. Takes nothing.
     */
    public void fillGhosts(List<List<ItemStack>> options) {
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            return;
        }
        List<Map.Entry<ItemKey, Long>> stock = TerminalWarehouses.online(server, terminal.getBlockPos())
                .map(TerminalWarehouses::stock).orElse(List.of());
        for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
            setGhost(cell, cell < options.size() ? CraftingGrid.stockedChoice(options.get(cell), stock) : ItemStack.EMPTY);
        }
        sync.force();
    }

    /**
     * Orders this many results of the order grid on a CPU of the line, once,
     * without saving a pattern; the cells are cleared once the order is made.
     */
    public void startOrder(int amount) {
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            message = "offline";
        } else {
            Optional<EncodedPattern> pattern = PatternEditing.encode(server, gridCells());
            message = pattern.isEmpty() ? "no_match" : TerminalRequests.craftOneOff(server, terminal, pattern.get(), Math.max(1, amount));
            if (message.isEmpty()) {
                for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
                    setGhost(cell, ItemStack.EMPTY);
                }
            }
        }
        sync.force();
    }

    /**
     * Saves the order grid as a pattern of the line without ordering; a
     * refused save keeps the grid and is told on the message line.
     */
    public void savePattern() {
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            message = "offline";
        } else {
            String saved = TerminalCraftables.line(server, terminal.getBlockPos())
                    .map(line -> PatternEditing.save(server, line, player, gridCells(), null, EncodedPattern.Mode.ANY)).orElse("no_storage");
            message = switch (saved) {
                case "" -> "";
                case "empty", "no_match" -> "no_match";
                default -> "pattern_" + saved;
            };
        }
        sync.force();
    }

    /** The line's warehouse, for rules and pushes. */
    private Optional<WarehouseNetwork> online() {
        return terminal.getLevel() instanceof ServerLevel server ? TerminalWarehouses.online(server, terminal.getBlockPos()) : Optional.empty();
    }

    /**
     * Requests this many of the key; deliveries land in the output slots. While
     * another site is shown it is a transfer from that site into this site's storage.
     */
    public void request(ItemKey key, int amount) {
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            message = "offline";
        } else if (!view.isEmpty()) {
            message = RemoteTransfers.request(server, terminal, view, key, Math.max(1, amount));
        } else {
            message = TerminalRequests.request(server, terminal, key, Math.max(1, amount));
        }
        sync.force();
    }

    /** Shows another site of the team, or this site with an empty key; an unknown key shows this site. */
    public void selectSite(String site) {
        String asked = site == null ? "" : site;
        boolean known = asked.isEmpty() || terminal.getLevel() instanceof ServerLevel server
                && RemoteTransfers.sites(server, terminal.getBlockPos()).stream().anyMatch(row -> row.key().equals(asked));
        view = known ? asked : "";
        message = "";
        sync.force();
    }

    /** What crafting this many of the key would take, craft and miss, as the craft dialog shows it. */
    public CraftPreviewPayload preview(ItemKey key, int amount) {
        return preview(key.stack(1), amount, CraftPreviewRequestPayload.Mode.CRAFT);
    }

    /**
     * The preview a dialog asks for: crafting the amount, a request that
     * ships what is requestable and crafts the rest, or an order of the
     * order grid's recipe.
     */
    public CraftPreviewPayload preview(ItemStack item, int amount, CraftPreviewRequestPayload.Mode mode) {
        int wanted = Math.max(1, amount);
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            return new CraftPreviewPayload(item, wanted, List.of(), List.of(), List.of(), "offline");
        }
        return switch (mode) {
            case CRAFT -> payload(item, wanted, TerminalCraftables.preview(server, terminal.getBlockPos(), ItemKey.of(item), wanted), 0, false);
            case REQUEST -> requestPreview(server, ItemKey.of(item), wanted);
            case ORDER -> orderPreview(server, wanted);
        };
    }

    private CraftPreviewPayload requestPreview(ServerLevel server, ItemKey key, int amount) {
        long available = TerminalWarehouses.online(server, terminal.getBlockPos()).map(warehouse -> warehouse.available(key)).orElse(0L);
        if (available >= amount) {
            return new CraftPreviewPayload(key.stack(1), amount, List.of(), List.of(), List.of(), "", amount, false);
        }
        OrderBook.Preview rest = TerminalCraftables.preview(server, terminal.getBlockPos(), key, (int) (amount - available));
        return payload(key.stack(1), amount, rest, (int) available, false);
    }

    private CraftPreviewPayload orderPreview(ServerLevel server, int amount) {
        Optional<EncodedPattern> pattern = PatternEditing.encode(server, gridCells());
        if (pattern.isEmpty()) {
            return new CraftPreviewPayload(ItemStack.EMPTY, amount, List.of(), List.of(), List.of(), "no_match");
        }
        ItemStack result = pattern.get().result();
        String refusal = TerminalCraftables.orderRefusal(server, terminal.getBlockPos());
        if (!refusal.isEmpty()) {
            return new CraftPreviewPayload(result, amount, List.of(), List.of(), List.of(), refusal);
        }
        Optional<DataNetwork> line = TerminalCraftables.line(server, terminal.getBlockPos());
        boolean saved = line.map(network -> PatternLibrary.contains(server, network, pattern.get(), null)).orElse(false);
        return payload(result, amount, TerminalCraftables.previewOneOff(server, terminal.getBlockPos(), pattern.get(), amount), 0, saved);
    }

    private static CraftPreviewPayload payload(ItemStack item, int amount, OrderBook.Preview preview, int inStock, boolean saved) {
        return new CraftPreviewPayload(item.copyWithCount(1), amount, rows(preview.fromStock()), rows(preview.toCraft()),
                rows(preview.missing()), preview.refusal(), inStock, saved);
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
        sync.force();
    }

    /**
     * Sets or removes the stock rule of an item: the Make pair (min and max
     * -1 for none), the cap (0 for none) and the export (-1 for none). A rule
     * that breaks the threshold rules is refused on the message line.
     */
    public void setRule(ItemKey key, int min, int max, int cap, int export, boolean remove) {
        Optional<WarehouseNetwork> network = online();
        if (network.isEmpty()) {
            message = "offline";
        } else if (remove) {
            network.get().removeStockRule(key);
        } else {
            String check = RuleThresholds.check(min, max, cap, export);
            if (check.equals(RuleThresholds.OK)) {
                check = network.get().setStockRule(key, max < 0 ? null : new StockRule.Make(min, max), cap, export);
            }
            message = check.equals(RuleThresholds.OK) ? "" : check.equals("offline") ? check : "rule_" + check;
        }
        sync.force();
    }

    public void cancel(int requestId) {
        if (terminal.getLevel() instanceof ServerLevel server) {
            TerminalRequests.cancel(server, terminal, requestId);
        }
        sync.force();
    }

    /** Stores the carried stack through the input row. */
    public void pushCarried() {
        if (getCarried().isEmpty() || !inputOpen()) {
            return;
        }
        setCarried(terminal.queueInput(getCarried()));
        sync.force();
    }

    /** Shift click: output and input slots go to the inventory, an inventory slot into the input row, which stores it. */
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
        ItemStack original = stack.copy();
        if (isGridSlot(index)) {
            return ItemStack.EMPTY;
        }
        if (index < inventoryStart) {
            if (!moveItemStackTo(stack, inventoryStart, gridStart, index < inventoryStart)) {
                return ItemStack.EMPTY;
            }
            QuickMoves.settle(slot, stack);
            return original;
        }
        if (!inputOpen() || !moveItemStackTo(stack, inputStart, inventoryStart, false)) {
            return ItemStack.EMPTY;
        }
        QuickMoves.settle(slot, stack);
        sync.force();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusShuttlesBlocks.STORAGE_TERMINAL.get());
    }

    /** Sends the content at once when the screen opens instead of with the next periodic check. */
    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();
        sendNow();
    }

    @Override
    public void broadcastFullState() {
        super.broadcastFullState();
        sendNow();
    }

    private void sendNow() {
        if (player instanceof ServerPlayer serverPlayer) {
            if (firstSyncTick < 0) {
                firstSyncTick = serverPlayer.level().getGameTime();
            }
            sync.sendNow(serverPlayer);
        }
    }

    /** The game time the content was first sent, -1 before that. */
    public long firstSyncTick() {
        return firstSyncTick;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        sync.tick(player);
    }

    /** The current stock, requests and message as the client should see them. */
    public StockSyncPayload stock() {
        String shown = message.isEmpty() && terminal.inputRefusal() != null ? terminal.inputRefusal() : message;
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            return new StockSyncPayload(terminal.getBlockPos(), false, "no_warehouse", List.of(), List.of(), shown, List.of(), List.of());
        }
        LineWarehouse line = TerminalWarehouses.line(server, terminal.getBlockPos());
        Optional<WarehouseNetwork> online = line.online();
        List<RequestRow> requests = TerminalRequests.rows(server, terminal, online);
        if (online.isEmpty()) {
            view = "";
            String reason = line.conflict() ? LineWarehouse.CONFLICT_KEY : "no_warehouse";
            return new StockSyncPayload(terminal.getBlockPos(), false, reason, List.of(), requests, shown, List.of(), List.of());
        }
        WarehouseNetwork warehouse = online.get();
        List<SiteRow> sites = RemoteTransfers.sites(server, terminal.getBlockPos());
        boolean noPower = warehouse.noPower();
        if (!view.isEmpty()) {
            var site = SiteRegistry.of(server).site(view);
            if (site.isPresent() && sites.stream().anyMatch(row -> row.key().equals(view))) {
                int state = RemoteTransfers.state(server, site.get()).ordinal();
                return new StockSyncPayload(terminal.getBlockPos(), true, "", entries(RemoteTransfers.stock(server, site.get())), requests,
                        shown, List.of(), List.of(), sites, view, state, noPower, false);
            }
            view = "";
        }
        return new StockSyncPayload(terminal.getBlockPos(), true, "", entries(TerminalWarehouses.stock(warehouse)), requests, shown,
                warehouse.ruleRows(), TerminalCraftables.of(server, terminal.getBlockPos(), MAX_ENTRIES), sites, "", -1, noPower,
                TerminalCraftables.canOrder(server, terminal.getBlockPos()));
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
        hash = hash * 31 + payload.view().hashCode();
        hash = hash * 31 + payload.viewState();
        hash = hash * 31 + (payload.noPower() ? 1 : 0);
        hash = hash * 31 + (payload.orderGrid() ? 1 : 0);
        hash = hash * 31 + payload.sites().hashCode();
        hash = hash * 31 + payload.reason().hashCode();
        hash = hash * 31 + payload.message().hashCode();
        for (StockEntry entry : payload.entries()) {
            hash = hash * 31 + entry.stack().getItem().hashCode();
            hash = hash * 31 + entry.count();
        }
        for (RuleRow rule : payload.rules()) {
            hash = hash * 31 + rule.stack().getItem().hashCode();
            hash = hash * 31 + rule.min();
            hash = hash * 31 + rule.max();
            hash = hash * 31 + rule.cap();
            hash = hash * 31 + rule.state().hashCode();
            hash = hash * 31 + rule.detail().getString().hashCode();
            hash = hash * 31 + rule.export();
            hash = hash * 31 + rule.holder().getString().hashCode();
            hash = hash * 31 + rule.exportState().hashCode();
        }
        for (ItemStack craftable : payload.craftables()) {
            hash = hash * 31 + craftable.getItem().hashCode();
        }
        for (RequestRow row : payload.requests()) {
            hash = hash * 31 + row.id();
            hash = hash * 31 + row.delivered();
            hash = hash * 31 + row.missing();
            hash = hash * 31 + row.state().hashCode();
            hash = hash * 31 + row.slots().hashCode();
        }
        return hash;
    }
}
