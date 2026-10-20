package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Tanker runs from Import gantries to Storage gantries: targets, amounts, leftovers, aborted runs and lifts. */
public final class TankerRunGameTests {

    private static final FluidKey WATER = FluidKey.of(new FluidStack(Fluids.WATER, 1));
    /** Ticks a test waits at most for a run to finish: a check, two stops, the drive there and home. */
    private static final int RUN = 400;

    private TankerRunGameTests() {
    }

    /** Water an Import gantry holds goes to an empty Storage tank on Any. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void importToStorageByTanker(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity target = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(source, 0, Fluids.WATER, 2000);
        Waits.when(helper, () -> TankerFixtures.total(target) == 2000 && TankerFixtures.out(helper) == 0, RUN,
                () -> "the water reaches the Storage gantry, got " + TankerFixtures.total(target), () -> {
            helper.assertTrue(TankerFixtures.total(source) == 0, "the Import gantry is empty");
            helper.assertTrue(target.tank(0).getFluid().is(Fluids.WATER), "the first empty tank on Any holds the water");
            helper.succeed();
        });
    }

    /** A half full tank locked to water wins over a nearer empty tank on Any. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void lockedTankPreferred(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity near = TankerFixtures.gantry(helper, TankerFixtures.NEAR, GantryMode.STORAGE);
        FluidGantryBlockEntity far = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        far.setLock(1, Fluids.WATER);
        TankerFixtures.fill(far, 1, Fluids.WATER, 8000);
        TankerFixtures.fill(source, 0, Fluids.WATER, 2000);
        Waits.when(helper, () -> TankerFixtures.amount(far, 1) == 10000 && TankerFixtures.out(helper) == 0, RUN,
                () -> "the water goes to the tank locked to water, got " + TankerFixtures.amount(far, 1), () -> {
            helper.assertTrue(TankerFixtures.total(near) == 0, "the nearer tank on Any stays empty");
            helper.succeed();
        });
    }

    /** A run on its way fills the only tank with room; the second Import tank's water waits instead of following it. */
    @ServusGameTest(timeoutTicks = RUN + 140)
    public static void runsOnTheWayCountAgainstRoom(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = TankerFixtures.withTanker(helper);
        garage.shuttles().setStackInSlot(1, TankerFixtures.tanker(ServusShuttlesItems.SHUTTLE_MK1.get()));
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity far = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        far.setLock(0, Fluids.WATER);
        far.setLock(1, Fluids.LAVA);
        TankerFixtures.fill(far, 0, Fluids.WATER, 12000);
        TankerFixtures.fill(source, 0, Fluids.WATER, 4000);
        TankerFixtures.fill(source, 1, Fluids.WATER, 4000);
        int[] most = {0};
        Waits.when(helper, () -> {
            most[0] = Math.max(most[0], TankerFixtures.out(helper));
            return TankerFixtures.amount(far, 0) == 16000 && TankerFixtures.out(helper) == 0;
        }, RUN, () -> "one run fills the locked tank, got " + TankerFixtures.amount(far, 0), () -> helper.runAfterDelay(100, () -> {
            helper.assertTrue(most[0] == 1, "only one tanker drove, got " + most[0]);
            helper.assertTrue(TankerFixtures.total(source) == 4000, "the other 4000 mB wait, got " + TankerFixtures.total(source));
            helper.assertTrue(TankerFixtures.amount(far, 0) == 16000, "nothing more came");
            helper.succeed();
        }));
    }

    /** 300 mB in an Import tank that is not full start no run. */
    @ServusGameTest(timeoutTicks = 200)
    public static void smallAmountWaits(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(source, 0, Fluids.WATER, 300);
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(TankerFixtures.amount(source, 0) == 300, "the water waits in the Import gantry");
            helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no run is queued");
            helper.succeed();
        });
    }

    /** A full Import tank gets a run even while its fluid stays below the min load. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void fullImportTankGoesBelowMinLoad(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity target = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        int capacity = source.tank(0).getCapacity();
        helper.runAfterDelay(2, () -> {
            TankerFixtures.network(helper).setTankerMinLoadForTests(capacity + 1);
            TankerFixtures.fill(source, 0, Fluids.WATER, capacity);
            TankerFixtures.fill(source, 1, Fluids.WATER, capacity - 1);
            Waits.when(helper, () -> TankerFixtures.total(target) == 4000 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "a Mk1 tanker brings 4000 mB of the full tank, got " + TankerFixtures.total(target), () -> {
                helper.assertTrue(TankerFixtures.amount(source, 0) == capacity - 4000, "the run took from the full tank");
                helper.assertTrue(TankerFixtures.amount(source, 1) == capacity - 1, "the tank that is not full waits");
                helper.succeed();
            });
        });
    }

    /** The source was drained to 2500 mB before the tanker came: it takes those and brings them. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void partialPickWhenDrained(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity target = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(source, 0, Fluids.WATER, 4000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            source.tank(0).drain(1500, false);
            Waits.when(helper, () -> TankerFixtures.total(target) == 2500 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "the tanker brings the 2500 mB left, got " + TankerFixtures.total(target), () -> {
                helper.assertTrue(TankerFixtures.total(source) == 0, "the source is empty");
                helper.succeed();
            });
        });
    }

    /** The locked target was filled by hand meanwhile: what fits goes in, the rest to the next tank with room. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void leftoverGoesToNextTank(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity near = TankerFixtures.gantry(helper, TankerFixtures.NEAR, GantryMode.STORAGE);
        FluidGantryBlockEntity far = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        far.setLock(0, Fluids.WATER);
        TankerFixtures.fill(source, 0, Fluids.WATER, 4000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            TankerFixtures.fill(far, 0, Fluids.WATER, 14000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 0 && TankerFixtures.total(source) == 0, RUN,
                    () -> "the tanker delivers and goes home", () -> {
                helper.assertTrue(TankerFixtures.amount(far, 0) == 16000, "the locked tank is full");
                int rest = TankerFixtures.amount(far, 1) + TankerFixtures.total(near);
                helper.assertTrue(rest == 2000, "the other 2000 mB went to the next tank, got " + rest);
                helper.succeed();
            });
        });
    }

    /** No other tank has room for the rest: it goes back into the source tank. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void leftoverBackToSource(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity far = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        far.setLock(0, Fluids.WATER);
        far.setLock(1, Fluids.LAVA);
        TankerFixtures.fill(source, 0, Fluids.WATER, 4000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            TankerFixtures.fill(far, 0, Fluids.WATER, 14000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 0 && TankerFixtures.amount(far, 0) == 16000, RUN,
                    () -> "the tanker fills the locked tank and comes home", () -> {
                helper.assertTrue(TankerFixtures.amount(source, 0) == 2000, "2000 mB are back in the source, got " + TankerFixtures.amount(source, 0));
                helper.succeed();
            });
        });
    }

    /** The target leaves Storage mode during the run: the water goes to another Storage gantry. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void targetModeChangedReroutes(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity near = TankerFixtures.gantry(helper, TankerFixtures.NEAR, GantryMode.STORAGE);
        FluidGantryBlockEntity far = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(source, 0, Fluids.WATER, 4000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            near.setMode(GantryMode.EXPORT);
            Waits.when(helper, () -> TankerFixtures.total(far) == 4000 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "the water reaches the other Storage gantry, got " + TankerFixtures.total(far), () -> {
                helper.assertTrue(TankerFixtures.total(near) == 0, "the gantry that left Storage got nothing");
                helper.succeed();
            });
        });
    }

    /** A loaded tanker that is recalled is back in its garage and its water is lost; no run is queued again by itself. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void recallLosesCargo(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity target = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(source, 0, Fluids.WATER, 3000);
        Waits.when(helper, () -> TankerFixtures.total(source) == 0, RUN, () -> "the tanker takes the water", () -> {
            TankerFixtures.network(helper).recallGarage(helper.absolutePos(TankerFixtures.GARAGE));
            helper.assertTrue(TankerFixtures.out(helper) == 0, "the tanker is back in its garage at once");
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(TankerFixtures.total(target) == 0 && TankerFixtures.total(source) == 0, "the water is lost");
                helper.assertTrue(TankerFixtures.network(helper).queuedJobs() == 0 && TankerFixtures.out(helper) == 0, "no run again");
                helper.succeed();
            });
        });
    }

    /** The water's way to the only Storage gantry goes up an elevator shaft; the tanker rides the lift car. */
    @ServusGameTest(timeoutTicks = RUN + 200)
    public static void tankerRidesTheLift(GameTestHelper helper) {
        BlockPos importRail = new BlockPos(4, 1, 4);
        BlockPos storageRail = new BlockPos(4, 3, 8);
        WarehouseFixtures.controller(helper, TankerFixtures.CONTROLLER);
        GantryFixtures.railAlongZ(helper, 4, 2, 5);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, new BlockPos(5, 1, 2), Direction.EAST, 0);
        WarehouseFixtures.stone(helper, new BlockPos(4, 0, 6));
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(4, y, 6), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        }
        for (int z = 7; z <= 9; z++) {
            for (int x = 3; x <= 5; x++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE.defaultBlockState());
            }
            helper.setBlock(new BlockPos(4, 3, z), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        }
        for (int z = 7; z <= 9; z++) {
            GantryFixtures.shape(helper, new BlockPos(4, 3, z));
        }
        garage.shuttles().setStackInSlot(0, TankerFixtures.tanker(ServusShuttlesItems.SHUTTLE_MK1.get()));
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, importRail, GantryMode.IMPORT);
        FluidGantryBlockEntity target = TankerFixtures.gantry(helper, storageRail, GantryMode.STORAGE);
        TankerFixtures.fill(source, 0, Fluids.WATER, 2000);
        Waits.when(helper, () -> TankerFixtures.total(target) == 2000, RUN + 150,
                () -> "the water rides the lift up, got " + TankerFixtures.total(target), () -> {
            helper.assertTrue(TankerFixtures.total(source) == 0, "the Import gantry is empty");
            helper.succeed();
        });
    }

    /** The warehouse's fluid stock follows every transfer: it counts the water once it is in Storage. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void indexFollowsTransfers(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity target = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(target, 1, Fluids.WATER, 1000);
        TankerFixtures.fill(source, 0, Fluids.WATER, 3000);
        Waits.when(helper, () -> TankerFixtures.network(helper).fluidIndex().count(WATER) == 1000, 40,
                () -> "the stock counts the Storage water only", () ->
                Waits.when(helper, () -> TankerFixtures.network(helper).fluidIndex().count(WATER) == 4000, RUN,
                        () -> "the stock counts the delivered water, got " + TankerFixtures.network(helper).fluidIndex().count(WATER), () -> {
                    helper.assertTrue(TankerFixtures.amount(target, 1) == 4000, "it went into the tank holding water");
                    helper.succeed();
                }));
    }
}
