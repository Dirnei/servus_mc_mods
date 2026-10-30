package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackShelfHandler;
import io.github.dirnei.mc.servus.shuttles.menu.HandheldMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.ShelfFill;
import io.github.dirnei.mc.servus.shuttles.rack.ShelfStock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.zone.ShelfZones;
import io.github.dirnei.mc.servus.shuttles.zone.WarehouseZoneData;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What the handheld does on the server: bind, scan a shelf in ZONE, FIND and
 * INFO, and the answers for the panel. Every scan covers the whole shelf of
 * the clicked bay; a held sweep counts shelves, keyed by their leftmost bay.
 */
public final class HandheldActions {

    private static final String MESSAGE = "message.servus_shuttles.handheld.";
    /** Ticks without a scan after which a held sweep ends. */
    private static final int SWEEP_QUIET_TICKS = 10;
    private static final SweepTracker<UUID, BlockPos> SWEEPS = new SweepTracker<>(SWEEP_QUIET_TICKS);

    private HandheldActions() {
    }

    public static HandheldMode mode(ItemStack handheld) {
        return handheld.getOrDefault(ServusShuttlesComponents.HANDHELD_MODE.get(), HandheldMode.ZONE);
    }

    /** Steps the mode, and drops a binding whose controller is gone so FIND says so. */
    public static void stepMode(Player player, ItemStack handheld, int step) {
        setMode(player, handheld, mode(handheld).step(step));
    }

    /**
     * Switches to that mode, ends pick target mode with its relocation, empties
     * the shelf basket, and drops a binding whose controller is gone so FIND says so.
     */
    public static void setMode(Player player, ItemStack handheld, HandheldMode mode) {
        handheld.set(ServusShuttlesComponents.HANDHELD_MODE.get(), mode);
        HandheldRelocation.stopPickingTarget(handheld);
        HandheldRelocation.leaveShelf(handheld);
        boundNetwork(player.level().getServer(), handheld);
    }

    /** Binds the handheld to the controller's warehouse. */
    public static void bind(Player player, ItemStack handheld, WarehouseControllerBlockEntity controller) {
        String name = controller.displayName();
        handheld.set(ServusShuttlesComponents.HANDHELD_BINDING.get(),
                new HandheldBinding(GlobalPos.of(controller.getLevel().dimension(), controller.getBlockPos()), name));
        player.sendOverlayMessage(Component.translatable(MESSAGE + "bound", name));
    }

    /**
     * The warehouse the handheld is bound to, when its controller still
     * stands loaded there; a binding whose controller is gone is dropped.
     */
    public static Optional<WarehouseNetwork> boundNetwork(MinecraftServer server, ItemStack handheld) {
        HandheldBinding binding = handheld.get(ServusShuttlesComponents.HANDHELD_BINDING.get());
        if (binding == null) {
            return Optional.empty();
        }
        ServerLevel level = server.getLevel(binding.controller().dimension());
        BlockPos pos = binding.controller().pos();
        if (level == null || !level.isLoaded(pos)) {
            return Optional.empty();
        }
        if (!(level.getBlockEntity(pos) instanceof WarehouseControllerBlockEntity controller)) {
            handheld.remove(ServusShuttlesComponents.HANDHELD_BINDING.get());
            return Optional.empty();
        }
        return controller.network();
    }

    /**
     * A mode payload from the player: switches the handheld of the open
     * handheld GUI, else the one in the main hand.
     */
    public static void applyMode(Player player, HandheldModePayload payload) {
        ItemStack handheld = player.containerMenu instanceof HandheldMenu menu ? menu.handheld() : player.getMainHandItem();
        if (handheld.getItem() instanceof HandheldItem) {
            setMode(player, handheld, payload.next(mode(handheld)));
        }
    }

    /**
     * A right click on a bay scans its shelf; what that does depends on the
     * mode. A held repeat on the shelf scanned last does nothing.
     *
     * @return the shelf's place in a held sweep, 0 for the first, or {@link SweepTracker#SKIP} for a repeat
     */
    public static int scan(Player player, ItemStack handheld, StorageNodeBlockEntity bay) {
        Level level = bay.getLevel();
        int index = SWEEPS.scan(player.getUUID(), ShelfZones.key(level, bay.getBlockPos()), level.getGameTime());
        if (index == SweepTracker.SKIP) {
            return index;
        }
        switch (mode(handheld)) {
            case ZONE -> assign(player, handheld, bay, index);
            case FIND -> {
                if (HandheldRelocation.pickingTarget(handheld)) {
                    HandheldRelocation.scanTarget(player, handheld, bay);
                } else {
                    findFromBay(player, handheld, bay);
                }
            }
            case INFO -> infoScan(player, handheld, bay);
        }
        return index;
    }

    /** The active zone id, or empty when none was chosen or the chosen zone was deleted. */
    public static Optional<Integer> activeZone(MinecraftServer server, ItemStack handheld) {
        Integer id = handheld.get(ServusShuttlesComponents.HANDHELD_ZONE.get());
        if (id == null || id != ZoneList.NONE && WarehouseZoneData.get(server).definition(id) == null) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    /**
     * ZONE: puts the shelf of the bay into the active zone. {@code index} is
     * the shelf's place in a held sweep; a shelf already in the zone buzzes
     * only when it is the first of the sweep.
     */
    static void assign(Player player, ItemStack handheld, StorageNodeBlockEntity bay, int index) {
        MinecraftServer server = player.level().getServer();
        Optional<Integer> active = activeZone(server, handheld);
        if (active.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(MESSAGE + "choose_zone"));
            return;
        }
        int zone = active.get();
        Level level = bay.getLevel();
        List<BlockPos> shelf = shelfOf(bay);
        String before = zoneName(bay.zoneDefinition());
        if (ShelfZones.bays(level, bay.getBlockPos()).stream()
                .allMatch(target -> target.zoneDefinition() == null ? zone == ZoneList.NONE : target.zoneId() == zone)) {
            if (index == 0) {
                send(player, new HandheldScanPayload(shelf, false, 0));
            }
            return;
        }
        ShelfZones.set(level, bay.getBlockPos(), zone);
        String after = zoneName(WarehouseZoneData.get(server).definition(zone));
        player.sendOverlayMessage(Component.translatable(MESSAGE + "assigned", code(bay), before, after));
        send(player, new HandheldScanPayload(shelf, true, index));
    }

    /** Every bay of the shelf of {@code bay}, from left to right. */
    private static List<BlockPos> shelfOf(StorageNodeBlockEntity bay) {
        return ShelfZones.shelf(bay.getLevel(), bay.getBlockPos());
    }

    private static String zoneName(ZoneDefinition zone) {
        return zone == null ? Component.translatable(MESSAGE + "no_zone").getString() : zone.name();
    }

    private static String code(StorageNodeBlockEntity bay) {
        return bay.locationCode().isEmpty() ? "--" : bay.locationCode();
    }

    /**
     * FIND by scanning a bay: flashes its shelf and opens the GUI with the
     * shelf's contents in the FIND list; a shelf basket of another shelf is emptied.
     */
    static void findFromBay(Player player, ItemStack handheld, StorageNodeBlockEntity bay) {
        List<BlockPos> shelf = shelfOf(bay);
        HandheldRelocation.enterShelf(handheld, shelf.getFirst());
        send(player, new HandheldScanPayload(shelf, true, 0));
        if (player instanceof ServerPlayer serverPlayer) {
            InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND) == handheld ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            HandheldItem.openGui(serverPlayer, hand, bay.getBlockPos());
        }
    }

    /** FIND: every loaded shelf of the bound warehouse holding the item glows on the player's screen. */
    public static void find(Player player, ItemStack handheld, ItemKey key) {
        Optional<WarehouseNetwork> network = boundNetwork(player.level().getServer(), handheld);
        if (network.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(MESSAGE + "not_bound"));
            return;
        }
        Found found = found(network.get(), key);
        int glowTicks = ShuttlesStatsManager.INSTANCE.handheldFindGlowSeconds() * 20;
        send(player, new HandheldFindResultPayload(key.stack(1), found.total(), found.shelves(), found.codes(), glowTicks));
    }

    /** The loaded shelves of a warehouse holding an item, each by its leftmost bay, their codes and the warehouse's total. */
    public record Found(long total, List<BlockPos> shelves, List<String> codes) {
    }

    public static Found found(WarehouseNetwork network, ItemKey key) {
        Map<BlockPos, String> shelves = new LinkedHashMap<>();
        ServerLevel level = network.level();
        for (BlockPos pos : network.index().sourcesHolding(key)) {
            if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof StorageNodeBlockEntity bay) {
                shelves.putIfAbsent(ShelfZones.key(level, pos), code(bay));
            }
        }
        return new Found(network.index().count(key), List.copyOf(shelves.keySet()), List.copyOf(shelves.values()));
    }

    /** INFO: flashes the shelf, keeps the bay as the last scanned one and sends what the shelf holds. */
    static void infoScan(Player player, ItemStack handheld, StorageNodeBlockEntity bay) {
        HandheldInfoPayload info = info(bay.getLevel(), bay.getBlockPos());
        if (info == null) {
            return;
        }
        handheld.set(ServusShuttlesComponents.HANDHELD_PIN.get(), bay.getBlockPos());
        send(player, new HandheldScanPayload(shelfOf(bay), true, 0));
        send(player, info);
    }

    /** Sends the INFO readout of the last scanned bay again, when it is still a loaded bay. */
    public static void resendInfo(ServerPlayer player, ItemStack handheld) {
        BlockPos pos = handheld.get(ServusShuttlesComponents.HANDHELD_PIN.get());
        HandheldInfoPayload info = pos == null ? null : info(player.level(), pos);
        if (info != null) {
            send(player, info);
        }
    }

    /** INFO: everything the shelf of the bay at {@code pos} holds, largest first, or null when there is no bay. Changes nothing. */
    public static HandheldInfoPayload info(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof StorageNodeBlockEntity bay) || !bay.zonable()) {
            return null;
        }
        int zone = bay.zoneDefinition() == null ? ZoneList.NONE : bay.zoneId();
        String code = bay.locationCode();
        if (bay instanceof FluidRackBlockEntity fluidRack) {
            FluidRackShelfHandler.Content content = fluidRack.shelfHandler().content();
            FluidStack fluid = content.fluid();
            List<ItemStack> top = fluid.isEmpty() ? List.of() : List.of(fluid.getFluid().getBucket().getDefaultInstance());
            List<Long> counts = fluid.isEmpty() ? List.of() : List.of((long) fluid.getAmount());
            return new HandheldInfoPayload(pos, code, zone, fluid.getAmount(), content.capacity(), top, counts, true);
        }
        Optional<ShelfStock> stock = ShelfStock.of(level, pos, Integer.MAX_VALUE);
        if (stock.isEmpty()) {
            return null;
        }
        ShelfFill<ItemKey> fill = stock.get().fill();
        List<ItemStack> top = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (ShelfFill.Entry<ItemKey> entry : fill.top()) {
            top.add(entry.key().stack(1));
            counts.add((long) entry.count());
        }
        return new HandheldInfoPayload(pos, code, zone, fill.used(), fill.total(), top, counts, false);
    }

    /** Loaded shelves per zone id over every warehouse of every loaded dimension. */
    public static Map<Integer, Integer> loadedShelfCounts(MinecraftServer server) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (WarehouseNetwork network : NetworkManager.of(level).set(WarehouseNetwork.TYPE).networks()) {
                network.zoneShelfCountsById().forEach((id, count) -> counts.merge(id, count, Integer::sum));
            }
        }
        return counts;
    }

    private static void send(Player player, CustomPacketPayload payload) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    /** Forgets a player's sweep, for example on logout. */
    public static void forget(UUID player) {
        SWEEPS.forget(player);
    }
}
