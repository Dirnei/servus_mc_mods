package io.github.dirnei.mc.servus.storage.gametest;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBox;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.StockRule;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crafting CPUs on the data line. The warehouse is a controller at (2,1,2)
 * and a Packing Station at (2,1,3) on rails at y 0, two racks west of those
 * rails and a garage east of them, a data duct on the
 * Packing Station and the Storage Terminal on top. Data ducts run along y 2
 * from the Packing Station east to a Crafting Station at (4,1,2) and on to the
 * CPU cube at x 6 to 8, z 1 to 3; a second cube can stand at z 5 to 7.
 */
public final class CraftingCpuGameTests {

    private static final String EMPTY = "empty_12x6x12";
    static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    /** Two racks under the controller's row, facing the rails at y 0 that link the controller and the Packing Station. */
    static final BlockPos RACK_A = new BlockPos(1, 0, 2);
    static final BlockPos RACK_B = new BlockPos(1, 0, 3);
    /** The garage east of the rail under the Packing Station, with one shuttle. */
    static final BlockPos GARAGE = new BlockPos(3, 0, 3);
    static final BlockPos STATION = new BlockPos(4, 1, 2);
    static final BlockPos PATTERNS = new BlockPos(4, 3, 2);
    static final BlockPos CPU_A = new BlockPos(6, 1, 1);
    private static final BlockPos CPU_B = new BlockPos(6, 1, 5);
    static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));
    static final ItemKey PLANKS = ItemKey.of(new ItemStack(Items.OAK_PLANKS));

    private CraftingCpuGameTests() {
    }

    // Setup

    private static boolean form(GameTestHelper helper, BlockPos at) {
        return CpuBuild.form(helper, at).formed();
    }

    static CpuBlockEntity cpu(GameTestHelper helper, BlockPos origin) {
        return CpuBuild.cpu(helper, origin);
    }

    /** The message key the carrot would show for a result. */
    private static String messageKey(CpuBox.Result result) {
        return CpuBlock.message(result).getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
    }

    /** A plain station and a Pattern Storage with the sticks pattern on the line. */
    private static CraftingStationBlockEntity sticksStation(GameTestHelper helper) {
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        helper.setBlock(PATTERNS, ServusStorageBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(PatternStorageBlock.FACING, Direction.NORTH));
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(0, StationJobs.sticks());
        return helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
    }

    /** Warehouse, terminal, station and the ducts to the first CPU's spot; the CPU itself when {@code withCpu}. */
    static void setup(GameTestHelper helper, boolean withCpu) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, CONTROLLER);
        for (BlockPos rail : List.of(CONTROLLER.below(), ACCESS.below())) {
            TerminalWiring.rail(helper, rail);
        }
        for (BlockPos pos : List.of(RACK_A, RACK_B)) {
            helper.setBlock(pos, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.EAST));
        }
        helper.setBlock(GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.EAST));
        helper.assertTrue(helper.getBlockEntity(GARAGE, ShuttleGarageBlockEntity.class).addShuttle(), "the garage takes a shuttle");
        TerminalWiring.place(helper, ACCESS);
        sticksStation(helper);
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(4, 2, 2), new BlockPos(5, 2, 2))) {
            TerminalWiring.dataDuct(helper, pos);
        }
        if (withCpu) {
            CpuBuild.formCube(helper, CPU_A);
        }
    }

    static final ItemKey CRAFTING_TABLE = ItemKey.of(new ItemStack(Items.CRAFTING_TABLE));

    /**
     * The CPU test line with a 2x2x2 CPU of {@code 8 - bufferBlocks} CPU blocks and
     * {@code bufferBlocks} Buffer blocks, the planks and crafting table patterns
     * next to the sticks pattern and {@code logs} oak logs in the first rack.
     */
    static CpuBlockEntity bufferedSetup(GameTestHelper helper, int bufferBlocks, int logs) {
        setup(helper, false);
        CpuBuild.box(helper, CPU_A, 2, 2, 2, 8 - bufferBlocks);
        CpuBlockEntity cpu = CpuBuild.formed(helper, CPU_A);
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, StationJobs.planks());
        patterns.set(2, StationJobs.craftingTable());
        stock(helper, RACK_A, Items.OAK_LOG, logs);
        return cpu;
    }

    /** Whether the line has the station and the CPU, so orders can be planned. */
    static boolean lineReady(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2)))
                .map(network -> !network.dataMembers(DataRole.PRODUCER).isEmpty()
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    /** The most of the key the warehouse and its Packing Station's buffers held at any tick so far, in {@code seen[0]}. */
    static int[] watchStorage(GameTestHelper helper, Item item) {
        int[] seen = {0};
        ItemKey key = ItemKey.of(new ItemStack(item));
        helper.onEachTick(() -> {
            int count = 0;
            if (helper.getBlockEntity(CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                count += controller.network().map(network -> (int) network.available(key)).orElse(0);
            }
            if (helper.getBlockEntity(ACCESS, BlockEntity.class) instanceof io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity point) {
                count += countIn(point.buffer(), item) + countIn(point.outgoing(), item);
            }
            seen[0] = Math.max(seen[0], count);
        });
        return seen;
    }

    static int countIn(io.github.dirnei.mc.servus.foundation.transfer.ItemSlots slots, Item item) {
        int count = 0;
        for (int i = 0; i < slots.getSlots(); i++) {
            if (slots.getStackInSlot(i).is(item)) {
                count += slots.getStackInSlot(i).getCount();
            }
        }
        return count;
    }

    /** Runs {@code then} once, on the first tick the order holds the key in its CPU's buffer. */
    static void whenBuffered(GameTestHelper helper, CpuBlockEntity cpu, WorkOrder order, ItemKey key, Runnable then) {
        // Polled every tick; onEachTick cannot be added while the test ticks.
        TerminalWiring.when(helper, () -> cpu.buffer().held(order.id(), key) > 0, 1200,
                () -> "the order should hold " + key.stack(1).getHoverName().getString() + " in the buffer, order "
                        + order.state() + " " + order.steps().stream().map(step -> step.state() + "/" + step.reason()).toList()
                        + " buffer " + cpu.buffer(), then);
    }

    static WarehouseNetwork warehouse(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        return controller.network().orElseThrow();
    }

    static DataNetwork line(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2))).orElseThrow();
    }

    static HeavyRackBlockEntity stock(GameTestHelper helper, BlockPos pos, Item item, int count) {
        if (count > 0) {
            MiniWarehouse.stock(helper, pos, new ItemStack(item, count));
        }
        return helper.getBlockEntity(pos, HeavyRackBlockEntity.class);
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
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuFormsAndUnforms(GameTestHelper helper) {
        CpuBuild.cube(helper, CPU_A);
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
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuIncompleteCubeDoesNotForm(GameTestHelper helper) {
        CpuBuild.cube(helper, CPU_A);
        helper.setBlock(CPU_A.offset(1, 1, 0), Blocks.AIR.defaultBlockState());
        CpuBox.Result result = CpuBuild.form(helper, CPU_A);
        helper.assertTrue(!result.formed() && result.reason() == CpuBox.Reason.GAP, "26 blocks do not form: " + result);
        helper.assertTrue(!helper.getBlockState(CPU_A).getValue(CpuBlock.FORMED), "the corner stays unformed");
        helper.succeed();
    }

    /** A formed CPU touching a data duct is a link member; the duct shows an arm toward it. */
    @ServusGameTest(timeoutTicks = 60)
    public static void cpuJoinsTheDataLine(GameTestHelper helper) {
        setup(helper, true);
        TerminalWiring.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40,
                () -> "the CPU should be on the line, members " + line(helper).dataMembers(DataRole.LINK), () -> {
                    helper.assertTrue(CpuOrders.cpus(helper.getLevel(), line(helper)).get(0) == cpu(helper, CPU_A), "the CPU found is the cube's master");
                    helper.succeed();
                });
    }

    // Boxes

    /** Eight CPU blocks form the smallest CPU from any block and get eight slots. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuSmallestBoxForms(GameTestHelper helper) {
        CpuBuild.box(helper, CPU_A, 2, 2, 2, 8);
        CpuBox.Result result = CpuBuild.form(helper, CPU_A.offset(1, 1, 0));
        helper.assertTrue(result.formed(), "a 2x2x2 box forms: " + result);
        helper.assertTrue(messageKey(result).equals("message.servus_storage.cpu.formed"), "the carrot says formed");
        for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 2; z++) {
                for (int x = 0; x < 2; x++) {
                    helper.assertTrue(helper.getBlockState(CPU_A.offset(x, y, z)).getValue(CpuBlock.FORMED), "every block is formed");
                    helper.assertTrue(cpu(helper, CPU_A.offset(x, y, z)).getBlockPos().equals(helper.absolutePos(CPU_A)),
                            "every block finds the master at the lowest corner");
                }
            }
        }
        helper.assertTrue(helper.getBlockState(CPU_A).getValue(CpuBlock.PART) == 0, "the corner is part 0");
        helper.succeed();
    }

    /** A 3x2x2 box of 4 CPU blocks and 8 Buffer blocks forms from a Buffer block: 4 slots, 8 stacks. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuBoxWithBuffersForms(GameTestHelper helper) {
        java.util.Set<BlockPos> cpus = java.util.Set.of(new BlockPos(0, 0, 0), new BlockPos(2, 1, 1), new BlockPos(1, 0, 1), new BlockPos(2, 0, 0));
        CpuBuild.box(helper, CPU_A, 3, 2, 2, offset -> !cpus.contains(offset));
        helper.assertTrue(helper.getBlockState(CPU_A.offset(1, 1, 0)).is(ServusStorageBlocks.CPU_BUFFER.get()), "the clicked block is a Buffer block");
        CpuBox.Result result = CpuBuild.form(helper, CPU_A.offset(1, 1, 0));
        helper.assertTrue(result.formed(), "the box forms: " + result);
        CpuBlockEntity cpu = cpu(helper, CPU_A);
        helper.assertTrue(cpu.slots() == 4 && cpu.room() == 8 && cpu.cpuBlocks() == 4 && cpu.bufferBlocks() == 8,
                "4 slots and 8 stacks, got " + cpu.slots() + " and " + cpu.room());
        helper.assertTrue(helper.getBlockState(CPU_A.offset(1, 1, 0)).is(ServusStorageBlocks.CPU_BUFFER.get()), "Buffer blocks stay Buffer blocks");
        helper.succeed();
    }

    /** A 3x3x3 box without its middle block says where the gap is. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuGapSaysWhere(GameTestHelper helper) {
        CpuBuild.cube(helper, CPU_A);
        helper.setBlock(CPU_A.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
        CpuBox.Result result = CpuBuild.form(helper, CPU_A);
        BlockPos middle = helper.absolutePos(CPU_A.offset(1, 1, 1));
        helper.assertTrue(!result.formed() && result.reason() == CpuBox.Reason.GAP
                && result.gap().equals(new CpuShape.Vec(middle.getX(), middle.getY(), middle.getZ())), "a gap at the middle: " + result);
        helper.assertTrue(messageKey(result).equals("message.servus_storage.cpu.gap"), "the carrot names the gap");
        helper.assertTrue(!helper.getBlockState(CPU_A).getValue(CpuBlock.FORMED), "nothing forms");
        helper.succeed();
    }

    /** An L shape is not a box. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuLShapeIsNoBox(GameTestHelper helper) {
        CpuBuild.box(helper, CPU_A, 3, 2, 2, 12);
        helper.setBlock(CPU_A.offset(2, 1, 0), Blocks.AIR.defaultBlockState());
        helper.setBlock(CPU_A.offset(2, 1, 1), Blocks.AIR.defaultBlockState());
        CpuBox.Result result = CpuBuild.form(helper, CPU_A);
        helper.assertTrue(result.reason() == CpuBox.Reason.NOT_A_BOX, "not a box: " + result);
        helper.assertTrue(messageKey(result).equals("message.servus_storage.cpu.not_a_box"), "the carrot says not a box");
        helper.succeed();
    }

    /** A flat 2x2x1 layer is too small. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuFlatIsTooSmall(GameTestHelper helper) {
        CpuBuild.box(helper, CPU_A, 2, 1, 2, 4);
        CpuBox.Result result = CpuBuild.form(helper, CPU_A);
        helper.assertTrue(result.reason() == CpuBox.Reason.TOO_SMALL, "too small: " + result);
        helper.assertTrue(messageKey(result).equals("message.servus_storage.cpu.too_small"), "the carrot says too small");
        helper.assertTrue(!helper.getBlockState(CPU_A).getValue(CpuBlock.FORMED), "nothing forms");
        helper.succeed();
    }

    /** A box of Buffer blocks only needs a CPU block. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuBufferOnlyRefused(GameTestHelper helper) {
        CpuBuild.box(helper, CPU_A, 2, 2, 2, 0);
        CpuBox.Result result = CpuBuild.form(helper, CPU_A);
        helper.assertTrue(result.reason() == CpuBox.Reason.NO_CPU_BLOCK, "no CPU block: " + result);
        helper.assertTrue(messageKey(result).equals("message.servus_storage.cpu.no_cpu_block"), "the carrot says no CPU block");
        helper.succeed();
    }

    /** A new box built against a formed CPU forms on its own; both keep their own slots. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuBoxBesideFormedFormsAlone(GameTestHelper helper) {
        CpuBuild.box(helper, CPU_A, 2, 2, 2, 8);
        CpuBlockEntity first = CpuBuild.formed(helper, CPU_A);
        BlockPos second = CPU_A.offset(2, 0, 0);
        CpuBuild.box(helper, second, 2, 2, 2, 8);
        CpuBlockEntity other = CpuBuild.formed(helper, second);
        helper.assertTrue(first != other && first.slots() == 8 && other.slots() == 8, "two CPUs with 8 slots each");
        helper.assertTrue(cpu(helper, CPU_A.offset(1, 1, 1)) == first && cpu(helper, second.offset(1, 1, 1)) == other,
                "each block keeps its own CPU");
        helper.setBlock(second.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
        helper.assertTrue(helper.getBlockState(CPU_A.offset(1, 0, 0)).getValue(CpuBlock.FORMED), "breaking the second leaves the first formed");
        helper.assertTrue(!helper.getBlockState(second).getValue(CpuBlock.FORMED), "the second unforms");
        helper.succeed();
    }

    /** Two unformed 2x2x2 boxes that touch along a face form one 4x2x2 CPU. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuTwoUnformedBoxesFormOne(GameTestHelper helper) {
        CpuBuild.box(helper, CPU_A, 2, 2, 2, 8);
        CpuBuild.box(helper, CPU_A.offset(2, 0, 0), 2, 2, 2, 8);
        CpuBox.Result result = CpuBuild.form(helper, CPU_A.offset(3, 1, 1));
        helper.assertTrue(result.formed() && result.box().size().equals(new CpuShape.Vec(4, 2, 2)), "one 4x2x2 box: " + result);
        CpuBlockEntity cpu = cpu(helper, CPU_A.offset(3, 1, 1));
        helper.assertTrue(cpu.getBlockPos().equals(helper.absolutePos(CPU_A)) && cpu.slots() == 16, "one CPU with 16 slots, got " + cpu.slots());
        helper.succeed();
    }

    /** A CPU whose only block on the duct is a Buffer block joins the line and takes orders. */
    @ServusGameTest(timeoutTicks = 100)
    public static void cpuBufferBlockJoinsTheLine(GameTestHelper helper) {
        setup(helper, false);
        // The duct at (5,2,2) touches only the box's block at (6,2,2), offset (0,1,1).
        CpuBuild.box(helper, CPU_A, 2, 2, 2, offset -> offset.equals(new BlockPos(0, 1, 1)) || offset.getY() == 1);
        helper.assertTrue(helper.getBlockState(new BlockPos(6, 2, 2)).is(ServusStorageBlocks.CPU_BUFFER.get()), "a Buffer block faces the duct");
        CpuBuild.formed(helper, CPU_A);
        TerminalWiring.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40,
                () -> "the CPU should be on the line", () -> {
                    CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
                    helper.assertTrue(placed.made() && placed.cpu().equals(helper.absolutePos(CPU_A)), "the CPU takes the order: " + placed.refusal());
                    helper.succeed();
                });
    }

    /** Slots come from CPU blocks: 8 CPU blocks give 8 slots and no buffer. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuSlotsFromCpuBlocks(GameTestHelper helper) {
        CpuBuild.box(helper, CPU_A, 2, 2, 2, 8);
        CpuBlockEntity cpu = CpuBuild.formed(helper, CPU_A);
        helper.assertTrue(cpu.slots() == 8 && cpu.room() == 0, "8 slots and no buffer, got " + cpu.slots() + " and " + cpu.room());
        helper.succeed();
    }

    // Orders

    /** A request that stock cannot fill becomes an order on the CPU; the sticks are crafted over the line and reach the terminal. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void cpuOrderCraftsShortfallOverTheLine(GameTestHelper helper) {
        setup(helper, true);
        HeavyRackBlockEntity planks = stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
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
                        helper.assertTrue(MiniWarehouse.count(planks, Items.OAK_PLANKS) == 6, "four planks should have been used, rack has " + MiniWarehouse.count(planks, Items.OAK_PLANKS));
                        helper.assertTrue(cpu.book().orders().stream().noneMatch(WorkOrder::open), "the order should be done: " + cpu.book().orders());
                        helper.succeed();
                    });
        });
    }

    /** Nothing makes bread: the request is refused as out of stock and a manual order says no producer. */
    @ServusGameTest(timeoutTicks = 100)
    public static void orderWithoutProducerIsRefused(GameTestHelper helper) {
        setup(helper, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 5);
            helper.assertTrue(menu.stock().message().equals("out_of_stock"), "bread cannot be made: " + menu.stock().message());
            helper.assertTrue(allOrders(helper).isEmpty() && menu.stock().requests().isEmpty(), "no order and no request");
            helper.assertTrue(warehouse(helper).order(ItemKey.of(new ItemStack(Items.BREAD)), 5).equals("no_producer"), "a manual bread order says no producer");
            helper.succeed();
        });
    }

    /** Sticks from planks from logs: the planks step runs first, then the sticks step, on the same station. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void twoLevelOrderRunsLeavesFirst(GameTestHelper helper) {
        setup(helper, true);
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, StationJobs.planks());
        HeavyRackBlockEntity logs = stock(helper, RACK_A, Items.OAK_LOG, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
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
                        helper.assertTrue(MiniWarehouse.count(logs, Items.OAK_LOG) == 1, "one log should be left, rack has " + MiniWarehouse.count(logs, Items.OAK_LOG));
                        helper.succeed();
                    });
        });
    }

    /** A second order finds the first order's planks reserved; cancelling the first frees them. */
    @ServusGameTest(timeoutTicks = 100)
    public static void cancelReleasesReservation(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 4);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void cpuSlotsQueueThirdOrder(GameTestHelper helper) {
        setup(helper, false);
        CpuBuild.formSlotted(helper, CPU_A);
        helper.runAfterDelay(10, () -> {
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            helper.assertTrue(cpu.slots() == 2, "two slots from two CPU blocks, got " + cpu.slots());
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
                            && java.util.Arrays.equals(contents.getArgs(), new Object[] {2, 2, 1, 0, 6}),
                    "summary should say 2 of 2 in use, 1 waiting and 0 of 6 buffer stacks, got " + summary.getString());
            helper.succeed();
        });
    }

    /** Two CPUs with two slots each share four orders without a wait. */
    @ServusGameTest(timeoutTicks = 100)
    public static void twoCpusShareOrders(GameTestHelper helper) {
        setup(helper, false);
        CpuBuild.formSlotted(helper, CPU_A);
        CpuBuild.formSlotted(helper, CPU_B);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void cpuUnformCancelsOrders(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 4);
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
    @ServusGameTest(timeoutTicks = 400)
    public static void noCpuLeavesShortfallMissing(GameTestHelper helper) {
        setup(helper, false);
        stock(helper, RACK_B, Items.STICK, 3);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
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
    @ServusGameTest(timeoutTicks = 1200)
    public static void stockRuleOrdersThroughCpu(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        stock(helper, RACK_B, Items.STICK, 2);
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
    @ServusGameTest(timeoutTicks = 300)
    public static void stockRuleIgnoresWhenEnough(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_B, Items.STICK, 10);
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
    @ServusGameTest(timeoutTicks = 300)
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
    @ServusGameTest(timeoutTicks = 300)
    public static void stockRuleWithoutCpu(GameTestHelper helper) {
        setup(helper, false);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        stock(helper, RACK_B, Items.STICK, 2);
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
    @ServusGameTest(timeoutTicks = 100)
    public static void stockRulesPersist(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, CONTROLLER);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        controller.rules().set(new StockRule(STICK, 8, 16));
        net.minecraft.nbt.CompoundTag tag = controller.saveWithoutMetadata(helper.getLevel().registryAccess());
        WarehouseControllerBlockEntity copy = new WarehouseControllerBlockEntity(helper.absolutePos(CONTROLLER), controller.getBlockState());
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        StockRule rule = copy.rules().get(STICK);
        helper.assertTrue(rule != null && rule.min() == 8 && rule.max() == 16, "the rule should survive save and load: " + rule);
        helper.succeed();
    }

    /** Any held item but the carrot falls through to the CPU summary; the carrot keeps forming the cube. */
    @ServusGameTest(timeoutTicks = 20)
    public static void cpuSummaryWithAnyItem(GameTestHelper helper) {
        BlockPos at = new BlockPos(3, 1, 3);
        helper.setBlock(at, ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(at);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, cobble);
        InteractionResult withBlock = helper.getBlockState(at).useItemOn(cobble, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(withBlock == InteractionResult.TRY_WITH_EMPTY_HAND, "cobblestone should reach the summary, got " + withBlock);
        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        InteractionResult withCarrot = helper.getBlockState(at).useItemOn(carrot, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(withCarrot == InteractionResult.PASS, "the carrot should keep its own use, got " + withCarrot);
        ItemStack buffer = new ItemStack(ServusStorageBlocks.CPU_BUFFER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, buffer);
        InteractionResult withBuffer = helper.getBlockState(at).useItemOn(buffer, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(withBuffer == InteractionResult.PASS, "a Buffer block should place against a CPU block, got " + withBuffer);
        helper.succeed();
    }

    /** Every block of a formed 3x2x2 box knows its place class; breaking one returns the rest to the casing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void cpuPartsFollowForming(GameTestHelper helper) {
        CpuBuild.box(helper, CPU_A, 3, 2, 2, 12);
        helper.assertTrue(form(helper, CPU_A), "the box should form");
        CpuShape.Vec size = new CpuShape.Vec(3, 2, 2);
        for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 2; z++) {
                for (int x = 0; x < 3; x++) {
                    int part = CpuBox.part(new CpuShape.Vec(x, y, z), size);
                    BlockState state = helper.getBlockState(CPU_A.offset(x, y, z));
                    helper.assertTrue(state.getValue(CpuBlock.FORMED) && state.getValue(CpuBlock.PART) == part,
                            "block " + x + "," + y + "," + z + " should be place " + part + ", got " + state);
                }
            }
        }
        helper.assertTrue(helper.getBlockState(CPU_A.offset(1, 1, 1)).getValue(CpuBlock.PART) == CpuShape.part(1, 2, 2),
                "the middle of the top back edge is middle on x only");
        helper.setBlock(CPU_A.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
        BlockState corner = helper.getBlockState(CPU_A.offset(2, 1, 1));
        helper.assertTrue(!corner.getValue(CpuBlock.FORMED) && corner.getValue(CpuBlock.PART) == 0 && !corner.getValue(CpuBlock.ACTIVE),
                "an unformed block shows the casing, got " + corner);
        helper.succeed();
    }

    /** The cube's lights run while its CPU has open orders and glow when it has none. */
    @ServusGameTest(timeoutTicks = 100)
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

    // Buffer

    /** Planks one order holds in the buffer are not stock for a second order, which plans with the warehouse only. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void bufferedPlanksNotForOthers(GameTestHelper helper) {
        CpuBlockEntity cpu = bufferedSetup(helper, 6, 1);
        TerminalWiring.when(helper, () -> lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            OrderBook book = cpu.book();
            WorkOrder first = book.create(CRAFTING_TABLE, 1, 0).order();
            helper.assertTrue(first != null, "the crafting table order should be made");
            whenBuffered(helper, cpu, first, PLANKS, () -> {
                OrderBook.Preview preview = book.preview(STICK, 4);
                helper.assertTrue(preview.fromStock().getOrDefault(PLANKS, 0) == 0,
                        "the buffered planks are no stock for others: " + preview.fromStock());
                OrderBook.Created second = book.create(STICK, 4, 0);
                helper.assertTrue(second.made() && second.order().reserved().getOrDefault(PLANKS, 0) == 0
                        && second.order().state() == WorkOrder.State.BLOCKED, "the second order misses planks: " + second.order().state());
                int held = cpu.buffer().held(first.id(), PLANKS);
                helper.assertTrue(held == 4, "the first order keeps its 4 planks, has " + held);
                book.cancel(second.order().id());
                TerminalWiring.when(helper, () -> first.state() == WorkOrder.State.DONE, 1200, () -> "the crafting table should be made", helper::succeed);
            });
        });
    }

    /** Cancelling an order with planks in the buffer sends them to the Packing Station; a shuttle puts them into a rack. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void cancelSendsBufferToStorage(GameTestHelper helper) {
        CpuBlockEntity cpu = bufferedSetup(helper, 6, 1);
        TerminalWiring.when(helper, () -> lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            WorkOrder order = cpu.book().create(CRAFTING_TABLE, 1, 0).order();
            whenBuffered(helper, cpu, order, PLANKS, () -> {
                helper.assertTrue(cpu.book().cancel(order.id()), "the order can be cancelled");
                TerminalWiring.when(helper, () -> warehouse(helper).available(PLANKS) == 4 && cpu.bufferUsed() == 0, 600,
                        () -> "the 4 planks should be put away, warehouse has " + warehouse(helper).available(PLANKS)
                                + ", buffer " + cpu.buffer(), helper::succeed);
            });
        });
    }

    /** Breaking a Buffer block while planks wait sends them to the Packing Station and a shuttle puts them into a rack. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void unformPutsBufferAway(GameTestHelper helper) {
        CpuBlockEntity cpu = bufferedSetup(helper, 6, 1);
        TerminalWiring.when(helper, () -> lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            WorkOrder order = cpu.book().create(CRAFTING_TABLE, 1, 0).order();
            whenBuffered(helper, cpu, order, PLANKS, () -> {
                BlockPos buffer = CPU_A.offset(1, 1, 0);
                helper.assertTrue(helper.getBlockState(buffer).is(ServusStorageBlocks.CPU_BUFFER.get()), "a Buffer block is broken");
                helper.setBlock(buffer, Blocks.AIR.defaultBlockState());
                helper.assertTrue(!helper.getBlockState(CPU_A).getValue(CpuBlock.FORMED), "the CPU unforms");
                helper.assertTrue(order.state() == WorkOrder.State.CANCELLED, "the order is cancelled");
                TerminalWiring.when(helper, () -> warehouse(helper).available(PLANKS) == 4, 600,
                        () -> "the 4 planks should be put away, warehouse has " + warehouse(helper).available(PLANKS), () -> {
                            helper.assertTrue(droppedNear(helper, CPU_A, 3, Items.OAK_PLANKS) == 0, "no planks dropped at the CPU");
                            helper.succeed();
                        });
            });
        });
    }

    /** Breaking a block while the only Packing Station's To storage buffer is full drops the planks beside the station. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void unformFullPackingStationDrops(GameTestHelper helper) {
        CpuBlockEntity cpu = bufferedSetup(helper, 6, 1);
        TerminalWiring.when(helper, () -> lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            WorkOrder order = cpu.book().create(CRAFTING_TABLE, 1, 0).order();
            whenBuffered(helper, cpu, order, PLANKS, () -> {
                io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity point =
                        helper.getBlockEntity(ACCESS, io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity.class);
                for (int i = 0; i < point.buffer().getSlots(); i++) {
                    point.buffer().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
                }
                helper.setBlock(CPU_A.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
                TerminalWiring.when(helper, () -> droppedNear(helper, ACCESS, 2, Items.OAK_PLANKS) == 4, 100,
                        () -> "the 4 planks should drop beside the Packing Station, found " + droppedNear(helper, ACCESS, 2, Items.OAK_PLANKS)
                                + " there and " + droppedNear(helper, CPU_A, 3, Items.OAK_PLANKS) + " at the CPU", helper::succeed);
            });
        });
    }

    /** Without a Packing Station on the line the buffered planks drop at the broken block. */
    @ServusGameTest(timeoutTicks = 100)
    public static void unformWithoutPackingStationDrops(GameTestHelper helper) {
        TerminalWiring.dataDuct(helper, new BlockPos(5, 2, 2));
        CpuBlockEntity cpu = CpuBuild.formSlotted(helper, CPU_A);
        cpu.buffer().expect(1, 7, PLANKS, 4, 64);
        cpu.buffer().land(PLANKS, 4);
        BlockPos broken = CPU_A.offset(1, 1, 1);
        helper.runAfterDelay(5, () -> {
            helper.setBlock(broken, Blocks.AIR.defaultBlockState());
            helper.assertTrue(droppedNear(helper, broken, 1, Items.OAK_PLANKS) == 4,
                    "the 4 planks drop at the broken block, found " + droppedNear(helper, broken, 1, Items.OAK_PLANKS));
            helper.succeed();
        });
    }

    /** A hopper above a formed CPU cannot put items in: the CPU has no item capability, even while its buffer expects planks. */
    @ServusGameTest(timeoutTicks = 100)
    public static void inletRefusesHopper(GameTestHelper helper) {
        CpuBlockEntity cpu = CpuBuild.formSlotted(helper, CPU_A);
        cpu.buffer().expect(1, 7, PLANKS, 4, 64);
        BlockPos hopperPos = CPU_A.offset(0, 2, 0);
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState());
        net.minecraft.world.level.block.entity.HopperBlockEntity hopper =
                helper.getBlockEntity(hopperPos, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.assertTrue(helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                helper.absolutePos(CPU_A.offset(0, 1, 0)), Direction.UP) == null, "a CPU block has no item capability");
        helper.assertTrue(helper.getLevel().getCapability(io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities.PACKET_INLET,
                helper.absolutePos(CPU_A.offset(0, 1, 0)), Direction.UP) != null, "a formed CPU block has a packet inlet");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 4, "the hopper keeps its planks");
            helper.assertTrue(cpu.buffer().held(7, PLANKS) == 0, "nothing reached the buffer");
            helper.succeed();
        });
    }

    /** The summary counts buffer stacks in use against the room. */
    @ServusGameTest(timeoutTicks = 100)
    public static void summaryShowsBuffer(GameTestHelper helper) {
        setup(helper, false);
        CpuBlockEntity cpu = CpuBuild.formSlotted(helper, CPU_A);
        helper.runAfterDelay(10, () -> {
            for (int i = 0; i < 3; i++) {
                helper.assertTrue(cpu.book().create(STICK, 8, 0).made(), "order " + i + " should be made");
            }
            cpu.buffer().expect(1, 1, PLANKS, 4, 64);
            cpu.buffer().expect(2, 2, PLANKS, 4, 64);
            cpu.buffer().expect(3, 3, PLANKS, 4, 64);
            cpu.buffer().land(PLANKS, 12);
            Component summary = cpu.summary();
            helper.assertTrue(summary.getContents() instanceof TranslatableContents contents
                            && java.util.Arrays.equals(contents.getArgs(), new Object[] {2, 2, 1, 3, 6}),
                    "summary should say 2 of 2 in use, 1 waiting and 3 of 6 buffer stacks, got " + summary.getString());
            helper.succeed();
        });
    }

    /** Planks lying as items within {@code radius} of a test position. */
    static int droppedNear(GameTestHelper helper, BlockPos at, int radius, Item item) {
        BlockPos pos = helper.absolutePos(at);
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos).inflate(radius);
        int count = 0;
        for (net.minecraft.world.entity.item.ItemEntity entity
                : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            if (entity.getItem().is(item)) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }

    // Direct delivery

    /** Sticks crafted for a request travel straight from the station to the terminal and never enter the warehouse. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void sticksGoStraightToTerminal(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int[] stockedSticks = {0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().ifPresent(network -> stockedSticks[0] = Math.max(stockedSticks[0], (int) network.available(STICK)));
            }
        });
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
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
    @ServusGameTest(timeoutTicks = 1000)
    public static void surplusIsPutAway(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
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
    @ServusGameTest(timeoutTicks = 1400)
    public static void chainDeliversOnlyFinal(GameTestHelper helper) {
        setup(helper, true);
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, StationJobs.planks());
        stock(helper, RACK_A, Items.OAK_LOG, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int[] seen = {0, 0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().ifPresent(network -> {
                    seen[0] = Math.max(seen[0], (int) network.available(PLANKS));
                    seen[1] = Math.max(seen[1], (int) network.available(STICK));
                });
            }
        });
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
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
    @ServusGameTest(timeoutTicks = 200)
    public static void deliverToSaved(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            new StorageTerminalMenu(1, player.getInventory(), terminal).request(STICK, 8);
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
