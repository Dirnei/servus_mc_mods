package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidOutboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.FluidOutboundMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks.Activity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.Locale;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

/**
 * Tanker runs that restock the kept tanks of a Fluid Outbound from Fluid
 * Rack sections: amounts, runs on the way, the min load, missing stock, no
 * tanker, leftovers and an Outbound that breaks during a run.
 */
public final class TankerRestockGameTests {

    /** Ticks a test waits at most for a run to finish: a check, two stops, the drive there and home. */
    private static final int RUN = 400;

    private TankerRestockGameTests() {
    }

    private static boolean lamp(GameTestHelper helper, Activity activity) {
        return WarehouseFixtures.activity(helper, TankerFixtures.OUTBOUND) == activity;
    }

    private static Component stateLine(GameTestHelper helper, FluidOutboundBlockEntity station) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Component line = new FluidOutboundMenu(1, player.getInventory(), station).waitingLine();
        return line == null ? Component.empty() : line;
    }

    private static boolean says(Component line, String key, String fluid) {
        return line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
                && line.getString().toLowerCase(Locale.ROOT).contains(fluid);
    }

    private static boolean saysMissing(Component line, String fluid) {
        return says(line, "gui.servus_shuttles.fluid_station.state.missing", fluid);
    }

    /** A tank keeping 8 B and holding 2 B is filled from two sections in two Mk1 runs. */
    @ServusGameTest(timeoutTicks = 2 * RUN + 20)
    public static void outboundRestockedFromRacks(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.WATER, 8000);
        TankerFixtures.fill(outbound, 0, Fluids.WATER, 2000);
        TankerFixtures.rack(helper, TankerFixtures.NEAR);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 16000);
            TankerFixtures.fill(helper, TankerFixtures.NEAR, Fluids.WATER, 4000);
            Waits.when(helper, () -> TankerFixtures.amount(outbound, 0) == 8000 && TankerFixtures.out(helper) == 0, 2 * RUN,
                    () -> "the kept tank is filled to 8000 mB, got " + TankerFixtures.amount(outbound, 0), () -> {
                int stored = TankerFixtures.amount(helper, TankerFixtures.NEAR) + TankerFixtures.amount(helper, TankerFixtures.FAR);
                helper.assertTrue(stored == 14000, "6000 mB left the racks, they hold " + stored);
                helper.assertTrue(TankerFixtures.amount(outbound, 1) == 0, "the second tank keeps nothing and gets nothing");
                helper.assertTrue(lamp(helper, Activity.IDLE), "the lamp is green once the keep is met");
                helper.succeed();
            });
        });
    }

    /** With two tankers only one drives at a time for a tank, and the second run brings only what is still missing. */
    @ServusGameTest(timeoutTicks = 2 * RUN + 20)
    public static void restockCountsRunsOnTheWay(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = TankerFixtures.withTanker(helper);
        garage.shuttles().setStackInSlot(1, TankerFixtures.tanker(ServusShuttlesItems.SHUTTLE_MK1.get()));
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.WATER, 8000);
        TankerFixtures.fill(outbound, 0, Fluids.WATER, 2000);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 16000);
            int[] most = {0, 0};
            boolean[] orange = {false};
            Waits.when(helper, () -> {
                most[0] = Math.max(most[0], TankerFixtures.out(helper));
                most[1] = Math.max(most[1], TankerFixtures.amount(outbound, 0));
                orange[0] |= TankerFixtures.out(helper) > 0 && lamp(helper, Activity.WORKING);
                return TankerFixtures.amount(outbound, 0) == 8000 && TankerFixtures.out(helper) == 0;
            }, 2 * RUN, () -> "the kept tank is filled to 8000 mB, got " + TankerFixtures.amount(outbound, 0), () -> {
                helper.assertTrue(most[0] == 1, "only one tanker drove at a time, got " + most[0]);
                helper.assertTrue(most[1] == 8000, "the tank never held more than its keep, got " + most[1]);
                helper.assertTrue(orange[0], "the lamp was orange while a run went");
                helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 10000, "6000 mB left the rack, it holds "
                        + TankerFixtures.amount(helper, TankerFixtures.FAR));
                helper.succeed();
            });
        });
    }

    /** A run below the min load that would not fill the tank waits; one that fills it goes. */
    @ServusGameTest(timeoutTicks = RUN + 200)
    public static void restockBelowMinLoadWaits(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.WATER, 8000);
        TankerFixtures.fill(outbound, 0, Fluids.WATER, 2000);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.network(helper).setTankerMinLoadForTests(3000);
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 1500);
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(TankerFixtures.amount(outbound, 0) == 2000, "the kept tank still holds 2000 mB");
                helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no run is queued");
                helper.assertTrue(!lamp(helper, Activity.BLOCKED), "a rack holds water, so the lamp is not red");
                outbound.stepKeep(0, -5000);
                Waits.when(helper, () -> TankerFixtures.amount(outbound, 0) == 3000 && TankerFixtures.out(helper) == 0, RUN,
                        () -> "a 1000 mB run that fills the tank goes, got " + TankerFixtures.amount(outbound, 0), () -> {
                    helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 500, "the rack gave 1000 mB, it holds "
                            + TankerFixtures.amount(helper, TankerFixtures.FAR));
                    helper.succeed();
                });
            });
        });
    }

    /** No section holds lava: no tanker drives, the lamp is red and the GUI says lava is missing from storage. */
    @ServusGameTest(timeoutTicks = 200)
    public static void missingStockRedLamp(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.LAVA, 4000);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 4000);
            Waits.when(helper, () -> lamp(helper, Activity.BLOCKED), 120, () -> "the Outbound's lamp turns red", () -> {
                Component line = stateLine(helper, outbound);
                helper.assertTrue(saysMissing(line, "lava"), "the state line says lava is missing: " + line.getString());
                helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no tanker drives");
                helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 4000, "the water stays in the rack");
                helper.succeed();
            });
        });
    }

    /** A rack holds water but no Tanker shuttle is in the warehouse: the lamp is red and the GUI says no tanker reaches it. */
    @ServusGameTest(timeoutTicks = 200)
    public static void noTankerRedLamp(GameTestHelper helper) {
        TankerFixtures.warehouse(helper);
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.WATER, 4000);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 8000);
            Waits.when(helper, () -> lamp(helper, Activity.BLOCKED), 120, () -> "the Outbound's lamp turns red", () -> {
                Component line = stateLine(helper, outbound);
                helper.assertTrue(line.getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals("gui.servus_shuttles.fluid_station.state.no_tanker"), "the GUI says no tanker comes: " + line.getString());
                helper.assertTrue(outbound.fluidActivity().noTanker(), "the station waits for a tanker");
                helper.succeed();
            });
        });
    }

    /** Lava appears in a rack: the restock starts by itself and the lamp turns green after it. */
    @ServusGameTest(timeoutTicks = RUN + 200)
    public static void stockAppearsRestockStarts(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.LAVA, 4000);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        Waits.when(helper, () -> lamp(helper, Activity.BLOCKED), 120, () -> "the lamp turns red first", () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.LAVA, 6000);
            Waits.when(helper, () -> TankerFixtures.amount(outbound, 0) == 4000 && TankerFixtures.out(helper) == 0
                            && lamp(helper, Activity.IDLE), RUN,
                    () -> "the lava comes and the lamp turns green, got " + TankerFixtures.amount(outbound, 0), () -> {
                helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 2000, "the rack gave 4000 mB of lava");
                helper.succeed();
            });
        });
    }

    /** The source was drained to 2500 mB before the tanker came: it takes those and brings them. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void restockPartialPick(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.WATER, 8000);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 4000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
                FluidRackFixtures.bay(helper, TankerFixtures.FAR).sectionHandler().drain(1500, false);
                Waits.when(helper, () -> TankerFixtures.amount(outbound, 0) == 2500 && TankerFixtures.out(helper) == 0, RUN,
                        () -> "the tanker brings the 2500 mB left, got " + TankerFixtures.amount(outbound, 0), () -> {
                    helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 0, "the rack is empty");
                    helper.succeed();
                });
            });
        });
    }

    /** A player poured 3 B into the kept tank meanwhile: what fits goes in, the rest back to a rack. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void restockLeftoverBackToRack(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.WATER, 8000);
        TankerFixtures.fill(outbound, 0, Fluids.WATER, 4000);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 10000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
                TankerFixtures.fill(outbound, 0, Fluids.WATER, 3000);
                Waits.when(helper, () -> TankerFixtures.amount(outbound, 0) == 8000 && TankerFixtures.out(helper) == 0
                                && TankerFixtures.amount(helper, TankerFixtures.FAR) == 9000, RUN,
                        () -> "1000 mB go in and 3000 mB back to the rack, Outbound " + TankerFixtures.amount(outbound, 0)
                                + ", rack " + TankerFixtures.amount(helper, TankerFixtures.FAR), helper::succeed);
            });
        });
    }

    /** The Outbound breaks while a tanker brings it water: the water goes back into a rack. */
    @ServusGameTest(timeoutTicks = RUN * 2)
    public static void outboundBrokenMidRunReturnsCargo(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        TankerFixtures.outbound(helper, Fluids.WATER, 4000);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.fill(helper, TankerFixtures.FAR, Fluids.WATER, 10000);
            Waits.when(helper, () -> TankerFixtures.out(helper) == 1 && TankerFixtures.amount(helper, TankerFixtures.FAR) == 6000, RUN,
                    () -> "the tanker loads the water", () -> {
                helper.destroyBlock(TankerFixtures.OUTBOUND);
                Waits.when(helper, () -> TankerFixtures.out(helper) == 0, RUN, () -> "the tanker comes home", () -> {
                    helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 10000, "the water is back in the rack, it holds "
                            + TankerFixtures.amount(helper, TankerFixtures.FAR));
                    helper.assertTrue(TankerFixtures.network(helper).queuedJobs() == 0, "no run is queued again");
                    helper.succeed();
                });
            });
        });
    }

    /** Water in a Fluid Inbound is no stock: the kept tank stays empty and its lamp is red. */
    @ServusGameTest(timeoutTicks = 200)
    public static void inboundIsNoRestockSource(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidOutboundBlockEntity outbound = TankerFixtures.outbound(helper, Fluids.WATER, 4000);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.fill(source, 0, Fluids.WATER, 8000);
        Waits.when(helper, () -> lamp(helper, Activity.BLOCKED), 120, () -> "the Outbound's lamp turns red", () -> {
            helper.assertTrue(saysMissing(stateLine(helper, outbound), "water"), "the state line says water is missing");
            helper.assertTrue(TankerFixtures.out(helper) == 0, "no tanker drives");
            helper.assertTrue(TankerFixtures.amount(source, 0) == 8000 && TankerFixtures.amount(outbound, 0) == 0, "the water stays in the Inbound");
            helper.succeed();
        });
    }
}
