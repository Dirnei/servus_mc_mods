package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.module.ModuleSlot;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Shuttle Workshop, shuttle modules and how the fleet uses them. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShuttleModuleGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos WS_POS = new BlockPos(5, 1, 5);
    /** Menu index of hotbar slot 0. */
    private static final int HOTBAR = ShuttleWorkshopMenu.INVENTORY_START + 27;

    private ShuttleModuleGameTests() {
    }

    private static ShuttleWorkshopMenu workshop(GameTestHelper helper, Player player) {
        helper.setBlock(WS_POS, ServusStorageBlocks.SHUTTLE_WORKSHOP.get().defaultBlockState());
        return new ShuttleWorkshopMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(WS_POS)));
    }

    private static boolean hasCargoHold(ItemStack shuttle) {
        return shuttle.is(ServusStorageItems.SHUTTLE.get())
                && ShuttleModules.of(shuttle).utility().equals(Optional.of(ServusStorageItems.CARGO_HOLD.get()));
    }

    /** Fit a Cargo Hold, carry the shuttle out and back in, take the module out again. */
    @GameTest(template = EMPTY)
    public static void workshopFitsCargoHold(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE.get()));
        player.getInventory().setItem(1, new ItemStack(ServusStorageItems.CARGO_HOLD.get()));

        menu.quickMoveStack(player, HOTBAR);
        helper.assertTrue(menu.shuttle().is(ServusStorageItems.SHUTTLE.get()), "the shuttle goes into the shuttle slot");
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(hasCargoHold(menu.shuttle()), "the Cargo Hold is fitted into the shuttle");
        helper.assertTrue(player.getInventory().getItem(1).isEmpty(), "the Cargo Hold item left the inventory");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).getItem().is(ServusStorageItems.CARGO_HOLD.get()), "the utility slot shows it");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.TOOL_SLOT).getItem().isEmpty(), "the tool slot stays empty");

        menu.quickMoveStack(player, ShuttleWorkshopMenu.SHUTTLE_SLOT);
        helper.assertTrue(menu.shuttle().isEmpty(), "the shuttle left the workshop");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).getItem().isEmpty(), "no shuttle, no modules shown");
        int at = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (hasCargoHold(player.getInventory().getItem(i))) {
                at = i;
            }
        }
        helper.assertTrue(at >= 0, "the player holds a shuttle with a Cargo Hold");

        int menuIndex = at < 9 ? HOTBAR + at : ShuttleWorkshopMenu.INVENTORY_START + at - 9;
        menu.quickMoveStack(player, menuIndex);
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).getItem().is(ServusStorageItems.CARGO_HOLD.get()),
                "back in the workshop the utility slot shows the Cargo Hold");

        menu.quickMoveStack(player, ShuttleWorkshopMenu.UTILITY_SLOT);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).isEmpty(), "the module is removed from the shuttle");
        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.shuttle(), new ItemStack(ServusStorageItems.SHUTTLE.get())),
                "a shuttle without modules is a plain shuttle again");
        helper.assertTrue(player.getInventory().countItem(ServusStorageItems.CARGO_HOLD.get()) == 1, "the Cargo Hold is back in the inventory");
        helper.succeed();
    }

    /** Modules need a shuttle and the slot of their kind; only one shuttle goes in. */
    @GameTest(template = EMPTY)
    public static void workshopRefusesWrongModules(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ItemStack cargo = new ItemStack(ServusStorageItems.CARGO_HOLD.get());
        helper.assertFalse(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).mayPlace(cargo), "no module without a shuttle");
        player.getInventory().setItem(1, cargo);
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 1, "shift click leaves the module in the inventory without a shuttle");

        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE.get(), 5));
        menu.quickMoveStack(player, HOTBAR);
        helper.assertTrue(menu.shuttle().getCount() == 1, "one shuttle goes in");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 4, "four stay in the inventory");
        helper.assertFalse(menu.getSlot(ShuttleWorkshopMenu.TOOL_SLOT).mayPlace(cargo), "a Cargo Hold does not fit the tool slot");
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.UTILITY_SLOT).mayPlace(cargo), "it fits the utility slot");
        helper.succeed();
    }

    /** The workshop keeps nothing: closing hands the shuttle back with its modules. */
    @GameTest(template = EMPTY)
    public static void workshopReturnsShuttleOnClose(GameTestHelper helper) {
        // Vanilla hands container items back only to a server player.
        Player player = helper.makeMockServerPlayerInLevel();
        ShuttleWorkshopMenu menu = workshop(helper, player);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE.get()));
        player.getInventory().setItem(1, new ItemStack(ServusStorageItems.CARGO_HOLD.get()));
        menu.quickMoveStack(player, HOTBAR);
        menu.quickMoveStack(player, HOTBAR + 1);
        menu.removed(player);
        boolean back = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            back |= hasCargoHold(player.getInventory().getItem(i));
        }
        helper.assertTrue(back, "the shuttle with its Cargo Hold is back in the inventory");
        helper.assertTrue(menu.shuttle().isEmpty(), "the workshop is empty");
        helper.succeed();
    }

    // Fleet: controller north of x 1 at z 3, terminal at x 1 z 4, rail along z 4 from x 2 to 9,
    // a garage behind the rail at x 2, a rack facing the rail at its far end.

    private static final BlockPos FL_CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos FL_TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos FL_GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos FL_RACK = new BlockPos(9, 1, 5);

    private static ItemStack cargoShuttle() {
        ItemStack shuttle = new ItemStack(ServusStorageItems.SHUTTLE.get());
        ShuttleModules.set(shuttle, ShuttleModules.EMPTY.with(ModuleSlot.UTILITY, Optional.of(ServusStorageItems.CARGO_HOLD.get())));
        return shuttle;
    }

    /** The warehouse above with 100 cobblestone in the rack; the shuttle goes into the garage's given slot. */
    private static ShuttleGarageBlockEntity fleetSetup(GameTestHelper helper, ItemStack shuttle, int slot) {
        helper.setBlock(FL_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, FL_TERMINAL);
        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 4), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(FL_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(FL_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(FL_GARAGE);
        garage.shuttles().setStackInSlot(slot, shuttle);
        HeavyRackBlockEntity rack = helper.getBlockEntity(FL_RACK);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        rack.items().insertItem(1, new ItemStack(Items.COBBLESTONE, 36), false);
        return garage;
    }

    private static WarehouseNetwork requestCobblestone(GameTestHelper helper, int amount) {
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(FL_CONTROLLER);
        StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, FL_TERMINAL);
        new StorageTerminalMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), terminal)
                .request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), amount);
        return controller.network().orElseThrow();
    }

    /** A Cargo Hold shuttle takes both pick jobs of a request for 100 and delivers them on one trip. */
    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void cargoHoldPicksTwoStacks(GameTestHelper helper) {
        fleetSetup(helper, cargoShuttle(), 0);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = requestCobblestone(helper, 100);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(network.garageOut(helper.absolutePos(FL_GARAGE)) == 1, "the shuttle is out on the picks");
                helper.assertTrue(network.ridingJobs() == 2 && network.queuedJobs() == 0,
                        "both jobs ride the one shuttle, riding " + network.ridingJobs() + ", queued " + network.queuedJobs());
                StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, FL_TERMINAL);
                TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 100, 600,
                        () -> "all 100 should arrive, got " + terminal.outputCount(Items.COBBLESTONE), helper::succeed);
            });
        });
    }

    /** A plain shuttle carries one stack: the second job of the request waits in the queue. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void plainShuttleTakesOneJob(GameTestHelper helper) {
        fleetSetup(helper, new ItemStack(ServusStorageItems.SHUTTLE.get()), 0);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = requestCobblestone(helper, 100);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(network.ridingJobs() == 1 && network.queuedJobs() == 1,
                        "one job rides, one waits, riding " + network.ridingJobs() + ", queued " + network.queuedJobs());
                helper.succeed();
            });
        });
    }

    /** The garage locks the slot of the shuttle that is out, not the first filled slot. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void garageLocksSlotOfShuttleOut(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = fleetSetup(helper, new ItemStack(ServusStorageItems.SHUTTLE.get()), 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            requestCobblestone(helper, 30);
            helper.runAfterDelay(8, () -> {
                garage.shuttles().setStackInSlot(0, new ItemStack(ServusStorageItems.SHUTTLE.get()));
                ShuttleGarageMenu menu = new ShuttleGarageMenu(1, player.getInventory(), garage);
                menu.broadcastChanges();
                helper.assertTrue(menu.out() == 1, "one shuttle is out");
                helper.assertTrue(menu.locked(1), "the slot of the shuttle that left is locked");
                helper.assertFalse(menu.locked(0), "the shuttle put in afterwards can be taken");
                helper.assertFalse(menu.getSlot(1).mayPickup(player), "the locked slot cannot be emptied");
                helper.succeed();
            });
        });
    }

    // Put-away: controller at x 1, rail along z 1 from x 2 to 10, an inbound station and a crate
    // beside it, the garage far away at x 10 so the station's stacks are all queued before it arrives.

    private static final BlockPos PW_CONTROLLER = new BlockPos(1, 1, 1);
    private static final BlockPos PW_STATION = new BlockPos(3, 1, 2);
    private static final BlockPos PW_CRATE = new BlockPos(6, 1, 2);
    private static final BlockPos PW_GARAGE = new BlockPos(10, 1, 2);

    /** A Cargo Hold shuttle loads every stack of the station at once and puts them away on one trip. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void cargoHoldPutAwayLoadsAll(GameTestHelper helper) {
        helper.setBlock(PW_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 2; x <= 10; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 1), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(PW_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        helper.setBlock(PW_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(PW_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(PW_GARAGE);
        helper.assertTrue(garage.addShuttle(cargoShuttle()), "the garage takes the Cargo Hold shuttle");
        InboundStationBlockEntity station = helper.getBlockEntity(PW_STATION);
        CrateBlockEntity crate = helper.getBlockEntity(PW_CRATE);
        helper.runAfterDelay(10, () -> {
            for (int i = 0; i < 3; i++) {
                ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 64), false);
            }
            TerminalWiring.when(helper, station::isEmpty, 150, () -> "the shuttle should load the station", () -> {
                helper.assertTrue(crate.count() == 0, "all three stacks left on one shuttle before any reached the crate, crate has " + crate.count());
                TerminalWiring.when(helper, () -> crate.count() == 192, 150,
                        () -> "all three stacks should reach the crate, it has " + crate.count(), helper::succeed);
            });
        });
    }
}
