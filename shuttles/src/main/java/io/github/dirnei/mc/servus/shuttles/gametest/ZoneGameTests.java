package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.shuttles.zone.ShelfZones;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;

/** Team zones, shelves pointing at them, one zone and one location code per shelf, and put-away by zone. */
public final class ZoneGameTests {

    private static final BlockPos ZL_RACK = new BlockPos(2, 1, 7);
    private static final BlockPos IZ_CONTROLLER = new BlockPos(6, 1, 10);
    private static final BlockPos IZ_ORES = new BlockPos(4, 1, 10);
    private static final BlockPos IZ_PLAIN = new BlockPos(5, 1, 10);
    private static final BlockPos IZ_SPARE = new BlockPos(7, 1, 10);
    private static final BlockPos IZ_IRON = new BlockPos(8, 1, 10);
    private static final int ROW_Z = 3;
    private static final BlockPos SC_CONTROLLER = new BlockPos(6, 1, 9);
    private static final BlockPos SC_MOVED = new BlockPos(3, 1, 9);

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

    private static BlockPos bayAt(int x) {
        return new BlockPos(x, 1, ROW_Z);
    }

    private static int zoneAt(GameTestHelper helper, int x) {
        return helper.getBlockEntity(bayAt(x), StorageNodeBlockEntity.class).zoneId();
    }

    /** Places a rack item on the floor at {@code x} of the row as a player looking south would, so it faces north. */
    private static void placeRack(GameTestHelper helper, int x, ItemStack item) {
        Placing.onTop(helper, item, new BlockPos(x, 0, ROW_Z), Direction.SOUTH, false);
    }

    private static ItemStack plainRack() {
        return new ItemStack(ServusShuttlesItems.HEAVY_RACK.get());
    }

    /** A rack item carrying a zone, as the Configuration Carrot leaves it after dismantling a zoned bay. */
    private static ItemStack zonedRack(GameTestHelper helper, int zone) {
        BlockPos spare = new BlockPos(8, 1, 8);
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, spare, Direction.NORTH);
        rack.setZone(zone);
        ItemStack item = plainRack();
        BlockEntityItems.saveToItem(rack, item, helper.getLevel().registryAccess());
        helper.setBlock(spare, Blocks.AIR.defaultBlockState());
        return item;
    }

    /**
     * A shelf of two plain racks at x 2 and 3, put into {@code zone} unless it
     * is {@link ZoneList#NONE}; {@code then} runs once both bays settled.
     */
    private static void shelfOfTwo(GameTestHelper helper, int zone, Runnable then) {
        RackPlacementGameTests.floor(helper);
        placeRack(helper, 2, plainRack());
        placeRack(helper, 3, plainRack());
        helper.runAfterDelay(2, () -> {
            if (zone != ZoneList.NONE) {
                ShelfZones.set(helper.getLevel(), helper.absolutePos(bayAt(2)), zone);
            }
            then.run();
        });
    }

    private static void assertShelfOfThreeIn(GameTestHelper helper, int zone, String what) {
        helper.assertTrue(HeavyRackBlock.shelf(helper.getLevel(), helper.absolutePos(bayAt(3))).size() == 3, "the three racks form one shelf");
        for (int x = 2; x <= 4; x++) {
            helper.assertTrue(zoneAt(helper, x) == zone, what + ": bay " + x + " has zone " + zoneAt(helper, x));
        }
    }

    /** An unzoned bay joining a zoned shelf takes the shelf's zone. */
    @ServusGameTest(timeoutTicks = 20)
    public static void unzonedBayJoinsZonedShelf(GameTestHelper helper) {
        int ores = zone(helper, "Ores", List.of(), List.of());
        shelfOfTwo(helper, ores, () -> {
            placeRack(helper, 4, plainRack());
            settled(helper, () -> assertShelfOfThreeIn(helper, ores, "the shelf of three is in Ores"));
        });
    }

    /** Runs the checks once placed bays had their tick to settle the shelf's zone, then succeeds. */
    private static void settled(GameTestHelper helper, Runnable checks) {
        helper.runAfterDelay(2, () -> {
            checks.run();
            helper.succeed();
        });
    }

    /** A bay carrying Food joining a shelf in Ores ends in Ores: the shelf's zone wins. */
    @ServusGameTest(timeoutTicks = 20)
    public static void shelfZoneWinsOverTheJoiningBay(GameTestHelper helper) {
        int ores = zone(helper, "Ores", List.of(), List.of());
        int food = zone(helper, "Food", List.of(), List.of());
        shelfOfTwo(helper, ores, () -> {
            placeRack(helper, 4, zonedRack(helper, food));
            settled(helper, () -> assertShelfOfThreeIn(helper, ores, "the shelf of three is in Ores"));
        });
    }

    /** A bay carrying Food joining an unzoned shelf puts the whole shelf into Food. */
    @ServusGameTest(timeoutTicks = 20)
    public static void zonedBayZonesUnzonedShelf(GameTestHelper helper) {
        int food = zone(helper, "Food", List.of(), List.of());
        shelfOfTwo(helper, ZoneList.NONE, () -> {
            placeRack(helper, 4, zonedRack(helper, food));
            settled(helper, () -> assertShelfOfThreeIn(helper, food, "the shelf of three is in Food"));
        });
    }

    /** An unzoned bay joining an unzoned shelf leaves it unzoned. */
    @ServusGameTest(timeoutTicks = 20)
    public static void unzonedJoiningUnzonedStaysUnzoned(GameTestHelper helper) {
        shelfOfTwo(helper, ZoneList.NONE, () -> {
            placeRack(helper, 4, plainRack());
            settled(helper, () -> assertShelfOfThreeIn(helper, ZoneList.NONE, "the shelf of three is unzoned"));
        });
    }

    /** A zoned rack item placed on its own is a shelf of one in its zone. */
    @ServusGameTest(timeoutTicks = 20)
    public static void loneBayKeepsItsZone(GameTestHelper helper) {
        int food = zone(helper, "Food", List.of(), List.of());
        RackPlacementGameTests.floor(helper);
        placeRack(helper, 6, zonedRack(helper, food));
        settled(helper, () -> helper.assertTrue(zoneAt(helper, 6) == food, "the placed bay is in Food, got " + zoneAt(helper, 6)));
    }

    /** Breaking the middle bay of a shelf of three in Ores leaves both bays in Ores. */
    @ServusGameTest(timeoutTicks = 20)
    public static void splitShelfKeepsItsZone(GameTestHelper helper) {
        int ores = zone(helper, "Ores", List.of(), List.of());
        shelfOfTwo(helper, ores, () -> {
            placeRack(helper, 4, plainRack());
            helper.runAfterDelay(2, () -> {
                helper.setBlock(bayAt(3), Blocks.AIR.defaultBlockState());
                settled(helper, () -> helper.assertTrue(zoneAt(helper, 2) == ores && zoneAt(helper, 4) == ores, "both bays left are in Ores"));
            });
        });
    }

    /** A layer placed on top of a shelf in Ores is a new shelf without a zone. */
    @ServusGameTest(timeoutTicks = 30)
    public static void layerOnTopIsUnzoned(GameTestHelper helper) {
        int ores = zone(helper, "Ores", List.of(), List.of());
        shelfOfTwo(helper, ores, () -> placeRack(helper, 4, plainRack()));
        helper.runAfterDelay(5, () -> {
            Placing.onTop(helper, new ItemStack(ServusShuttlesItems.HEAVY_RACK.get(), 3), bayAt(3), Direction.SOUTH, false);
            settled(helper, () -> {
                for (int x = 2; x <= 4; x++) {
                    BlockPos top = bayAt(x).above();
                    helper.assertTrue(helper.getBlockEntity(top, StorageNodeBlockEntity.class).zoneId() == ZoneList.NONE,
                            "the layer bay " + x + " is unzoned");
                }
                assertShelfOfThreeIn(helper, ores, "the shelf below stays in Ores");
            });
        });
    }

    /** An empty Fluid Rack joining a Fluid Rack shelf in Cooling ends in Cooling. */
    @ServusGameTest(timeoutTicks = 40)
    public static void fluidRackBayJoinsZonedShelf(GameTestHelper helper) {
        BlockPos[] bays = {new BlockPos(2, 1, 5), new BlockPos(3, 1, 5), new BlockPos(4, 1, 5)};
        FluidRackFixtures.row(helper, Direction.NORTH, bays[0], bays[1]);
        int cooling = zone(helper, "Cooling", List.of(), List.of());
        helper.runAfterDelay(2, () -> {
            ShelfZones.set(helper.getLevel(), helper.absolutePos(bays[0]), cooling);
            FluidRackFixtures.place(helper, bays[2], Direction.NORTH, FluidRackFixtures.item());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(FluidRackFixtures.shelf(helper, bays[0]).size() == 3, "the three Fluid Racks form one shelf");
                for (BlockPos pos : bays) {
                    helper.assertTrue(FluidRackFixtures.bay(helper, pos).zoneId() == cooling, "bay " + pos + " is in Cooling");
                }
                helper.succeed();
            });
        });
    }

    /**
     * Rails in front of three bays 8 north of the controller spot, 4, 3 and 2
     * west of it, and rails on to the controller spot and to the spot 3 blocks
     * further west.
     */
    private static void codedRails(GameTestHelper helper) {
        for (int x = 2; x <= 4; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 2));
        }
        for (int z = 3; z <= 7; z++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(4, 1, z));
        }
        for (int x = 3; x <= 6; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 8));
        }
    }

    /** A south facing shelf of three Heavy Duty Racks on the coded rails, from west to east, with the controller. */
    private static BlockPos[] codedShelf(GameTestHelper helper) {
        BlockPos[] bays = {new BlockPos(2, 1, 1), new BlockPos(3, 1, 1), new BlockPos(4, 1, 1)};
        codedRails(helper);
        BlockState base = ServusShuttlesBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.SOUTH);
        // Facing south, a rack's own left is east.
        for (int i = 0; i < bays.length; i++) {
            helper.setBlock(bays[i], base.setValue(HeavyRackBlock.RIGHT, i > 0).setValue(HeavyRackBlock.LEFT, i < bays.length - 1));
        }
        WarehouseFixtures.controller(helper, SC_CONTROLLER);
        return bays;
    }

    private static StorageNodeBlockEntity node(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, StorageNodeBlockEntity.class);
    }

    private static boolean shelfReads(GameTestHelper helper, List<BlockPos> bays, String code, BlockPos codeBay) {
        for (BlockPos pos : bays) {
            StorageNodeBlockEntity bay = node(helper, pos);
            if (!bay.locationCode().equals(code) || bay.codeBay() != pos.equals(codeBay)) {
                return false;
            }
        }
        return true;
    }

    private static String codes(GameTestHelper helper, List<BlockPos> bays) {
        return bays.stream().map(pos -> node(helper, pos).locationCode() + (node(helper, pos).codeBay() ? "*" : "")).toList().toString();
    }

    /** Every bay of a shelf reads the code of its bay closest to the controller, the only code bay; it moves with the controller. */
    @ServusGameTest(timeoutTicks = 120)
    public static void shelfCodeFollowsTheClosestBay(GameTestHelper helper) {
        List<BlockPos> bays = List.of(codedShelf(helper));
        Waits.when(helper, () -> shelfReads(helper, bays, "N8.W2+0", bays.get(2)), 30,
                () -> "every bay reads N8.W2+0 from the east bay, got " + codes(helper, bays), () -> {
            helper.setBlock(SC_CONTROLLER, Blocks.AIR.defaultBlockState());
            WarehouseFixtures.controller(helper, SC_MOVED);
            Waits.when(helper, () -> shelfReads(helper, bays, "N8.E0+0", bays.get(1)), 30,
                    () -> "after the move the middle bay gives N8.E0+0, got " + codes(helper, bays), helper::succeed);
        });
    }

    /** Breaking a shelf's code bay gives the bays left the code of the next closest one. */
    @ServusGameTest(timeoutTicks = 120)
    public static void shelfLosingItsCodeBayGetsANewCode(GameTestHelper helper) {
        BlockPos[] shelf = codedShelf(helper);
        List<BlockPos> bays = List.of(shelf);
        Waits.when(helper, () -> shelfReads(helper, bays, "N8.W2+0", bays.get(2)), 30,
                () -> "the shelf reads N8.W2+0, got " + codes(helper, bays), () -> {
            helper.setBlock(shelf[2], Blocks.AIR.defaultBlockState());
            List<BlockPos> left = List.of(shelf[0], shelf[1]);
            Waits.when(helper, () -> shelfReads(helper, left, "N8.W3+0", shelf[1]), 30,
                    () -> "the two bays left read N8.W3+0, got " + codes(helper, left), helper::succeed);
        });
    }

    /** A Fluid Rack shelf has one code on all its bays, from its bay closest to the controller. */
    @ServusGameTest(timeoutTicks = 120)
    public static void fluidRackShelfHasOneCode(GameTestHelper helper) {
        BlockPos[] shelf = {new BlockPos(2, 1, 1), new BlockPos(3, 1, 1), new BlockPos(4, 1, 1)};
        codedRails(helper);
        FluidRackFixtures.row(helper, Direction.SOUTH, shelf);
        WarehouseFixtures.controller(helper, SC_CONTROLLER);
        List<BlockPos> bays = List.of(shelf);
        Waits.when(helper, () -> shelfReads(helper, bays, "N8.W2+0", shelf[2]), 60,
                () -> "every Fluid Rack bay reads N8.W2+0 from the east bay, got " + codes(helper, bays), helper::succeed);
    }
}
