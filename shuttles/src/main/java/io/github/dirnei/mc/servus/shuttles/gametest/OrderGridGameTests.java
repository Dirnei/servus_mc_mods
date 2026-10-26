package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
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
import io.github.dirnei.mc.servus.shuttles.terminal.StockEntry;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
    private static final Identifier STICK_RECIPE = Identifier.withDefaultNamespace("stick");
    private static final Identifier ELEVATOR_RECIPE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "elevator");

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

    /** A plank above a plank of this kind: sticks. */
    private static void sticksGrid(StorageTerminalMenu menu, Item planks) {
        menu.setGhost(1, new ItemStack(planks));
        menu.setGhost(4, new ItemStack(planks));
    }

    /** How many of the item From stock lists in the preview. */
    private static long fromStock(CraftPreviewPayload preview, Item item) {
        return preview.fromStock().stream().filter(entry -> entry.stack().is(item)).mapToLong(StockEntry::count).sum();
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

    /**
     * JEI's transfer lays the recipe out with the accepted item the warehouse
     * holds most of, takes nothing, opens as Any and keeps the mode of an open grid.
     */
    @ServusGameTest(timeoutTicks = 200)
    public static void transferSetsStockedGhosts(GameTestHelper helper) {
        HeavyRackBlockEntity rack = StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.BIRCH_PLANKS, 5));
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            menu.clickMenuButton(player, RecipeEditorButtons.MODE);
            menu.fillFromRecipe(STICK_RECIPE, false);
            List<ItemStack> used = menu.gridCells().stream().filter(cell -> !cell.isEmpty()).toList();
            helper.assertTrue(used.size() == 2 && used.stream().allMatch(cell -> cell.is(Items.OAK_PLANKS)),
                    "the stocked oak planks are chosen, got " + menu.gridCells());
            helper.assertTrue(menu.mode() == EncodedPattern.Mode.ANY, "a transfer that opens the grid starts as Any");
            helper.assertTrue(player.getInventory().countItem(Items.BIRCH_PLANKS) == 5, "the inventory keeps its planks");
            helper.assertTrue(WarehouseFixtures.count(rack, Items.OAK_PLANKS) == 16, "the rack keeps its planks");
            CraftPreviewPayload preview = menu.preview(ItemStack.EMPTY, 4, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(preview.item().is(Items.STICK) && preview.patternSaved(), "the sticks pattern is known, got " + preview);
            menu.clickMenuButton(player, RecipeEditorButtons.MODE);
            menu.fillFromRecipe(STICK_RECIPE, true);
            helper.assertTrue(menu.mode() == EncodedPattern.Mode.EXACT, "a transfer into the open grid keeps its mode");
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
            menu.fillFromRecipe(ELEVATOR_RECIPE, false);
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

    /** An Any one-off order of sticks takes the spruce planks the warehouse holds most of and leaves the oak planks. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void oneOffAnyUsesSpruce(GameTestHelper helper) {
        HeavyRackBlockEntity rack = StationHandsGameTests.setup(helper, 3);
        WarehouseFixtures.stock(helper, EXTRA, new ItemStack(Items.SPRUCE_PLANKS, 40));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu) && TerminalWiring.stockOf(menu, Items.SPRUCE_PLANKS) == 40, 160,
                () -> "stations, CPU and spruce planks should be on the line", () -> {
            sticksGrid(menu, Items.OAK_PLANKS);
            helper.assertTrue(menu.mode() == EncodedPattern.Mode.ANY, "the order grid starts as Any");
            CraftPreviewPayload preview = menu.preview(ItemStack.EMPTY, 16, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(fromStock(preview, Items.SPRUCE_PLANKS) == 8 && preview.missing().isEmpty() && preview.startable(),
                    "the preview takes 8 spruce planks, got " + preview);
            menu.startOrder(16);
            helper.assertTrue(menu.mode() == EncodedPattern.Mode.ANY && menu.gridCells().stream().allMatch(ItemStack::isEmpty),
                    "a started order empties the grid and asks for Any again");
            StorageTerminalBlockEntity terminal = menu.terminal();
            Waits.when(helper, () -> terminal.outputCount(Items.STICK) >= 16, 1200, () -> "16 sticks should land in the output slots", () -> {
                helper.assertTrue(WarehouseFixtures.count(rack, Items.OAK_PLANKS) == 3, "the oak planks stay in the warehouse");
                helper.succeed();
            });
        });
    }

    /** An Exact one-off order needs exactly its planks; the same grid as Any takes the stocked ones. */
    @ServusGameTest(timeoutTicks = 200)
    public static void oneOffExactWaitsForBirch(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        WarehouseFixtures.stock(helper, EXTRA, new ItemStack(Items.SPRUCE_PLANKS, 40));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            sticksGrid(menu, Items.BIRCH_PLANKS);
            CraftPreviewPayload any = menu.preview(ItemStack.EMPTY, 8, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(any.missing().isEmpty() && any.startable(), "Any takes stocked planks, got " + any);
            menu.clickMenuButton(player, RecipeEditorButtons.MODE);
            CraftPreviewPayload exact = menu.preview(ItemStack.EMPTY, 8, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(exact.missing().stream().anyMatch(entry -> entry.stack().is(Items.BIRCH_PLANKS)) && !exact.startable(),
                    "Exact misses birch planks, got " + exact);
            helper.succeed();
        });
    }

    /** An Any one-off order keeps its mode through a save and load of its CPU's orders and still crafts from spruce. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void oneOffAnyKeepsModeAfterReload(GameTestHelper helper) {
        HeavyRackBlockEntity rack = StationHandsGameTests.setup(helper, 3);
        WarehouseFixtures.stock(helper, EXTRA, new ItemStack(Items.SPRUCE_PLANKS, 40));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu) && TerminalWiring.stockOf(menu, Items.SPRUCE_PLANKS) == 40, 160,
                () -> "stations, CPU and spruce planks should be on the line", () -> {
            sticksGrid(menu, Items.OAK_PLANKS);
            menu.startOrder(16);
            CpuBlockEntity cpu = CpuOrders.cpus(helper.getLevel(), StationHandsGameTests.line(helper)).get(0);
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            cpu.book().load(cpu.book().save(registries), registries);
            WorkOrder order = cpu.book().orders().get(cpu.book().orders().size() - 1);
            helper.assertTrue(order.oneOff() != null && order.oneOff().any(), "the loaded order keeps its Any one-off pattern");
            StorageTerminalBlockEntity terminal = menu.terminal();
            Waits.when(helper, () -> terminal.outputCount(Items.STICK) >= 16, 1200,
                    () -> "the sticks should land after the reload, order " + order.state() + " " + order.reason(), () -> {
                helper.assertTrue(WarehouseFixtures.count(rack, Items.OAK_PLANKS) == 3, "the oak planks stay in the warehouse");
                helper.succeed();
            });
        });
    }

    /** Save as pattern stores the mode the grid is set to. */
    @ServusGameTest(timeoutTicks = 200)
    public static void saveAsPatternKeepsExact(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            craftingTableGrid(menu);
            menu.clickMenuButton(player, RecipeEditorButtons.MODE);
            menu.savePattern();
            helper.assertTrue(PatternLibrary.entries(helper.getLevel(), StationHandsGameTests.line(helper)).stream()
                            .anyMatch(entry -> entry.pattern().result().is(Items.CRAFTING_TABLE) && !entry.pattern().any()),
                    "the crafting table pattern is stored as Exact");
            helper.succeed();
        });
    }

    /** Save shows only while the line has no pattern this one duplicates under the chosen mode. */
    @ServusGameTest(timeoutTicks = 200)
    public static void saveHiddenForSecondAnyPattern(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            craftingTableGrid(menu);
            menu.savePattern();
            for (int cell : SQUARE) {
                menu.setGhost(cell, new ItemStack(Items.BIRCH_PLANKS));
            }
            CraftPreviewPayload any = menu.preview(ItemStack.EMPTY, 1, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertTrue(any.patternSaved(), "an Any birch table duplicates the Any oak table, got " + any);
            menu.clickMenuButton(player, RecipeEditorButtons.MODE);
            CraftPreviewPayload exact = menu.preview(ItemStack.EMPTY, 1, CraftPreviewRequestPayload.Mode.ORDER);
            helper.assertFalse(exact.patternSaved(), "an Exact birch table is new, got " + exact);
            helper.succeed();
        });
    }

    /** Clear empties the grid and keeps its mode; Cancel empties it and asks for Any again. */
    @ServusGameTest(timeoutTicks = 200)
    public static void clearButtonEmptiesGrid(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 16);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = menu(helper, player);
        Waits.when(helper, () -> stocked(helper, menu), 160, () -> "stations and CPU should join the line", () -> {
            craftingTableGrid(menu);
            menu.clickMenuButton(player, RecipeEditorButtons.MODE);
            menu.clickMenuButton(player, RecipeEditorButtons.CLEAR);
            helper.assertTrue(menu.gridCells().stream().allMatch(ItemStack::isEmpty), "Clear empties every cell");
            helper.assertTrue(menu.mode() == EncodedPattern.Mode.EXACT, "Clear keeps the mode");
            craftingTableGrid(menu);
            menu.clickMenuButton(player, RecipeEditorButtons.CANCEL);
            helper.assertTrue(menu.gridCells().stream().allMatch(ItemStack::isEmpty) && menu.mode() == EncodedPattern.Mode.ANY,
                    "Cancel empties the grid and asks for Any again");
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
