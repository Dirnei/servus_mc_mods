package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.order.WorkOrder;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternEditing;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** The Storage Terminal's order grid: when it is offered, ghost cells, one-off orders and saving their pattern. */
public final class OrderGridGameTests {

    /** Where {@link StationHandsGameTests#setup} puts the Packing Station; the terminal sits two blocks above. */
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    /** The mini warehouse's rails start here, so its Packing Station stands at {@link #MINI_TERMINAL}. */
    private static final BlockPos MINI_ORIGIN = new BlockPos(4, 1, 2);
    private static final BlockPos MINI_TERMINAL = new BlockPos(5, 1, 3);
    private static final int[] SQUARE = {0, 1, 3, 4};
    private static final int[] RACK_IRON = {0, 2, 6, 8};
    /** The second rack of {@link StationHandsGameTests#setup}, for extra stock. */
    private static final BlockPos EXTRA = new BlockPos(1, 0, 3);
    private static final Identifier OAK = Identifier.withDefaultNamespace("oak_planks");

    private OrderGridGameTests() {
    }

    private static StorageTerminalMenu menu(GameTestHelper helper, Player player) {
        return new StorageTerminalMenu(1, player.getInventory(), TerminalWiring.at(helper, ACCESS));
    }

    /** The CPU, stations and terminal are on the line and the index lists the planks. */
    private static boolean stocked(GameTestHelper helper, StorageTerminalMenu menu) {
        return DataLines.ready(helper, TerminalWiring.terminalPos(ACCESS), 2)
                && menu.stock().entries().stream().anyMatch(entry -> entry.stack().is(Items.OAK_PLANKS));
    }

    /** Four oak planks in a square: a crafting table, which no pattern of the fixture makes. */
    private static void craftingTableGrid(StorageTerminalMenu menu) {
        for (int cell : SQUARE) {
            menu.setGhost(cell, new ItemStack(Items.OAK_PLANKS));
        }
    }

    /** Rails made of the wood of these planks. */
    private static ItemStack rail(String planks, int count) {
        ItemStack rail = new ItemStack(ServusShuttlesItems.RAIL.get(), count);
        rail.set(MaterialContent.MATERIAL.get(), Identifier.withDefaultNamespace(planks));
        return rail;
    }

    private static boolean craftingTableSaved(GameTestHelper helper) {
        return PatternLibrary.entries(helper.getLevel(), StationHandsGameTests.line(helper)).stream()
                .anyMatch(entry -> entry.pattern().result().is(Items.CRAFTING_TABLE));
    }

    /** A terminal whose line has no Crafting CPU and no Crafting Station offers no order grid. */
    @ServusGameTest(timeoutTicks = 100)
    public static void noOrderGridWithoutCpu(GameTestHelper helper) {
        MiniWarehouse.build(helper, MINI_ORIGIN).withTerminal().stock(new ItemStack(Items.OAK_PLANKS, 8));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalMenu menu = TerminalWiring.menu(helper, player, MINI_TERMINAL);
            helper.assertTrue(menu.stock().online(), "the warehouse should be online");
            helper.assertFalse(menu.stock().orderGrid(), "no Craft button without a CPU and a Crafting Station");
            craftingTableGrid(menu);
            menu.startOrder(1);
            helper.assertTrue(menu.terminal().requests().isEmpty(), "no order is made, message " + menu.stock().message());
            helper.succeed();
        });
    }

    /** With a formed CPU and Crafting Stations on the line the terminal offers the order grid. */
    @ServusGameTest(timeoutTicks = 200)
    public static void orderGridWithCpuAndStation(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            helper.assertTrue(menu.stock().orderGrid(), "the Craft button should be offered");
            helper.succeed();
        });
    }

    /** JEI's transfer sets each cell to the accepted item the warehouse holds most of and takes nothing. */
    @ServusGameTest(timeoutTicks = 200)
    public static void transferSetsStockedGhosts(GameTestHelper helper) {
        HeavyRackBlockEntity rack = StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.BIRCH_PLANKS, 5));
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            List<ItemStack> planks = List.of(new ItemStack(Items.BIRCH_PLANKS), new ItemStack(Items.OAK_PLANKS));
            menu.fillGhosts(List.of(List.of(), planks, List.of(), List.of(), planks, List.of(), List.of(), List.of(), List.of()));
            helper.assertTrue(menu.gridCells().get(1).is(Items.OAK_PLANKS) && menu.gridCells().get(4).is(Items.OAK_PLANKS),
                    "the stocked oak planks are chosen, got " + menu.gridCells());
            helper.assertTrue(player.getInventory().countItem(Items.BIRCH_PLANKS) == 5, "the inventory keeps its planks");
            helper.assertTrue(WarehouseFixtures.count(rack, Items.OAK_PLANKS) == 16, "the rack keeps its planks");
            CraftPreviewPayload preview = menu.preview(ItemStack.EMPTY, 4, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(preview.item().is(Items.STICK) && preview.patternSaved(), "the sticks pattern is known, got " + preview);
            helper.succeed();
        });
    }

    /** JEI's bare Rail finds the Rail the warehouse holds most of, with its wood. */
    @ServusGameTest(timeoutTicks = 200)
    public static void transferPicksStockedRail(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        WarehouseFixtures.stock(helper, EXTRA, rail("spruce_planks", 10), rail("oak_planks", 40));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu) && TerminalWiring.stockOf(menu, ServusShuttlesItems.RAIL.get()) == 50, 160,
                () -> "stations, CPU and Rails should be on the line", () -> {
            List<ItemStack> iron = List.of(new ItemStack(Items.IRON_INGOT));
            List<ItemStack> bareRail = List.of(new ItemStack(ServusShuttlesItems.RAIL.get()));
            menu.fillGhosts(List.of(iron, iron, iron, iron, bareRail, iron, iron, iron, iron));
            ItemStack centre = menu.gridCells().get(4);
            helper.assertTrue(ItemStack.isSameItemSameComponents(centre, rail("oak_planks", 1)),
                    "the centre holds the stocked oak Rail, got " + centre + " " + centre.getComponentsPatch());
            helper.assertTrue(menu.gridCells().get(0).is(Items.IRON_INGOT), "the unstocked iron takes the first item");
            ItemStack result = PatternEditing.encode(helper.getLevel(), menu.gridCells()).map(EncodedPattern::result).orElse(ItemStack.EMPTY);
            helper.assertTrue(result.is(ServusShuttlesItems.ELEVATOR.get()) && OAK.equals(result.get(MaterialContent.MATERIAL.get())),
                    "the grid makes oak Elevators, got " + result.getComponents());
            helper.succeed();
        });
    }

    /** The rack grid's result carries the wood of its planks, equals the order preview's item and saves. */
    @ServusGameTest(timeoutTicks = 200)
    public static void rackGridResultMatchesPreview(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        WarehouseFixtures.stock(helper, EXTRA, new ItemStack(Items.IRON_INGOT, 16));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            for (int cell : RACK_IRON) {
                menu.setGhost(cell, new ItemStack(Items.IRON_INGOT));
            }
            for (int cell = 3; cell < 6; cell++) {
                menu.setGhost(cell, new ItemStack(Items.OAK_PLANKS));
            }
            ItemStack result = PatternEditing.encode(helper.getLevel(), menu.gridCells()).map(EncodedPattern::result).orElse(ItemStack.EMPTY);
            helper.assertTrue(result.is(ServusShuttlesItems.HEAVY_RACK.get()) && result.getCount() == 3
                            && OAK.equals(result.get(ServusShuttlesComponents.RACK_WOOD.get())),
                    "the grid makes three oak racks, got " + result + " " + result.getComponents());
            CraftPreviewPayload preview = menu.preview(ItemStack.EMPTY, 3, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(ItemStack.isSameItemSameComponents(preview.item(), result) && !preview.patternSaved(),
                    "the preview is the same oak rack, got " + preview);
            menu.savePattern();
            helper.assertTrue(menu.stock().message().isEmpty(), "no message, got " + menu.stock().message());
            helper.assertTrue(PatternLibrary.entries(helper.getLevel(), StationHandsGameTests.line(helper)).stream()
                    .anyMatch(entry -> ItemStack.isSameItemSameComponents(entry.pattern().result(), result)), "the oak rack pattern is stored");
            helper.succeed();
        });
    }

    /** Start with a recipe no pattern has makes a producing request, and the crafting table lands in the output. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void oneOffOrderCrafts(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            craftingTableGrid(menu);
            CraftPreviewPayload preview = menu.preview(ItemStack.EMPTY, 1, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(preview.startable() && !preview.patternSaved(), "a one-off crafting table can start, got " + preview);
            menu.startOrder(1);
            StorageTerminalBlockEntity terminal = menu.terminal();
            helper.assertTrue(menu.stock().message().isEmpty() && terminal.requests().size() == 1,
                    "one request should be listed, message " + menu.stock().message());
            helper.assertFalse(craftingTableSaved(helper), "a one-off start saves no pattern");
            helper.assertTrue(menu.gridCells().stream().allMatch(ItemStack::isEmpty), "the started order clears the grid");
            Waits.when(helper, () -> terminal.outputCount(Items.CRAFTING_TABLE) >= 1, 1200,
                    () -> "the crafting table should land in the output slots", helper::succeed);
        });
    }

    /** Save stores the grid as a pattern of the line and starts no order. */
    @ServusGameTest(timeoutTicks = 200)
    public static void saveAsPatternStores(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            craftingTableGrid(menu);
            menu.savePattern();
            helper.assertTrue(menu.stock().message().isEmpty(), "no message, got " + menu.stock().message());
            helper.assertTrue(menu.terminal().requests().isEmpty(), "saving starts no order");
            helper.assertTrue(craftingTableSaved(helper), "the crafting table pattern is stored");
            CraftPreviewPayload preview = menu.preview(ItemStack.EMPTY, 1, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(preview.patternSaved(), "the grid is now a known pattern");
            helper.succeed();
        });
    }

    /** Save as pattern from the order grid stores Any where Any is possible. */
    @ServusGameTest(timeoutTicks = 200)
    public static void orderGridSavesAny(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            craftingTableGrid(menu);
            menu.savePattern();
            helper.assertTrue(menu.stock().message().isEmpty(), "no message, got " + menu.stock().message());
            helper.assertTrue(PatternLibrary.entries(helper.getLevel(), StationHandsGameTests.line(helper)).stream()
                            .anyMatch(entry -> entry.pattern().result().is(Items.CRAFTING_TABLE) && entry.pattern().any()),
                    "the crafting table pattern is stored as Any");
            helper.succeed();
        });
    }

    /** A refused save keeps the grid, stores nothing and tells why on the message line. */
    @ServusGameTest(timeoutTicks = 200)
    public static void refusedSaveKeepsGrid(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            menu.setGhost(4, new ItemStack(Items.STICK));
            menu.savePattern();
            helper.assertTrue(menu.stock().message().equals("no_match"), "the grid matches nothing, got " + menu.stock().message());
            helper.assertTrue(menu.gridCells().get(4).is(Items.STICK), "the grid keeps its stick");
            helper.assertTrue(menu.terminal().requests().isEmpty(), "no order starts");
            helper.succeed();
        });
    }

    /** A one-off order keeps its pattern through a save and load of its CPU's orders and still delivers. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void oneOffOrderSurvivesReload(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            craftingTableGrid(menu);
            menu.startOrder(1);
            CpuBlockEntity cpu = CpuOrders.cpus(helper.getLevel(), StationHandsGameTests.line(helper)).get(0);
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            cpu.book().load(cpu.book().save(registries), registries);
            WorkOrder order = cpu.book().orders().get(cpu.book().orders().size() - 1);
            helper.assertTrue(order.oneOff() != null && order.oneOff().result().is(Items.CRAFTING_TABLE),
                    "the loaded order keeps its one-off pattern");
            StorageTerminalBlockEntity terminal = menu.terminal();
            Waits.when(helper, () -> terminal.outputCount(Items.CRAFTING_TABLE) >= 1, 1200,
                    () -> "the crafting table should land after the reload, order " + order.state() + " " + order.reason(), helper::succeed);
        });
    }
}
