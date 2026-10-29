package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.shuttles.zone.WarehouseZoneData;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;

/** Team zones, bays pointing at them, location codes and put-away by zone. */
public final class ZoneGameTests {

    private static final BlockPos ZL_RACK = new BlockPos(2, 1, 7);
    private static final BlockPos IZ_CONTROLLER = new BlockPos(6, 1, 10);
    private static final BlockPos IZ_ORES = new BlockPos(4, 1, 10);
    private static final BlockPos IZ_PLAIN = new BlockPos(5, 1, 10);
    private static final BlockPos IZ_SPARE = new BlockPos(7, 1, 10);
    private static final BlockPos IZ_IRON = new BlockPos(8, 1, 10);

    private ZoneGameTests() {
    }

    private static int zone(GameTestHelper helper, String name, List<ItemStack> items, List<String> tags) {
        return ZoneFixtures.zone(helper, name, new ZoneFilter(items, tags));
    }

    /** Stores a stack the way a shuttle put-away routes it, straight into each chosen rack; returns what found no room. */
    private static ItemStack store(WarehouseNetwork network, ItemStack stack) {
        ItemStack left = stack.copy();
        while (!left.isEmpty()) {
            Optional<BlockPos> target = network.putAwayTarget(left, pos -> true);
            if (target.isEmpty()) {
                break;
            }
            left = network.insertAt(target.get(), left);
        }
        return left;
    }

    /** The saved form of the team zones reads back the same zones, ids and counter. */
    @ServusGameTest(timeoutTicks = 20)
    public static void zonesSurviveSaveAndLoad(GameTestHelper helper) {
        WarehouseZoneData data = WarehouseZoneData.get(helper.getLevel().getServer());
        UUID team = UUID.randomUUID();
        int id = data.create(team, "Ores", 1, new ZoneFilter(List.of(new ItemStack(Items.RAW_IRON)), List.of("c:ores")))
                .orElseThrow().id();
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = WarehouseZoneData.CODEC.encodeStart(ops, data).getOrThrow();
        WarehouseZoneData loaded = WarehouseZoneData.CODEC.parse(ops, saved).getOrThrow();
        ZoneList.Zone<ZoneFilter> zone = loaded.list().get(id);
        helper.assertTrue(zone != null && zone.name().equals("Ores") && zone.team().equals(team) && zone.colour() == 1,
                "the zone reads back with name, team and colour, got " + zone);
        helper.assertTrue(zone.filter().tags().equals(List.of("c:ores")) && zone.filter().items().size() == 1
                && zone.filter().items().get(0).is(Items.RAW_IRON), "the filter reads back, got " + zone.filter());
        helper.assertTrue(loaded.list().nextId() == data.list().nextId(), "the id counter reads back");
        helper.succeed();
    }

    /** A bay keeps its zone id through the carrot dismantle and place cycle, and shows the zone in its title. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dismantleKeepsZone(GameTestHelper helper) {
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, ZL_RACK, Direction.NORTH);
        int food = zone(helper, "Food", List.of(new ItemStack(Items.BREAD)), List.of());
        rack.setZone(food);
        helper.assertTrue(rack.zone().equals("Food") && rack.getDisplayName().getString().endsWith("(Food)"),
                "the rack shows its zone, got " + rack.getDisplayName().getString());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack drop = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
        BlockEntityItems.saveToItem(rack, drop, helper.getLevel().registryAccess());
        helper.setBlock(ZL_RACK, Blocks.AIR.defaultBlockState());
        WarehouseFixtures.rack(helper, ZL_RACK, Direction.NORTH);
        BlockItem.updateCustomBlockEntityTag(helper.getLevel(), player, helper.absolutePos(ZL_RACK), drop);
        HeavyRackBlockEntity placed = helper.getBlockEntity(ZL_RACK, HeavyRackBlockEntity.class);
        helper.assertTrue(placed.zoneId() == food && placed.zone().equals("Food"), "the placed rack is in Food, got '" + placed.zone() + "'");
        helper.assertTrue(placed.zoneFilter().items().size() == 1, "the placed rack applies the zone's filter");
        helper.succeed();
    }

    /** A rack saved with a zone stamped by a Zone Label of an earlier version loads unzoned. */
    @ServusGameTest(timeoutTicks = 40)
    public static void oldStampedZoneLoadsUnzoned(GameTestHelper helper) {
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, ZL_RACK, Direction.NORTH);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        rack.saveCustomOnly(output);
        output.child("Zone").putString("Name", "Ores");
        ItemStack old = new ItemStack(ServusShuttlesBlocks.HEAVY_RACK.get());
        BlockItem.setBlockEntityData(old, rack.getType(), output);
        helper.setBlock(ZL_RACK, Blocks.AIR.defaultBlockState());
        WarehouseFixtures.rack(helper, ZL_RACK, Direction.NORTH);
        BlockItem.updateCustomBlockEntityTag(helper.getLevel(), helper.makeMockPlayer(GameType.SURVIVAL), helper.absolutePos(ZL_RACK), old);
        HeavyRackBlockEntity placed = helper.getBlockEntity(ZL_RACK, HeavyRackBlockEntity.class);
        helper.assertTrue(placed.zoneId() == ZoneList.NONE && placed.zoneDefinition() == null && placed.zone().isEmpty(),
                "the old stamped zone is dropped, got '" + placed.zone() + "'");
        helper.succeed();
    }

    /** Put-away follows the zones: exact items beat tags, filtered racks refuse other items, unzoned racks catch full zones. */
    @ServusGameTest(timeoutTicks = 200)
    public static void insertRespectsZones(GameTestHelper helper) {
        for (int x = 4; x <= 8; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 9));
        }
        WarehouseFixtures.controller(helper, IZ_CONTROLLER);
        for (BlockPos pos : List.of(IZ_ORES, IZ_PLAIN, IZ_SPARE, IZ_IRON)) {
            WarehouseFixtures.rack(helper, pos, Direction.NORTH);
        }
        HeavyRackBlockEntity ores = helper.getBlockEntity(IZ_ORES, HeavyRackBlockEntity.class);
        HeavyRackBlockEntity iron = helper.getBlockEntity(IZ_IRON, HeavyRackBlockEntity.class);
        HeavyRackBlockEntity plain = helper.getBlockEntity(IZ_PLAIN, HeavyRackBlockEntity.class);
        HeavyRackBlockEntity spare = helper.getBlockEntity(IZ_SPARE, HeavyRackBlockEntity.class);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(IZ_CONTROLLER, WarehouseControllerBlockEntity.class);
        String oresName = "Ores " + UUID.randomUUID().toString().substring(0, 6);
        String ironName = "Iron " + UUID.randomUUID().toString().substring(0, 6);
        ores.setZone(zone(helper, oresName, List.of(), List.of("c:ores")));
        iron.setZone(zone(helper, ironName, List.of(new ItemStack(Items.IRON_ORE)), List.of()));
        plain.items().insertItem(0, new ItemStack(Items.BREAD, 1), false);

        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.status() == WarehouseNetwork.Status.ONLINE, "warehouse should be online");
            helper.assertTrue(network.zoneBlockCounts().equals(Map.of(oresName, 1, ironName, 1)),
                    "two zones with one block each, got " + network.zoneBlockCounts());

            ItemStack left = store(network, new ItemStack(Items.IRON_ORE, 10));
            helper.assertTrue(left.isEmpty(), "iron ore should fit");
            helper.assertTrue(WarehouseFixtures.count(iron, Items.IRON_ORE) == 10, "iron ore should land in the Iron rack (exact beats tag), had " + WarehouseFixtures.count(iron, Items.IRON_ORE));
            helper.assertTrue(ores.isEmpty(), "the Ores rack should not take it while the exact match has room");

            left = store(network, new ItemStack(Items.COPPER_ORE, 10));
            helper.assertTrue(left.isEmpty(), "copper ore should fit");
            helper.assertTrue(WarehouseFixtures.count(ores, Items.COPPER_ORE) == 10, "copper ore should land in the Ores rack by tag");
            helper.assertTrue(WarehouseFixtures.count(plain, Items.COPPER_ORE) == 0 && spare.isEmpty(), "unzoned racks must not take ore while a zone has room");

            helper.runAfterDelay(10, () -> {
                ItemStack bread = store(network, new ItemStack(Items.BREAD, 5));
                helper.assertTrue(bread.isEmpty(), "bread should fit");
                helper.assertTrue(WarehouseFixtures.count(plain, Items.BREAD) == 6, "bread should join the unzoned rack already holding bread, had " + WarehouseFixtures.count(plain, Items.BREAD));

                WarehouseFixtures.fill(iron, Items.IRON_ORE);
                WarehouseFixtures.fill(ores, Items.COPPER_ORE);
                ItemStack ore = store(network, new ItemStack(Items.IRON_ORE, 7));
                helper.assertTrue(ore.isEmpty(), "with the zones full an unzoned rack takes the ore");
                helper.assertTrue(WarehouseFixtures.count(plain, Items.IRON_ORE) + WarehouseFixtures.count(spare, Items.IRON_ORE) == 7,
                        "the ore should be in the unzoned racks");

                WarehouseFixtures.fill(plain, Items.DIRT);
                WarehouseFixtures.fill(spare, Items.DIRT);
                ore = store(network, new ItemStack(Items.IRON_ORE, 7));
                helper.assertTrue(ore.getCount() == 7, "with everything full nothing is stored, left " + ore.getCount());
                helper.succeed();
            });
        });
    }

    /** Editing a zone's filter changes put-away at once, without touching the bay. */
    @ServusGameTest(timeoutTicks = 100)
    public static void editedFilterAppliesAtOnce(GameTestHelper helper) {
        for (int x = 4; x <= 6; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 9));
        }
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, IZ_CONTROLLER);
        HeavyRackBlockEntity zoned = WarehouseFixtures.rack(helper, IZ_ORES, Direction.NORTH);
        HeavyRackBlockEntity plain = WarehouseFixtures.rack(helper, IZ_PLAIN, Direction.NORTH);
        int ores = zone(helper, "Ores", List.of(), List.of("c:ores"));
        zoned.setZone(ores);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.putAwayTarget(new ItemStack(Items.COPPER_ORE), pos -> true).equals(Optional.of(zoned.getBlockPos())),
                    "copper ore goes to the Ores bay while the zone takes c:ores");
            WarehouseZoneData.get(helper.getLevel().getServer()).edit(ores, "Ores", 0,
                    new ZoneFilter(List.of(new ItemStack(Items.IRON_ORE)), List.of()));
            helper.assertTrue(network.putAwayTarget(new ItemStack(Items.COPPER_ORE), pos -> true).equals(Optional.of(plain.getBlockPos())),
                    "after the edit copper ore goes to the unzoned bay");
            helper.assertTrue(network.putAwayTarget(new ItemStack(Items.IRON_ORE), pos -> true).equals(Optional.of(zoned.getBlockPos())),
                    "iron ore goes to the Ores bay");
            helper.succeed();
        });
    }

    /** Deleting a zone unzones its bays: they show no zone and take any item. */
    @ServusGameTest(timeoutTicks = 100)
    public static void deletedZoneUnzonesBays(GameTestHelper helper) {
        for (int x = 4; x <= 6; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 9));
        }
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, IZ_CONTROLLER);
        HeavyRackBlockEntity zoned = WarehouseFixtures.rack(helper, IZ_ORES, Direction.NORTH);
        int bulk = zone(helper, "Bulk", List.of(new ItemStack(Items.COBBLESTONE)), List.of());
        zoned.setZone(bulk);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.putAwayTarget(new ItemStack(Items.BREAD), pos -> true).isEmpty(), "the Bulk bay refuses bread");
            WarehouseZoneData.get(helper.getLevel().getServer()).delete(bulk);
            helper.assertTrue(zoned.zone().isEmpty() && zoned.zoneDefinition() == null, "the bay shows no zone");
            helper.assertTrue(network.putAwayTarget(new ItemStack(Items.BREAD), pos -> true).equals(Optional.of(zoned.getBlockPos())),
                    "the unzoned bay takes bread");
            helper.succeed();
        });
    }

    /**
     * A bay's code is its offset from the controller; without a controller it
     * has none, and a controller placed one block further east gives it a new one.
     */
    @ServusGameTest(timeoutTicks = 100)
    public static void locationCodesFollowController(GameTestHelper helper) {
        BlockPos rackPos = new BlockPos(4, 1, 1);
        BlockPos first = new BlockPos(4, 1, 9);
        BlockPos moved = new BlockPos(5, 1, 9);
        for (int z = 2; z <= 8; z++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(4, 1, z));
        }
        WarehouseFixtures.railOnStone(helper, new BlockPos(5, 1, 8));
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, rackPos, Direction.SOUTH);
        WarehouseFixtures.controller(helper, first);
        Waits.when(helper, () -> rack.locationCode().equals("N8.E0+0"), 20, () -> "the bay reads N8.E0+0, got '" + rack.locationCode() + "'", () -> {
            helper.setBlock(first, Blocks.AIR.defaultBlockState());
            Waits.when(helper, () -> rack.locationCode().isEmpty(), 20, () -> "without a controller the bay has no code, got '"
                    + rack.locationCode() + "'", () -> {
                WarehouseFixtures.controller(helper, moved);
                Waits.when(helper, () -> rack.locationCode().equals("N8.W1+0"), 20,
                        () -> "after the move the bay reads N8.W1+0, got '" + rack.locationCode() + "'", helper::succeed);
            });
        });
    }

    /** With its zone full and no unzoned storage the stack waits with a red lamp, and leaves once an unzoned rack appears. */
    @ServusGameTest(timeoutTicks = 400)
    public static void stationBacksUpWhenZoneFull(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(3, 1, 4));
        HeavyRackBlockEntity full = warehouse.rack();
        full.setZone(zone(helper, "Ores", List.of(), List.of("c:ores")));
        WarehouseFixtures.fill(full, Items.IRON_ORE);
        InboundStationBlockEntity station = warehouse.withInbound();
        BlockPos stationPos = warehouse.inboundPos();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.IRON_ORE, 7), false);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!station.isEmpty(), "the ore should wait in the buffer");
                helper.assertTrue(WarehouseFixtures.blocked(helper, stationPos), "the lamp should be red");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_ROOM,
                        "reason should be no room, was " + station.blockedReason());
                helper.assertTrue("Ores".equals(station.blockedZone()), "blocked zone should read Ores, was " + station.blockedZone());
                BlockPos freshPos = warehouse.railPos(0).south();
                HeavyRackBlockEntity fresh = WarehouseFixtures.rack(helper, freshPos, Direction.NORTH);
                Waits.when(helper, () -> WarehouseFixtures.count(fresh, Items.IRON_ORE) == 7, MiniWarehouse.TRIP_TICKS * 2,
                        () -> "the waiting ore should go into the new unzoned rack, had " + WarehouseFixtures.count(fresh, Items.IRON_ORE), () -> {
                    helper.assertTrue(station.isEmpty(), "the buffer should be empty again");
                    helper.assertTrue(!WarehouseFixtures.blocked(helper, stationPos), "the lamp should be back to normal");
                    helper.succeed();
                });
            });
        });
    }
}
