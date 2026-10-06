package io.github.dirnei.mc.servus.storage.menu;

import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageMenus;
import io.github.dirnei.mc.servus.storage.station.DispatchStationBlockEntity;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload.ZoneRow;
import io.github.dirnei.mc.servus.storage.zone.ZoneNames;
import io.github.dirnei.mc.servus.storage.zone.ZoneRules;
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
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The station's buffer, the player inventory and the rule editor of the
 * selected zone: nine ghost slots that read through the controller's rules.
 */
public class DispatchStationMenu extends AbstractContainerMenu {

    public static final int BUFFER_Y = 18;
    public static final int GHOST_Y = 92;
    public static final int INVENTORY_Y = 150;
    public static final int HOTBAR_Y = 208;
    private static final int COLUMNS = 9;
    private static final int SYNC_INTERVAL = 5;

    private final DispatchStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final Player player;
    private final int bufferSlots;
    private final int ghostStart;
    private final int inventoryStart;
    /** The client's copy of the selected zone's filter, filled by vanilla slot sync. */
    private final ItemStackHandler clientFilter = new ItemStackHandler(ZoneRules.FILTER_SLOTS);
    private String selected = "";
    private int syncTimer;
    private int lastSignature = Integer.MIN_VALUE;

    public DispatchStationMenu(int containerId, Inventory playerInventory, DispatchStationBlockEntity station) {
        super(ServusStorageMenus.DISPATCH_STATION.get(), containerId);
        this.station = station;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        this.bufferSlots = station.buffer().getSlots();
        for (int slot = 0; slot < bufferSlots; slot++) {
            addSlot(new SlotItemHandler(station.buffer(), slot, 8 + (slot % COLUMNS) * 18, BUFFER_Y + (slot / COLUMNS) * 18));
        }
        ghostStart = slots.size();
        for (int i = 0; i < ZoneRules.FILTER_SLOTS; i++) {
            addSlot(new GhostSlot(this::currentFilter, i, 8 + i * 18, GHOST_Y));
        }
        inventoryStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }
    }

    public static DispatchStationMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof DispatchStationBlockEntity station) {
            return new DispatchStationMenu(containerId, playerInventory, station);
        }
        throw new IllegalStateException("No dispatch station at " + pos);
    }

    public DispatchStationBlockEntity station() {
        return station;
    }

    /** Index of the first ghost slot. */
    public int ghostStart() {
        return ghostStart;
    }

    /** Index of the first player inventory slot. */
    public int inventoryStart() {
        return inventoryStart;
    }

    /** The selected zone key, empty for none. */
    public String selected() {
        return selected;
    }

    private Optional<WarehouseNetwork> network() {
        return station.network();
    }

    private Optional<ZoneRules> rules() {
        return network().filter(network -> network.status() == WarehouseNetwork.Status.ONLINE).flatMap(WarehouseNetwork::rules);
    }

    /** The filter handler the ghost slots show: the selected zone's rule on the server, the synced copy on the client. */
    private IItemHandler currentFilter() {
        if (player.level().isClientSide()) {
            return clientFilter;
        }
        if (selected.isEmpty()) {
            return null;
        }
        return rules().map(rules -> rules.get(selected)).map(ZoneRules.Entry::filter).orElse(null);
    }

    // Server side edits

    public void selectZone(String key) {
        String zoneKey = ZoneNames.key(key);
        if (zoneKey.equals(selected)) {
            return;
        }
        selected = zoneKey;
        if (!selected.isEmpty()) {
            network().ifPresent(network -> rules().ifPresent(rules -> rules.entry(selected, network.zoneDisplay(selected))));
        }
        forceSync();
    }

    public void setTags(String line) {
        if (selected.isEmpty()) {
            return;
        }
        rules().ifPresent(rules -> rules.setTags(selected, line));
        forceSync();
    }

    public void setOverflow(boolean overflow) {
        if (selected.isEmpty()) {
            return;
        }
        rules().ifPresent(rules -> rules.setOverflow(selected, overflow));
        forceSync();
    }

    private void forceSync() {
        lastSignature = Integer.MIN_VALUE;
        syncTimer = SYNC_INTERVAL;
    }

    // Clicks

    /** Ghost slots take a copy of the cursor and never hand anything out. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= ghostStart && slotId < inventoryStart && slots.get(slotId) instanceof GhostSlot ghost) {
            if (!player.level().isClientSide() && currentFilter() != null) {
                ghost.setGhost(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Shift click moves between the buffer and the player inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= ghostStart && index < inventoryStart) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < bufferSlots
                ? moveItemStackTo(stack, inventoryStart, slots.size(), true)
                : moveItemStackTo(stack, 0, bufferSlots, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ServusStorageBlocks.DISPATCH_STATION.get());
    }

    // Sync

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
        ZoneRulesSyncPayload payload = state();
        int signature = payload.hashCode();
        if (signature != lastSignature) {
            lastSignature = signature;
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    /** The zones, the selected rule and the station's state as the client should see them. */
    public ZoneRulesSyncPayload state() {
        Optional<WarehouseNetwork> network = network();
        String blockedReason = station.blockedReason() == null ? "" : station.blockedReason().name();
        ItemStack blockedStack = station.blockedStack().copy();
        if (network.isEmpty()) {
            return new ZoneRulesSyncPayload(false, "not_connected", List.of(), "", "", false, blockedReason, blockedStack, station.blockedZone(), station.isEmpty());
        }
        WarehouseNetwork warehouse = network.get();
        return switch (warehouse.status()) {
            case NO_CONTROLLER -> new ZoneRulesSyncPayload(false, "no_controller", List.of(), "", "", false, blockedReason, blockedStack, station.blockedZone(), station.isEmpty());
            case MULTIPLE_CONTROLLERS -> new ZoneRulesSyncPayload(false, "multiple_controllers", List.of(), "", "", false, blockedReason, blockedStack, station.blockedZone(), station.isEmpty());
            case ONLINE -> {
                Optional<ZoneRules> rules = warehouse.rules();
                List<ZoneRow> rows = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : warehouse.zoneBlockCounts().entrySet()) {
                    ZoneRules.Entry rule = rules.map(r -> r.get(entry.getKey())).orElse(null);
                    rows.add(new ZoneRow(entry.getKey(), warehouse.zoneDisplay(entry.getKey()), entry.getValue(),
                            rule != null && !rule.tags().isEmpty() || rule != null && hasItems(rule), rule != null && rule.overflow()));
                }
                ZoneRules.Entry current = rules.map(r -> r.get(selected)).orElse(null);
                yield new ZoneRulesSyncPayload(true, "", rows, selected,
                        current == null ? "" : current.tagLine(), current != null && current.overflow(),
                        blockedReason, blockedStack, station.blockedZone(), station.isEmpty());
            }
        };
    }

    private static boolean hasItems(ZoneRules.Entry rule) {
        for (int i = 0; i < rule.filter().getSlots(); i++) {
            if (!rule.filter().getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
