package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Tanker runs from a Fluid Inbound to Fluid Rack shelves: targets, amounts, leftovers, recalled runs and lifts. */
public final class TankerRunGameTests {

    private static final FluidKey WATER = FluidKey.of(new FluidStack(Fluids.WATER, 1));
    /** Ticks a test waits at most for a run to finish: a check, two stops, the drive there and home. */
    private static final int RUN = 400;

    private TankerRunGameTests() {
    }

    /** Water an Inbound holds goes to an empty unzoned shelf. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void inboundToRackByTanker(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        TankerFixtures.fill(source, Fluids.WATER, 2000);
        Waits.when(helper, () -> TankerFixtures.amount(helper, TankerFixtures.FAR) == 2000 && TankerFixtures.out(helper) == 0, RUN,
                () -> "the water reaches the shelf, got " + TankerFixtures.amount(helper, TankerFixtures.FAR), () -> {
            helper.assertTrue(TankerFixtures.amount(source) == 0, "the Inbound is empty");
            helper.succeed();
        });
    }

    /** A half full shelf zoned for water wins over a nearer empty unzoned one. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void zonedShelfPreferred(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.NEAR);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.zone(helper, TankerFixtures.FAR, Items.WATER_BUCKET);
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 8000);
            TankerFixtures.fill(source, Fluids.WATER, 2000);
            Waits.when(helper, () -> TankerFixtures.amount(helper, TankerFixtures.FAR) == 10000 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "the water goes to the zoned shelf, got " + TankerFixtures.amount(helper, TankerFixtures.FAR), () -> {
                helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.NEAR) == 0, "the nearer unzoned shelf stays empty");
                helper.succeed();
            });
        });
    }

    /** A run on its way fills the only shelf with room; the rest of the Inbound's water waits instead of following it. */
    @ServusGameTest(timeoutTicks = RUN + 140)
    public static void runsOnTheWayCountAgainstRoom(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = TankerFixtures.withTanker(helper);
        garage.shuttles().setStackInSlot(1, TankerFixtures.tanker(ServusShuttlesItems.SHUTTLE_MK1.get()));
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.zone(helper, TankerFixtures.FAR, Items.WATER_BUCKET);
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 12000);
            TankerFixtures.fill(source, Fluids.WATER, 8000);
            int[] most = {0};
            Waits.when(helper, () -> {
                most[0] = Math.max(most[0], TankerFixtures.out(helper));
                return TankerFixtures.amount(helper, TankerFixtures.FAR) == 16000 && TankerFixtures.out(helper) == 0;
            }, RUN, () -> "one run fills the shelf, got " + TankerFixtures.amount(helper, TankerFixtures.FAR), () -> helper.runAfterDelay(100, () -> {
                helper.assertTrue(most[0] == 1, "only one tanker drove, got " + most[0]);
                helper.assertTrue(TankerFixtures.amount(source) == 4000, "the other 4000 mB wait, got " + TankerFixtures.amount(source));
                helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 16000, "nothing more came");
                helper.succeed();
            }));
        });
    }

    /** A full Inbound tank gets a run even while its fluid stays below the min load. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void fullInboundTankGoesBelowMinLoad(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        int capacity = source.tank().getCapacity();
        helper.runAfterDelay(2, () -> {
            TankerFixtures.network(helper).setTankerMinLoadForTests(capacity + 1);
            TankerFixtures.fill(source, Fluids.WATER, capacity);
            Waits.when(helper, () -> TankerFixtures.amount(helper, TankerFixtures.FAR) == 4000 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "a Mk1 tanker brings 4000 mB of the full tank, got " + TankerFixtures.amount(helper, TankerFixtures.FAR), () -> {
                helper.assertTrue(TankerFixtures.amount(source) == capacity - 4000, "the run took from the full tank");
                helper.succeed();
            });
        });
    }

    /** The source was drained to 2500 mB before the tanker came: it takes those and brings them. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void partialPickWhenDrained(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        TankerFixtures.fill(source, Fluids.WATER, 4000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            source.tank().drain(1500, false);
            Waits.when(helper, () -> TankerFixtures.amount(helper, TankerFixtures.FAR) == 2500 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "the tanker brings the 2500 mB left, got " + TankerFixtures.amount(helper, TankerFixtures.FAR), () -> {
                helper.assertTrue(TankerFixtures.amount(source) == 0, "the source is empty");
                helper.succeed();
            });
        });
    }

    /** The zoned target was filled by hand meanwhile: what fits goes in, the rest to the next shelf with room. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void leftoverGoesToNextShelf(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.NEAR);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.zone(helper, TankerFixtures.FAR, Items.WATER_BUCKET);
            TankerFixtures.fill(source, Fluids.WATER, 4000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
                TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 14000);
                Waits.when(helper, () -> TankerFixtures.out(helper) == 0 && TankerFixtures.amount(source) == 0, RUN,
                        () -> "the tanker delivers and goes home", () -> {
                    helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 16000, "the zoned shelf is full");
                    int rest = TankerFixtures.amount(helper, TankerFixtures.NEAR);
                    helper.assertTrue(rest == 2000, "the other 2000 mB went to the next shelf, got " + rest);
                    helper.succeed();
                });
            });
        });
    }

    /** No other shelf has room for the rest: it goes back into the source tank. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void leftoverBackToSource(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(source, Fluids.WATER, 4000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
                TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 14000);
                Waits.when(helper, () -> TankerFixtures.out(helper) == 0 && TankerFixtures.amount(helper, TankerFixtures.FAR) == 16000, RUN,
                        () -> "the tanker fills the shelf and comes home", () -> {
                    helper.assertTrue(TankerFixtures.amount(source) == 2000, "2000 mB are back in the source, got " + TankerFixtures.amount(source));
                    helper.succeed();
                });
            });
        });
    }

    /** A loaded tanker that is recalled is back in its garage and its water is lost; no run is queued again by itself. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void recallLosesCargo(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        TankerFixtures.fill(source, Fluids.WATER, 3000);
        Waits.when(helper, () -> TankerFixtures.amount(source) == 0, RUN, () -> "the tanker takes the water", () -> {
            TankerFixtures.network(helper).recallGarage(helper.absolutePos(TankerFixtures.GARAGE));
            helper.assertTrue(TankerFixtures.out(helper) == 0, "the tanker is back in its garage at once");
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 0 && TankerFixtures.amount(source) == 0, "the water is lost");
                helper.assertTrue(TankerFixtures.network(helper).queuedJobs() == 0 && TankerFixtures.out(helper) == 0, "no run again");
                helper.succeed();
            });
        });
    }

    /** The water's way to the only shelf goes up an elevator shaft; the tanker rides the lift car. */
    @ServusGameTest(timeoutTicks = RUN + 200)
    public static void tankerRidesTheLift(GameTestHelper helper) {
        BlockPos inbound = new BlockPos(5, 1, 4);
        BlockPos bay = new BlockPos(3, 3, 8);
        WarehouseFixtures.controller(helper, TankerFixtures.CONTROLLER);
        RailFixtures.railAlongZ(helper, 4, 2, 5);
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
            RailFixtures.shape(helper, new BlockPos(4, 3, z));
        }
        garage.shuttles().setStackInSlot(0, TankerFixtures.tanker(ServusShuttlesItems.SHUTTLE_MK1.get()));
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper, inbound);
        TankerFixtures.rack(helper, bay);
        TankerFixtures.fill(source, Fluids.WATER, 2000);
        Waits.when(helper, () -> TankerFixtures.amount(helper, bay) == 2000, RUN + 150,
                () -> "the water rides the lift up, got " + TankerFixtures.amount(helper, bay), () -> {
            helper.assertTrue(TankerFixtures.amount(source) == 0, "the Inbound is empty");
            helper.succeed();
        });
    }

    /** The warehouse's fluid stock follows every transfer: it counts the water once it is in a shelf. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void indexFollowsTransfers(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 1000);
            TankerFixtures.fill(source, Fluids.WATER, 3000);
            Waits.when(helper, () -> TankerFixtures.network(helper).fluidIndex().count(WATER) == 1000, 40,
                    () -> "the stock counts the shelf's water only", () ->
                    Waits.when(helper, () -> TankerFixtures.network(helper).fluidIndex().count(WATER) == 4000, RUN,
                            () -> "the stock counts the delivered water, got " + TankerFixtures.network(helper).fluidIndex().count(WATER), () -> {
                        helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 4000, "it went into the shelf");
                        helper.succeed();
                    }));
        });
    }
}
