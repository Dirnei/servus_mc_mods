package io.github.dirnei.mc.servus.shuttles.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.PlayerInventorySlots;
import io.github.dirnei.mc.servus.foundation.team.Teams;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldActions;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMenuStatePayload;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMenus;
import io.github.dirnei.mc.servus.shuttles.zone.WarehouseZoneData;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The handheld's GUI: the team's zone list with the zone form, and the bound
 * warehouse's stock for FIND. Holds only the player inventory, so items can be
 * picked up for the form's ghost slots, which the screen keeps itself.
 */
public class HandheldMenu extends AbstractContainerMenu {

    public static final int INVENTORY_X = 48;
    public static final int INVENTORY_Y = 174;
    /** How many stock rows the FIND list gets at most. */
    private static final int STOCK_LIMIT = 500;

    private final InteractionHand hand;
    private final Player player;
    private final UUID team;
    private Map<Integer, Integer> bayCounts = Map.of();
    private boolean bound;
    private List<ItemStack> stock = List.of();
    private List<Long> stockCounts = List.of();

    public HandheldMenu(int containerId, Inventory playerInventory, InteractionHand hand, UUID team) {
        super(ServusShuttlesMenus.HANDHELD.get(), containerId);
        this.hand = hand;
        this.player = playerInventory.player;
        this.team = team;
        PlayerInventorySlots.add(this::addSlot, playerInventory, INVENTORY_X, INVENTORY_Y);
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

    public int bayCount(int zoneId) {
        return bayCounts.getOrDefault(zoneId, 0);
    }

    public boolean bound() {
        return bound;
    }

    public List<ItemStack> stock() {
        return stock;
    }

    public List<Long> stockCounts() {
        return stockCounts;
    }

    /** Client: takes the state the server sent. */
    public void acceptState(HandheldMenuStatePayload payload) {
        bayCounts = Map.copyOf(payload.bayCounts());
        bound = payload.bound();
        stock = List.copyOf(payload.items());
        stockCounts = List.copyOf(payload.counts());
    }

    /** Server: sends bay counts and the bound warehouse's stock. */
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
        PacketDistributor.sendToPlayer(player, new HandheldMenuStatePayload(HandheldActions.loadedBayCounts(player.level().getServer()),
                network.isPresent(), items, counts));
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

    /** Server: FIND for an item picked in the list. */
    public void find(ServerPlayer player, ItemStack item) {
        if (!item.isEmpty()) {
            HandheldActions.find(player, handheld(), ItemKey.of(item));
        }
    }

    private static boolean ownZone(ServerPlayer player, int id) {
        ZoneList.Zone<ZoneFilter> zone = WarehouseZoneData.get(player.level().getServer()).list().get(id);
        return zone != null && zone.team().equals(Teams.teamOf(player));
    }

    /** One of each ghost item, at most nine, and tags parsed again. */
    private static ZoneFilter clean(ZoneFilter filter) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : filter.items()) {
            if (!stack.isEmpty() && items.size() < ZoneFilter.SLOTS) {
                items.add(stack.copyWithCount(1));
            }
        }
        return new ZoneFilter(items, ZoneFilter.parseTags(String.join(",", filter.tags())));
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
