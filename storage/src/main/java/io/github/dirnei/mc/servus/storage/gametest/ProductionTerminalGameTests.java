package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.OrderRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.RecipeRow;
import io.github.dirnei.mc.servus.storage.production.ProductionTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.production.ProductionView;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Production Terminal on the data line. Same layout as the CPU tests: the
 * warehouse controller at (2,1,2) with crates west, an Access Point at (2,1,3)
 * with a data duct and Counter Terminal above, data ducts along y 2 to a
 * Crafting Station at (4,1,2) and the CPU cube at x 6 to 8, z 1 to 3. The
 * Production Terminal stands on the duct at (5,2,2).
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProductionTerminalGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos CRATE = new BlockPos(1, 1, 2);
    private static final BlockPos STATION = new BlockPos(4, 1, 2);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 2);
    private static final BlockPos LONE = new BlockPos(10, 1, 10);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    private ProductionTerminalGameTests() {
    }

    private static void setup(GameTestHelper helper, boolean withCpu) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, ACCESS);
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION);
        station.pattern().setStackInSlot(1, new ItemStack(Items.OAK_PLANKS));
        station.pattern().setStackInSlot(4, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(station.saveFromPattern().isEmpty(), "the stick pattern should save");
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(4, 2, 2), new BlockPos(5, 2, 2))) {
            TerminalWiring.dataDuct(helper, pos);
        }
        if (withCpu) {
            for (int part = 0; part < CpuShape.PARTS; part++) {
                CpuShape.Vec offset = CpuShape.offset(part);
                helper.setBlock(CPU.offset(offset.x(), offset.y(), offset.z()), ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
            }
            helper.assertTrue(ServusStorageBlocks.CRAFTING_CPU.get().tryForm(helper.getLevel(), helper.absolutePos(CPU)), "the cube should form");
        }
        terminal(helper, TERMINAL);
    }

    private static void terminal(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.PRODUCTION_TERMINAL.get().defaultBlockState());
    }

    private static boolean lit(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ONLINE);
    }

    private static ProductionPayload view(GameTestHelper helper, BlockPos pos) {
        return ProductionView.of(helper.getLevel(), helper.absolutePos(pos));
    }

    private static DataNetwork line(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(5, 2, 2))).orElseThrow();
    }

    private static CpuBlockEntity cpu(GameTestHelper helper) {
        return CpuBlock.master(helper.getLevel(), helper.absolutePos(CPU));
    }

    /** On a line with a CPU the terminal is a link member and its lamp lights. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void productionTerminalJoinsLineAndLights(GameTestHelper helper) {
        setup(helper, true);
        TerminalWiring.when(helper, () -> lit(helper, TERMINAL), 60, () -> "the lamp should light", () -> {
            helper.assertTrue(line(helper).dataMembers(DataRole.LINK).stream()
                    .anyMatch(member -> member.pos().equals(helper.absolutePos(TERMINAL))), "the terminal should be a link member");
            helper.succeed();
        });
    }

    /** Alone the lamp stays dark and the view says it is not on a line. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void productionTerminalAloneIsDark(GameTestHelper helper) {
        terminal(helper, LONE);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!lit(helper, LONE), "the lamp should stay dark");
            ProductionPayload view = view(helper, LONE);
            helper.assertTrue(!view.online() && view.reason().equals("no_line"), "offline with no_line, got " + view.reason());
            helper.succeed();
        });
    }

    /** An idle Crafting Station is listed with its sticks recipe. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void productionTerminalListsIdleStation(GameTestHelper helper) {
        setup(helper, false);
        TerminalWiring.when(helper, () -> !view(helper, TERMINAL).machines().isEmpty(), 40, () -> "the station should be listed", () -> {
            MachineRow machine = view(helper, TERMINAL).machines().get(0);
            helper.assertTrue(machine.pos().equals(helper.absolutePos(STATION)) && machine.state().equals("idle"),
                    "the station should be idle, got " + machine.state());
            helper.assertTrue(machine.recipes().size() == 1, "one recipe, got " + machine.recipes().size());
            RecipeRow recipe = machine.recipes().get(0);
            helper.assertTrue(recipe.outputs().get(0).is(Items.STICK) && recipe.outputs().get(0).getCount() == 4
                            && recipe.inputs().get(0).is(Items.OAK_PLANKS) && recipe.inputs().get(0).getCount() == 2,
                    "4 sticks from 2 planks, got " + recipe);
            helper.succeed();
        });
    }

    /** A CPU with an order shows its slot use, the order and its step. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void productionTerminalListsCpuOrder(GameTestHelper helper) {
        setup(helper, true);
        TerminalWiring.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            ProductionPayload view = view(helper, TERMINAL);
            helper.assertTrue(view.online() && view.cpus().size() == 1, "one CPU row, got " + view.cpus());
            CpuRow row = view.cpus().get(0);
            helper.assertTrue(row.cpu().equals(cpu(helper).getBlockPos()) && row.slots() == 2 && row.inUse() == 1 && row.waiting() == 0,
                    "1 of 2 slots, 0 waiting, got " + row);
            helper.assertTrue(row.orders().size() == 1, "one order");
            OrderRow order = row.orders().get(0);
            helper.assertTrue(order.item().is(Items.STICK) && order.amount() == 8 && order.requestId() == 0 && order.open(),
                    "an open manual order for 8 sticks, got " + order);
            helper.assertTrue(order.steps().size() == 1 && order.steps().get(0).producer().equals(helper.absolutePos(STATION))
                    && order.steps().get(0).output().is(Items.STICK), "its sticks step on the station, got " + order.steps());
            helper.succeed();
        });
    }

    /** Cancelling through the terminal cancels the order on its CPU and releases its reservations. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void productionTerminalCancelsOrder(GameTestHelper helper) {
        setup(helper, true);
        CrateBlockEntity crate = helper.getBlockEntity(CRATE);
        crate.insert(new ItemStack(Items.OAK_PLANKS, 10), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TerminalWiring.when(helper, () -> CpuOrders.cpus(helper.getLevel(), line(helper)).size() == 1, 40, () -> "the CPU should be on the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made");
            WorkOrder order = placed.created().order();
            ProductionTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
            ProductionTerminalMenu menu = new ProductionTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.cancel(placed.cpu(), order.id()), "the cancel should go through");
            helper.assertTrue(order.state() == WorkOrder.State.CANCELLED, "the order should be cancelled, got " + order.state());
            helper.assertTrue(cpu(helper).book().reservedTotal().isEmpty(), "reservations released, got " + cpu(helper).book().reservedTotal());
            helper.succeed();
        });
    }

    /** A line with a warehouse but no CPU is online and has no CPU rows. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void productionTerminalNoCpuRows(GameTestHelper helper) {
        setup(helper, false);
        TerminalWiring.when(helper, () -> lit(helper, TERMINAL), 60, () -> "the warehouse should light the lamp", () -> {
            ProductionPayload view = view(helper, TERMINAL);
            helper.assertTrue(view.online() && view.cpus().isEmpty(), "online without CPU rows, got " + view.cpus());
            helper.succeed();
        });
    }
}
