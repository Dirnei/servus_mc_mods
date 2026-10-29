package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackSectionHandler;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.SectionFill;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.zone.WarehouseZoneData;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** What the handheld does on the server: bind, scan in ZONE, FIND and INFO, and the answers for the panel. */
public final class HandheldActions {

    private static final String MESSAGE = "message.servus_shuttles.handheld.";
    /** Ticks without a scan after which a held sweep ends. */
    private static final int SWEEP_QUIET_TICKS = 10;
    private static final int INFO_TOP_ITEMS = 3;
    /** How far from the player an INFO request may ask, in blocks. */
    private static final int INFO_RANGE = 64;
    private static final SweepTracker<UUID, BlockPos> SWEEPS = new SweepTracker<>(SWEEP_QUIET_TICKS);

    private HandheldActions() {
    }

    public static HandheldMode mode(ItemStack handheld) {
        return handheld.getOrDefault(ServusShuttlesComponents.HANDHELD_MODE.get(), HandheldMode.ZONE);
    }

    /** Steps the mode, and drops a binding whose controller is gone so FIND says so. */
    public static void stepMode(Player player, ItemStack handheld, int step) {
        handheld.set(ServusShuttlesComponents.HANDHELD_MODE.get(), mode(handheld).step(step));
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

    /** A right click on a bay: what it does depends on the mode. */
    public static void scan(Player player, ItemStack handheld, StorageNodeBlockEntity bay) {
        switch (mode(handheld)) {
            case ZONE -> assign(player, handheld, bay, player.isShiftKeyDown());
            case FIND -> findFromBay(player, handheld, bay);
            case INFO -> pin(handheld, bay.getBlockPos());
        }
    }

    /** The active zone id, or empty when none was chosen or the chosen zone was deleted. */
    public static Optional<Integer> activeZone(MinecraftServer server, ItemStack handheld) {
        Integer id = handheld.get(ServusShuttlesComponents.HANDHELD_ZONE.get());
        if (id == null || id != ZoneList.NONE && WarehouseZoneData.get(server).definition(id) == null) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    /** ZONE: puts the bay, its section when sneaking, or a Fluid Rack's whole section into the active zone. */
    static void assign(Player player, ItemStack handheld, StorageNodeBlockEntity bay, boolean section) {
        MinecraftServer server = player.level().getServer();
        Optional<Integer> active = activeZone(server, handheld);
        if (active.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(MESSAGE + "choose_zone"));
            return;
        }
        int zone = active.get();
        Level level = bay.getLevel();
        BlockPos pos = bay.getBlockPos();
        int index = SWEEPS.scan(player.getUUID(), pos, level.getGameTime());
        if (index == SweepTracker.SKIP) {
            return;
        }
        List<StorageNodeBlockEntity> bays = targets(level, bay, section);
        String before = zoneName(bay.zoneDefinition());
        if (bays.stream().allMatch(target -> target.zoneDefinition() == null ? zone == ZoneList.NONE : target.zoneId() == zone)) {
            if (index == 0) {
                send(player, new HandheldScanPayload(List.of(pos), false, 0));
            }
            return;
        }
        if (bay instanceof FluidRackBlockEntity) {
            FluidRackBlock.stampSection(level, pos, zone);
        } else {
            bays.forEach(target -> target.setZone(zone));
        }
        String after = zoneName(WarehouseZoneData.get(server).definition(zone));
        Component message = bays.size() > 1
                ? Component.translatable(MESSAGE + "assigned_section", bays.size(), before, after)
                : Component.translatable(MESSAGE + "assigned", code(bay), before, after);
        player.sendOverlayMessage(message);
        send(player, new HandheldScanPayload(bays.stream().map(StorageNodeBlockEntity::getBlockPos).toList(), true, index));
    }

    /** The bays a ZONE scan sets: a Fluid Rack's section always, a rack's section when sneaking, else the bay. */
    private static List<StorageNodeBlockEntity> targets(Level level, StorageNodeBlockEntity bay, boolean section) {
        List<BlockPos> positions;
        if (bay instanceof FluidRackBlockEntity) {
            positions = FluidRackBlock.section(level, bay.getBlockPos());
        } else if (section && bay instanceof HeavyRackBlockEntity) {
            positions = HeavyRackBlock.section(level, bay.getBlockPos());
        } else {
            return List.of(bay);
        }
        List<StorageNodeBlockEntity> bays = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof StorageNodeBlockEntity node && node.zonable()) {
                bays.add(node);
            }
        }
        return bays.isEmpty() ? List.of(bay) : bays;
    }

    private static String zoneName(ZoneDefinition zone) {
        return zone == null ? Component.translatable(MESSAGE + "no_zone").getString() : zone.name();
    }

    private static String code(StorageNodeBlockEntity bay) {
        return bay.locationCode().isEmpty() ? "--" : bay.locationCode();
    }

    /** FIND by scanning a bay: searches for the item the bay holds most. */
    static void findFromBay(Player player, ItemStack handheld, StorageNodeBlockEntity bay) {
        ItemKey top = bay instanceof HeavyRackBlockEntity rack ? topItem(rack) : null;
        if (top == null) {
            send(player, new HandheldScanPayload(List.of(bay.getBlockPos()), false, 0));
            return;
        }
        find(player, handheld, top);
    }

    private static ItemKey topItem(HeavyRackBlockEntity rack) {
        List<SectionFill.Entry<ItemKey>> top = SectionFill.of(rack.items().states(), 1).top();
        return top.isEmpty() ? null : top.get(0).key();
    }

    /** FIND: every loaded bay of the bound warehouse holding the item glows on the player's screen. */
    public static void find(Player player, ItemStack handheld, ItemKey key) {
        Optional<WarehouseNetwork> network = boundNetwork(player.level().getServer(), handheld);
        if (network.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(MESSAGE + "not_bound"));
            return;
        }
        handheld.set(ServusShuttlesComponents.HANDHELD_FIND.get(), key.stack(1));
        Found found = found(network.get(), key);
        int glowTicks = ShuttlesStatsManager.INSTANCE.handheldFindGlowSeconds() * 20;
        send(player, new HandheldFindResultPayload(key.stack(1), found.total(), found.bays(), found.codes(), glowTicks));
    }

    /** The loaded bays of a warehouse holding an item, their codes and the warehouse's total. */
    public record Found(long total, List<BlockPos> bays, List<String> codes) {
    }

    public static Found found(WarehouseNetwork network, ItemKey key) {
        List<BlockPos> bays = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        ServerLevel level = network.level();
        for (BlockPos pos : network.index().sourcesHolding(key)) {
            if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof StorageNodeBlockEntity bay) {
                bays.add(pos);
                codes.add(code(bay));
            }
        }
        return new Found(network.index().count(key), bays, codes);
    }

    /** INFO: pins the bay, or unpins it when it is the pinned one. */
    static void pin(ItemStack handheld, BlockPos pos) {
        if (pos.equals(handheld.get(ServusShuttlesComponents.HANDHELD_PIN.get()))) {
            handheld.remove(ServusShuttlesComponents.HANDHELD_PIN.get());
        } else {
            handheld.set(ServusShuttlesComponents.HANDHELD_PIN.get(), pos);
        }
    }

    /** INFO: what the bay at {@code pos} holds, or null when there is no bay. Changes nothing. */
    public static HandheldInfoPayload info(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof StorageNodeBlockEntity bay) || !bay.zonable()) {
            return null;
        }
        int zone = bay.zoneDefinition() == null ? ZoneList.NONE : bay.zoneId();
        if (bay instanceof FluidRackBlockEntity fluidRack) {
            FluidRackSectionHandler.Content content = fluidRack.sectionHandler().content();
            FluidStack fluid = content.fluid();
            List<ItemStack> top = fluid.isEmpty() ? List.of() : List.of(fluid.getFluid().getBucket().getDefaultInstance());
            List<Long> counts = fluid.isEmpty() ? List.of() : List.of((long) fluid.getAmount());
            return new HandheldInfoPayload(pos, zone, fluid.getAmount(), content.capacity(), top, counts, true);
        }
        if (!(bay instanceof HeavyRackBlockEntity rack)) {
            return null;
        }
        SectionFill<ItemKey> fill = SectionFill.of(rack.items().states(), INFO_TOP_ITEMS);
        List<ItemStack> top = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (SectionFill.Entry<ItemKey> entry : fill.top()) {
            top.add(entry.key().stack(1));
            counts.add((long) entry.count());
        }
        return new HandheldInfoPayload(pos, zone, fill.used(), fill.total(), top, counts, false);
    }

    /** Answers an INFO request when the player holds a handheld and the bay is in reach of sight. */
    public static void answerInfo(Player player, BlockPos pos) {
        if (!(player.getMainHandItem().getItem() instanceof HandheldItem) && !(player.getOffhandItem().getItem() instanceof HandheldItem)) {
            return;
        }
        if (player.blockPosition().distSqr(pos) > INFO_RANGE * INFO_RANGE) {
            return;
        }
        HandheldInfoPayload info = info(player.level(), pos);
        if (info != null) {
            send(player, info);
        }
    }

    /** Loaded bays per zone id over every warehouse of every loaded dimension. */
    public static Map<Integer, Integer> loadedBayCounts(MinecraftServer server) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (WarehouseNetwork network : NetworkManager.of(level).set(WarehouseNetwork.TYPE).networks()) {
                network.zoneBlockCountsById().forEach((id, count) -> counts.merge(id, count, Integer::sum));
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
