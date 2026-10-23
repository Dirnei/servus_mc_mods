package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.CounterData;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Jobs, shuttles with their cargo, held loads and station claims survive a reload of their block. */
public final class FleetPersistenceGameTests {

    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);

    private FleetPersistenceGameTests() {
    }

    /** Unloads the block entity at {@code pos} as its chunk would, lets {@code meanwhile} change the world, then loads it from its tag. */
    private static <T extends BlockEntity> T reload(GameTestHelper helper, BlockPos pos, Consumer<CompoundTag> edit, Runnable meanwhile) {
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(pos);
        BlockEntity old = level.getBlockEntity(absolute);
        CompoundTag tag = old.saveWithFullMetadata(level.registryAccess());
        edit.accept(tag);
        old.onChunkUnloaded();
        meanwhile.run();
        BlockEntity fresh = BlockEntity.loadStatic(absolute, level.getBlockState(absolute), tag, level.registryAccess());
        level.setBlockEntity(fresh);
        level.invalidateCapabilities(absolute);
        @SuppressWarnings("unchecked")
        T loaded = (T) level.getBlockEntity(absolute);
        return loaded;
    }

    private static int dropped(GameTestHelper helper, Item item) {
        int count = 0;
        for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }

    private static void insert(InboundStationBlockEntity station, ItemStack stack) {
        ItemStack left = Transfer.insertItem(station.intake(), stack, false);
        if (!left.isEmpty()) {
            throw new IllegalStateException("the intake should take the stack");
        }
    }

    /** Two put-away jobs get two ids that count up, and the world counter knows them. */
    @ServusGameTest(timeoutTicks = 100)
    public static void jobIdsCountUp(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            warehouse.garage().setActive(false);
            insert(station, new ItemStack(Items.COBBLESTONE, 64));
            insert(station, new ItemStack(Items.DIRT, 64));
            Waits.when(helper, () -> station.isClaimed(0) && station.isClaimed(1), 40, () -> "both slots should be claimed", () -> {
                int first = station.claimedBy(0);
                int second = station.claimedBy(1);
                helper.assertTrue(first > 0 && second > first, "ids count up, got " + first + " and " + second);
                int last = CounterData.get(helper.getLevel().getServer(), CounterData.Kind.JOB_IDS).last();
                helper.assertTrue(last >= second, "the world counter is at " + last);
                helper.succeed();
            });
        });
    }

    /** A garage reloaded while its shuttle carries cobblestone to the rack: nothing drops and the cobblestone lands in the rack. */
    @ServusGameTest(timeoutTicks = 400)
    public static void garageReloadMidTripDelivers(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            insert(station, new ItemStack(Items.COBBLESTONE, 40));
            Waits.when(helper, () -> station.isEmpty() && warehouse.network().ridingJobs() == 1, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the shuttle should load the cobblestone", () -> {
                        reload(helper, warehouse.garagePos(), tag -> { }, () -> { });
                        helper.runAfterDelay(1, () -> {
                            helper.assertTrue(dropped(helper, Items.COBBLESTONE) == 0, "nothing drops at the reload");
                            helper.assertTrue(warehouse.network().ridingJobs() == 1, "the shuttle still carries the job after the reload");
                            helper.assertTrue(warehouse.network().garageOut(helper.absolutePos(warehouse.garagePos())) == 1,
                                    "the shuttle is still out");
                        });
                        Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 40, MiniWarehouse.TRIP_TICKS * 2,
                                () -> "the rack should get 40 cobblestone, has " + warehouse.rackCount(Items.COBBLESTONE)
                                        + ", dropped " + dropped(helper, Items.COBBLESTONE), () -> {
                                    helper.assertTrue(dropped(helper, Items.COBBLESTONE) == 0, "nothing dropped");
                                    helper.succeed();
                                });
                    });
        });
    }

    /** A controller reloaded with a queued put-away keeps the one job, and the shuttle serves it once the garage runs. */
    @ServusGameTest(timeoutTicks = 400)
    public static void controllerReloadKeepsQueue(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            warehouse.garage().setActive(false);
            insert(station, new ItemStack(Items.COBBLESTONE, 40));
            Waits.when(helper, () -> warehouse.network().queuedJobs() == 1, 40, () -> "the put-away should be queued", () -> {
                int id = station.claimedBy(0);
                reload(helper, warehouse.controllerPos(), tag -> { }, () -> { });
                helper.runAfterDelay(30, () -> {
                    WarehouseNetwork network = warehouse.network();
                    helper.assertTrue(network.queuedJobs() == 1, "one job is queued after the reload, got " + network.queuedJobs());
                    helper.assertTrue(station.claimedBy(0) == id, "the slot keeps its claim");
                    warehouse.garage().setActive(true);
                    Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 40, MiniWarehouse.TRIP_TICKS * 2,
                            () -> "the rack should get the cobblestone", helper::succeed);
                });
            });
        });
    }

    /** A station reloaded with a claimed slot keeps the claim and no second job is made for the stack. */
    @ServusGameTest(timeoutTicks = 400)
    public static void stationReloadKeepsClaim(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            warehouse.garage().setActive(false);
            insert(station(helper, warehouse), new ItemStack(Items.COBBLESTONE, 40));
            Waits.when(helper, () -> warehouse.network().queuedJobs() == 1, 40, () -> "the put-away should be queued", () -> {
                int id = station(helper, warehouse).claimedBy(0);
                InboundStationBlockEntity loaded = reload(helper, warehouse.inboundPos(), tag -> { }, () -> { });
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(loaded.claimedBy(0) == id, "the claim survives, got " + loaded.claimedBy(0) + " for " + id);
                    helper.assertTrue(warehouse.network().queuedJobs() == 1, "no second job, queued " + warehouse.network().queuedJobs());
                    warehouse.garage().setActive(true);
                    Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 40 && loaded.isEmpty(), MiniWarehouse.TRIP_TICKS * 2,
                            () -> "the rack should get the cobblestone", helper::succeed);
                });
            });
        });
    }

    private static InboundStationBlockEntity station(GameTestHelper helper, MiniWarehouse warehouse) {
        return helper.getBlockEntity(warehouse.inboundPos(), InboundStationBlockEntity.class);
    }

    /** A slot claimed by a job nobody knows is released after two sweeps and its stack is put away. */
    @ServusGameTest(timeoutTicks = 700)
    public static void orphanClaimIsReleased(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            station.claim(0, Integer.MAX_VALUE);
            insert(station, new ItemStack(Items.COBBLESTONE, 40));
            helper.runAfterDelay(20, () -> helper.assertTrue(station.claimedBy(0) == Integer.MAX_VALUE && warehouse.network().queuedJobs() == 0
                    && warehouse.network().ridingJobs() == 0, "the claimed stack waits"));
            Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 40, 650,
                    () -> "the orphan claim should be released and the stack put away", helper::succeed);
        });
    }

    /** Loads held for a Packing Station survive its reload and drop when it is broken. */
    @ServusGameTest(timeoutTicks = 100)
    public static void heldLoadsSurviveAndDrop(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withPackingStation();
        BlockPos packing = helper.absolutePos(warehouse.packingPos());
        helper.runAfterDelay(10, () -> {
            warehouse.network().holdForTests(packing, new ItemStack(Items.OAK_PLANKS, 20));
            reload(helper, warehouse.packingPos(), tag -> { }, () -> { });
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(warehouse.network().heldCount(packing) == 20, "20 planks are held after the reload, got "
                        + warehouse.network().heldCount(packing));
                helper.assertTrue(dropped(helper, Items.OAK_PLANKS) == 0, "nothing dropped at the reload");
                helper.setBlock(warehouse.packingPos(), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(dropped(helper, Items.OAK_PLANKS) == 20, "the held planks drop, got " + dropped(helper, Items.OAK_PLANKS));
                    helper.succeed();
                });
            });
        });
    }

    /** Tags from before fleet state (no warehouse state, no claims) load into an empty fleet and free slots. */
    @ServusGameTest(timeoutTicks = 400)
    public static void oldTagsLoadEmpty(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            warehouse.garage().setActive(false);
            insert(station(helper, warehouse), new ItemStack(Items.BREAD, 16));
            Waits.when(helper, () -> warehouse.network().queuedJobs() == 1, 40, () -> "the put-away should be queued", () -> {
                reload(helper, warehouse.controllerPos(), tag -> tag.remove("Warehouse"), () -> { });
                InboundStationBlockEntity loaded = reload(helper, warehouse.inboundPos(), tag -> tag.remove("Claims"), () -> { });
                ShuttleGarageBlockEntity garage = reload(helper, warehouse.garagePos(), tag -> tag.remove("Warehouse"), () -> { });
                helper.runAfterDelay(5, () -> {
                    garage.setActive(true);
                    Waits.when(helper, () -> warehouse.rackCount(Items.BREAD) == 16 && loaded.isEmpty(), MiniWarehouse.TRIP_TICKS * 3,
                            () -> "the bread should be put away by a new job", helper::succeed);
                });
            });
        });
    }

    /** A harvester whose garage reloads while it carries wheat puts the wheat away and finishes the field. */
    @ServusGameTest(timeoutTicks = 800)
    public static void harvesterReloadKeepsCargo(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, FarmFixtures.RAIL_Z - 1), new BlockPos(6, 1, FarmFixtures.RAIL_Z + 1),
                new BlockPos(4, 1, FarmFixtures.RAIL_Z + 1), new BlockPos(7, 1, FarmFixtures.RAIL_Z - 1)};
        for (BlockPos plot : plots) {
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        }
        FarmFixtures.Farm farm = FarmFixtures.farm(helper, FarmFixtures.shuttleWith(ServusShuttlesItems.HARVESTER.get()));
        BlockPos garage = helper.absolutePos(FarmFixtures.GARAGE);
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            Waits.when(helper, () -> FarmFixtures.network(helper).shuttleViews(garage).stream().anyMatch(view -> view.cargo().is(Items.WHEAT)),
                    400, () -> "the harvester should carry wheat", () -> {
                        reload(helper, FarmFixtures.GARAGE, tag -> { }, () -> { });
                        Waits.when(helper, () -> WarehouseFixtures.count(farm.rack(), Items.WHEAT) == plots.length && FarmFixtures.out(helper) == 0,
                                600, () -> "the four wheat should reach the rack, it holds " + WarehouseFixtures.count(farm.rack(), Items.WHEAT)
                                        + ", out " + FarmFixtures.out(helper), () -> {
                                    helper.assertTrue(FarmFixtures.noItemsLying(helper), "nothing drops");
                                    helper.succeed();
                                });
                    });
        });
    }

    /** A shuttle restored after its target's rail was broken goes home and drops its cargo at the garage. */
    @ServusGameTest(timeoutTicks = 400)
    public static void strandedCargoEndsAtGarage(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            insert(station, new ItemStack(Items.COBBLESTONE, 40));
            Waits.when(helper, () -> station.isEmpty() && warehouse.network().ridingJobs() == 1, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the shuttle should load the cobblestone", () -> {
                        reload(helper, warehouse.garagePos(), tag -> { },
                                () -> helper.setBlock(warehouse.railPos(0), Blocks.AIR.defaultBlockState()));
                        Waits.when(helper, () -> dropped(helper, Items.COBBLESTONE) == 40, MiniWarehouse.TRIP_TICKS * 2,
                                () -> "the cobblestone should drop at the garage, dropped " + dropped(helper, Items.COBBLESTONE), () -> {
                                    helper.assertTrue(warehouse.rackCount(Items.COBBLESTONE) == 0, "the rack gets nothing");
                                    helper.succeed();
                                });
                    });
        });
    }
}
