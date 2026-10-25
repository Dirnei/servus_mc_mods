package io.github.dirnei.mc.servus.shuttles.gametest;

import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.GARAGE;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.RAIL_Z;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.farm;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.network;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.noItemsLying;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.out;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldWork;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleFleet;
import io.github.dirnei.mc.servus.shuttles.shuttle.StrandReason;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;

/** Slot locks on field runs: the Field Head buffer, supply runs and the harvest put-away, on the farm of {@link FarmFixtures}. */
public final class FieldLockGameTests {

    /** Beside the farm rails' west end, nearer to the field than the farm's own rack. */
    private static final BlockPos NEAR_RACK = new BlockPos(3, 1, RAIL_Z + 1);
    private static final int JOB = 900_001;
    private static final int OTHER_JOB = 900_002;

    private FieldLockGameTests() {
    }

    private static ItemStack harvester() {
        return FarmFixtures.shuttleWith(ServusShuttlesItems.HARVESTER.get());
    }

    private static List<SlotLock> seedLocks(FieldHeadBlockEntity head) {
        return head.buffer().slots().locksOf(SupplyBuffer.Part.SEEDS.ordinal());
    }

    /** The locks of that kind and item in the inventory. */
    private static List<SlotLock> locks(LockedSlots slots, SlotLock.Kind kind, Item item) {
        return slots.locked().stream().map(LockedSlots.Locked::lock)
                .filter(lock -> lock.kind() == kind && lock.key().item() == item).toList();
    }

    /**
     * A supply run locks the room in the head's seed part and the seeds in
     * the rack before the shuttle leaves, both for its job, fills the head
     * and leaves no lock behind.
     */
    @ServusGameTest(timeoutTicks = 500)
    public static void supplyRunLocksHeadAndRack(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        HeavyRackBlockEntity rack = farm.rack();
        WarehouseFixtures.put(rack, new ItemStack(Items.WHEAT_SEEDS, 64));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            Waits.when(helper, () -> !seedLocks(head).isEmpty(), 100, () -> "the supply run should lock the head's seed part", () -> {
                SlotLock in = seedLocks(head).getFirst();
                List<SlotLock> out = locks(rack.items(), SlotLock.Kind.OUT, Items.WHEAT_SEEDS);
                helper.assertTrue(in.kind() == SlotLock.Kind.IN && in.amount() == 64, "the head part is IN locked for 64 seeds, got " + in);
                helper.assertTrue(out.size() == 1 && out.getFirst().amount() == 64 && out.getFirst().jobId() == in.jobId(),
                        "the rack slot is OUT locked for the same run, got " + out);
                Waits.when(helper, () -> head.supplyCount(SupplyBuffer.Part.SEEDS) == 64 && out(helper) == 0, 400,
                        () -> "the seeds should reach the head, it holds " + head.supplyCount(SupplyBuffer.Part.SEEDS), () -> {
                    helper.assertTrue(seedLocks(head).isEmpty() && rack.items().locked().isEmpty(), "no lock is left after the run");
                    helper.assertTrue(WarehouseFixtures.count(rack, Items.WHEAT_SEEDS) == 0 && noItemsLying(helper), "nothing is lost");
                    helper.succeed();
                });
            });
        });
    }

    /**
     * Room a job reserved in a head part takes nothing from others, items a
     * job reserved are not taken by others, the jobs themselves fill and take
     * as reserved, and the locks survive saving and loading.
     */
    @ServusGameTest(timeoutTicks = 60)
    public static void headPartsKeepTheirReservations(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, harvester());
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            SupplyBuffer.Part seeds = SupplyBuffer.Part.SEEDS;
            int level = head.supplyLevel(seeds);
            head.fillSupply(seeds, Items.WHEAT_SEEDS, 20);
            head.lockSupplyIn(JOB, seeds, level - 20);
            helper.assertTrue(head.fillSupply(seeds, Items.WHEAT_SEEDS, 64) == 0 && head.supplyRoom(seeds) == 0,
                    "reserved room takes no spare seeds");
            helper.assertTrue(head.lockSupplyOut(OTHER_JOB, seeds, 15) == 15 && head.supplyAvailable(seeds) == 5,
                    "a field shuttle reserves 15 of the 20 seeds beside the reserved room");

            CompoundTag tag = head.saveWithFullMetadata(helper.getLevel().registryAccess());
            FieldHeadBlockEntity copy = new FieldHeadBlockEntity(head.getBlockPos(), head.getBlockState());
            copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
            helper.assertTrue(copy.buffer().count(seeds) == 20 && copy.buffer().reservedIn(seeds, 0) == level - 20
                    && copy.buffer().available(seeds) == 5, "the locks survive saving");

            helper.assertTrue(head.takeSupply(seeds, 20) == 5, "others take only the 5 seeds nobody reserved");
            helper.assertTrue(head.takeSupplyAs(OTHER_JOB, seeds, 15) == 15, "the field shuttle takes its 15");
            helper.assertTrue(head.fillSupplyAs(JOB, seeds, Items.WHEAT_SEEDS, level - 20) == level - 20, "the supply run fills its reserved room");
            helper.assertTrue(seedLocks(head).isEmpty() && head.supplyCount(seeds) == level - 20, "no lock is left, the part holds "
                    + head.supplyCount(seeds));
            helper.succeed();
        });
    }

    /**
     * The harvest goes into the rack that already holds wheat, even though an
     * empty rack stands nearer, and that rack holds an IN lock for the wheat
     * while the harvester is on its way.
     */
    @ServusGameTest(timeoutTicks = 600)
    public static void harvestGoesToTheHoldingRackWithLocks(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z - 1)};
        for (BlockPos plot : plots) {
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        }
        FarmFixtures.Farm farm = farm(helper, harvester());
        FarmFixtures.carrier(farm);
        HeavyRackBlockEntity near = WarehouseFixtures.rack(helper, NEAR_RACK, Direction.NORTH);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT, 5));
        boolean[] lockedOnTheWay = {false};
        helper.onEachTick(() -> lockedOnTheWay[0] |= !locks(farm.rack().items(), SlotLock.Kind.IN, Items.WHEAT).isEmpty());
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> helper.getBlockState(plots[0]).isAir() && helper.getBlockState(plots[1]).isAir() && out(helper) == 0
                            && WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 7 && !farm.head().toStorageHoldsItems()
                            && farm.rack().items().locked().isEmpty() && near.items().locked().isEmpty(), 550,
                    () -> "the wheat should be harvested and stored", () -> {
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 7 && WarehouseFixtures.count(near, Items.WHEAT) == 0,
                        "the wheat joins the rack holding wheat, it holds " + WarehouseFixtures.count(farm.rack(), Items.WHEAT)
                                + ", the near rack " + WarehouseFixtures.count(near, Items.WHEAT));
                helper.assertTrue(lockedOnTheWay[0], "the holding rack was IN locked for the wheat on the way");
                helper.assertTrue(farm.rack().items().locked().isEmpty() && near.items().locked().isEmpty(), "no lock is left");
                helper.assertTrue(noItemsLying(helper), "nothing drops");
                helper.succeed();
            });
        });
    }

    /**
     * The head's To storage buffer has one free slot and the harvester brings
     * carrots and potatoes: one crop fills the slot, the harvester parks in
     * its garage with the other, and unloads it once a slot is free.
     */
    @ServusGameTest(timeoutTicks = 800)
    public static void fullToStorageParksTheHarvester(GameTestHelper helper) {
        BlockPos carrots = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos potatoes = new BlockPos(5, 1, RAIL_Z + 1);
        FieldFixtures.crop(helper, carrots, Blocks.CARROTS, 7);
        FieldFixtures.crop(helper, potatoes, Blocks.POTATOES, 7);
        FarmFixtures.Farm farm = farm(helper, harvester());
        LockedSlots toStorage = farm.head().toStorage();
        for (int slot = 1; slot < toStorage.getSlots(); slot++) {
            toStorage.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        BlockPos garage = helper.absolutePos(GARAGE);
        helper.runAfterDelay(2, () -> {
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> helper.getBlockState(carrots).isAir() && helper.getBlockState(potatoes).isAir()
                            && WarehouseFixtures.blocked(helper, GARAGE) && network(helper).shuttleViews(garage).isEmpty(), 500,
                    () -> "the harvester should park in its garage with the potatoes", () -> {
                List<ShuttleFleet.Stranded> stranded = network(helper).stranded(garage);
                Item unloaded = toStorage.getStackInSlot(0).getItem();
                Item parked = unloaded == Items.CARROT ? Items.POTATO : Items.CARROT;
                helper.assertTrue(unloaded == Items.CARROT || unloaded == Items.POTATO, "one crop fills the free slot, it holds " + toStorage.getStackInSlot(0));
                helper.assertTrue(stranded.size() == 1 && stranded.getFirst().cargo().is(parked)
                        && stranded.getFirst().reason() == StrandReason.HEAD_FULL, "the garage names the other crop parked: " + stranded);
                helper.assertTrue(farm.head().work() == FieldWork.COLLECT_NO_SHUTTLE,
                        "without a plain shuttle the head says nobody collects: " + farm.head().work());
                helper.assertTrue(noItemsLying(helper), "nothing drops");
                toStorage.setStackInSlot(1, ItemStack.EMPTY);
                Waits.when(helper, () -> toStorage.getStackInSlot(1).is(parked) && out(helper) == 0
                                && WarehouseFixtures.activity(helper, GARAGE) != StorageNodeBlocks.Activity.BLOCKED, 300,
                        () -> "the parked crop should be unloaded at the head once a slot is free", helper::succeed);
            });
        });
    }

}
