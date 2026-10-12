package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** The Storage Terminal's crafting grid: crafting, refill from instant stock, pull, clear, close and JEI fill. */
public final class CraftingGridGameTests {

    private static final String EMPTY = "empty_12x6x12";
    /** The mini warehouse's rails start here, so its Packing Station stands at {@link #TERMINAL}. */
    private static final BlockPos ORIGIN = new BlockPos(4, 1, 2);
    private static final BlockPos TERMINAL = new BlockPos(5, 1, 3);

    private CraftingGridGameTests() {
    }

    /** A mini warehouse whose rack holds planks and the terminal on its Packing Station; runs the test once the warehouse is up. */
    private static void setup(GameTestHelper helper, int planks, Consumer<Fixture> test) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal().stock(new ItemStack(Items.OAK_PLANKS, planks));
        HeavyRackBlockEntity rack = warehouse.rack();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), TerminalWiring.at(helper, TERMINAL));
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            test.accept(new Fixture(menu, rack, player));
        });
    }

    private record Fixture(StorageTerminalMenu menu, HeavyRackBlockEntity rack, Player player) {
        int planks() {
            return MiniWarehouse.count(rack, Items.OAK_PLANKS);
        }
    }

    /** A plank above a plank in the grid. */
    private static void sticksPattern(StorageTerminalMenu menu, int count) {
        menu.craftSlots().setItem(1, new ItemStack(Items.OAK_PLANKS, count));
        menu.craftSlots().setItem(4, new ItemStack(Items.OAK_PLANKS, count));
    }

    private static ItemStack result(StorageTerminalMenu menu) {
        return menu.getSlot(menu.resultIndex()).getItem();
    }

    private static boolean gridEmpty(StorageTerminalMenu menu) {
        for (int slot = 0; slot < 9; slot++) {
            if (!menu.craftSlots().getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Taking the result crafts four sticks from the grid alone; the rack keeps its planks. */
    @ServusGameTest(timeoutTicks = 100)
    public static void gridCraftsSticks(GameTestHelper helper) {
        setup(helper, 10, f -> {
            sticksPattern(f.menu(), 1);
            helper.assertTrue(result(f.menu()).is(Items.STICK) && result(f.menu()).getCount() == 4,
                    "the result shows four sticks, got " + result(f.menu()));
            f.menu().clicked(f.menu().resultIndex(), 0, ContainerInput.PICKUP, f.player());
            helper.assertTrue(f.menu().getCarried().is(Items.STICK) && f.menu().getCarried().getCount() == 4,
                    "the cursor holds four sticks, got " + f.menu().getCarried());
            helper.assertTrue(gridEmpty(f.menu()), "the grid is not refilled from the warehouse");
            helper.assertTrue(f.planks() == 10, "the rack keeps its planks, holds " + f.planks());
            helper.succeed();
        });
    }

    /** Shift clicking the result crafts only what the grid holds: two planks give four sticks, the rack's planks stay. */
    @ServusGameTest(timeoutTicks = 100)
    public static void gridShiftCraftUsesOnlyGrid(GameTestHelper helper) {
        setup(helper, 6, f -> {
            sticksPattern(f.menu(), 1);
            f.menu().clicked(f.menu().resultIndex(), 0, ContainerInput.QUICK_MOVE, f.player());
            int sticks = f.player().getInventory().countItem(Items.STICK);
            helper.assertTrue(sticks == 4, "four sticks in the inventory, got " + sticks);
            helper.assertTrue(gridEmpty(f.menu()), "the grid is empty");
            helper.assertTrue(f.planks() == 6, "the rack keeps its planks, holds " + f.planks());
            helper.succeed();
        });
    }

    /** After a craft the grid is not refilled from the rack. */
    @ServusGameTest(timeoutTicks = 100)
    public static void gridDoesNotRefillFromRack(GameTestHelper helper) {
        setup(helper, 32, f -> {
            sticksPattern(f.menu(), 1);
            f.menu().clicked(f.menu().resultIndex(), 0, ContainerInput.PICKUP, f.player());
            helper.assertTrue(gridEmpty(f.menu()) && result(f.menu()).isEmpty(), "the grid stays empty after the craft");
            helper.assertTrue(f.planks() == 32, "the rack keeps its 32 planks, holds " + f.planks());
            helper.succeed();
        });
    }

    /** JEI fill takes from the player inventory only, never from the warehouse. */
    @ServusGameTest(timeoutTicks = 100)
    public static void gridFillFromInventoryOnly(GameTestHelper helper) {
        setup(helper, 10, f -> {
            f.player().getInventory().add(new ItemStack(Items.COAL, 3));
            List<List<ItemStack>> options = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) {
                options.add(List.of());
            }
            options.set(1, List.of(new ItemStack(Items.COAL)));
            options.set(4, List.of(new ItemStack(Items.OAK_PLANKS)));
            f.menu().fillGrid(options, false);
            helper.assertTrue(f.menu().craftSlots().getItem(1).is(Items.COAL), "coal in slot 1 from the player");
            helper.assertTrue(f.menu().craftSlots().getItem(4).isEmpty(), "no planks: the player has none and the rack gives none");
            helper.assertTrue(f.planks() == 10, "the rack keeps its planks");
            helper.assertTrue(f.player().getInventory().countItem(Items.COAL) == 2, "one coal left the inventory");
            helper.succeed();
        });
    }

    /** The clear button moves the grid into the warehouse. */
    @ServusGameTest(timeoutTicks = 400)
    public static void gridClearStores(GameTestHelper helper) {
        setup(helper, 4, f -> {
            sticksPattern(f.menu(), 1);
            f.menu().clearGrid();
            helper.assertTrue(gridEmpty(f.menu()), "the grid is empty");
            helper.assertTrue(result(f.menu()).isEmpty(), "no result without ingredients");
            TerminalWiring.when(helper, () -> f.planks() == 6, 350,
                    () -> "the rack should get the two planks back through the input, holds " + f.planks(), helper::succeed);
        });
    }

    /** Closing the terminal moves the grid into the warehouse. */
    @ServusGameTest(timeoutTicks = 400)
    public static void gridCloseStores(GameTestHelper helper) {
        setup(helper, 4, f -> {
            f.menu().craftSlots().setItem(0, new ItemStack(Items.OAK_PLANKS, 3));
            f.menu().removed(f.player());
            helper.assertTrue(f.player().getInventory().countItem(Items.OAK_PLANKS) == 0, "nothing went to the player");
            TerminalWiring.when(helper, () -> f.planks() == 7, 350,
                    () -> "the rack should get three planks back through the input, holds " + f.planks(), helper::succeed);
        });
    }
}
