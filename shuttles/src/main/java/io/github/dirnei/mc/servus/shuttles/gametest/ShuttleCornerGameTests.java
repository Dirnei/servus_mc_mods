package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleDrive;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Shuttles slow to their corner speed where their path turns and keep their speed on straight runs. */
public final class ShuttleCornerGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 6);
    private static final BlockPos TERMINAL = new BlockPos(3, 1, 7);
    private static final BlockPos GARAGE = new BlockPos(3, 1, 5);
    private static final int ROW_Z = 6;
    private static final double EPSILON = 1e-4;

    private ShuttleCornerGameTests() {
    }

    /** The Mk3 corner speed with eight Speed upgrades is 0.14 by the example numbers. */
    @ServusGameTest
    public static void shuttleJsonHasCornerSpeed(GameTestHelper helper) {
        ShuttleTiers tiers = ShuttlesStatsManager.INSTANCE.shuttleTiers();
        helper.assertTrue(Math.abs(tiers.corner() - 0.1) < EPSILON && Math.abs(tiers.cornerPerCount() - 0.005) < EPSILON,
                "shuttle.json gives corner 0.1 and 0.005 per upgrade, got " + tiers.corner() + " and " + tiers.cornerPerCount());
        helper.assertTrue(Math.abs(tiers.cornerSpeed(3, 8) - 0.14) < EPSILON, "a full Mk3 takes corners at 0.14, got " + tiers.cornerSpeed(3, 8));
        helper.succeed();
    }

    /** A Mk3 with eight Speed upgrades drives no faster than 0.14 through the corner and faster on the straights. */
    @ServusGameTest(timeoutTicks = 400)
    public static void fastShuttleSlowsAtCorner(GameTestHelper helper) {
        drive(helper, ServusShuttlesItems.SHUTTLE_MK3.get(), 8, true, (inCorner, elsewhere) -> {
            helper.assertTrue(inCorner > 0 && inCorner <= 0.14 + EPSILON, "through the corner at most 0.14, was " + inCorner);
            helper.assertTrue(elsewhere > 0.2, "faster than the corner speed on the straights, was " + elsewhere);
        });
    }

    /** A plain shuttle slows only a little, from 0.15 to 0.1. */
    @ServusGameTest(timeoutTicks = 400)
    public static void plainShuttleBarelySlows(GameTestHelper helper) {
        drive(helper, ServusShuttlesItems.SHUTTLE_MK1.get(), 0, true, (inCorner, elsewhere) -> {
            helper.assertTrue(inCorner > 0 && inCorner <= 0.1 + EPSILON, "through the corner at most 0.1, was " + inCorner);
            helper.assertTrue(elsewhere > 0.1 + EPSILON, "at its top speed of 0.15 on the straights, was " + elsewhere);
        });
    }

    /** On a straight rail a Mk3 with eight Speed upgrades reaches far beyond its corner speed. */
    @ServusGameTest(timeoutTicks = 400)
    public static void straightRunKeepsTopSpeed(GameTestHelper helper) {
        drive(helper, ServusShuttlesItems.SHUTTLE_MK3.get(), 8, false, (inCorner, elsewhere) -> {
            helper.assertTrue(inCorner == 0, "a straight path has no corner");
            helper.assertTrue(elsewhere > 0.3, "it speeds up well past its corner speed, was " + elsewhere);
        });
    }

    private interface Speeds {
        void check(double inCorner, double elsewhere);
    }

    /**
     * A warehouse with one shuttle in the garage and cobblestone in a rack at the far end of the rail: a row east
     * from the garage, then south when {@code turn} (corner at its east end), else further east. The shuttle fetches
     * the cobblestone; its highest speeds within half a block of a corner and elsewhere are checked once it is back.
     */
    private static void drive(GameTestHelper helper, Item mk, int speedUpgrades, boolean turn, Speeds speeds) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        TerminalWiring.place(helper, TERMINAL);
        WarehouseFixtures.railRow(helper, 3, turn ? 7 : 12, ROW_Z);
        BlockPos rack;
        if (turn) {
            for (int z = ROW_Z + 1; z <= 11; z++) {
                WarehouseFixtures.stone(helper, new BlockPos(7, 0, z));
                helper.setBlock(new BlockPos(7, 1, z), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
            }
            rack = new BlockPos(8, 1, 11);
            WarehouseFixtures.rack(helper, rack, Direction.WEST);
        } else {
            rack = new BlockPos(12, 1, ROW_Z + 1);
            WarehouseFixtures.rack(helper, rack, Direction.NORTH);
        }
        WarehouseFixtures.stock(helper, rack, new ItemStack(Items.COBBLESTONE, 16));
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, GARAGE, Direction.NORTH, 0);
        ItemStack shuttle = new ItemStack(mk);
        ShuttleModules.set(shuttle, new ShuttleModules(Optional.empty(), speedUpgrades, 0));
        garage.shuttles().insertItem(0, shuttle, false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        double[] highest = {0, 0};
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        helper.onEachTick(() -> controller.network().ifPresent(network -> {
            for (ShuttleView view : network.shuttleViews(helper.absolutePos(GARAGE))) {
                record(view, helper.absolutePos(GARAGE), highest);
            }
        }));

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 16);
            Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 16, 360,
                    () -> "16 cobblestone should arrive, had " + terminal.outputCount(Items.COBBLESTONE), () -> {
                        speeds.check(highest[0], highest[1]);
                        helper.succeed();
                    });
        });
    }

    /**
     * Puts the view's speed into the highest seen within half a block of a
     * corner of the rail, or elsewhere; the turn through the garage's doorway
     * onto the home rail counts as elsewhere.
     */
    private static void record(ShuttleView view, BlockPos garage, double[] highest) {
        if (!view.moving()) {
            return;
        }
        double speed = view.drive().speed();
        double at = view.segment() + view.progress();
        List<BlockPos> path = view.path();
        boolean nearCorner = false;
        for (int i = 1; i < path.size() - 1; i++) {
            BlockPos a = path.get(i - 1);
            BlockPos b = path.get(i);
            BlockPos c = path.get(i + 1);
            if (!a.equals(garage) && !c.equals(garage)
                    && ShuttleDrive.corner(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), c.getX(), c.getY(), c.getZ())
                    && Math.abs(at - i) <= 0.5) {
                nearCorner = true;
            }
        }
        int slot = nearCorner ? 0 : 1;
        highest[slot] = Math.max(highest[slot], speed);
    }
}
