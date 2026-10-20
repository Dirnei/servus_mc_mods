package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.menu.FluidGantryMenu;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks.Activity;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

/** No room, no run: the Import gantry's red lamp and state line, and the lamps while runs go. */
public final class TankerLampGameTests {

    private static final int RUN = 400;

    private TankerLampGameTests() {
    }

    /** The lamp of the gantry over that rail, on the master and on the beam that shows it. */
    private static boolean lamp(GameTestHelper helper, BlockPos rail, Activity activity) {
        return WarehouseFixtures.activity(helper, rail.west()) == activity && WarehouseFixtures.activity(helper, rail.above()) == activity;
    }

    private static Component stateLine(GameTestHelper helper, FluidGantryBlockEntity gantry) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        return new FluidGantryMenu(1, player.getInventory(), gantry).stateLine();
    }

    /** Every Storage tank is locked to water and lava comes in: no tanker drives, the lamp is red and the GUI names lava. */
    @ServusGameTest(timeoutTicks = 200)
    public static void noRoomNoRunRedLamp(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        storage.setLock(0, Fluids.WATER);
        storage.setLock(1, Fluids.WATER);
        TankerFixtures.fill(source, 0, Fluids.LAVA, 2000);
        Waits.when(helper, () -> lamp(helper, TankerFixtures.IMPORT, Activity.BLOCKED), 120, () -> "the Import gantry's lamp turns red", () -> {
            Component line = stateLine(helper, source);
            helper.assertTrue(line.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("gui.servus_shuttles.fluid_gantry.state.no_room"), "the state line says there is no room: " + line.getString());
            helper.assertTrue(line.getString().toLowerCase(Locale.ROOT).contains("lava"), "it names lava: " + line.getString());
            helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no tanker drives");
            helper.assertTrue(TankerFixtures.amount(source, 0) == 2000, "the lava stays in the Import gantry");
            helper.succeed();
        });
    }

    /** Room appears when a tank goes back to Any: the run starts by itself and the lamp turns green after it. */
    @ServusGameTest(timeoutTicks = RUN + 200)
    public static void roomAppearsRunsStart(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        FluidGantryBlockEntity storage = TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        storage.setLock(0, Fluids.WATER);
        storage.setLock(1, Fluids.WATER);
        TankerFixtures.fill(source, 0, Fluids.LAVA, 2000);
        Waits.when(helper, () -> lamp(helper, TankerFixtures.IMPORT, Activity.BLOCKED), 120, () -> "the lamp turns red first", () -> {
            storage.setLock(1, null);
            Waits.when(helper, () -> TankerFixtures.amount(storage, 1) == 2000 && lamp(helper, TankerFixtures.IMPORT, Activity.IDLE), RUN,
                    () -> "the lava goes to the tank on Any and the lamp turns green", () -> {
                helper.assertTrue(TankerFixtures.total(source) == 0, "the Import gantry is empty");
                helper.succeed();
            });
        });
    }

    /** While a run is queued or under way the lamps of its source and target are orange. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void workingLampWhileRunQueued(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidGantryBlockEntity source = TankerFixtures.gantry(helper, TankerFixtures.IMPORT, GantryMode.IMPORT);
        TankerFixtures.gantry(helper, TankerFixtures.FAR, GantryMode.STORAGE);
        TankerFixtures.fill(source, 0, Fluids.WATER, 2000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            helper.assertTrue(lamp(helper, TankerFixtures.IMPORT, Activity.WORKING), "the Import gantry's lamp is orange");
            helper.assertTrue(lamp(helper, TankerFixtures.FAR, Activity.WORKING), "the Storage gantry's lamp is orange");
            Waits.when(helper, () -> TankerFixtures.out(helper) == 0 && lamp(helper, TankerFixtures.IMPORT, Activity.IDLE)
                            && lamp(helper, TankerFixtures.FAR, Activity.IDLE), RUN,
                    () -> "both lamps turn green after the run", helper::succeed);
        });
    }
}
