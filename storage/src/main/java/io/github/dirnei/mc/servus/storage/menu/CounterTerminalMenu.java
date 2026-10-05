package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.index.WarehouseIndex;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
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
import net.neoforged.neoforge.network.PacketDistributor;

/** Player inventory plus the warehouse behind the terminal. */
public class CounterTerminalMenu extends AbstractContainerMenu {

    public static final int INVENTORY_Y = 112;
    public static final int HOTBAR_Y = 170;
    private static final int SYNC_INTERVAL = 10;
    private static final int MAX_ENTRIES = 4096;

    private final CounterTerminalBlockEntity terminal;
    private final ContainerLevelAccess access;
    private final Player player;
    private int syncTimer;
    private long lastSignature = Long.MIN_VALUE;

    public CounterTerminalMenu(int containerId, Inventory playerInventory, CounterTerminalBlockEntity terminal) {
        super(ServusStorageMenus.COUNTER_TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
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

    private Optional<WarehouseNetwork> online() {
        return terminal.network().filter(network -> network.status() == WarehouseNetwork.Status.ONLINE);
    }

    // Operations (server side)

    /** Pulls one stack of the key to the cursor, or into the inventory. */
    public void pull(ItemKey key, boolean toInventory) {
        Optional<WarehouseNetwork> network = online();
        if (network.isEmpty()) {
            return;
        }
        ItemStack carried = getCarried();
        int room;
        if (toInventory) {
            room = key.stack(1).getMaxStackSize();
        } else if (carried.isEmpty()) {
            room = key.stack(1).getMaxStackSize();
        } else if (key.matches(carried)) {
            room = carried.getMaxStackSize() - carried.getCount();
        } else {
            return;
        }
        if (room <= 0) {
            return;
        }
        ItemStack taken = network.get().extract(key, room);
        if (taken.isEmpty()) {
            return;
        }
        if (toInventory) {
            if (!player.getInventory().add(taken) && !taken.isEmpty()) {
                giveBack(network.get(), taken);
            }
        } else if (carried.isEmpty()) {
            setCarried(taken);
        } else {
            carried.grow(taken.getCount());
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

    private void giveBack(WarehouseNetwork network, ItemStack stack) {
        ItemStack left = network.insert(stack);
        if (!left.isEmpty()) {
            player.drop(left, false);
        }
    }

    private void forceSync() {
        lastSignature = Long.MIN_VALUE;
        syncTimer = SYNC_INTERVAL;
    }

    /** Shift click in the inventory stores that slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Optional<WarehouseNetwork> network = online();
        if (network.isEmpty() || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
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

    /** The current stock as the client should see it. */
    public StockSyncPayload stock() {
        Optional<WarehouseNetwork> network = terminal.network();
        if (network.isEmpty()) {
            return new StockSyncPayload(false, "not_connected", List.of());
        }
        WarehouseNetwork warehouse = network.get();
        return switch (warehouse.status()) {
            case NO_CONTROLLER -> new StockSyncPayload(false, "no_controller", List.of());
            case MULTIPLE_CONTROLLERS -> new StockSyncPayload(false, "multiple_controllers", List.of());
            case ONLINE -> new StockSyncPayload(true, "", entries(warehouse.index()));
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
        for (StockEntry entry : payload.entries()) {
            hash = hash * 31 + entry.stack().getItem().hashCode();
            hash = hash * 31 + entry.count();
        }
        return hash;
    }
}
