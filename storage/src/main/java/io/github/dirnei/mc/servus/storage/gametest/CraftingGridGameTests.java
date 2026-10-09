package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Counter Terminal's crafting grid: crafting, refill from instant stock, pull, clear, close and JEI fill. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CraftingGridGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CRATE = new BlockPos(3, 1, 3);
    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 3);
    private static final BlockPos TERMINAL = new BlockPos(5, 1, 3);
    private static final BlockPos RACK = new BlockPos(4, 1, 2);

    private CraftingGridGameTests() {
    }

    /** A crate with planks beside a controller, the terminal wiring beside it; runs the test once the warehouse is up. */
    private static void setup(GameTestHelper helper, int planks, Consumer<Fixture> test) {
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, TERMINAL);
        CrateBlockEntity crate = helper.getBlockEntity(CRATE);
        if (planks > 0) {
            crate.insert(new ItemStack(Items.OAK_PLANKS, planks), false);
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), TerminalWiring.at(helper, TERMINAL));
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            test.accept(new Fixture(menu, crate, player));
        });
    }

    private record Fixture(CounterTerminalMenu menu, CrateBlockEntity crate, Player player) {
    }

    /** A plank above a plank in the grid. */
    private static void sticksPattern(CounterTerminalMenu menu, int count) {
        menu.craftSlots().setItem(1, new ItemStack(Items.OAK_PLANKS, count));
        menu.craftSlots().setItem(4, new ItemStack(Items.OAK_PLANKS, count));
    }

    private static ItemStack result(CounterTerminalMenu menu) {
        return menu.getSlot(menu.resultIndex()).getItem();
    }

    private static boolean gridEmpty(CounterTerminalMenu menu) {
        for (int slot = 0; slot < 9; slot++) {
            if (!menu.craftSlots().getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Taking the result crafts four sticks, and the used planks come back from the crate. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gridCraftsSticks(GameTestHelper helper) {
        setup(helper, 10, f -> {
            sticksPattern(f.menu(), 1);
            helper.assertTrue(result(f.menu()).is(Items.STICK) && result(f.menu()).getCount() == 4,
                    "the result shows four sticks, got " + result(f.menu()));
            f.menu().clicked(f.menu().resultIndex(), 0, ClickType.PICKUP, f.player());
            helper.assertTrue(f.menu().getCarried().is(Items.STICK) && f.menu().getCarried().getCount() == 4,
                    "the cursor holds four sticks, got " + f.menu().getCarried());
            helper.assertTrue(f.menu().craftSlots().getItem(1).is(Items.OAK_PLANKS) && f.menu().craftSlots().getItem(4).is(Items.OAK_PLANKS),
                    "the grid is refilled from the crate");
            helper.assertTrue(f.crate().count(0) == 8, "the crate gave two planks, holds " + f.crate().count(0));
            helper.succeed();
        });
    }

    /** Shift clicking the result crafts until the crate runs dry. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gridShiftCraftRefills(GameTestHelper helper) {
        setup(helper, 6, f -> {
            sticksPattern(f.menu(), 1);
            f.menu().clicked(f.menu().resultIndex(), 0, ClickType.QUICK_MOVE, f.player());
            int sticks = f.player().getInventory().countItem(Items.STICK);
            helper.assertTrue(sticks == 16, "sixteen sticks in the inventory, got " + sticks);
            helper.assertTrue(f.crate().count(0) == 0, "the crate is empty, holds " + f.crate().count(0));
            helper.assertTrue(gridEmpty(f.menu()), "the grid is empty");
            helper.succeed();
        });
    }

    /** Planks in a Heavy Duty Rack are not used to refill the grid. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gridSkipsRackStock(GameTestHelper helper) {
        helper.setBlock(RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK);
        rack.items().insertItem(0, new ItemStack(Items.OAK_PLANKS, 32), false);
        setup(helper, 0, f -> {
            sticksPattern(f.menu(), 1);
            f.menu().clicked(f.menu().resultIndex(), 0, ClickType.QUICK_MOVE, f.player());
            helper.assertTrue(f.player().getInventory().countItem(Items.STICK) == 4, "one craft only");
            helper.assertTrue(gridEmpty(f.menu()), "the grid stays empty");
            helper.assertTrue(rack.items().getStackInSlot(0).getCount() == 32, "the rack keeps its planks");
            helper.succeed();
        });
    }

    /** The clear button moves the grid into the warehouse. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gridClearStores(GameTestHelper helper) {
        setup(helper, 4, f -> {
            sticksPattern(f.menu(), 1);
            f.menu().clearGrid();
            helper.assertTrue(gridEmpty(f.menu()), "the grid is empty");
            helper.assertTrue(f.crate().count(0) == 6, "the crate got the two planks back, holds " + f.crate().count(0));
            helper.assertTrue(result(f.menu()).isEmpty(), "no result without ingredients");
            helper.succeed();
        });
    }

    /** Closing the terminal moves the grid into the warehouse. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gridCloseStores(GameTestHelper helper) {
        setup(helper, 4, f -> {
            f.menu().craftSlots().setItem(0, new ItemStack(Items.OAK_PLANKS, 3));
            f.menu().removed(f.player());
            helper.assertTrue(f.crate().count(0) == 7, "the crate got three planks back, holds " + f.crate().count(0));
            helper.assertTrue(f.player().getInventory().countItem(Items.OAK_PLANKS) == 0, "nothing went to the player");
            helper.succeed();
        });
    }

    /** JEI fill takes from the crate first and from the player inventory for what the warehouse lacks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gridFillFromCrateFirst(GameTestHelper helper) {
        setup(helper, 10, f -> {
            f.player().getInventory().add(new ItemStack(Items.OAK_PLANKS, 5));
            f.player().getInventory().add(new ItemStack(Items.COAL, 3));
            List<List<ItemStack>> options = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) {
                options.add(List.of());
            }
            options.set(1, List.of(new ItemStack(Items.COAL)));
            options.set(4, List.of(new ItemStack(Items.OAK_PLANKS)));
            f.menu().fillGrid(options, false);
            helper.assertTrue(f.menu().craftSlots().getItem(4).is(Items.OAK_PLANKS), "planks in slot 4");
            helper.assertTrue(f.crate().count(0) == 9, "the plank came from the crate, crate holds " + f.crate().count(0));
            helper.assertTrue(f.player().getInventory().countItem(Items.OAK_PLANKS) == 5, "the player keeps their planks");
            helper.assertTrue(f.menu().craftSlots().getItem(1).is(Items.COAL), "coal in slot 1 from the player");
            helper.assertTrue(f.player().getInventory().countItem(Items.COAL) == 2, "one coal left the inventory");
            helper.succeed();
        });
    }

    /** Alt click puts a stack from instant stock onto the empty cursor. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gridPullToCursor(GameTestHelper helper) {
        setup(helper, 80, f -> {
            f.menu().pullToCursor(ItemKey.of(new ItemStack(Items.OAK_PLANKS)));
            helper.assertTrue(f.menu().getCarried().is(Items.OAK_PLANKS) && f.menu().getCarried().getCount() == 64,
                    "the cursor holds a stack, got " + f.menu().getCarried());
            helper.assertTrue(f.crate().count(0) == 16, "the crate holds the rest, " + f.crate().count(0));
            f.menu().pullToCursor(ItemKey.of(new ItemStack(Items.OAK_PLANKS)));
            helper.assertTrue(f.crate().count(0) == 16, "a carried stack blocks another pull");
            helper.succeed();
        });
    }
}
