package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleProfile;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

/** The Tanker module: installing it and the jobs a Tanker shuttle takes. */
public final class TankerGameTests {

    private static final BlockPos WORKSHOP = new BlockPos(5, 1, 5);
    /** Menu index of hotbar slot 0. */
    private static final int HOTBAR = ShuttleWorkshopMenu.INVENTORY_START + 27;

    private TankerGameTests() {
    }

    /** A Tanker replaces the Harvester of a Mk1 shuttle; the Harvester goes to the player and the shuttle still rides lifts. */
    @ServusGameTest
    public static void tankerInstallsAsTool(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, WORKSHOP);
        player.getInventory().setItem(0, FarmFixtures.shuttleWith(ServusShuttlesItems.HARVESTER.get()));
        player.getInventory().setItem(1, new ItemStack(ServusShuttlesItems.TANKER.get()));
        menu.quickMoveStack(player, HOTBAR);
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).tool().equals(Optional.of(ServusShuttlesItems.TANKER.get())), "the Tanker is the tool");
        helper.assertTrue(player.getInventory().countItem(ServusShuttlesItems.HARVESTER.get()) == 1, "the Harvester is back with the player");
        ShuttleProfile profile = ShuttleModules.profile(menu.shuttle(), ShuttlesStatsManager.INSTANCE.shuttleTiers());
        helper.assertTrue(profile.fieldOnly() && !profile.liftless(), "a Tanker shuttle takes only its own jobs and rides lifts");
        helper.assertTrue(profile.fluidCapacity() == 4000, "a Mk1 Tanker carries 4000 mB, got " + profile.fluidCapacity());
        helper.succeed();
    }

    /** With only a Tanker shuttle in the garage, a stack in an Inbound Station and a station feed wait; the Tanker stays home. */
    @ServusGameTest(timeoutTicks = 200)
    public static void tankerTakesNoItemJobs(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(1, 1, 3), false);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, warehouse.garagePos(), Direction.NORTH, 0);
        garage.shuttles().setStackInSlot(0, TankerFixtures.tanker(ServusShuttlesItems.SHUTTLE_MK1.get()));
        warehouse.stock(new ItemStack(Items.COBBLESTONE, 32));
        InboundStationBlockEntity inbound = warehouse.withInbound();
        inbound.buffer().insertItem(new ItemStack(Items.OAK_LOG, 16), false);
        OutboundStationBlockEntity outbound = WarehouseFixtures.outbound(helper, warehouse.railPos(0).south(), Direction.SOUTH);
        helper.runAfterDelay(120, () -> {
            String refusal = warehouse.network().feedStation(outbound, ItemKey.of(new ItemStack(Items.COBBLESTONE)), 8);
            helper.assertTrue(refusal.equals("needs_shuttle"), "a feed needs a shuttle for warehouse jobs, got " + refusal);
            helper.assertTrue(WarehouseFixtures.count(inbound.buffer(), Items.OAK_LOG) == 16, "the stack waits in the Inbound Station");
            helper.assertTrue(warehouse.rackCount(Items.COBBLESTONE) == 32, "nothing left the rack");
            helper.assertTrue(warehouse.network().garageOut(helper.absolutePos(warehouse.garagePos())) == 0, "the Tanker stays home");
            helper.succeed();
        });
    }

    /** A shuttle without a Tanker never collects from a Fluid Inbound. */
    @ServusGameTest(timeoutTicks = 200)
    public static void toolessShuttleTakesNoTankerRun(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = TankerFixtures.warehouse(helper);
        garage.shuttles().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        FluidInboundBlockEntity source = TankerFixtures.inbound(helper);
        TankerFixtures.rack(helper, TankerFixtures.FAR);
        TankerFixtures.fill(source, 0, Fluids.WATER, 2000);
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(TankerFixtures.amount(source, 0) == 2000, "the water stays in the Inbound");
            helper.assertTrue(TankerFixtures.amount(helper, TankerFixtures.FAR) == 0, "the rack gets nothing");
            helper.assertTrue(TankerFixtures.out(helper) == 0, "the shuttle stays home");
            helper.succeed();
        });
    }
}
