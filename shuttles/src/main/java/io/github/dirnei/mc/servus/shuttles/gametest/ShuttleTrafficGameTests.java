package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Shuttles sharing rails: following with a gap, passing the other way,
 * waiting behind a loading shuttle, merging in turn, a ring of followers,
 * leaving one garage in turn and aisle traffic in the planned way.
 */
public final class ShuttleTrafficGameTests {

    private static final int Z = 6;
    private static final BlockPos CONTROLLER = new BlockPos(0, 1, Z);
    private static final double GAP = 1.0;
    private static final double EPSILON = 1e-6;

    private ShuttleTrafficGameTests() {
    }

    /** What a test saw of the drawn shuttles of some garages, tick by tick. */
    private static final class Watch {
        /** The smallest distance between two drawn shuttles driving or facing the same way. */
        double closest = Double.POSITIVE_INFINITY;
        /** The smallest distance between any two drawn shuttles. */
        double closestAny = Double.POSITIVE_INFINITY;
        final Map<BlockPos, Boolean> led = new HashMap<>();
        final Map<BlockPos, Integer> lastSeen = new HashMap<>();
        final Map<BlockPos, List<ShuttleView>> now = new HashMap<>();
        int ticks;
        /** Whether the shuttles were sent; the watch counts from then on. */
        boolean active;
        /** A further check the test runs every tick after the watch has looked. */
        Runnable each = () -> {
        };
    }

    private static WarehouseNetwork network(GameTestHelper helper) {
        return helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().orElseThrow();
    }

    private static ItemStack shuttle(Item mk, int speed) {
        ItemStack shuttle = new ItemStack(mk);
        ShuttleModules.set(shuttle, new ShuttleModules(Optional.empty(), speed, 0));
        return shuttle;
    }

    private static ShuttleGarageBlockEntity garage(GameTestHelper helper, BlockPos pos, Direction facing, ItemStack... shuttles) {
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, pos, facing, 0);
        for (int slot = 0; slot < shuttles.length; slot++) {
            garage.shuttles().insertItem(slot, shuttles[slot], false);
        }
        return garage;
    }

    /** Rail positions along x at z, from x0 to x1 in that order, as absolute positions. */
    private static List<BlockPos> alongX(GameTestHelper helper, int x0, int x1, int z) {
        List<BlockPos> path = new ArrayList<>();
        int step = x1 >= x0 ? 1 : -1;
        for (int x = x0; x != x1 + step; x += step) {
            path.add(helper.absolutePos(new BlockPos(x, 1, z)));
        }
        return path;
    }

    /** Rail positions along z at x, from z0 to z1 in that order, as absolute positions. */
    private static List<BlockPos> alongZ(GameTestHelper helper, int x, int z0, int z1) {
        List<BlockPos> path = new ArrayList<>();
        int step = z1 >= z0 ? 1 : -1;
        for (int z = z0; z != z1 + step; z += step) {
            path.add(helper.absolutePos(new BlockPos(x, 1, z)));
        }
        return path;
    }

    private static List<BlockPos> join(List<BlockPos> first, List<BlockPos> second) {
        List<BlockPos> path = new ArrayList<>(first);
        path.addAll(second.subList(1, second.size()));
        return path;
    }

    private static void send(GameTestHelper helper, BlockPos garage, int slot, List<BlockPos> path) {
        helper.assertTrue(network(helper).sendShuttleAlong(helper.absolutePos(garage), slot, path), "the shuttle in slot " + slot + " leaves");
    }

    /** Where a drawn shuttle is, in block coordinates of its rail. */
    private static double[] where(ShuttleView view) {
        List<BlockPos> path = view.path();
        if (view.segment() >= path.size() - 1) {
            BlockPos end = path.get(path.size() - 1);
            return new double[] {end.getX(), end.getY(), end.getZ()};
        }
        BlockPos from = path.get(view.segment());
        BlockPos to = path.get(view.segment() + 1);
        double p = view.progress();
        return new double[] {from.getX() + (to.getX() - from.getX()) * p, from.getY() + (to.getY() - from.getY()) * p,
                from.getZ() + (to.getZ() - from.getZ()) * p};
    }

    /** The step the shuttle drives or last drove, as {dx, dz}. */
    private static int[] heading(ShuttleView view) {
        List<BlockPos> path = view.path();
        int from = Math.min(view.segment(), path.size() - 2);
        if (from < 0) {
            return new int[] {0, 0};
        }
        BlockPos a = path.get(from);
        BlockPos b = path.get(from + 1);
        return new int[] {Integer.signum(b.getX() - a.getX()), Integer.signum(b.getZ() - a.getZ())};
    }

    private static double distance(ShuttleView a, ShuttleView b) {
        double[] p = where(a);
        double[] q = where(b);
        return Math.sqrt((p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]) + (p[2] - q[2]) * (p[2] - q[2]));
    }

    /**
     * Watches the drawn shuttles of the garages every tick once it is active.
     * Called while the test is set up: the scheduler must not get new tick
     * tasks from inside a running one.
     */
    private static Watch watch(GameTestHelper helper, BlockPos... garages) {
        Watch watch = new Watch();
        helper.onEachTick(() -> {
            if (!watch.active) {
                return;
            }
            WarehouseNetwork network = network(helper);
            watch.ticks++;
            List<ShuttleView> all = new ArrayList<>();
            for (BlockPos garage : garages) {
                List<ShuttleView> views = network.shuttleViews(helper.absolutePos(garage));
                watch.now.put(garage, views);
                all.addAll(views);
                if (!views.isEmpty()) {
                    watch.lastSeen.put(garage, watch.ticks);
                }
                for (ShuttleView view : views) {
                    if (view.lead().isPresent()) {
                        watch.led.put(garage, true);
                    }
                }
            }
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    double distance = distance(all.get(i), all.get(j));
                    watch.closestAny = Math.min(watch.closestAny, distance);
                    if (Arrays.equals(heading(all.get(i)), heading(all.get(j)))) {
                        watch.closest = Math.min(watch.closest, distance);
                    }
                }
            }
            watch.each.run();
        });
        return watch;
    }

    private static boolean allHome(GameTestHelper helper, BlockPos... garages) {
        for (BlockPos garage : garages) {
            if (network(helper).garageOut(helper.absolutePos(garage)) > 0) {
                return false;
            }
        }
        return true;
    }

    /** A fast Mk3 leaving right behind a plain Mk1 on one aisle follows it a block behind and never passes it. */
    @ServusGameTest(timeoutTicks = 300)
    public static void fastShuttleFollowsSlowOne(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        WarehouseFixtures.railRow(helper, 1, 11, Z);
        BlockPos fast = new BlockPos(1, 1, Z - 1);
        BlockPos slow = new BlockPos(2, 1, Z + 1);
        garage(helper, fast, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK3.get(), 8));
        garage(helper, slow, Direction.SOUTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        Watch watch = watch(helper, fast, slow);
        helper.runAfterDelay(5, () -> {
            send(helper, fast, 0, alongX(helper, 1, 11, Z));
            send(helper, slow, 0, alongX(helper, 2, 11, Z));
            watch.active = true;
            Waits.when(helper, () -> watch.ticks > 5 && allHome(helper, fast, slow), 280,
                    () -> "both shuttles should reach the end of the aisle, closest " + watch.closest, () -> {
                helper.assertTrue(watch.closest >= GAP - EPSILON, "the fast shuttle stays a block behind, closest " + watch.closest);
                helper.assertTrue(watch.led.getOrDefault(fast, false), "the fast shuttle follows the slow one");
                helper.assertTrue(watch.lastSeen.get(fast) >= watch.lastSeen.get(slow), "the fast shuttle never passes the slow one");
                helper.succeed();
            });
        });
    }

    /** Two shuttles driving towards each other on one aisle pass without waiting. */
    @ServusGameTest(timeoutTicks = 200)
    public static void oppositeHeadingsPass(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        WarehouseFixtures.railRow(helper, 1, 11, Z);
        BlockPos west = new BlockPos(1, 1, Z - 1);
        BlockPos east = new BlockPos(11, 1, Z - 1);
        garage(helper, west, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        garage(helper, east, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        Watch watch = watch(helper, west, east);
        helper.runAfterDelay(5, () -> {
            send(helper, west, 0, alongX(helper, 1, 11, Z));
            send(helper, east, 0, alongX(helper, 11, 1, Z));
            watch.active = true;
            Waits.when(helper, () -> watch.ticks > 5 && allHome(helper, west, east), 180,
                    () -> "both shuttles should drive through, closest " + watch.closestAny, () -> {
                helper.assertTrue(watch.closestAny < 0.5, "they met on the aisle, closest " + watch.closestAny);
                helper.assertFalse(watch.led.getOrDefault(west, false) || watch.led.getOrDefault(east, false), "neither waited for the other");
                helper.succeed();
            });
        });
    }

    /**
     * A shuttle following one that stops at a bay for its pick stops a block
     * behind it, and drives on once the picker has turned back towards the
     * terminal, passing it the other way.
     */
    @ServusGameTest(timeoutTicks = 400)
    public static void waitsBehindLoadingShuttle(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        WarehouseFixtures.railRow(helper, 1, 11, Z);
        BlockPos follower = new BlockPos(1, 1, Z - 1);
        BlockPos picker = new BlockPos(2, 1, Z - 1);
        BlockPos terminalPos = new BlockPos(3, 1, Z + 1);
        BlockPos rack = new BlockPos(8, 1, Z + 1);
        TerminalWiring.place(helper, terminalPos);
        WarehouseFixtures.rack(helper, rack, Direction.NORTH);
        WarehouseFixtures.stock(helper, rack, new ItemStack(Items.COBBLESTONE, 16));
        garage(helper, follower, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        garage(helper, picker, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        double bayX = helper.absolutePos(rack).getX();
        double[] furthestWhileLoading = {Double.NEGATIVE_INFINITY};
        boolean[] sawStop = {false};
        Watch watch = watch(helper, follower, picker);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, terminalPos);
            new StorageTerminalMenu(1, player.getInventory(), terminal).request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 16);
            send(helper, follower, 0, alongX(helper, 1, 11, Z));
            watch.active = true;
            watch.each = () -> {
                List<ShuttleView> picking = watch.now.getOrDefault(picker, List.of());
                List<ShuttleView> following = watch.now.getOrDefault(follower, List.of());
                for (ShuttleView view : picking) {
                    boolean loading = !view.moving() && view.drive().equals(ShuttleView.Drive.STILL) && Math.abs(where(view)[0] - bayX) < EPSILON;
                    if (loading && !following.isEmpty()) {
                        sawStop[0] = true;
                        furthestWhileLoading[0] = Math.max(furthestWhileLoading[0], where(following.get(0))[0]);
                    }
                }
            };
            Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 16 && allHome(helper, follower, picker), 380,
                    () -> "the pick should arrive and both shuttles get home, output " + terminal.outputCount(Items.COBBLESTONE), () -> {
                helper.assertTrue(sawStop[0], "the follower was behind the picker while it loaded");
                helper.assertTrue(furthestWhileLoading[0] <= bayX - GAP + EPSILON,
                        "the follower stays a block behind the loading picker, got to x " + (furthestWhileLoading[0] - bayX) + " from the bay");
                helper.assertTrue(watch.closest >= GAP - EPSILON, "never closer than a block in one lane, closest " + watch.closest);
                helper.succeed();
            });
        });
    }

    /** At a junction the shuttle with the shorter way goes first; the other follows it a block behind. */
    @ServusGameTest(timeoutTicks = 300)
    public static void mergeGoesToCloserShuttle(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        WarehouseFixtures.railRow(helper, 1, 11, Z);
        for (int z = 1; z < Z; z++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(8, 1, z));
        }
        BlockPos main = new BlockPos(5, 1, Z - 1);
        BlockPos branch = new BlockPos(8, 1, 0);
        garage(helper, main, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        garage(helper, branch, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        BlockPos junction = helper.absolutePos(new BlockPos(8, 1, Z));
        int[] reached = {-1, -1};
        Watch watch = watch(helper, main, branch);
        helper.runAfterDelay(5, () -> {
            send(helper, main, 0, alongX(helper, 5, 11, Z));
            send(helper, branch, 0, join(alongZ(helper, 8, 1, Z), alongX(helper, 8, 11, Z)));
            watch.active = true;
            watch.each = () -> {
                note(watch.now.getOrDefault(main, List.of()), junction, reached, 0, watch.ticks);
                note(watch.now.getOrDefault(branch, List.of()), junction, reached, 1, watch.ticks);
            };
            Waits.when(helper, () -> watch.ticks > 5 && allHome(helper, main, branch), 280,
                    () -> "both shuttles should reach the end of the aisle", () -> {
                helper.assertTrue(reached[0] >= 0 && reached[1] >= 0, "both passed the junction");
                helper.assertTrue(reached[0] < reached[1], "the closer shuttle went first, ticks " + reached[0] + " and " + reached[1]);
                helper.assertTrue(watch.closest >= GAP - EPSILON, "never closer than a block in one lane, closest " + watch.closest);
                helper.assertTrue(watch.led.getOrDefault(branch, false), "the branch shuttle waited for the other");
                helper.succeed();
            });
        });
    }

    /** Notes the first tick a shuttle of the list stands on or past the junction on its way east. */
    private static void note(List<ShuttleView> views, BlockPos junction, int[] reached, int index, int tick) {
        for (ShuttleView view : views) {
            double[] at = where(view);
            if (reached[index] < 0 && Math.abs(at[2] - junction.getZ()) < EPSILON && at[0] >= junction.getX() - EPSILON) {
                reached[index] = tick;
            }
        }
    }

    /** Four shuttles round a loop of four rails, each right behind the next, keep driving round together. */
    @ServusGameTest(timeoutTicks = 300)
    public static void ringOfShuttlesKeepsMoving(GameTestHelper helper) {
        BlockPos[] loop = {new BlockPos(5, 1, 6), new BlockPos(6, 1, 6), new BlockPos(6, 1, 7), new BlockPos(5, 1, 7)};
        for (BlockPos rail : loop) {
            WarehouseFixtures.railOnStone(helper, rail);
        }
        BlockPos controller = new BlockPos(4, 1, 6);
        WarehouseFixtures.controller(helper, controller);
        BlockPos[] garages = {new BlockPos(5, 1, 5), new BlockPos(6, 1, 5), new BlockPos(6, 1, 8), new BlockPos(5, 1, 8)};
        Direction[] facings = {Direction.NORTH, Direction.NORTH, Direction.SOUTH, Direction.SOUTH};
        for (int i = 0; i < garages.length; i++) {
            garage(helper, garages[i], facings[i], shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        }
        int laps = 4;
        double[] closest = {Double.POSITIVE_INFINITY};
        boolean[] sent = {false};
        helper.onEachTick(() -> {
            if (!sent[0]) {
                return;
            }
            List<ShuttleView> all = new ArrayList<>();
            for (BlockPos garage : garages) {
                all.addAll(ringNetwork(helper, controller).shuttleViews(helper.absolutePos(garage)));
            }
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    closest[0] = Math.min(closest[0], distance(all.get(i), all.get(j)));
                }
            }
        });
        helper.runAfterDelay(5, () -> {
            WarehouseNetwork network = ringNetwork(helper, controller);
            sent[0] = true;
            for (int i = 0; i < garages.length; i++) {
                List<BlockPos> path = new ArrayList<>();
                for (int step = 0; step <= laps * loop.length; step++) {
                    path.add(helper.absolutePos(loop[(i + step) % loop.length]));
                }
                helper.assertTrue(network.sendShuttleAlong(helper.absolutePos(garages[i]), 0, path), "shuttle " + i + " leaves");
            }
            helper.runAfterDelay(100, () -> {
                for (BlockPos garage : garages) {
                    List<ShuttleView> views = network.shuttleViews(helper.absolutePos(garage));
                    helper.assertTrue(views.size() == 1 && views.get(0).segment() >= 4,
                            "every shuttle went more than a lap, the one of " + garage + " is at " + (views.isEmpty() ? "home" : views.get(0).segment()));
                }
                Waits.when(helper, () -> {
                    for (BlockPos garage : garages) {
                        if (network.garageOut(helper.absolutePos(garage)) > 0) {
                            return false;
                        }
                    }
                    return true;
                }, 180, () -> "every shuttle should finish its laps", () -> {
                    // Round a corner two shuttles a block apart on the path are 0.71 blocks apart.
                    helper.assertTrue(closest[0] >= Math.sqrt(0.5) - EPSILON, "the ring keeps its gaps, closest " + closest[0]);
                    helper.succeed();
                });
            });
        });
    }

    private static WarehouseNetwork ringNetwork(GameTestHelper helper, BlockPos controller) {
        return helper.getBlockEntity(controller, WarehouseControllerBlockEntity.class).network().orElseThrow();
    }

    /** Two shuttles sent out of one garage at once leave one after the other, the lower number first. */
    @ServusGameTest(timeoutTicks = 300)
    public static void twoShuttlesLeaveOneGarageInTurn(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        WarehouseFixtures.railRow(helper, 1, 11, Z);
        BlockPos garage = new BlockPos(1, 1, Z - 1);
        garage(helper, garage, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0),
                shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        boolean[] lowerAhead = {true};
        Watch watch = watch(helper, garage);
        helper.runAfterDelay(5, () -> {
            send(helper, garage, 0, alongX(helper, 1, 11, Z));
            send(helper, garage, 1, alongX(helper, 1, 11, Z));
            watch.active = true;
            watch.each = () -> {
                List<ShuttleView> views = watch.now.getOrDefault(garage, List.of());
                if (views.size() == 2) {
                    ShuttleView ahead = where(views.get(0))[0] > where(views.get(1))[0] ? views.get(0) : views.get(1);
                    ShuttleView behind = ahead == views.get(0) ? views.get(1) : views.get(0);
                    if (ShuttleItem.number(ahead.shuttle()).orElse(0) > ShuttleItem.number(behind.shuttle()).orElse(0)) {
                        lowerAhead[0] = false;
                    }
                }
            };
            Waits.when(helper, () -> watch.ticks > 5 && allHome(helper, garage), 280, () -> "both shuttles should reach the end", () -> {
                helper.assertTrue(watch.closest >= GAP - EPSILON, "the second leaves once the first is a block away, closest " + watch.closest);
                helper.assertTrue(lowerAhead[0], "the lower number leaves first");
                helper.succeed();
            });
        });
    }

    /**
     * Two aisles of equal length join two rails. A shuttle driving along the
     * one a planned way would take makes the plan take the other; a shuttle
     * driving it the other way does not.
     */
    @ServusGameTest(timeoutTicks = 100)
    public static void busyAisleCostsMore(GameTestHelper helper) {
        plannedAroundTraffic(helper, true);
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void oncomingTrafficIsFree(GameTestHelper helper) {
        plannedAroundTraffic(helper, false);
    }

    private static void plannedAroundTraffic(GameTestHelper helper, boolean sameWay) {
        int north = 3;
        int south = 9;
        WarehouseFixtures.railRow(helper, 2, 9, north);
        WarehouseFixtures.railRow(helper, 2, 9, south);
        for (int z = north + 1; z < south; z++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(2, 1, z));
            WarehouseFixtures.railOnStone(helper, new BlockPos(9, 1, z));
        }
        BlockPos controller = new BlockPos(1, 1, Z);
        WarehouseFixtures.controller(helper, controller);
        BlockPos northGarage = new BlockPos(5, 1, north - 1);
        BlockPos southGarage = new BlockPos(5, 1, south + 1);
        garage(helper, northGarage, Direction.NORTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        garage(helper, southGarage, Direction.SOUTH, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 0));
        BlockPos from = helper.absolutePos(new BlockPos(2, 1, Z));
        BlockPos to = helper.absolutePos(new BlockPos(9, 1, Z));
        helper.runAfterDelay(5, () -> {
            WarehouseNetwork network = helper.getBlockEntity(controller, WarehouseControllerBlockEntity.class).network().orElseThrow();
            List<BlockPos> before = network.plannedWay(from, to);
            boolean viaNorth = before.contains(helper.absolutePos(new BlockPos(6, 1, north)));
            int busy = viaNorth ? north : south;
            BlockPos garage = viaNorth ? northGarage : southGarage;
            helper.assertTrue(!before.isEmpty(), "a way leads round");
            helper.assertTrue(network.sendShuttleAlong(helper.absolutePos(garage), 0,
                    sameWay ? alongX(helper, 5, 9, busy) : alongX(helper, 5, 2, busy)), "the shuttle leaves");
            helper.runAfterDelay(12, () -> {
                List<BlockPos> after = network.plannedWay(from, to);
                if (sameWay) {
                    helper.assertTrue(!after.contains(helper.absolutePos(new BlockPos(6, 1, busy))),
                            "the plan avoids the aisle with a shuttle driving the same way, got " + after);
                } else {
                    helper.assertTrue(after.equals(before), "oncoming traffic does not change the plan, got " + after);
                }
                helper.succeed();
            });
        });
    }
}
