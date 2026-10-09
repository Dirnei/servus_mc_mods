package io.github.dirnei.mc.servus.storage.gametest;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.StockRule;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crafting CPUs on the data line. The warehouse is a controller at (2,1,2)
 * with crates west of it and an Access Point at (2,1,3), a data duct on the
 * Access Point and the Counter Terminal on top. Data ducts run along y 2
 * from the Access Point east to a Crafting Station at (4,1,2) and on to the
 * CPU cube at x 6 to 8, z 1 to 3; a second cube can stand at z 5 to 7.
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CraftingCpuGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos CRATE_A = new BlockPos(1, 1, 2);
    private static final BlockPos CRATE_B = new BlockPos(1, 1, 3);
    private static final BlockPos STATION = new BlockPos(4, 1, 2);
    private static final BlockPos PATTERNS = new BlockPos(4, 3, 2);
    private static final BlockPos CPU_A = new BlockPos(6, 1, 1);
    private static final BlockPos CPU_B = new BlockPos(6, 1, 5);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));
    private static final ItemKey PLANKS = ItemKey.of(new ItemStack(Items.OAK_PLANKS));

    private CraftingCpuGameTests() {
    }

    // Setup

    private static void cube(GameTestHelper helper, BlockPos origin, int skip) {
        for (int part = 0; part < CpuShape.PARTS; part++) {
            if (part == skip) {
                continue;
            }
            CpuShape.Vec offset = CpuShape.offset(part);
            helper.setBlock(origin.offset(offset.x(), offset.y(), offset.z()), ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
        }
    }

    private static boolean form(GameTestHelper helper, BlockPos at) {
        return ServusStorageBlocks.CRAFTING_CPU.get().tryForm(helper.getLevel(), helper.absolutePos(at));
    }

    private static CpuBlockEntity cpu(GameTestHelper helper, BlockPos origin) {
        CpuBlockEntity cpu = CpuBlock.master(helper.getLevel(), helper.absolutePos(origin));
        if (cpu == null) {
            throw new AssertionError("no formed CPU at " + origin);
        }
        return cpu;
    }

    /** A plain station and a Pattern Storage with the sticks pattern on the line. */
    private static CraftingStationBlockEntity sticksStation(GameTestHelper helper) {
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        helper.setBlock(PATTERNS, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS);
        patterns.set(0, StationJobs.sticks());
        return helper.getBlockEntity(STATION);
    }

    /** Warehouse, terminal, station and the ducts to the first CPU's spot; the CPU itself when {@code withCpu}. */
    private static void setup(GameTestHelper helper, boolean withCpu) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (BlockPos pos : List.of(CRATE_A, CRATE_B)) {
            helper.setBlock(pos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        }
        TerminalWiring.place(helper, ACCESS);
        sticksStation(helper);
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(4, 2, 2), new BlockPos(5, 2, 2))) {
            TerminalWiring.dataDuct(helper, pos);
        }
        if (withCpu) {
            cube(helper, CPU_A, -1);
            helper.assertTrue(form(helper, CPU_A), "the cube should form");
        }
    }

    private static WarehouseNetwork warehouse(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER);
        return controller.network().orElseThrow();
    }

    private static DataNetwork line(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2))).orElseThrow();
    }

    private static CrateBlockEntity crate(GameTestHelper helper, BlockPos pos, Item item, int count) {
        CrateBlockEntity crate = helper.getBlockEntity(pos);
        if (count > 0) {
            crate.insert(new ItemStack(item, count), false);
        }
        return crate;
    }

    private static RuleRow ruleRow(WarehouseNetwork network, Item item) {
        for (RuleRow row : network.ruleRows()) {
            if (row.stack().is(item)) {
                return row;
            }
        }
        return null;
    }

    private static List<WorkOrder> allOrders(GameTestHelper helper) {
        return CpuOrders.cpus(helper.getLevel(), line(helper)).stream().flatMap(cpu -> cpu.book().orders().stream()).toList();
    }

    // Forming

    /** 27 blocks form a cube with the master at the lowest corner; breaking one unforms the rest. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void cpuFormsAndUnforms(GameTestHelper helper) {
        cube(helper, CPU_A, -1);
        helper.assertTrue(form(helper, CPU_A.offset(1, 1, 1)), "clicking the middle block should form the cube");
        for (int part = 0; part < CpuShape.PARTS; part++) {
            CpuShape.Vec offset = CpuShape.offset(part);
            var state = helper.getBlockState(CPU_A.offset(offset.x(), offset.y(), offset.z()));
            helper.assertTrue(state.getValue(CpuBlock.FORMED) && state.getValue(CpuBlock.PART) == part, "part " + part + " should be formed");
        }
        helper.assertTrue(CpuBlock.master(helper.getLevel(), helper.absolutePos(CPU_A.offset(2, 2, 2))) != null, "every block finds its master");
        helper.setBlock(CPU_A.offset(1, 2, 1), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!helper.getBlockState(CPU_A).getValue(CpuBlock.FORMED), "the master unforms");
        helper.assertTrue(!helper.getBlockState(CPU_A.offset(2, 0, 2)).getValue(CpuBlock.FORMED), "the far corner unforms");
        helper.succeed();
    }

    /** A cube with one block missing does not form. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void cpuIncompleteCubeDoesNotForm(GameTestHelper helper) {
        cube(helper, CPU_A, 13);
        helper.assertTrue(!form(helper, CPU_A), "26 blocks do not form");
        helper.assertTrue(!helper.getBlockState(CPU_A).getValue(CpuBlock.FORMED), "the corner stays unformed");
        helper.succeed();
    }

    /** A formed CPU touching a data duct is a link member; the duct shows an arm toward it. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void cpuJoinsTheDataLine(GameTestHelper helper) {
        setup(helper, true);
        TerminalWiring.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40,
                () -> "the CPU should be on the line, members " + line(helper).dataMembers(DataRole.LINK), () -> {
                    helper.assertTrue(CpuOrders.cpus(helper.getLevel(), line(helper)).get(0) == cpu(helper, CPU_A), "the CPU found is the cube's master");
                    helper.succeed();
                });
    }

    // Orders

    /** A request that stock cannot fill becomes an order on the CPU; the sticks are crafted over the line and reach the terminal. */
    @GameTest(template = EMPTY, timeoutTicks = 1000)
    public static void cpuOrderCraftsShortfallOverTheLine(GameTestHelper helper) {
        setup(helper, true);
        CrateBlockEntity planks = crate(helper, CRATE_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 8);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should be accepted, got " + menu.stock().message());
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            List<WorkOrder> orders = cpu.book().orders();
            helper.assertTrue(orders.size() == 1 && orders.get(0).amount() == 8 && orders.get(0).steps().size() == 1
                    && orders.get(0).steps().get(0).batches() == 2, "one order with one sticks step of two batches, got " + orders);
            helper.assertTrue(orders.get(0).steps().get(0).producer().equals(helper.absolutePos(STATION)), "the step runs on the station");
            helper.assertTrue(orders.get(0).reserved().getOrDefault(PLANKS, 0) == 4, "four planks should be reserved");
            List<RequestRow> rows = menu.stock().requests();
            helper.assertTrue(rows.size() == 1 && rows.get(0).state().equals("PRODUCING"), "the request should be producing, got " + rows);
            // The sticks land straight in the terminal; the order is booked done on a following tick.
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) == 8
                            && cpu.book().orders().stream().noneMatch(WorkOrder::open), 900,
                    () -> "eight sticks should reach the terminal, had " + terminal.outputCount(Items.STICK) + " orders " + cpu.book().orders()
                            + " steps " + cpu.book().orders().get(0).steps().stream().map(st -> st.state() + "/" + st.reason()).toList(), () -> {
                        helper.assertTrue(planks.count() == 6, "four planks should have been used, crate has " + planks.count());
                        helper.assertTrue(cpu.book().orders().stream().noneMatch(WorkOrder::open), "the order should be done: " + cpu.book().orders());
                        helper.succeed();
                    });
        });
    }

    /** Nothing makes bread: the request is refused as out of stock and a manual order says no producer. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void orderWithoutProducerIsRefused(GameTestHelper helper) {
        setup(helper, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 5);
            helper.assertTrue(menu.stock().message().equals("out_of_stock"), "bread cannot be made: " + menu.stock().message());
            helper.assertTrue(allOrders(helper).isEmpty() && menu.stock().requests().isEmpty(), "no order and no request");
            helper.assertTrue(warehouse(helper).order(ItemKey.of(new ItemStack(Items.BREAD)), 5).equals("no_producer"), "a manual bread order says no producer");
            helper.succeed();
        });
    }

    /** Sticks from planks from logs: the planks step runs first, then the sticks step, on the same station. */
    @GameTest(template = EMPTY, timeoutTicks = 1400)
    public static void twoLevelOrderRunsLeavesFirst(GameTestHelper helper) {
        setup(helper, true);
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS);
        patterns.set(1, StationJobs.planks());
        CrateBlockEntity logs = crate(helper, CRATE_A, Items.OAK_LOG, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 8);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should be accepted, got " + menu.stock().message());
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            List<WorkOrder> orders = cpu.book().orders();
            helper.assertTrue(orders.size() == 1 && orders.get(0).steps().size() == 2, "one order with two steps, got " + orders);
            List<OrderStep> steps = orders.get(0).steps();
            helper.assertTrue(steps.get(0).output().is(Items.OAK_PLANKS) && steps.get(0).batches() == 1, "planks first, one batch: " + steps.get(0).output());
            helper.assertTrue(steps.get(1).output().is(Items.STICK) && steps.get(1).batches() == 2, "sticks second, two batches: " + steps.get(1).output());
            helper.assertTrue(orders.get(0).reserved().getOrDefault(ItemKey.of(new ItemStack(Items.OAK_LOG)), 0) == 1, "one log should be reserved");
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) == 8, 1300,
                    () -> "eight sticks should reach the terminal, had " + terminal.outputCount(Items.STICK) + " steps "
                            + cpu.book().orders().get(0).steps().stream().map(st -> st.state() + "/" + st.reason()).toList(), () -> {
                        helper.assertTrue(logs.count() == 1, "one log should be left, crate has " + logs.count());
                        helper.succeed();
                    });
        });
    }

    /** A second order finds the first order's planks reserved; cancelling the first frees them. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void cancelReleasesReservation(GameTestHelper helper) {
        setup(helper, true);
        crate(helper, CRATE_A, Items.OAK_PLANKS, 4);
        helper.runAfterDelay(10, () -> {
            OrderBook book = cpu(helper, CPU_A).book();
            OrderBook.Created first = book.create(STICK, 8, 0);
            OrderBook.Created second = book.create(STICK, 8, 0);
            helper.assertTrue(first.made() && second.made(), "both orders should be created");
            helper.assertTrue(first.order().reserved().getOrDefault(PLANKS, 0) == 4, "the first order reserves the four planks");
            helper.assertTrue(second.order().state() == WorkOrder.State.BLOCKED && second.order().reason().startsWith("missing"),
                    "the second order finds no planks: " + second.order().state() + " " + second.order().reason());
            helper.assertTrue(book.cancel(first.order().id()), "the first order can be cancelled");
            helper.assertTrue(book.reservedTotal().getOrDefault(PLANKS, 0) == 0, "cancelling frees the planks");
            helper.assertTrue(first.order().state() == WorkOrder.State.CANCELLED, "the first order is cancelled");
            helper.succeed();
        });
    }

    // Slots and choice

    /** One CPU with two slots: the third order waits, and the summary says so. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void cpuSlotsQueueThirdOrder(GameTestHelper helper) {
        setup(helper, true);
        helper.runAfterDelay(10, () -> {
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            helper.assertTrue(cpu.slots() == 2, "two slots from cpu.json, got " + cpu.slots());
            for (int i = 0; i < 3; i++) {
                CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
                helper.assertTrue(placed.made() && placed.cpu().equals(cpu.getBlockPos()), "order " + i + " should land on the CPU: " + placed.refusal());
            }
            List<WorkOrder> orders = cpu.book().orders();
            helper.assertTrue(orders.get(0).state() != WorkOrder.State.WAITING && orders.get(1).state() != WorkOrder.State.WAITING,
                    "the two oldest take the slots: " + orders.get(0).state() + " " + orders.get(1).state());
            helper.assertTrue(orders.get(2).state() == WorkOrder.State.WAITING && orders.get(2).reason().equals(OrderBook.WAITING_REASON),
                    "the third waits for the CPU: " + orders.get(2).state());
            Component summary = cpu.summary();
            helper.assertTrue(summary.getContents() instanceof TranslatableContents contents
                            && java.util.Arrays.equals(contents.getArgs(), new Object[] {2, 2, 1}),
                    "summary should say 2 of 2 in use and 1 waiting, got " + summary.getString());
            helper.succeed();
        });
    }

    /** Two CPUs with two slots each share four orders without a wait. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void twoCpusShareOrders(GameTestHelper helper) {
        setup(helper, true);
        cube(helper, CPU_B, -1);
        helper.assertTrue(form(helper, CPU_B), "the second cube should form");
        TerminalWiring.dataDuct(helper, new BlockPos(5, 2, 3));
        TerminalWiring.dataDuct(helper, new BlockPos(5, 2, 4));
        TerminalWiring.dataDuct(helper, new BlockPos(5, 2, 5));
        TerminalWiring.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 2, 40,
                () -> "both CPUs should be on the line", () -> {
                    for (int i = 0; i < 4; i++) {
                        helper.assertTrue(CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0).made(), "order " + i + " should be made");
                    }
                    CpuBlockEntity a = cpu(helper, CPU_A);
                    CpuBlockEntity b = cpu(helper, CPU_B);
                    helper.assertTrue(a.book().openCount() == 2 && b.book().openCount() == 2,
                            "two orders each, got " + a.book().openCount() + " and " + b.book().openCount());
                    helper.assertTrue(allOrders(helper).stream().noneMatch(order -> order.state() == WorkOrder.State.WAITING), "no order waits");
                    helper.succeed();
                });
    }

    /** Breaking a block of the CPU cancels its orders and releases their reservations. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void cpuUnformCancelsOrders(GameTestHelper helper) {
        setup(helper, true);
        crate(helper, CRATE_A, Items.OAK_PLANKS, 4);
        helper.runAfterDelay(10, () -> {
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            OrderBook.Created created = cpu.book().create(STICK, 8, 0);
            helper.assertTrue(created.made() && cpu.book().reservedTotal().getOrDefault(PLANKS, 0) == 4, "the order reserves four planks");
            helper.setBlock(CPU_A.offset(2, 2, 2), Blocks.AIR.defaultBlockState());
            helper.assertTrue(created.order().state() == WorkOrder.State.CANCELLED, "the order is cancelled: " + created.order().state());
            helper.assertTrue(cpu.book().reservedTotal().isEmpty(), "reservations are released");
            helper.assertTrue(!helper.getBlockState(CPU_A).getValue(CpuBlock.FORMED), "the cube is unformed");
            helper.succeed();
        });
    }

    /** Without a CPU on the line the shortfall stays missing while the rest is delivered. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void noCpuLeavesShortfallMissing(GameTestHelper helper) {
        setup(helper, false);
        crate(helper, CRATE_B, Items.STICK, 3);
        crate(helper, CRATE_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 8);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request for the three sticks in stock is accepted: " + menu.stock().message());
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) == 3
                            && menu.stock().requests().size() == 1 && menu.stock().requests().get(0).state().equals("MISSING"), 300,
                    () -> "three sticks delivered and the rest missing, had " + terminal.outputCount(Items.STICK) + " " + menu.stock().requests(),
                    () -> {
                        helper.assertTrue(menu.stock().requests().get(0).missing() == 5, "five sticks are missing");
                        helper.succeed();
                    });
        });
    }

    // Stock rules

    /** A rule below its minimum orders up to the maximum through the CPU and reads ok once the sticks are in stock. */
    @GameTest(template = EMPTY, timeoutTicks = 1200)
    public static void stockRuleOrdersThroughCpu(GameTestHelper helper) {
        setup(helper, true);
        crate(helper, CRATE_A, Items.OAK_PLANKS, 10);
        crate(helper, CRATE_B, Items.STICK, 2);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = warehouse(helper);
            network.setStockRule(STICK, 8, 16);
            TerminalWiring.when(helper, () -> !cpu(helper, CPU_A).book().orders().isEmpty(), 60, () -> "the rule should order", () -> {
                List<WorkOrder> orders = cpu(helper, CPU_A).book().orders();
                helper.assertTrue(orders.size() == 1 && orders.get(0).amount() == 14 && orders.get(0).requestId() == OrderBook.RULE_ORIGIN,
                        "the rule should order 14 sticks, got " + orders);
                RuleRow row = ruleRow(network, Items.STICK);
                helper.assertTrue(row != null && row.state().equals("ordering"), "the rule should read ordering: " + row);
                TerminalWiring.when(helper, () -> network.index().count(STICK) >= 16
                                && "ok".equals(ruleRow(network, Items.STICK).state()), 1100,
                        () -> "at least 16 sticks and ok expected, have " + network.index().count(STICK) + " " + ruleRow(network, Items.STICK),
                        helper::succeed);
            });
        });
    }

    /** A rule above its minimum orders nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void stockRuleIgnoresWhenEnough(GameTestHelper helper) {
        setup(helper, true);
        crate(helper, CRATE_B, Items.STICK, 10);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = warehouse(helper);
            network.setStockRule(STICK, 8, 16);
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(allOrders(helper).isEmpty(), "no order should be created");
                RuleRow row = ruleRow(network, Items.STICK);
                helper.assertTrue(row != null && row.state().equals("ok"), "the rule should read ok: " + row);
                helper.succeed();
            });
        });
    }

    /** A rule for an item nothing makes reads no producer and orders nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void stockRuleWithoutProducerShowsReason(GameTestHelper helper) {
        setup(helper, true);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = warehouse(helper);
            network.setStockRule(ItemKey.of(new ItemStack(Items.BREAD)), 8, 16);
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(allOrders(helper).isEmpty(), "no order should be created");
                RuleRow row = ruleRow(network, Items.BREAD);
                helper.assertTrue(row != null && row.state().equals("no_producer"), "the rule should read no producer: " + row);
                helper.assertTrue(network.removeStockRule(ItemKey.of(new ItemStack(Items.BREAD))), "the rule can be removed");
                helper.assertTrue(network.ruleRows().isEmpty(), "no rules left");
                helper.succeed();
            });
        });
    }

    /** Without a CPU on the warehouse's line a low rule reads no CPU. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void stockRuleWithoutCpu(GameTestHelper helper) {
        setup(helper, false);
        crate(helper, CRATE_A, Items.OAK_PLANKS, 10);
        crate(helper, CRATE_B, Items.STICK, 2);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = warehouse(helper);
            network.setStockRule(STICK, 8, 16);
            TerminalWiring.when(helper, () -> {
                RuleRow row = ruleRow(network, Items.STICK);
                return row != null && row.state().equals("no_cpu");
            }, 200, () -> "the rule should read no CPU: " + ruleRow(network, Items.STICK), helper::succeed);
        });
    }

    /** Rules survive a save and load of the controller. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void stockRulesPersist(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER);
        controller.rules().set(new StockRule(STICK, 8, 16));
        net.minecraft.nbt.CompoundTag tag = controller.saveWithoutMetadata(helper.getLevel().registryAccess());
        WarehouseControllerBlockEntity copy = new WarehouseControllerBlockEntity(helper.absolutePos(CONTROLLER), controller.getBlockState());
        copy.loadWithComponents(tag, helper.getLevel().registryAccess());
        StockRule rule = copy.rules().get(STICK);
        helper.assertTrue(rule != null && rule.min() == 8 && rule.max() == 16, "the rule should survive save and load: " + rule);
        helper.succeed();
    }

    /** Any held item but the carrot falls through to the CPU summary; the carrot keeps forming the cube. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void cpuSummaryWithAnyItem(GameTestHelper helper) {
        BlockPos at = new BlockPos(3, 1, 3);
        helper.setBlock(at, ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(at);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, cobble);
        ItemInteractionResult withBlock = helper.getBlockState(at).useItemOn(cobble, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(withBlock == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, "cobblestone should reach the summary, got " + withBlock);
        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        ItemInteractionResult withCarrot = helper.getBlockState(at).useItemOn(carrot, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(withCarrot == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION, "the carrot should keep its own use, got " + withCarrot);
        helper.succeed();
    }

    /** Every block of a formed cube knows its part; breaking one returns the rest to the casing. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void cpuPartsFollowForming(GameTestHelper helper) {
        cube(helper, CPU_A, -1);
        helper.assertTrue(form(helper, CPU_A), "the cube should form");
        for (int part = 0; part < CpuShape.PARTS; part++) {
            CpuShape.Vec offset = CpuShape.offset(part);
            BlockState state = helper.getBlockState(CPU_A.offset(offset.x(), offset.y(), offset.z()));
            helper.assertTrue(state.getValue(CpuBlock.FORMED) && state.getValue(CpuBlock.PART) == part,
                    "part " + part + " should know its place, got " + state);
        }
        helper.setBlock(CPU_A.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
        BlockState corner = helper.getBlockState(CPU_A.offset(2, 2, 2));
        helper.assertTrue(!corner.getValue(CpuBlock.FORMED) && corner.getValue(CpuBlock.PART) == 0 && !corner.getValue(CpuBlock.ACTIVE),
                "an unformed block shows the casing, got " + corner);
        helper.succeed();
    }

    /** The cube's lights run while its CPU has open orders and glow when it has none. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void cpuLightsFollowOrders(GameTestHelper helper) {
        setup(helper, true);
        helper.runAfterDelay(10, () -> {
            BlockPos far = CPU_A.offset(2, 2, 2);
            helper.assertTrue(!helper.getBlockState(far).getValue(CpuBlock.ACTIVE), "an idle CPU glows");
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should land: " + placed.refusal());
            TerminalWiring.when(helper, () -> helper.getBlockState(far).getValue(CpuBlock.ACTIVE), 20,
                    () -> "the lights should run with an order", () -> {
                        cpu(helper, CPU_A).book().cancelAll();
                        TerminalWiring.when(helper, () -> !helper.getBlockState(far).getValue(CpuBlock.ACTIVE)
                                        && !helper.getBlockState(CPU_A).getValue(CpuBlock.ACTIVE), 20,
                                () -> "the lights should glow again once the order is cancelled", helper::succeed);
                    });
        });
    }

    // Direct delivery

    /** Sticks crafted for a request travel straight from the station to the terminal and never enter the warehouse. */
    @GameTest(template = EMPTY, timeoutTicks = 1000)
    public static void sticksGoStraightToTerminal(GameTestHelper helper) {
        setup(helper, true);
        crate(helper, CRATE_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int[] stockedSticks = {0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(CONTROLLER) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().ifPresent(network -> stockedSticks[0] = Math.max(stockedSticks[0], (int) network.available(STICK)));
            }
        });
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 8);
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            WorkOrder order = cpu.book().orders().get(0);
            helper.assertTrue(helper.absolutePos(TerminalWiring.terminalPos(ACCESS)).equals(order.deliverTo()),
                    "the order should remember the terminal, got " + order.deliverTo());
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) == 8
                            && cpu.book().orders().stream().noneMatch(WorkOrder::open), 900,
                    () -> "eight sticks should reach the terminal, had " + terminal.outputCount(Items.STICK), () -> {
                        helper.assertTrue(stockedSticks[0] == 0, "no stick should ever be in the warehouse, saw " + stockedSticks[0]);
                        helper.assertTrue(order.directSent() == 8, "all eight sent directly, got " + order.directSent());
                        helper.assertTrue(menu.stock().requests().isEmpty(), "the request should be done: " + menu.stock().requests());
                        helper.succeed();
                    });
        });
    }

    /** Two sticks for a request: one craft gives four, two go to the terminal and two are put away. */
    @GameTest(template = EMPTY, timeoutTicks = 1000)
    public static void surplusIsPutAway(GameTestHelper helper) {
        setup(helper, true);
        crate(helper, CRATE_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 2);
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) == 2 && warehouse(helper).available(STICK) == 2
                            && cpu.book().orders().stream().noneMatch(WorkOrder::open), 900,
                    () -> "two sticks at the terminal and two in storage, had " + terminal.outputCount(Items.STICK)
                            + " and " + warehouse(helper).available(STICK), () -> {
                        helper.assertTrue(terminal.outputCount(Items.STICK) == 2, "the terminal gets only what it asked for");
                        helper.succeed();
                    });
        });
    }

    /** Sticks from planks from logs: the planks go to storage, only the sticks go straight to the terminal. */
    @GameTest(template = EMPTY, timeoutTicks = 1400)
    public static void chainDeliversOnlyFinal(GameTestHelper helper) {
        setup(helper, true);
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS);
        patterns.set(1, StationJobs.planks());
        crate(helper, CRATE_A, Items.OAK_LOG, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int[] seen = {0, 0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(CONTROLLER) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().ifPresent(network -> {
                    seen[0] = Math.max(seen[0], (int) network.available(PLANKS));
                    seen[1] = Math.max(seen[1], (int) network.available(STICK));
                });
            }
        });
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 8);
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.STICK) == 8, 1300,
                    () -> "eight sticks should reach the terminal, had " + terminal.outputCount(Items.STICK), () -> {
                        helper.assertTrue(seen[0] > 0, "the planks should pass through storage");
                        helper.assertTrue(seen[1] == 0, "no stick should be in the warehouse, saw " + seen[1]);
                        helper.assertTrue(terminal.outputCount(Items.OAK_PLANKS) == 0, "no planks at the terminal");
                        helper.succeed();
                    });
        });
    }

    /** The terminal link and the direct count are saved with the order. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void deliverToSaved(GameTestHelper helper) {
        setup(helper, true);
        crate(helper, CRATE_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            new CounterTerminalMenu(1, player.getInventory(), terminal).request(STICK, 8);
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            int id = cpu.book().orders().get(0).id();
            CpuBlockEntity loaded = PersistGameTests.reload(helper, cpu.getBlockPos().subtract(helper.absolutePos(BlockPos.ZERO)));
            WorkOrder order = loaded.book().order(id);
            helper.assertTrue(order != null && helper.absolutePos(TerminalWiring.terminalPos(ACCESS)).equals(order.deliverTo()),
                    "the order should still name the terminal after a reload");
            helper.succeed();
        });
    }
}
