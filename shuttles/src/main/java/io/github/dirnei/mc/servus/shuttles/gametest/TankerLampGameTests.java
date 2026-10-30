package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.zone.ShelfZones;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks.Activity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/** No room, no run: the Fluid Inbound's red lamp until room appears, and its lamp while runs go. */
public final class TankerLampGameTests {

    private static final int RUN = 400;

    private TankerLampGameTests() {
    }

    private static boolean lamp(GameTestHelper helper, Activity activity) {
        return WarehouseFixtures.activity(helper, TankerFixtures.INBOUND) == activity;
    }

    /** The only shelf is zoned for water and lava comes in; once the zone is taken off, the run starts and the lamp turns green after it. */
    @ServusGameTest(timeoutTicks = RUN + 200)
    public static void roomAppearsRunsStart(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        helper.runAfterDelay(2, () -> {
            TankerFixtures.zone(helper, TankerFixtures.FAR, Items.WATER_BUCKET);
            TankerFixtures.fill(source, Fluids.LAVA, 2000);
            Waits.when(helper, () -> lamp(helper, Activity.BLOCKED), 120, () -> "the lamp turns red first", () -> {
                helper.assertTrue(TankerFixtures.out(helper) == 0 && TankerFixtures.network(helper).queuedJobs() == 0, "no tanker drives");
                ShelfZones.set(helper.getLevel(), helper.absolutePos(TankerFixtures.FAR), ZoneList.NONE);
                Waits.when(helper, () -> TankerFixtures.amount(helper, TankerFixtures.FAR) == 2000 && lamp(helper, Activity.IDLE), RUN,
                        () -> "the lava goes to the unzoned shelf and the lamp turns green", () -> {
                    helper.assertTrue(TankerFixtures.amount(source) == 0, "the Inbound is empty");
                    helper.succeed();
                });
            });
        });
    }

    /** While a run is queued or under way the Inbound's lamp is orange; it turns green after the run. */
    @ServusGameTest(timeoutTicks = RUN + 20)
    public static void workingLampWhileRunQueued(GameTestHelper helper) {
        TankerFixtures.withTanker(helper);
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        TankerFixtures.fill(source, Fluids.WATER, 2000);
        Waits.when(helper, () -> TankerFixtures.out(helper) == 1, RUN, () -> "a tanker drives out", () -> {
            helper.assertTrue(lamp(helper, Activity.WORKING), "the Inbound's lamp is orange");
            Waits.when(helper, () -> TankerFixtures.out(helper) == 0 && lamp(helper, Activity.IDLE), RUN,
                    () -> "the lamp turns green after the run", helper::succeed);
        });
    }
}
