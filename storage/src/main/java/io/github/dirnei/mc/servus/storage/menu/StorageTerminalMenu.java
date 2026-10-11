package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import io.github.dirnei.mc.servus.storage.terminal.TerminalCraftables;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Storage;
import io.github.dirnei.mc.servus.storage.terminal.TerminalRequests;
import io.github.dirnei.mc.servus.storage.terminal.TerminalWarehouses;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The terminal's output and input slots, the player inventory, a 3x3
 * crafting grid with its result and the warehouses the terminal reaches over
 * its data line. Slot order: outputs, inputs, player inventory, grid, result.
 * The grid and result only take part while the crafting popup is open on the
 * client; the server always keeps them.
 */
public class StorageTerminalMenu extends AbstractContainerMenu {

    private static final int SYNC_INTERVAL = 10;
    private static final int MAX_ENTRIES = 4096;

    private final StorageTerminalBlockEntity terminal;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int outputSlots;
    private final int inputStart;
    private final int inventoryStart;
    private final int gridStart;
    private final int resultIndex;
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private String message = "";
    /** The key of the other site the grid shows, empty for this site. */
    private String view = "";
    private int syncTimer;
    private long lastSignature = Long.MIN_VALUE;
    private long firstSyncTick = -1;
    /** Whether the crafting popup shows; only the client screen changes it. */
    private boolean craftOpen = true;

    public StorageTerminalMenu(int containerId, Inventory playerInventory, StorageTerminalBlockEntity terminal) {
        super(ServusStorageMenus.STORAGE_TERMINAL.get(), containerId);
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
            addSlot(new ResourceHandlerSlot(terminal.input(), terminal.input()::set, slot, Storage.inputX(slot), Storage.inputY(slot)));
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
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(craftSlots, slot, Storage.craftX(slot), Storage.craftY(slot)) {
                @Override
                public boolean isActive() {
                    return craftOpen;
                }
            });
        }
        resultIndex = slots.size();
        addSlot(new GridResultSlot(player, craftSlots, resultSlots, Storage.CRAFT_RESULT.x(), Storage.CRAFT_RESULT.y()));
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

    /** Shows or hides the crafting popup's slots; the client screen calls this. */
    public void setCraftOpen(boolean open) {
        craftOpen = open;
    }

    public boolean craftOpen() {
        return craftOpen;
    }

    /** Index of the first player inventory slot. */
    public int inventoryStart() {
        return inventoryStart;
    }

    /** Index of the first crafting grid slot. */
    public int gridStart() {
        return gridStart;
    }

    /** Index of the crafting result slot. */
    public int resultIndex() {
        return resultIndex;
    }

    public CraftingContainer craftSlots() {
        return craftSlots;
    }

    // Crafting grid (server side)

    /** The grid changed: work out the result like a crafting table does. */
    @Override
    public void slotsChanged(Container container) {
        if (container == craftSlots) {
            access.execute((level, pos) -> updateResult(level));
        }
    }

    /** Vanilla CraftingMenu.slotChangedCraftingGrid, written out for this menu's result slot index. */
    private void updateResult(Level level) {
        if (level.isClientSide()) {
            return;
        }
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = ServusRecipes.craftingFor(level, input);
        // Players that are not ServerPlayers (GameTest mock players) skip the limited crafting check.
        if (recipe.isPresent() && (!(player instanceof ServerPlayer serverPlayer) || resultSlots.setRecipeUsed(serverPlayer, recipe.get()))) {
            ItemStack assembled = recipe.get().value().assemble(input);
            if (assembled.isItemEnabled(level.enabledFeatures())) {
                result = assembled;
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(resultIndex, result);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), resultIndex, result));
        }
    }

    /** The warehouses the terminal reaches now, empty on the client. */
    private List<WarehouseNetwork> warehouses() {
        return terminal.getLevel() instanceof ServerLevel server
                ? TerminalWarehouses.reachable(server, terminal.getBlockPos()) : List.of();
    }

    /** Takes up to the amount of the key from instant warehouse stock (crates and foreign inventories, never racks). */
    private ItemStack takeInstant(ItemKey key, int amount) {
        ItemStack result = ItemStack.EMPTY;
        for (WarehouseNetwork network : warehouses()) {
            int wanted = amount - result.getCount();
            if (wanted <= 0) {
                break;
            }
            ItemStack taken = network.extract(key, wanted);
            if (taken.isEmpty()) {
                continue;
            }
            if (result.isEmpty()) {
                result = taken;
            } else {
                result.grow(taken.getCount());
            }
        }
        return result;
    }

    /** Hands a stack to the input row, which stores it through a Packing Station; returns what did not fit. */
    private ItemStack store(ItemStack stack) {
        return terminal.queueInput(stack);
    }

    /** After a craft, every grid slot that lost an ingredient gets one more of it from instant stock. */
    private void refill(ItemStack[] before) {
        if (player.level().isClientSide()) {
            return;
        }
        for (int slot = 0; slot < before.length; slot++) {
            ItemStack old = before[slot];
            if (old.isEmpty()) {
                continue;
            }
            ItemStack now = craftSlots.getItem(slot);
            int lost = now.isEmpty() ? old.getCount() : ItemStack.isSameItemSameComponents(now, old) ? old.getCount() - now.getCount() : 0;
            if (lost <= 0) {
                continue;
            }
            ItemStack got = takeInstant(ItemKey.of(old), lost);
            if (got.isEmpty()) {
                continue;
            }
            if (now.isEmpty()) {
                craftSlots.setItem(slot, got);
            } else {
                now.grow(got.getCount());
                craftSlots.setItem(slot, now);
            }
        }
    }

    /** Alt click on a stock entry: up to one stack from instant stock onto the empty cursor. Not for another site. */
    public void pullToCursor(ItemKey key) {
        if (!getCarried().isEmpty() || !view.isEmpty()) {
            return;
        }
        setCarried(takeInstant(key, key.stack(1).getMaxStackSize()));
        forceSync();
    }

    /** Moves every grid stack into the warehouse; the rest goes to the player. */
    public void clearGrid() {
        if (player.level().isClientSide()) {
            return;
        }
        for (int slot = 0; slot < craftSlots.getContainerSize(); slot++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack rest = store(stack);
            if (!rest.isEmpty()) {
                player.getInventory().placeItemBackInInventory(rest);
            }
        }
        slotsChanged(craftSlots);
        forceSync();
    }

    /**
     * JEI transfer: clears the grid, then fills each slot with the first
     * acceptable stack found in instant stock, else in the player inventory.
     * With max, a slot takes up to a stack instead of one item.
     */
    public void fillGrid(List<List<ItemStack>> options, boolean max) {
        if (player.level().isClientSide()) {
            return;
        }
        clearGrid();
        for (int slot = 0; slot < Math.min(options.size(), craftSlots.getContainerSize()); slot++) {
            for (ItemStack option : options.get(slot)) {
                if (option.isEmpty()) {
                    continue;
                }
                int count = max ? option.getMaxStackSize() : 1;
                ItemStack got = takeInstant(ItemKey.of(option), count);
                if (got.isEmpty()) {
                    got = takeFromInventory(option, count);
                }
                if (!got.isEmpty()) {
                    craftSlots.setItem(slot, got);
                    break;
                }
            }
        }
        slotsChanged(craftSlots);
        forceSync();
    }

    private ItemStack takeFromInventory(ItemStack like, int count) {
        Inventory inventory = player.getInventory();
        ItemStack result = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getNonEquipmentItems().size() && result.getCount() < count; i++) {
            ItemStack stack = inventory.getNonEquipmentItems().get(i);
            if (!ItemStack.isSameItemSameComponents(stack, like)) {
                continue;
            }
            ItemStack taken = stack.split(count - result.getCount());
            if (result.isEmpty()) {
                result = taken;
            } else {
                result.grow(taken.getCount());
            }
        }
        return result;
    }

    /** Closing the terminal empties the grid into the warehouse, the rest into the player inventory. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        clearGrid();
    }

    /** Takes the crafted result like a crafting table and refills the grid from the warehouse afterwards. */
    private final class GridResultSlot extends ResultSlot {

        GridResultSlot(Player player, CraftingContainer grid, Container result, int x, int y) {
            super(player, grid, result, 0, x, y);
        }

        @Override
        public boolean isActive() {
            return craftOpen;
        }

        @Override
        public void onTake(Player taker, ItemStack stack) {
            ItemStack[] before = new ItemStack[craftSlots.getContainerSize()];
            for (int slot = 0; slot < before.length; slot++) {
                before[slot] = craftSlots.getItem(slot).copy();
            }
            super.onTake(taker, stack);
            refill(before);
        }
    }

    /** The first reachable warehouse, for rules and pushes. */
    private Optional<WarehouseNetwork> online() {
        return terminal.getLevel() instanceof ServerLevel server ? TerminalRequests.first(server, terminal.getBlockPos()) : Optional.empty();
    }

    // Operations (server side)

    /**
     * Requests this many of the key; deliveries land in the output slots. While
     * another site is shown it is a transfer from that site into this site's storage.
     */
    public void request(ItemKey key, int amount) {
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            message = "offline";
        } else if (!view.isEmpty()) {
            message = io.github.dirnei.mc.servus.storage.terminal.RemoteTransfers.request(server, terminal, view, key, Math.max(1, amount));
        } else {
            message = TerminalRequests.request(server, terminal, key, Math.max(1, amount));
        }
        forceSync();
    }

    /** The key of the site the grid shows, empty for this site. */
    public String view() {
        return view;
    }

    /** Shows another site of the team, or this site with an empty key; an unknown key shows this site. */
    public void selectSite(String site) {
        String asked = site == null ? "" : site;
        boolean known = asked.isEmpty() || terminal.getLevel() instanceof ServerLevel server
                && io.github.dirnei.mc.servus.storage.terminal.RemoteTransfers.sites(server, terminal.getBlockPos()).stream()
                .anyMatch(row -> row.key().equals(asked));
        view = known ? asked : "";
        message = "";
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

    /** Stores the carried stack through the input row. */
    public void pushCarried() {
        if (getCarried().isEmpty()) {
            return;
        }
        setCarried(terminal.queueInput(getCarried()));
        forceSync();
    }

    private void forceSync() {
        lastSignature = Long.MIN_VALUE;
        syncTimer = SYNC_INTERVAL;
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
        if (index == resultIndex) {
            ItemStack original = stack.copy();
            access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, player));
            if (!moveItemStackTo(stack, inventoryStart, gridStart, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            player.drop(stack, false);
            return original;
        }
        if (index >= gridStart) {
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, inventoryStart, gridStart, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return original;
        }
        if (index < inventoryStart) {
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, inventoryStart, gridStart, true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return original;
        }
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, inputStart, inventoryStart, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        forceSync();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.STORAGE_TERMINAL.get());
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
                firstSyncTick = serverPlayer.level().getGameTime();
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
        String shown = message.isEmpty() && terminal.inputRefusal() != null ? terminal.inputRefusal() : message;
        if (!(terminal.getLevel() instanceof ServerLevel server)) {
            return new StockSyncPayload(terminal.getBlockPos(), false, "no_warehouse", List.of(), List.of(), shown, List.of(), List.of());
        }
        List<WarehouseNetwork> warehouses = TerminalWarehouses.reachable(server, terminal.getBlockPos());
        List<RequestRow> requests = TerminalRequests.rows(server, terminal, warehouses);
        if (warehouses.isEmpty()) {
            view = "";
            return new StockSyncPayload(terminal.getBlockPos(), false, "no_warehouse", List.of(), requests, shown, List.of(), List.of());
        }
        List<io.github.dirnei.mc.servus.storage.terminal.SiteRow> sites =
                io.github.dirnei.mc.servus.storage.terminal.RemoteTransfers.sites(server, terminal.getBlockPos());
        if (!view.isEmpty()) {
            var site = io.github.dirnei.mc.servus.storage.site.SiteRegistry.of(server).site(view);
            if (site.isPresent() && sites.stream().anyMatch(row -> row.key().equals(view))) {
                int state = io.github.dirnei.mc.servus.storage.terminal.RemoteTransfers.state(server, site.get()).ordinal();
                return new StockSyncPayload(terminal.getBlockPos(), true, "",
                        entries(io.github.dirnei.mc.servus.storage.terminal.RemoteTransfers.stock(server, site.get())), requests, shown,
                        List.of(), List.of(), sites, view, state);
            }
            view = "";
        }
        return new StockSyncPayload(terminal.getBlockPos(), true, "", entries(TerminalWarehouses.stock(warehouses)), requests, shown,
                warehouses.get(0).ruleRows(), TerminalCraftables.of(server, terminal.getBlockPos(), MAX_ENTRIES), sites, "", -1);
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
        hash = hash * 31 + payload.sites().hashCode();
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
