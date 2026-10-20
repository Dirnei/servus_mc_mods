package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.FluidGantryMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks.Activity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Tanker runs that restock kept Export gantry tanks from Storage gantries: amounts, sources, missing stock and leftovers. */
public final class TankerRestockGameTests {

    /** Ticks a test waits at most for a run to finish: a check, two stops, the drive there and home. */
    private static final int RUN = 400;
    /** The Export gantry stands over the first rail of the fixture. */
    private static final BlockPos EXPORT = TankerFixtures.IMPORT;

    private TankerRestockGameTests() {
    }

    /** An Export gantry whose left tank is locked to the fluid and keeps that level. */
    private static FluidGantryBlockEntity kept(GameTestHelper helper, BlockPos rail, Fluid fluid, int keep) {
        FluidGantryBlockEntity gantry = TankerFixtures.gantry(helper, rail, GantryMode.EXPORT);
        gantry.setLock(0, fluid);
        gantry.setKeep(0, keep);
        return gantry;
    }

    private static boolean lamp(GameTestHelper helper, BlockPos rail, Activity activity) {
        return WarehouseFixtures.activity(helper, rail.west()) == activity && WarehouseFixtures.activity(helper, rail.above()) == activity;
    }

    private static Component stateLine(GameTestHelper helper, FluidGantryBlockEntity gantry) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        return new FluidGantryMenu(1, player.getInventory(), gantry).stateLine();
    }

    private static boolean saysMissing(Component line, String fluid) {
        return line.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("gui.servus_shuttles.fluid_gantry.state.missing")
                && line.getString().toLowerCase(Locale.ROOT).contains(fluid);
    }

    /** A tank keeping 8 B and holding 2 B is filled from a Storage gantry in two Mk1 runs. */
    @ServusGameTest(timeoutTicks = 2 * RUN + 20)
    public static void exportRestockedFromStorage(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity export = kept(helper, EXPORT, Fluids.WATER, 8000);
        TankerFixtures.fill(export, 0, Fluids.WATER, 2000);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(storage, 0, Fluids.WATER, 16000);
        TankerFixtures.fill(storage, 1, Fluids.WATER, 4000);
        Waits.when(helper, () -> TankerFixtures.amount(export, 0) == 8000 && TankerFixtures.out(helper) == 0, 2 * RUN,
                () -> "the Export tank is filled to 8000 mB, got " + TankerFixtures.amount(export, 0), () -> {
            helper.assertTrue(TankerFixtures.total(storage) == 14000, "6000 mB left the Storage gantry, it holds " + TankerFixtures.total(storage));
            helper.assertTrue(TankerFixtures.amount(export, 1) == 0, "the right tank keeps nothing and gets nothing");
            helper.succeed();
        });
    }

    /** With two tankers only one drives at a time for a tank, and the second run brings only what is still missing. */
    @ServusGameTest(timeoutTicks = 2 * RUN + 20)
    public static void restockCountsRunsOnTheWay(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = TankerFixtures.withTanker(helper);
        garage.shuttles().setStackInSlot(1, TankerFixtures.tanker(ServusShuttlesItems.SHUTTLE_MK1.get()));
        FluidGantryBlockEntity export = kept(helper, EXPORT, Fluids.WATER, 8000);
        TankerFixtures.fill(export, 0, Fluids.WATER, 2000);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(storage, 0, Fluids.WATER, 16000);
        int[] most = {0, 0};
        Waits.when(helper, () -> {
            most[0] = Math.max(most[0], TankerFixtures.out(helper));
            most[1] = Math.max(most[1], TankerFixtures.amount(export, 0));
            return TankerFixtures.amount(export, 0) == 8000 && TankerFixtures.out(helper) == 0;
        }, 2 * RUN, () -> "the Export tank is filled to 8000 mB, got " + TankerFixtures.amount(export, 0), () -> {
            helper.assertTrue(most[0] == 1, "only one tanker drove at a time, got " + most[0]);
            helper.assertTrue(most[1] == 8000, "the tank never held more than its keep level, got " + most[1]);
            helper.assertTrue(TankerFixtures.total(storage) == 10000, "6000 mB left storage, it holds " + TankerFixtures.total(storage));
            helper.succeed();
        });
    }

    /** A run below the min load that would not fill the tank waits; one that fills it goes. */
    @ServusGameTest(timeoutTicks = RUN + 200)
    public static void restockBelowMinLoadWaits(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity export = kept(helper, EXPORT, Fluids.WATER, 8000);
        TankerFixtures.fill(export, 0, Fluids.WATER, 2000);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.network(helper).setTankerMinLoadForTests(3000);
            TankerFixtures.fill(storage, 0, Fluids.WATER, 1500);
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(TankerFixtures.amount(export, 0) == 2000, "the Export tank still holds 2000 mB");
                helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no run is queued");
                helper.assertTrue(!lamp(helper, EXPORT, Activity.BLOCKED), "storage holds water, so the lamp is not red");
                export.setKeep(0, 3000);
                Waits.when(helper, () -> TankerFixtures.amount(export, 0) == 3000 && TankerFixtures.out(helper) == 0, RUN,
                        () -> "a 1000 mB run that fills the tank goes, got " + TankerFixtures.amount(export, 0), () -> {
                    helper.assertTrue(TankerFixtures.total(storage) == 500, "storage gave 1000 mB, it holds " + TankerFixtures.total(storage));
                    helper.succeed();
                });
            });
        });
    }

    /** No Storage tank holds lava: no tanker drives, the Export lamp is red and the GUI names lava. */
    @ServusGameTest(timeoutTicks = 200)
    public static void missingStockRedLamp(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity export = kept(helper, EXPORT, Fluids.LAVA, 4000);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(storage, 0, Fluids.WATER, 4000);
        Waits.when(helper, () -> lamp(helper, EXPORT, Activity.BLOCKED), 120, () -> "the Export gantry's lamp turns red", () -> {
            Component line = stateLine(helper, export);
            helper.assertTrue(saysMissing(line, "lava"), "the state line says lava is missing: " + line.getString());
            helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no tanker drives");
            helper.assertTrue(TankerFixtures.total(storage) == 4000, "the water stays in storage");
            helper.succeed();
        });
    }

    /** Lava appears in storage: the restock starts by itself and the lamp turns green after it. */
    @ServusGameTest(timeoutTicks = RUN + 200)
    public static void stockAppearsRestockStarts(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity export = kept(helper, EXPORT, Fluids.LAVA, 4000);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        Waits.when(helper, () -> lamp(helper, EXPORT, Activity.BLOCKED), 120, () -> "the lamp turns red first", () -> {
            TankerFixtures.fill(storage, 1, Fluids.LAVA, 6000);
            Waits.when(helper, () -> TankerFixtures.amount(export, 0) == 4000 && TankerFixtures.out(helper) == 0
                            && lamp(helper, EXPORT, Activity.IDLE), RUN,
                    () -> "the lava comes and the lamp turns green, got " + TankerFixtures.amount(export, 0), () -> {
                helper.assertTrue(TankerFixtures.amount(storage, 1) == 2000, "storage gave 4000 mB of lava");
                helper.succeed();
            });
        });
    }

    /** The source was drained to 2500 mB before the tanker came: it takes those and brings them. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void restockPartialPick(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity export = kept(helper, EXPORT, Fluids.WATER, 8000);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(storage, 0, Fluids.WATER, 4000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            storage.tank(0).drain(1500, false);
            Waits.when(helper, () -> TankerFixtures.amount(export, 0) == 2500 && TankerFixtures.out(helper) == 0, RUN,
                    () -> "the tanker brings the 2500 mB left, got " + TankerFixtures.amount(export, 0), () -> {
                helper.assertTrue(TankerFixtures.total(storage) == 0, "storage is empty");
                helper.succeed();
            });
        });
    }

    /** A player poured 3 B into the kept tank meanwhile: what fits goes in, the rest back to a Storage tank. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void restockLeftoverBackToStorage(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity export = kept(helper, EXPORT, Fluids.WATER, 16000);
        TankerFixtures.fill(export, 0, Fluids.WATER, 12000);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(storage, 0, Fluids.WATER, 10000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            TankerFixtures.fill(export, 0, Fluids.WATER, 3000);
            Waits.when(helper, () -> TankerFixtures.amount(export, 0) == 16000 && TankerFixtures.out(helper) == 0
                            && TankerFixtures.total(storage) == 9000, RUN,
                    () -> "1000 mB go in and 3000 mB back to storage, Export " + TankerFixtures.amount(export, 0)
                            + ", storage " + TankerFixtures.total(storage), helper::succeed);
        });
    }

    /** Water in an Import gantry is no stock: the kept tank stays empty and its lamp is red. */
    @ServusGameTest(timeoutTicks = 200)
    public static void importIsNoRestockSource(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity export = kept(helper, TankerFixtures.NEAR, Fluids.WATER, 4000);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.IMPORT);
        TankerFixtures.fill(source, 0, Fluids.WATER, 8000);
        Waits.when(helper, () -> lamp(helper, TankerFixtures.NEAR, Activity.BLOCKED), 120, () -> "the Export gantry's lamp turns red", () -> {
            helper.assertTrue(saysMissing(stateLine(helper, export), "water"), "the state line says water is missing");
            helper.assertTrue(TankerFixtures.out(helper) == 0, "no tanker drives");
            helper.assertTrue(TankerFixtures.amount(source, 0) == 8000 && TankerFixtures.amount(export, 0) == 0, "the water stays in the Import gantry");
            helper.succeed();
        });
    }
}
