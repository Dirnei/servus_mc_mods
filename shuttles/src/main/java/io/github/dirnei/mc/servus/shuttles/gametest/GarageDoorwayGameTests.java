package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Shuttles stand inside their garage and pass its doorway: the door time, parking inside, no way through another garage. */
public final class GarageDoorwayGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos HOME = new BlockPos(2, 1, 4);
    private static final BlockPos RACK = new BlockPos(8, 1, 5);
    private static final BlockPos MIDDLE_GARAGE = new BlockPos(5, 1, 4);
    private static final BlockPos MIDDLE_CONTROLLER = new BlockPos(5, 1, 3);

    private GarageDoorwayGameTests() {
    }

    /** Controller, terminal, a rail along z 4 from x 2 to 8, a garage behind its west end with one shuttle, cobblestone in a rack at the east end. */
    private static void warehouse(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        TerminalWiring.place(helper, TERMINAL);
        WarehouseFixtures.railRow(helper, 2, 8, 4);
        WarehouseFixtures.garage(helper, GARAGE, Direction.SOUTH, 1);
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.COBBLESTONE, 16));
    }

    private static WarehouseNetwork network(GameTestHelper helper) {
        return helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().orElseThrow();
    }

    private static StorageTerminalBlockEntity request(GameTestHelper helper, int amount) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, TERMINAL);
        new StorageTerminalMenu(1, player.getInventory(), terminal).request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), amount);
        return terminal;
    }

    /** The shuttle's way out starts in the garage's cell and its way home ends there, through the doorway from the home rail. */
    @ServusGameTest(timeoutTicks = 400)
    public static void shuttleParksInsideItsGarage(GameTestHelper helper) {
        warehouse(helper);
        BlockPos garage = helper.absolutePos(GARAGE);
        ShuttleView[] first = {null};
        ShuttleView[] last = {null};
        helper.onEachTick(() -> helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().ifPresent(network -> {
            List<ShuttleView> views = network.shuttleViews(garage);
            if (!views.isEmpty()) {
                first[0] = first[0] == null ? views.get(0) : first[0];
                last[0] = views.get(0);
            }
        }));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = request(helper, 16);
            Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 16 && network(helper).garageOut(garage) == 0
                            && network(helper).shuttleViews(garage).isEmpty(), 360,
                    () -> "the cobblestone arrives and the shuttle parks, output " + terminal.outputCount(Items.COBBLESTONE), () -> {
                helper.assertTrue(first[0] != null && first[0].path().get(0).equals(garage) && first[0].path().get(1).equals(helper.absolutePos(HOME)),
                        "the way out starts inside the garage and passes the home rail, got " + (first[0] == null ? "none" : first[0].path()));
                List<BlockPos> home = last[0].path();
                helper.assertTrue(home.getLast().equals(garage) && home.get(home.size() - 2).equals(helper.absolutePos(HOME)),
                        "the way home ends inside the garage, through the doorway from the home rail, got " + home);
                helper.succeed();
            });
        });
    }

    /** A shuttle given a job is shown standing inside its garage for the door time, then drives out. */
    @ServusGameTest(timeoutTicks = 200)
    public static void shuttleWaitsTheDoorTimeBeforeLeaving(GameTestHelper helper) {
        warehouse(helper);
        BlockPos garage = helper.absolutePos(GARAGE);
        long[] shown = {-1};
        long[] moved = {-1};
        boolean[] stillInside = {true};
        helper.onEachTick(() -> helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().ifPresent(network -> {
            List<ShuttleView> views = network.shuttleViews(garage);
            if (views.isEmpty() || moved[0] >= 0) {
                return;
            }
            ShuttleView view = views.get(0);
            if (shown[0] < 0) {
                shown[0] = helper.getTick();
            }
            if (view.segment() > 0 || view.progress() > 0) {
                moved[0] = helper.getTick();
            } else {
                stillInside[0] &= view.path().get(0).equals(garage) && view.drive().equals(ShuttleView.Drive.STILL);
            }
        }));
        helper.runAfterDelay(10, () -> {
            request(helper, 16);
            int door = ShuttlesStatsManager.INSTANCE.doorTicks();
            Waits.when(helper, () -> moved[0] >= 0, 100, () -> "the shuttle drives out", () -> {
                long waited = moved[0] - shown[0];
                helper.assertTrue(stillInside[0], "while it waits the shuttle stands still inside its garage");
                helper.assertTrue(waited >= door && waited <= door + 2, "it waits the door time of " + door + " ticks, waited " + waited);
                helper.succeed();
            });
        });
    }

    /**
     * A job that comes in while the shuttle on its way home is already at its
     * doorway waits until it has parked: its next way out starts inside the
     * garage, after the door time.
     */
    @ServusGameTest(timeoutTicks = 600)
    public static void jobWhilePullingInWaitsForParking(GameTestHelper helper) {
        warehouse(helper);
        BlockPos garage = helper.absolutePos(GARAGE);
        StorageTerminalBlockEntity[] terminal = {null};
        List<?>[] homePath = {null};
        List<?>[] nextPath = {null};
        helper.onEachTick(() -> helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().ifPresent(network -> {
            List<ShuttleView> views = network.shuttleViews(garage);
            if (views.isEmpty() || nextPath[0] != null) {
                return;
            }
            ShuttleView view = views.get(0);
            List<BlockPos> path = view.path();
            if (homePath[0] == null) {
                double left = path.size() - 1 - view.segment() - view.progress();
                if (terminal[0] != null && path.getLast().equals(garage) && !path.getFirst().equals(garage) && left > 0 && left <= 1.5) {
                    homePath[0] = path;
                    request(helper, 8);
                }
            } else if (!path.equals(homePath[0])) {
                nextPath[0] = path;
            }
        }));
        helper.runAfterDelay(10, () -> {
            terminal[0] = request(helper, 8);
            Waits.when(helper, () -> terminal[0].outputCount(Items.COBBLESTONE) == 16 && network(helper).garageOut(garage) == 0, 560,
                    () -> "both requests arrive, output " + terminal[0].outputCount(Items.COBBLESTONE), () -> {
                helper.assertTrue(homePath[0] != null, "the second request came in while the shuttle pulled in");
                helper.assertTrue(nextPath[0] != null && nextPath[0].getFirst().equals(garage),
                        "the shuttle parks before the next job and leaves from inside, got " + nextPath[0]);
                helper.succeed();
            });
        });
    }

    /**
     * An empty garage between two rails, its doorway to the east rail and its
     * front to the west one; the rails meet only at the controller, which no
     * shuttle drives through. The shuttle of the garage on the west rail
     * cannot serve a rack on the east rail through the middle garage, so the
     * request needs a shuttle and nothing leaves.
     */
    @ServusGameTest(timeoutTicks = 150)
    public static void noShortcutThroughAGarage(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, MIDDLE_CONTROLLER);
        TerminalWiring.place(helper, TERMINAL);
        WarehouseFixtures.railRow(helper, 2, 4, 4);
        WarehouseFixtures.railOnStone(helper, MIDDLE_CONTROLLER.west());
        WarehouseFixtures.garage(helper, MIDDLE_GARAGE, Direction.WEST, 0);
        WarehouseFixtures.railRow(helper, 6, 8, 4);
        WarehouseFixtures.railOnStone(helper, MIDDLE_CONTROLLER.east());
        WarehouseFixtures.garage(helper, GARAGE, Direction.SOUTH, 1);
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.COBBLESTONE, 16));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Waits.when(helper, () -> helper.getBlockEntity(MIDDLE_CONTROLLER, WarehouseControllerBlockEntity.class).network()
                        .map(network -> network.index().count(ItemKey.of(new ItemStack(Items.COBBLESTONE))) == 16).orElse(false), 60,
                () -> "the rack behind the middle garage belongs to the warehouse", () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), TerminalWiring.at(helper, TERMINAL));
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 16);
            helper.assertTrue(menu.stock().message().equals("needs_shuttle"), "no shuttle gets to the rack, got " + menu.stock().message());
            helper.runAfterDelay(30, () -> {
                WarehouseNetwork network = helper.getBlockEntity(MIDDLE_CONTROLLER, WarehouseControllerBlockEntity.class).network().orElseThrow();
                helper.assertTrue(network.shuttleViews(helper.absolutePos(GARAGE)).isEmpty(), "the west shuttle stays inside");
                helper.assertTrue(WarehouseFixtures.count(helper.getBlockEntity(RACK, HeavyRackBlockEntity.class),
                        Items.COBBLESTONE) == 16, "the rack keeps its cobblestone");
                helper.succeed();
            });
        });
    }
}
