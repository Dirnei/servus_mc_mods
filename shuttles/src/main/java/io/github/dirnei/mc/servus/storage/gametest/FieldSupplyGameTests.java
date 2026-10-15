package io.github.dirnei.mc.servus.storage.gametest;

import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.HEAD;
import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.RAIL_Z;
import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.farm;
import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.network;
import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.out;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FertilizeMode;
import io.github.dirnei.mc.servus.storage.field.FertilizeStatus;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.field.SupplyBuffer;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/** Supply runs that keep a Field Head's buffer stocked, on the farm of {@link FarmFixtures} and behind an elevator shaft. */
public final class FieldSupplyGameTests {

    private FieldSupplyGameTests() {
    }

    private static ItemStack plainShuttle() {
        return new ItemStack(ServusStorageItems.SHUTTLE_MK1.get());
    }

    /** Quick fertilize calls and two doses per plot, the same numbers as the Spreader tests of the batch. */
    private static void quickFertilizing() {
        StorageStatsManager.INSTANCE.setFertilizeInterval(20);
        StorageStatsManager.INSTANCE.setDosesPerPlot(2);
    }

    /** Bone meal after planting with that reserve. */
    private static void boneMeal(FieldHeadBlockEntity head, int reserve) {
        head.setFertilizer(new ItemStack(Items.BONE_MEAL));
        head.setFertilizeMode(FertilizeMode.AFTER_PLANTING);
        head.setReserve(reserve);
    }

    private static int buffered(FieldHeadBlockEntity head, SupplyBuffer.Part part) {
        return head.supplyCount(part);
    }

    private static int age(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
    }

    private static String buffer(FieldHeadBlockEntity head) {
        return "seeds " + buffered(head, SupplyBuffer.Part.SEEDS) + ", fertilizer " + buffered(head, SupplyBuffer.Part.FERTILIZER)
                + ", anti-corrosion " + buffered(head, SupplyBuffer.Part.ANTI_CORROSION);
    }

    /** A shuttle without a tool brings seeds, bone meal and honeycomb, each up to its level, in two trips of two stacks. */
    @ServusGameTest(timeoutTicks = 700)
    public static void supplyRunFillsTheBuffer(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, plainShuttle());
        HeavyRackBlockEntity rack = farm.rack();
        WarehouseFixtures.put(rack, new ItemStack(Items.WHEAT_SEEDS, 64), new ItemStack(Items.WHEAT_SEEDS, 36),
                new ItemStack(Items.BONE_MEAL, 30), new ItemStack(Items.HONEYCOMB, 10));
        FieldHeadBlockEntity head = farm.head();
        int antiCorrosion = StorageStatsManager.INSTANCE.fieldAntiCorrosionLevel();
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            boneMeal(head, 0);
            Waits.when(helper, () -> buffered(head, SupplyBuffer.Part.SEEDS) == 64 && buffered(head, SupplyBuffer.Part.FERTILIZER) == 30
                    && buffered(head, SupplyBuffer.Part.ANTI_CORROSION) == antiCorrosion && out(helper) == 0, 650,
                    () -> "the buffer should fill, it holds " + buffer(head) + ", out " + out(helper), () -> {
                helper.assertTrue(WarehouseFixtures.count(rack, Items.WHEAT_SEEDS) == 36, "36 seeds stay in the rack");
                helper.assertTrue(WarehouseFixtures.count(rack, Items.BONE_MEAL) == 0, "all bone meal went to the buffer");
                helper.assertTrue(WarehouseFixtures.count(rack, Items.HONEYCOMB) == 10 - antiCorrosion, "the rest of the honeycomb stays");
                helper.assertTrue(FarmFixtures.noItemsLying(helper), "nothing should drop on the ground");
                helper.succeed();
            });
        });
    }

    /** The reserve stays in the rack: 30 bone meal and a reserve of 10 bring 20 into the buffer. */
    @ServusGameTest(timeoutTicks = 400)
    public static void supplyKeepsTheReserve(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, plainShuttle());
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 30));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            boneMeal(head, 10);
            Waits.when(helper, () -> buffered(head, SupplyBuffer.Part.FERTILIZER) == 20 && out(helper) == 0, 300,
                    () -> "20 bone meal should reach the buffer, it holds " + buffer(head), () -> helper.runAfterDelay(40, () -> {
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.BONE_MEAL) == 10, "the reserve of 10 stays in the rack");
                helper.assertTrue(buffered(head, SupplyBuffer.Part.FERTILIZER) == 20 && network(helper).queuedJobs() == 0,
                        "no further supply run takes the reserve");
                helper.succeed();
            }));
        });
    }

    /** The wax reserve stays in the rack: 10 honeycomb and a reserve of 5 bring 5 into the buffer. */
    @ServusGameTest(timeoutTicks = 400)
    public static void supplyKeepsTheAntiCorrosionReserve(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, plainShuttle());
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.HONEYCOMB, 10));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            head.setAntiCorrosionReserve(5);
            Waits.when(helper, () -> buffered(head, SupplyBuffer.Part.ANTI_CORROSION) == 5 && out(helper) == 0, 300,
                    () -> "5 honeycomb should reach the buffer, it holds " + buffer(head), () -> helper.runAfterDelay(40, () -> {
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.HONEYCOMB) == 5, "the reserve of 5 stays in the rack");
                helper.assertTrue(buffered(head, SupplyBuffer.Part.ANTI_CORROSION) == 5 && network(helper).queuedJobs() == 0,
                        "no further supply run takes the reserve");
                helper.succeed();
            }));
        });
    }

    /**
     * Only a Spreader and a Harvester in the garage: no supply run, and the
     * head says no shuttle can bring seeds or fertilizer, with a red bar.
     */
    @ServusGameTest(timeoutTicks = 200)
    public static void noToollessShuttleShowsStatus(GameTestHelper helper) {
        quickFertilizing();
        BlockPos farmland = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos fresh = new BlockPos(6, 1, RAIL_Z + 1);
        helper.setBlock(farmland.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        FarmFixtures.Farm farm = farm(helper, FarmFixtures.shuttleWith(ServusStorageItems.SPREADER.get()));
        farm.garage().shuttles().setStackInSlot(1, FarmFixtures.shuttleWith(ServusStorageItems.HARVESTER.get()));
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 30), new ItemStack(Items.WHEAT_SEEDS, 10));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            FieldFixtures.water(head);
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            boneMeal(head, 0);
            FieldFixtures.crop(helper, fresh, Blocks.WHEAT, 0);
            head.recheck(helper.absolutePos(fresh));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(head.fertilizeStatus() == FertilizeStatus.NO_SUPPLY_SHUTTLE,
                        "the Fertilizer tab should say no shuttle can bring supplies, got " + head.fertilizeStatus());
                helper.assertTrue(head.work() == FieldWork.NO_SUPPLY_SHUTTLE, "the Field tab should say no shuttle can bring seeds, got " + head.work());
                helper.assertTrue(head.problem() && helper.getBlockState(HEAD).getValue(FieldHeadBlock.PROBLEM), "the bar on the head is red");
                helper.assertTrue(out(helper) == 0 && network(helper).queuedJobs() == 0, "no shuttle drives out");
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.BONE_MEAL) == 30
                        && WarehouseFixtures.count(farm.rack(), Items.WHEAT_SEEDS) == 10, "the rack keeps its stock");
                helper.succeed();
            });
        });
    }

    /** Wheat seeds in the buffer, then carrots set: the next supply run brings carrots and takes the wheat seeds back. */
    @ServusGameTest(timeoutTicks = 800)
    public static void staleSeedsGoBack(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, plainShuttle());
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT_SEEDS, 64), new ItemStack(Items.CARROT, 64));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            Waits.when(helper, () -> buffered(head, SupplyBuffer.Part.SEEDS) == 64 && out(helper) == 0, 350,
                    () -> "the wheat seeds should reach the buffer, it holds " + buffer(head), () -> {
                head.setCrop(new ItemStack(Items.CARROT));
                Waits.when(helper, () -> head.buffer().item(SupplyBuffer.Part.SEEDS) == Items.CARROT && buffered(head, SupplyBuffer.Part.SEEDS) == 64
                        && WarehouseFixtures.count(farm.rack(), Items.WHEAT_SEEDS) == 64 && out(helper) == 0, 400,
                        () -> "carrots should replace the wheat seeds, buffer " + buffer(head) + ", rack wheat seeds "
                                + WarehouseFixtures.count(farm.rack(), Items.WHEAT_SEEDS), () -> {
                    helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.CARROT) == 0, "the carrots left the rack");
                    helper.assertTrue(FarmFixtures.noItemsLying(helper), "nothing should drop on the ground");
                    helper.succeed();
                });
            });
        });
    }

    /** Breaking the head drops its buffer. */
    @ServusGameTest(timeoutTicks = 60)
    public static void bufferDropsWhenBroken(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, plainShuttle());
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.assertTrue(head.fillSupply(SupplyBuffer.Part.SEEDS, Items.WHEAT_SEEDS, 40) == 40, "the buffer takes 40 seeds");
            helper.setBlock(HEAD, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(dropped(helper, Items.WHEAT_SEEDS) == 40, "the 40 seeds drop, got " + dropped(helper, Items.WHEAT_SEEDS));
                helper.succeed();
            });
        });
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

    /** The buffer is kept when the head is saved and loaded. */
    @ServusGameTest(timeoutTicks = 60)
    public static void bufferSurvivesReload(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, plainShuttle());
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            boneMeal(head, 0);
            head.fillSupply(SupplyBuffer.Part.SEEDS, Items.WHEAT_SEEDS, 40);
            head.fillSupply(SupplyBuffer.Part.FERTILIZER, Items.BONE_MEAL, 12);
            head.fillSupply(SupplyBuffer.Part.ANTI_CORROSION, Items.HONEYCOMB, 5);
            CompoundTag tag = head.saveWithFullMetadata(helper.getLevel().registryAccess());
            FieldHeadBlockEntity copy = new FieldHeadBlockEntity(head.getBlockPos(), head.getBlockState());
            copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
            for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
                helper.assertTrue(copy.buffer().item(part) == head.buffer().item(part) && copy.buffer().count(part) == head.buffer().count(part),
                        part + " should survive saving, got " + copy.buffer().count(part) + " of " + copy.buffer().item(part));
            }
            helper.assertTrue(copy.buffer().count(SupplyBuffer.Part.SEEDS) == 40, "40 seeds after loading");
            helper.succeed();
        });
    }

    /**
     * The only bone meal lies in a rack upstairs behind the elevator shaft:
     * the shuttle without a tool rides the lift, brings it into the head's
     * buffer, and the Spreader, which never rides a lift, doses the field.
     */
    @ServusGameTest(timeoutTicks = 900)
    public static void supplyRidesTheLift(GameTestHelper helper) {
        quickFertilizing();
        HarvesterGameTests.shaftBase(helper, FarmFixtures.shuttleWith(ServusStorageItems.SPREADER.get()), plainShuttle());
        helper.setBlock(new BlockPos(3, 3, 3), ServusStorageBlocks.RAIL.get().defaultBlockState());
        HeavyRackBlockEntity upstairs = WarehouseFixtures.rack(helper, new BlockPos(3, 3, 4), Direction.NORTH);
        WarehouseFixtures.put(upstairs, new ItemStack(Items.BONE_MEAL, 20));
        helper.setBlock(new BlockPos(5, 0, 3), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(5, 1, 3), ServusStorageBlocks.RAIL.get().defaultBlockState());
        for (int x = 6; x <= 8; x++) {
            FieldFixtures.rail(helper, new BlockPos(x, 1, 3), ServusStorageBlocks.FARM_RAIL.get());
        }
        WarehouseFixtures.stone(helper, new BlockPos(9, 0, 3));
        FieldHeadBlockEntity head = FieldFixtures.head(helper, new BlockPos(9, 1, 3));
        BlockPos plot = new BlockPos(7, 1, 4);
        BlockPos bottom = helper.absolutePos(HarvesterGameTests.SHAFT_BOTTOM);
        boolean[] carMoved = {false};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(HarvesterGameTests.SHAFT_CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().map(network -> network.lift(bottom)).ifPresent(car -> carMoved[0] |= car.moving());
            }
        });
        helper.runAfterDelay(3, () -> {
            FieldFixtures.water(head);
            boneMeal(head, 0);
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 0);
            head.recheck(helper.absolutePos(plot));
            Waits.when(helper, () -> age(helper, plot) >= 4 && WarehouseFixtures.count(upstairs, Items.BONE_MEAL) == 0
                    && buffered(head, SupplyBuffer.Part.FERTILIZER) == 18
                    && HarvesterGameTests.shaftNetwork(helper).garageOut(helper.absolutePos(HarvesterGameTests.SHAFT_GARAGE)) == 0, 850,
                    () -> "the bone meal should come down the lift and the wheat get two doses, age " + age(helper, plot) + ", upstairs "
                            + WarehouseFixtures.count(upstairs, Items.BONE_MEAL) + ", buffer " + buffer(head)
                            + ", fertilize " + head.fertilizeStatus(), () -> {
                helper.assertTrue(carMoved[0], "the supply run rode the lift car");
                helper.succeed();
            });
        });
    }
}
