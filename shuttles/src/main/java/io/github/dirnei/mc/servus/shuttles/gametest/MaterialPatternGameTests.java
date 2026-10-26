package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.order.OrderBook;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternEditing;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternLibrary;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.terminal.MaterialCraftable;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestRow;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalCraftables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Material patterns on a CPU: the Heavy Duty Rack whose wood follows its
 * planks and the Elevator whose material follows its Rail. Uses the station
 * fixture of {@link StationHandsGameTests}; patterns go into the free slots of
 * its Pattern Storage.
 */
public final class MaterialPatternGameTests {

    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos EXTRA = new BlockPos(1, 0, 3);
    private static final int[] RACK_IRON = {0, 2, 6, 8};
    private static final int[] RAIL_IRON = {0, 2, 3, 5, 6, 8};

    private MaterialPatternGameTests() {
    }

    /** Iron in the corners and the planks across the middle row. */
    static List<ItemStack> rackGrid(Item planks) {
        List<ItemStack> cells = empty();
        for (int cell : RACK_IRON) {
            cells.set(cell, new ItemStack(Items.IRON_INGOT));
        }
        for (int cell = 3; cell < 6; cell++) {
            cells.set(cell, new ItemStack(planks));
        }
        return cells;
    }

    /** Iron around a Rail. */
    private static List<ItemStack> elevatorGrid(ItemStack rail) {
        List<ItemStack> cells = empty();
        for (int cell = 0; cell < 9; cell++) {
            cells.set(cell, cell == 4 ? rail : new ItemStack(Items.IRON_INGOT));
        }
        return cells;
    }

    /** Iron down both sides around the planks in the middle. */
    private static List<ItemStack> railGrid(Item planks) {
        List<ItemStack> cells = empty();
        for (int cell : RAIL_IRON) {
            cells.set(cell, new ItemStack(Items.IRON_INGOT));
        }
        cells.set(4, new ItemStack(planks));
        return cells;
    }

    private static List<ItemStack> empty() {
        List<ItemStack> cells = new ArrayList<>();
        for (int cell = 0; cell < 9; cell++) {
            cells.add(ItemStack.EMPTY);
        }
        return cells;
    }

    private static ItemStack rail(String planks, int count) {
        ItemStack rail = new ItemStack(ServusShuttlesItems.RAIL.get(), count);
        rail.set(MaterialContent.MATERIAL.get(), Identifier.withDefaultNamespace(planks));
        return rail;
    }

    private static EncodedPattern pattern(GameTestHelper helper, List<ItemStack> cells, EncodedPattern.Mode mode) {
        return PatternEditing.encode(helper.getLevel(), cells).orElseThrow().withMode(mode);
    }

    /** What the grid makes, as the key of a request for that variant. */
    private static ItemKey made(GameTestHelper helper, List<ItemStack> cells) {
        return ItemKey.of(PatternEditing.encode(helper.getLevel(), cells).orElseThrow().result());
    }

    /** Stores the patterns in the fixture's Pattern Storage from slot 1 on. */
    private static void store(GameTestHelper helper, EncodedPattern... patterns) {
        PatternStorageBlockEntity storage = PatternLibrary.storages(helper.getLevel(), StationHandsGameTests.line(helper)).get(0);
        for (int i = 0; i < patterns.length; i++) {
            storage.set(i + 1, patterns[i]);
        }
    }

    private static OrderBook.Preview preview(GameTestHelper helper, ItemKey key, int amount) {
        return CpuOrders.preview(helper.getLevel(), StationHandsGameTests.line(helper), key, amount);
    }

    private static int count(Map<ItemKey, Integer> list, ItemKey key) {
        return list.getOrDefault(key, 0);
    }

    private static int count(Map<ItemKey, Integer> list, Item item) {
        return list.entrySet().stream().filter(entry -> entry.getKey().item() == item).mapToInt(Map.Entry::getValue).sum();
    }

    /** The fixture with the extra stock, once the line and the stock are ready. */
    private static void ready(GameTestHelper helper, int oakPlanks, List<ItemStack> extra, Item shown, Runnable test) {
        StationHandsGameTests.setup(helper, oakPlanks);
        WarehouseFixtures.stock(helper, EXTRA, extra.toArray(ItemStack[]::new));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = TerminalWiring.menu(helper, player, ACCESS);
        BooleanSupplier stocked = () -> DataLines.ready(helper, TerminalWiring.terminalPos(ACCESS), 2) && TerminalWiring.stockOf(menu, shown) > 0;
        Waits.when(helper, stocked, 160, () -> "stations, CPU and stock should be on the line", test);
    }

    /** An Any rack pattern saved with oak planks makes spruce racks from spruce planks. */
    @ServusGameTest(timeoutTicks = 200)
    public static void spruceRackFromOakPattern(GameTestHelper helper) {
        ready(helper, 16, List.of(new ItemStack(Items.IRON_INGOT, 16), new ItemStack(Items.SPRUCE_PLANKS, 20)), Items.SPRUCE_PLANKS, () -> {
            store(helper, pattern(helper, rackGrid(Items.OAK_PLANKS), EncodedPattern.Mode.ANY));
            ItemKey spruceRack = made(helper, rackGrid(Items.SPRUCE_PLANKS));
            OrderBook.Preview preview = preview(helper, spruceRack, 3);
            helper.assertTrue(preview.missing().isEmpty() && count(preview.toCraft(), spruceRack) == 3
                            && count(preview.fromStock(), Items.SPRUCE_PLANKS) == 3 && count(preview.fromStock(), Items.IRON_INGOT) == 4
                            && count(preview.fromStock(), Items.OAK_PLANKS) == 0,
                    "3 spruce racks from 3 spruce planks and 4 iron, got " + preview);
            helper.succeed();
        });
    }

    /** With no spruce planks in stock the order crafts them from spruce logs first. */
    @ServusGameTest(timeoutTicks = 200)
    public static void sprucePlanksCraftedForRack(GameTestHelper helper) {
        ready(helper, 16, List.of(new ItemStack(Items.IRON_INGOT, 16), new ItemStack(Items.SPRUCE_LOG, 4)), Items.SPRUCE_LOG, () -> {
            EncodedPattern planks = pattern(helper, List.of(new ItemStack(Items.SPRUCE_LOG)), EncodedPattern.Mode.ANY);
            store(helper, pattern(helper, rackGrid(Items.OAK_PLANKS), EncodedPattern.Mode.ANY), planks);
            ItemKey spruceRack = made(helper, rackGrid(Items.SPRUCE_PLANKS));
            OrderBook.Preview preview = preview(helper, spruceRack, 3);
            helper.assertTrue(preview.missing().isEmpty() && count(preview.toCraft(), Items.SPRUCE_PLANKS) >= 3
                            && count(preview.toCraft(), spruceRack) == 3 && count(preview.fromStock(), Items.SPRUCE_LOG) == 1,
                    "spruce planks are crafted from a spruce log for the racks, got " + preview);
            helper.succeed();
        });
    }

    /** An Any Elevator pattern saved with an oak Rail takes a stocked spruce Rail for a spruce Elevator. */
    @ServusGameTest(timeoutTicks = 200)
    public static void spruceElevatorUsesSpruceRail(GameTestHelper helper) {
        ready(helper, 16, List.of(new ItemStack(Items.IRON_INGOT, 16), rail("spruce_planks", 2)), ServusShuttlesItems.RAIL.get(), () -> {
            store(helper, pattern(helper, elevatorGrid(rail("oak_planks", 1)), EncodedPattern.Mode.ANY));
            ItemKey spruceElevator = made(helper, elevatorGrid(rail("spruce_planks", 1)));
            OrderBook.Preview preview = preview(helper, spruceElevator, 4);
            helper.assertTrue(preview.missing().isEmpty() && count(preview.fromStock(), ItemKey.of(rail("spruce_planks", 1))) == 1
                            && count(preview.toCraft(), spruceElevator) == 4,
                    "the spruce Elevator takes the spruce Rail, got " + preview);
            helper.succeed();
        });
    }

    /** An Exact oak Elevator asks for an oak Rail, which an Any Rail pattern saved with spruce planks makes from oak planks. */
    @ServusGameTest(timeoutTicks = 200)
    public static void exactOakElevatorCraftsOakRail(GameTestHelper helper) {
        ready(helper, 16, List.of(new ItemStack(Items.IRON_INGOT, 32)), Items.IRON_INGOT, () -> {
            store(helper, pattern(helper, elevatorGrid(rail("oak_planks", 1)), EncodedPattern.Mode.EXACT),
                    pattern(helper, railGrid(Items.SPRUCE_PLANKS), EncodedPattern.Mode.ANY));
            ItemKey oakElevator = made(helper, elevatorGrid(rail("oak_planks", 1)));
            OrderBook.Preview preview = preview(helper, oakElevator, 4);
            helper.assertTrue(preview.missing().isEmpty() && count(preview.toCraft(), ItemKey.of(rail("oak_planks", 1))) >= 1
                            && count(preview.fromStock(), Items.OAK_PLANKS) == 1,
                    "an oak Rail is crafted from oak planks for the oak Elevator, got " + preview);
            helper.succeed();
        });
    }

    /** Crafting the rack without a wood makes the wood the warehouse holds most of, and the request names it. */
    @ServusGameTest(timeoutTicks = 200)
    public static void genericRackTakesMostStockedWood(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 5);
        WarehouseFixtures.stock(helper, EXTRA, new ItemStack(Items.IRON_INGOT, 16), new ItemStack(Items.BIRCH_PLANKS, 40));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = TerminalWiring.menu(helper, player, ACCESS);
        Waits.when(helper, () -> DataLines.ready(helper, TerminalWiring.terminalPos(ACCESS), 2) && TerminalWiring.stockOf(menu, Items.BIRCH_PLANKS) == 40,
                160, () -> "stations, CPU and birch planks should be on the line", () -> {
            store(helper, pattern(helper, rackGrid(Items.OAK_PLANKS), EncodedPattern.Mode.ANY));
            ItemKey birchRack = made(helper, rackGrid(Items.BIRCH_PLANKS));
            ItemKey generic = ItemKey.of(new ItemStack(ServusShuttlesItems.HEAVY_RACK.get()));
            OrderBook.Preview preview = preview(helper, generic, 2);
            helper.assertTrue(birchRack.equals(preview.made()) && count(preview.fromStock(), Items.BIRCH_PLANKS) == 3,
                    "the generic rack resolves to birch, got " + preview);
            menu.craft(generic, 2);
            List<RequestRow> requests = menu.stock().requests();
            helper.assertTrue(requests.size() == 1 && ItemKey.of(requests.get(0).stack()).equals(birchRack),
                    "the request names birch racks, got " + requests + " message " + menu.stock().message());
            helper.succeed();
        });
    }

    /** With no planks in stock the generic rack takes the pattern's own wood and crafts its planks. */
    @ServusGameTest(timeoutTicks = 200)
    public static void genericRackWithoutStockCraftsOwnPlanks(GameTestHelper helper) {
        ready(helper, 0, List.of(new ItemStack(Items.IRON_INGOT, 16), new ItemStack(Items.OAK_LOG, 4)), Items.OAK_LOG, () -> {
            store(helper, pattern(helper, rackGrid(Items.OAK_PLANKS), EncodedPattern.Mode.ANY), StationJobs.planks());
            ItemKey oakRack = made(helper, rackGrid(Items.OAK_PLANKS));
            OrderBook.Preview preview = preview(helper, ItemKey.of(new ItemStack(ServusShuttlesItems.HEAVY_RACK.get())), 3);
            helper.assertTrue(oakRack.equals(preview.made()) && preview.missing().isEmpty() && count(preview.toCraft(), Items.OAK_PLANKS) >= 3,
                    "oak planks are crafted for oak racks, got " + preview);
            helper.succeed();
        });
    }

    /** The craftable list shows an Exact rack by its wood, and one rack without a wood for an Any rack pattern. */
    @ServusGameTest(timeoutTicks = 200)
    public static void craftableListShowsOneRack(GameTestHelper helper) {
        ready(helper, 16, List.of(new ItemStack(Items.IRON_INGOT, 16)), Items.IRON_INGOT, () -> {
            BlockPos terminal = helper.absolutePos(TerminalWiring.terminalPos(ACCESS));
            store(helper, pattern(helper, rackGrid(Items.SPRUCE_PLANKS), EncodedPattern.Mode.EXACT));
            List<ItemStack> exact = racks(TerminalCraftables.of(helper.getLevel(), terminal, 4096));
            helper.assertTrue(exact.size() == 1 && ItemKey.of(exact.get(0)).equals(made(helper, rackGrid(Items.SPRUCE_PLANKS))),
                    "the Exact rack is listed by its wood, got " + exact);
            store(helper, pattern(helper, rackGrid(Items.SPRUCE_PLANKS), EncodedPattern.Mode.EXACT),
                    pattern(helper, rackGrid(Items.OAK_PLANKS), EncodedPattern.Mode.ANY));
            List<ItemStack> any = racks(TerminalCraftables.of(helper.getLevel(), terminal, 4096));
            helper.assertTrue(any.size() == 1 && !any.get(0).has(ServusShuttlesComponents.RACK_WOOD.get()),
                    "one rack without a wood, got " + any);
            List<MaterialCraftable> materials = TerminalCraftables.materials(helper.getLevel(), terminal);
            helper.assertTrue(materials.size() == 1 && materials.get(0).follows().tagKey().equals("tag.item.minecraft.planks"),
                    "the entry says its wood follows the planks, got " + materials);
            helper.succeed();
        });
    }

    private static List<ItemStack> racks(List<ItemStack> craftables) {
        return craftables.stream().filter(stack -> stack.is(ServusShuttlesItems.HEAVY_RACK.get())).toList();
    }
}
