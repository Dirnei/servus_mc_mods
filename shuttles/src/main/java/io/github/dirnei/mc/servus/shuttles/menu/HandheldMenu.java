package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldActions;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBasketPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMenuStatePayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldOrders;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldRelocation;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.zone.ShelfZones;
import io.github.dirnei.mc.servus.shuttles.zone.WarehouseZoneData;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The handheld's GUI: the team's zone list with the zone form in ZONE, the
 * last scanned shelf with its shelf basket in INFO, and the bound
 * warehouse's stock with the warehouse basket in STOCK. It holds no slots;
 * the zone form picks its filter in an item picker.
 */
public class HandheldMenu extends AbstractContainerMenu {

    /** How many stock rows the STOCK list gets at most. */
    private static final int STOCK_LIMIT = 500;

    private final InteractionHand hand;
    private final Player player;
    private final UUID team;
    private Map<Integer, Integer> shelfCounts = Map.of();
    private boolean bound;
    private List<ItemStack> stock = List.of();
    private List<Long> stockCounts = List.of();

    public HandheldMenu(int containerId, Inventory playerInventory, InteractionHand hand, UUID team) {
        super(ServusShuttlesMenus.HANDHELD.get(), containerId);
        this.hand = hand;
        this.player = playerInventory.player;
        this.team = team;
    }

    public static HandheldMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new HandheldMenu(containerId, playerInventory, buf.readEnum(InteractionHand.class), UUIDUtil.STREAM_CODEC.decode(buf));
    }

    /** What the client needs to build this menu. */
    public static void write(RegistryFriendlyByteBuf buf, InteractionHand hand, UUID team) {
        buf.writeEnum(hand);
        UUIDUtil.STREAM_CODEC.encode(buf, team);
    }

    public ItemStack handheld() {
        return player.getItemInHand(hand);
    }

    /** The team whose zones this GUI lists. */
    public UUID team() {
        return team;
    }

    /** The active zone id, or null when none was chosen. */
    public Integer activeZone() {
        return handheld().get(ServusShuttlesComponents.HANDHELD_ZONE.get());
    }

    /** How many loaded shelves are in the zone. */
    public int shelfCount(int zoneId) {
        return shelfCounts.getOrDefault(zoneId, 0);
    }

    public boolean bound() {
        return bound;
    }

    /** The bound warehouse's stock for STOCK, largest first. */
    public List<ItemStack> stock() {
        return stock;
    }

    public List<Long> stockCounts() {
        return stockCounts;
    }

    /** Client: takes the state the server sent. */
    public void acceptState(HandheldMenuStatePayload payload) {
        shelfCounts = Map.copyOf(payload.shelfCounts());
        bound = payload.bound();
        stock = List.copyOf(payload.items());
        stockCounts = List.copyOf(payload.counts());
    }

    /** Server: sends shelf counts, the stock of the bound warehouse, and the last INFO readout. */
    public void sendState(ServerPlayer player) {
        Optional<WarehouseNetwork> network = HandheldActions.boundNetwork(player.level().getServer(), handheld());
        List<ItemStack> items = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        network.ifPresent(warehouse -> {
            List<Map.Entry<ItemKey, Long>> entries = new ArrayList<>(warehouse.index().entries());
            entries.sort(Map.Entry.<ItemKey, Long>comparingByValue().reversed());
            for (Map.Entry<ItemKey, Long> entry : entries.subList(0, Math.min(STOCK_LIMIT, entries.size()))) {
                items.add(entry.getKey().stack(1));
                counts.add(entry.getValue());
            }
        });
        PacketDistributor.sendToPlayer(player, new HandheldMenuStatePayload(HandheldActions.loadedShelfCounts(player.level().getServer()),
                network.isPresent(), items, counts));
        HandheldActions.resendInfo(player, handheld());
    }

    /** Server: makes a zone of the player's team, or "No zone" for 0, the active one. */
    public void pick(ServerPlayer player, int id) {
        if (id == ZoneList.NONE || ownZone(player, id)) {
            handheld().set(ServusShuttlesComponents.HANDHELD_ZONE.get(), id);
        }
    }

    /** Server: creates a zone ({@code id} 0) or edits one of the player's team; a created zone becomes the active one. */
    public void save(ServerPlayer player, int id, String name, int colour, ZoneFilter filter) {
        WarehouseZoneData data = WarehouseZoneData.get(player.level().getServer());
        ZoneFilter clean = clean(filter);
        if (id == ZoneList.NONE) {
            data.create(Teams.teamOf(player), name, colour, clean)
                    .ifPresent(zone -> handheld().set(ServusShuttlesComponents.HANDHELD_ZONE.get(), zone.id()));
        } else if (ownZone(player, id)) {
            data.edit(id, name, colour, clean);
        }
        sendState(player);
    }

    /** Server: deletes a zone of the player's team. */
    public void delete(ServerPlayer player, int id) {
        if (ownZone(player, id)) {
            WarehouseZoneData.get(player.level().getServer()).delete(id);
        }
        sendState(player);
    }

    /** Server: finds an item picked in the INFO or STOCK list. */
    public void find(ServerPlayer player, ItemStack item) {
        if (!item.isEmpty()) {
            HandheldActions.find(player, handheld(), ItemKey.of(item));
        }
    }

    /**
     * Server: the key of the Heavy Duty Rack shelf INFO shows, the shelf of
     * the last scanned bay; null outside INFO or when that bay is gone.
     */
    public @Nullable BlockPos shelfKey() {
        BlockPos pin = handheld().get(ServusShuttlesComponents.HANDHELD_PIN.get());
        Level level = player.level();
        if (HandheldActions.mode(handheld()) != HandheldMode.INFO || pin == null || !level.isLoaded(pin)
                || !(level.getBlockEntity(pin) instanceof HeavyRackBlockEntity)) {
            return null;
        }
        return ShelfZones.key(level, pin);
    }

    /** Server: an edit of the basket of the screen shown, the shelf basket in INFO or the warehouse basket in STOCK. */
    public void editBasket(Player player, HandheldBasketPayload payload) {
        switch (HandheldActions.mode(handheld())) {
            case STOCK -> HandheldRelocation.edit(player, handheld(), null, payload);
            case INFO -> {
                BlockPos shelf = shelfKey();
                if (shelf != null) {
                    HandheldRelocation.edit(player, handheld(), shelf, payload);
                }
            }
            case ZONE -> {
            }
        }
    }

    /**
     * Server: Relocate from the shelf INFO shows stashes its basket, puts the handheld into pick target mode and
     * closes the GUI; refused outside INFO, without entries or without binding.
     *
     * @return whether pick target mode started
     */
    public boolean relocate(Player player) {
        BlockPos shelf = shelfKey();
        if (shelf == null || !HandheldRelocation.relocate(player, handheld(), shelf)) {
            return false;
        }
        player.closeContainer();
        return true;
    }

    /** Server: REQUEST on STOCK, sends the lockers for the picker. */
    public void openPicker(ServerPlayer player) {
        if (HandheldActions.mode(handheld()) == HandheldMode.STOCK) {
            PacketDistributor.sendToPlayer(player, HandheldOrders.lockers(player, handheld()));
        }
    }

    /** Server: SEND in the locker picker, orders the STOCK basket to the locker at {@code dock}. */
    public void request(ServerPlayer player, BlockPos dock) {
        if (HandheldActions.mode(handheld()) == HandheldMode.STOCK) {
            HandheldOrders.request(player, handheld(), dock);
        }
    }

    private static boolean ownZone(ServerPlayer player, int id) {
        ZoneList.Zone<ZoneFilter> zone = WarehouseZoneData.get(player.level().getServer()).list().get(id);
        return zone != null && zone.team().equals(Teams.teamOf(player));
    }

    /** One of each filter item and at most nine of them, and at most nine tags, parsed again. */
    private static ZoneFilter clean(ZoneFilter filter) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : filter.items()) {
            if (!stack.isEmpty() && items.size() < ZoneFilter.SLOTS) {
                items.add(stack.copyWithCount(1));
            }
        }
        List<String> tags = ZoneFilter.parseTags(String.join(",", filter.tags()));
        return new ZoneFilter(items, tags.subList(0, Math.min(ZoneFilter.SLOTS, tags.size())));
    }

    /**
     * Also syncs the player's inventory: this menu has no slots, and the server
     * only syncs the open menu, so edits of the held handheld (basket, zone)
     * would not reach the screen until it closes.
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer) {
            player.inventoryMenu.broadcastChanges();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof HandheldItem;
    }
}
