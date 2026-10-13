package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.power.WakeCost;
import io.github.dirnei.mc.servus.storage.production.ProductionRows;
import io.github.dirnei.mc.servus.storage.production.ProductionView;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.site.SiteChunks;
import io.github.dirnei.mc.servus.storage.site.SiteRecord;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.SiteWakes;
import io.github.dirnei.mc.servus.storage.site.WakeAllowance;
import io.github.dirnei.mc.servus.storage.site.WakeMachine;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Warehouse power: every consumer pays FE from the controller's buffer
 * (shuttles per block, Crafting Stations per working tick, wakes per chunk and
 * tick), each stops on its own refused draw and goes on by itself, and the
 * no power state shows on the lamp, the GUI and the terminal while the stock
 * stays visible and racks stay usable by hand. Expected FE follow the numbers
 * in {@code storage/power.json}.
 */
public final class WarehousePowerGameTests {

    // A long line: rails x 1 to 7 at z 3, the rack at x 1 facing them, the
    // controller and the garage north of x 6 and 7, the Packing Station with
    // its terminal south of x 7. A pick drives six rails out and six back.
    private static final int RAIL_Z = 3;
    private static final BlockPos L_RACK = new BlockPos(1, 1, 2);
    private static final BlockPos L_CONTROLLER = new BlockPos(6, 1, 2);
    private static final BlockPos L_GARAGE = new BlockPos(7, 1, 2);
    private static final BlockPos L_PACKING = new BlockPos(7, 1, 4);
    private static final ItemKey COBBLE = ItemKey.of(new ItemStack(Items.COBBLESTONE));
    /** The mini warehouse of the small tests. */
    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);
    /** The site of the wake tests: a controller alone, one chunk. */
    private static final BlockPos SITE = new BlockPos(2, 1, 2);
    private static final int GRACE = 20;

    private WarehousePowerGameTests() {
    }

    // Helpers

    private static long perBlock() {
        return (long) Math.ceil(StorageStatsManager.INSTANCE.shuttleFePerBlock());
    }

    private static long perCraftTick() {
        return (long) Math.ceil(StorageStatsManager.INSTANCE.craftingStationFePerTick());
    }

    private static WarehouseControllerBlockEntity longLine(GameTestHelper helper, int cobble) {
        for (int x = 1; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 0, RAIL_Z), Blocks.STONE.defaultBlockState());
            TerminalWiring.rail(helper, new BlockPos(x, 1, RAIL_Z));
        }
        helper.setBlock(L_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.SOUTH));
        helper.setBlock(L_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, L_CONTROLLER);
        helper.setBlock(L_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        helper.assertTrue(helper.getBlockEntity(L_GARAGE, ShuttleGarageBlockEntity.class).addShuttle(), "the garage takes a shuttle");
        TerminalWiring.place(helper, L_PACKING);
        if (cobble > 0) {
            MiniWarehouse.stock(helper, L_RACK, new ItemStack(Items.COBBLESTONE, cobble));
        }
        return helper.getBlockEntity(L_CONTROLLER, WarehouseControllerBlockEntity.class);
    }

    private static int capacity(WarehouseControllerBlockEntity controller) {
        return controller.energy().capacity();
    }

    private static StorageTerminalMenu menu(GameTestHelper helper, StorageTerminalBlockEntity terminal) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        return new StorageTerminalMenu(1, player.getInventory(), terminal);
    }

    private static int out(GameTestHelper helper, WarehouseControllerBlockEntity controller, BlockPos garage) {
        return controller.network().map(network -> network.garageOut(helper.absolutePos(garage))).orElse(-1);
    }

    private static StorageNodeBlocks.Activity lamp(GameTestHelper helper, BlockPos controller) {
        return helper.getBlockState(controller).getValue(StorageNodeBlocks.ACTIVITY);
    }

    private static WarehouseControllerBlockEntity site(GameTestHelper helper) {
        StorageStatsManager.INSTANCE.setWakeGraceTicks(GRACE);
        helper.setBlock(SITE, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, SITE);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(SITE, WarehouseControllerBlockEntity.class);
        controller.setTeam(UUID.randomUUID());
        controller.upgrades().insertItem(0, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
        return controller;
    }

    private static Optional<SiteWakes.Status> wake(GameTestHelper helper) {
        return SiteWakes.status(helper.getLevel().dimension(), helper.absolutePos(SITE));
    }

    private static long wakePerTick(int chunks) {
        return WakeCost.perTick(chunks, StorageStatsManager.INSTANCE.wakeFePerChunkPerTick());
    }

    // Idle

    /** A warehouse with stock and a shuttle that does nothing pays nothing. */
    @ServusGameTest(timeoutTicks = 200)
    public static void idleWarehouseDrawsNothing(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal().stock(new ItemStack(Items.COBBLESTONE, 64));
        WarehouseControllerBlockEntity controller = warehouse.controller();
        controller.setEnergy(5000);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(controller.energyStored() == 5000, "an idle warehouse keeps its 5000 FE, has " + controller.energyStored());
            helper.assertTrue(controller.usedLastSecond() == 0, "nothing used, readout " + controller.usedLastSecond());
            helper.assertTrue(!controller.noPower(), "not in the no power state");
            helper.succeed();
        });
    }

    // Shuttles

    /** Six rails out to the rack and six back to the Packing Station: twelve blocks paid. */
    @ServusGameTest(timeoutTicks = 400)
    public static void shuttleTripCostsPerBlock(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = longLine(helper, 16);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, L_PACKING);
            StorageTerminalMenu menu = menu(helper, terminal);
            menu.request(COBBLE, 16);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request goes through, got " + menu.stock().message());
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 16 && out(helper, controller, L_GARAGE) == 0, 360,
                    () -> "the cobblestone should arrive and the shuttle park, output " + terminal.outputCount(Items.COBBLESTONE), () -> {
                long paid = capacity(controller) - controller.energyStored();
                helper.assertTrue(paid == 12 * perBlock(), "twelve blocks cost " + 12 * perBlock() + " FE, paid " + paid);
                helper.succeed();
            });
        });
    }

    /** With FE for eight blocks the shuttle stops with its cargo on the way back and finishes once FE arrives. */
    @ServusGameTest(timeoutTicks = 600)
    public static void shuttleStopsAndResumesWithCargo(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = longLine(helper, 16);
        HeavyRackBlockEntity rack = helper.getBlockEntity(L_RACK, HeavyRackBlockEntity.class);
        helper.runAfterDelay(10, () -> {
            controller.setEnergy((int) (8 * perBlock() + perBlock() / 2));
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, L_PACKING);
            menu(helper, terminal).request(COBBLE, 16);
            TerminalWiring.when(helper, () -> controller.noPower() && MiniWarehouse.count(rack, Items.COBBLESTONE) == 0, 300,
                    () -> "the shuttle should load and then run out of FE, stored " + controller.energyStored(), () -> {
                helper.runAfterDelay(40, () -> {
                    helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "the cobblestone has not arrived");
                    helper.assertTrue(out(helper, controller, L_GARAGE) == 1, "the shuttle stands out on the rail");
                    helper.assertTrue(controller.energyStored() < perBlock(), "nothing was taken for a refused block, stored " + controller.energyStored());
                    WarehouseNetwork network = controller.network().orElseThrow();
                    helper.assertTrue(network.shuttleViews(helper.absolutePos(L_GARAGE)).stream().anyMatch(view -> view.cargo().is(Items.COBBLESTONE)),
                            "the stopped shuttle keeps its cargo");
                    PowerFixtures.fill(controller);
                    TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 16, 200,
                            () -> "with FE back the shuttle delivers, output " + terminal.outputCount(Items.COBBLESTONE), helper::succeed);
                });
            });
        });
    }

    /** In the no power state a request is taken but no shuttle leaves; the job starts once FE comes. */
    @ServusGameTest(timeoutTicks = 400)
    public static void noNewJobsWithoutPower(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal().stock(new ItemStack(Items.COBBLESTONE, 32));
        WarehouseControllerBlockEntity controller = warehouse.controller();
        helper.runAfterDelay(10, () -> {
            controller.setEnergy(0);
            StorageTerminalBlockEntity terminal = warehouse.terminal();
            StorageTerminalMenu menu = menu(helper, terminal);
            menu.request(COBBLE, 32);
            StockSyncPayload stock = menu.stock();
            helper.assertTrue(stock.message().isEmpty(), "the request is accepted, got " + stock.message());
            List<RequestRow> rows = stock.requests();
            helper.assertTrue(rows.size() == 1 && rows.get(0).state().equals("NO_POWER"), "the request waits for power, got " + rows);
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(out(helper, controller, warehouse.garagePos()) == 0, "no shuttle left the garage");
                helper.assertTrue(warehouse.rackCount(Items.COBBLESTONE) == 32, "the rack still holds the stock");
                PowerFixtures.fill(controller);
                TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 32, MiniWarehouse.TRIP_TICKS * 2,
                        () -> "with FE the job starts and delivers, output " + terminal.outputCount(Items.COBBLESTONE), helper::succeed);
            });
        });
    }

    // Crafting Stations

    /** A station on the CPU test line, with the sticks job and planks for {@code crafts} crafts. */
    private static CraftingStationBlockEntity craftingLine(GameTestHelper helper, int planks, int crafts) {
        CraftingCpuGameTests.setup(helper, false);
        CraftingStationBlockEntity station = helper.getBlockEntity(CraftingCpuGameTests.STATION, CraftingStationBlockEntity.class);
        if (planks > 0) {
            station.inputs().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, planks));
        }
        StationJobs.sticksJob(station, crafts);
        return station;
    }

    private static WarehouseControllerBlockEntity cpuController(GameTestHelper helper) {
        return helper.getBlockEntity(CraftingCpuGameTests.CONTROLLER, WarehouseControllerBlockEntity.class);
    }

    /** One craft of sticks pays every working tick: work ticks times the FE per tick. */
    @ServusGameTest(timeoutTicks = 200)
    public static void craftCostsPerWorkingTick(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingLine(helper, 2, 1);
        WarehouseControllerBlockEntity controller = cpuController(helper);
        TerminalWiring.when(helper, () -> station.outputs().getStackInSlot(0).getCount() == 4, station.workTicks() + 60,
                () -> "the station crafts four sticks, progress " + station.progress() + " waits " + station.waitingForPower(), () -> {
            long paid = capacity(controller) - controller.energyStored();
            long expected = station.workTicks() * perCraftTick();
            helper.assertTrue(paid == expected, "one craft costs " + expected + " FE, paid " + paid);
            helper.succeed();
        });
    }

    /** Power lost part way: the craft stops with its progress and finishes the rest once FE arrives. */
    @ServusGameTest(timeoutTicks = 300)
    public static void craftPausesAndResumesProgress(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingLine(helper, 2, 1);
        WarehouseControllerBlockEntity controller = cpuController(helper);
        int cut = station.workTicks() * 3 / 4;
        TerminalWiring.when(helper, () -> station.progress() >= cut, station.workTicks() + 40, () -> "the craft should get going", () -> {
            controller.setEnergy(0);
            int kept = station.progress();
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(station.progress() == kept, "the progress stays at " + kept + ", is " + station.progress());
                helper.assertTrue(station.waitingForPower(), "the station says it waits for power");
                helper.assertTrue(station.outputs().getStackInSlot(0).isEmpty(), "nothing crafted yet");
                helper.assertTrue(!helper.getBlockState(CraftingCpuGameTests.STATION).getValue(
                        io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock.LIT), "the working face is off");
                PowerFixtures.fill(controller);
                TerminalWiring.when(helper, () -> station.outputs().getStackInSlot(0).getCount() == 4, station.workTicks() - kept + 10,
                        () -> "the craft finishes the rest, progress " + station.progress(), () -> {
                    helper.assertTrue(!station.waitingForPower(), "no longer waiting");
                    helper.succeed();
                });
            });
        });
    }

    /** A station that holds a job but no inputs pays nothing. */
    @ServusGameTest(timeoutTicks = 200)
    public static void waitingForInputsIsFree(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingLine(helper, 0, 2);
        WarehouseControllerBlockEntity controller = cpuController(helper);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(station.working(), "the station holds its job");
            helper.assertTrue(controller.energyStored() == capacity(controller), "nothing paid, stored " + controller.energyStored());
            helper.assertTrue(!station.waitingForPower(), "it waits for inputs, not for power");
            helper.succeed();
        });
    }

    /** A CPU step on a station waits for power with its order kept, the Production Terminal says so, and it finishes. */
    @ServusGameTest(timeoutTicks = 1200)
    public static void cpuStepWaitsForPowerAndFinishes(GameTestHelper helper) {
        CraftingCpuGameTests.setup(helper, true);
        MiniWarehouse.stock(helper, CraftingCpuGameTests.RACK_A, new ItemStack(Items.OAK_PLANKS, 10));
        CraftingStationBlockEntity station = helper.getBlockEntity(CraftingCpuGameTests.STATION, CraftingStationBlockEntity.class);
        WarehouseControllerBlockEntity controller = cpuController(helper);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, CraftingCpuGameTests.ACCESS);
            menu(helper, terminal).request(CraftingCpuGameTests.STICK, 4);
            CpuBlockEntity cpu = CraftingCpuGameTests.cpu(helper, CraftingCpuGameTests.CPU_A);
            helper.assertTrue(cpu.book().orders().size() == 1, "one order, got " + cpu.book().orders());
            WorkOrder order = cpu.book().orders().get(0);
            TerminalWiring.when(helper, () -> station.progress() >= 5, 900, () -> "the station should start the step", () -> {
                controller.setEnergy(0);
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(station.waitingForPower(), "the station waits for power");
                    helper.assertTrue(order.open(), "the order stays open");
                    List<ProductionRows.JobRow> jobs = ProductionView.of(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2))).jobs();
                    helper.assertTrue(jobs.stream().anyMatch(job -> job.status().equals("no_power")),
                            "the Production Terminal shows the step waiting for power, got " + jobs.stream().map(ProductionRows.JobRow::status).toList());
                    PowerFixtures.fill(controller);
                    TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) == 4 && !order.open(), 400,
                            () -> "the sticks arrive after FE comes back, output " + terminal.outputCount(Items.STICK), helper::succeed);
                });
            });
        });
    }

    // Waking

    /** A held wake of a one chunk site pays the chunk's FE every tick. */
    @ServusGameTest(timeoutTicks = 200)
    public static void wakeCostsPerChunkPerTick(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = site(helper);
        Object token = new Object();
        helper.runAfterDelay(3, () -> {
            SiteWakes.Status held = SiteWakes.hold(helper.getLevel(), helper.absolutePos(SITE), token).orElseThrow();
            helper.assertTrue(held.awake(), "the site wakes, was " + held);
            helper.runAfterDelay(5, () -> {
                int before = controller.energyStored();
                helper.runAfterDelay(20, () -> {
                    long paid = before - controller.energyStored();
                    long expected = 20 * wakePerTick(controller.span().size());
                    SiteWakes.release(token);
                    helper.assertTrue(paid == expected, "twenty ticks awake cost " + expected + " FE, paid " + paid);
                    helper.succeed();
                });
            });
        });
    }

    /** A controller with less than one second of FE does not wake; with enough it does. A sleeping site's record decides for it. */
    @ServusGameTest(timeoutTicks = 200)
    public static void wakeRefusedWithoutStoredFe(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = site(helper);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(3, () -> {
            long toStart = WakeCost.toStart(controller.span().size(), StorageStatsManager.INSTANCE.wakeFePerChunkPerTick());
            controller.setEnergy((int) toStart - 1);
            SiteWakes.Status refused = SiteWakes.request(level, helper.absolutePos(SITE)).orElseThrow();
            helper.assertTrue(!refused.awake() && refused.reason() == WakeAllowance.Reason.NO_POWER && refused.forced() == 0,
                    "refused for no power, was " + refused);
            controller.setEnergy(5000);
            SiteWakes.Status woken = SiteWakes.request(level, helper.absolutePos(SITE)).orElseThrow();
            helper.assertTrue(woken.awake(), "with 5000 FE it wakes, was " + woken);
            // A sleeping site of 9 chunks far away with 100 FE in its record.
            BlockPos camp = helper.absolutePos(SITE).offset(320000, 0, 0);
            Set<Long> span = new HashSet<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    span.add(SiteChunks.ofBlock(camp.getX() + dx * 16, camp.getZ() + dz * 16));
                }
            }
            SiteRegistry.of(level).remember(level.dimension(), camp, "Dry Camp", controller.team().orElseThrow(), span, 1, Map.of(),
                    true, true, true, 100);
            SiteWakes.Status camped = SiteWakes.request(level, camp).orElseThrow();
            helper.assertTrue(!camped.awake() && camped.reason() == WakeAllowance.Reason.NO_POWER && camped.forced() == 0,
                    "the sleeping camp with 100 FE does not wake, was " + camped);
            SiteRegistry.of(level).remove(level.dimension(), camp);
            SiteWakes.forget(level, camp);
            helper.succeed();
        });
    }

    /** A held wake whose controller runs dry ends at once, releases its chunks and says no power. */
    @ServusGameTest(timeoutTicks = 300)
    public static void wakeEndsWhenDry(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = site(helper);
        Object token = new Object();
        helper.runAfterDelay(3, () -> {
            long toStart = WakeCost.toStart(controller.span().size(), StorageStatsManager.INSTANCE.wakeFePerChunkPerTick());
            controller.setEnergy((int) (toStart * 2));
            SiteWakes.Status held = SiteWakes.hold(helper.getLevel(), helper.absolutePos(SITE), token).orElseThrow();
            helper.assertTrue(held.awake(), "the site wakes, was " + held);
            TerminalWiring.when(helper, () -> wake(helper).map(status -> status.state() == WakeMachine.State.WAITING).orElse(false), 200,
                    () -> "the wake ends once the FE is gone, was " + wake(helper) + ", stored " + controller.energyStored(), () -> {
                SiteWakes.Status status = wake(helper).orElseThrow();
                helper.assertTrue(status.reason() == WakeAllowance.Reason.NO_POWER, "the reason is no power, was " + status);
                helper.assertTrue(status.forced() == 0, "every chunk is released");
                SiteWakes.release(token);
                helper.succeed();
            });
        });
    }

    /** While online the site's record keeps the controller's FE with the stock snapshot. */
    @ServusGameTest(timeoutTicks = 300)
    public static void recordKeepsEnergyWhileAsleep(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = site(helper);
        helper.runAfterDelay(3, () -> {
            controller.setEnergy(4321);
            TerminalWiring.when(helper, () -> record(helper).map(site -> site.energy() == 4321).orElse(false),
                    StorageStatsManager.DEFAULT_SITE_SNAPSHOT_INTERVAL * 2 + 20,
                    () -> "the record keeps 4321 FE, has " + record(helper).map(SiteRecord::energy), helper::succeed);
        });
    }

    private static Optional<SiteRecord> record(GameTestHelper helper) {
        return SiteRegistry.of(helper.getLevel()).site(helper.getLevel().dimension(), helper.absolutePos(SITE));
    }

    // The no power state

    /** The lamp is red with an empty buffer, the GUI says no power, and both clear once FE comes. */
    @ServusGameTest(timeoutTicks = 100)
    public static void lampRedWithoutPower(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        WarehouseControllerBlockEntity controller = warehouse.controller();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(lamp(helper, warehouse.controllerPos()) == StorageNodeBlocks.Activity.IDLE, "green with FE");
            controller.setEnergy(0);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(lamp(helper, warehouse.controllerPos()) == StorageNodeBlocks.Activity.BLOCKED, "red without FE");
                helper.assertTrue(WarehouseControllerMenu.status(controller).noPower(), "the GUI says no power");
                helper.assertTrue(controller.network().orElseThrow().summary().getString().contains("no power"), "the summary says no power");
                PowerFixtures.fill(controller);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(lamp(helper, warehouse.controllerPos()) == StorageNodeBlocks.Activity.IDLE, "green again");
                    helper.assertTrue(!WarehouseControllerMenu.status(controller).noPower(), "the GUI line is gone");
                    helper.succeed();
                });
            });
        });
    }

    /** Without power the terminal still shows the stock and says the warehouse has no power. */
    @ServusGameTest(timeoutTicks = 100)
    public static void terminalShowsNoPowerAndKeepsStock(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal().stock(new ItemStack(Items.IRON_INGOT, 20));
        helper.runAfterDelay(10, () -> {
            warehouse.controller().setEnergy(0);
            StockSyncPayload stock = menu(helper, warehouse.terminal()).stock();
            helper.assertTrue(stock.online() && stock.noPower(), "online and without power, got online " + stock.online() + " no power " + stock.noPower());
            long iron = stock.entries().stream().filter(entry -> entry.stack().is(Items.IRON_INGOT)).mapToLong(StockEntry::count).sum();
            helper.assertTrue(iron == 20, "the grid still shows 20 iron, got " + iron);
            PowerFixtures.fill(warehouse.controller());
            helper.runAfterDelay(25, () -> {
                helper.assertTrue(!menu(helper, warehouse.terminal()).stock().noPower(), "the line is gone with FE");
                helper.succeed();
            });
        });
    }

    /** Without power a player still opens a rack's section and takes items out by hand. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackUsableByHandWithoutPower(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).stock(new ItemStack(Items.IRON_INGOT, 20));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(5, () -> {
            warehouse.controller().setEnergy(0);
            helper.assertTrue(warehouse.controller().noPower(), "no power");
            HeavyRackMenu menu = HeavyRackMenu.forSection(1, player.getInventory(), warehouse.rack());
            int slot = -1;
            for (int i = 0; i < menu.slots.size(); i++) {
                if (menu.getSlot(i).getItem().is(Items.IRON_INGOT)) {
                    slot = i;
                    break;
                }
            }
            helper.assertTrue(slot >= 0, "the rack's GUI shows the iron");
            menu.quickMoveStack(player, slot);
            helper.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 20, "the player took the iron, has "
                    + player.getInventory().countItem(Items.IRON_INGOT));
            helper.assertTrue(warehouse.rackCount(Items.IRON_INGOT) == 0, "the rack is empty");
            helper.succeed();
        });
    }

    /** The readout sums the FE drawn during the last second and forgets older draws. */
    @ServusGameTest(timeoutTicks = 100)
    public static void useReadoutCountsLastSecond(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        WarehouseControllerBlockEntity controller = warehouse.controller();
        helper.runAfterDelay(5, () -> {
            controller.setEnergy(1000);
            helper.assertTrue(controller.drawEnergy(100) && controller.drawEnergy(150), "both draws are paid");
            helper.assertTrue(!controller.drawEnergy(5000), "a draw larger than the buffer is refused");
            helper.assertTrue(controller.energyStored() == 750, "only the paid draws were taken, stored " + controller.energyStored());
            helper.assertTrue(controller.usedLastSecond() == 250, "the readout shows 250, got " + controller.usedLastSecond());
            helper.assertTrue(WarehouseControllerMenu.status(controller).used() == 250, "the GUI gets the readout");
            helper.assertTrue(controller.noPower(), "the refused draw shows the no power state");
            helper.runAfterDelay(25, () -> {
                helper.assertTrue(controller.usedLastSecond() == 0, "a second later the readout is 0, got " + controller.usedLastSecond());
                helper.assertTrue(!controller.noPower(), "the no power state ends a second after the refusal");
                helper.succeed();
            });
        });
    }
}
