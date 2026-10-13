package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.StockProvider;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Which blocks join a warehouse: rails, rack fronts and sections, and what counts as stock. */
public final class WarehouseLinkGameTests {

    private static final BlockPos RAIL_RACK = new BlockPos(2, 1, 3);
    private static final BlockPos RAIL_A = new BlockPos(3, 1, 3);
    private static final BlockPos RAIL_B = new BlockPos(4, 1, 3);
    private static final BlockPos RAIL_CONTROLLER = new BlockPos(5, 1, 3);
    private static final BlockPos RAIL_TERMINAL = new BlockPos(6, 1, 3);
    private static final BlockPos RAIL_CHEST = new BlockPos(3, 1, 4);
    private static final BlockPos RACK = new BlockPos(8, 1, 2);
    private static final BlockPos RACK_CONTROLLER = new BlockPos(9, 1, 2);
    private static final BlockPos RACK_TERMINAL = new BlockPos(10, 1, 2);
    private static final BlockPos FR_RACK = new BlockPos(5, 1, 9);
    private static final BlockPos FR_SIDE_RAIL = new BlockPos(6, 1, 9);
    private static final BlockPos FR_SIDE_CONTROLLER = new BlockPos(7, 1, 9);
    private static final BlockPos FR_SIDE_TERMINAL = new BlockPos(8, 1, 9);
    private static final BlockPos FR_FRONT_RAIL = new BlockPos(5, 1, 8);
    private static final BlockPos FR_FRONT_CONTROLLER = new BlockPos(4, 1, 8);
    private static final BlockPos FR_FRONT_TERMINAL = new BlockPos(3, 1, 8);
    private static final int SR_Z = 5;
    private static final BlockPos SR_CONTROLLER = new BlockPos(8, 1, 7);
    private static final BlockPos SR_TERMINAL = new BlockPos(9, 1, 8);
    private static final BlockPos SR_GARAGE = new BlockPos(6, 1, 7);
    private static final BlockPos SS_CONTROLLER = new BlockPos(5, 1, 3);
    private static final BlockPos SS_RACK = new BlockPos(6, 1, 3);
    private static final BlockPos SS_TERMINAL = new BlockPos(9, 1, 4);
    private static final BlockPos SS_GARAGE = new BlockPos(3, 1, 3);

    private WarehouseLinkGameTests() {
    }

    /** Two rails link a rack to the controller; a chest beside a rail stays out of the index. */
    @ServusGameTest(timeoutTicks = 200)
    public static void railsLinkRackAndController(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RAIL_RACK, Direction.EAST);
        helper.setBlock(RAIL_A.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(RAIL_B.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(RAIL_A, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(RAIL_B, ServusStorageBlocks.RAIL.get().defaultBlockState());
        WarehouseFixtures.controller(helper, RAIL_CONTROLLER);
        TerminalWiring.place(helper, RAIL_TERMINAL);
        helper.setBlock(RAIL_CHEST, Blocks.CHEST.defaultBlockState());
        WarehouseFixtures.stock(helper, RAIL_RACK, new ItemStack(Items.COBBLESTONE, 40));
        ChestBlockEntity chest = helper.getBlockEntity(RAIL_CHEST, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.BREAD, 10));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RAIL_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online through the rails");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.COBBLESTONE) == 40, "stock should list 40 cobblestone from the rack");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.BREAD) == 0, "bread beside a rail must not be indexed");

            helper.setBlock(RAIL_A, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(10, () -> {
                StorageTerminalMenu after = new StorageTerminalMenu(1, player.getInventory(), terminal);
                helper.assertTrue(after.stock().online(), "controller side stays online");
                helper.assertTrue(TerminalWiring.stockOf(after, Items.COBBLESTONE) == 0, "rack stock should leave the index after the rail is broken");
                helper.assertTrue(WarehouseFixtures.lit(helper, TerminalWiring.terminalPos(RAIL_TERMINAL)), "terminal lamp stays lit beside the controller");
                helper.succeed();
            });
        });
    }

    /** A rack with a front rail of the warehouse is indexed and shows the item types it holds the most of. */
    @ServusGameTest(timeoutTicks = 200)
    public static void heavyRackIndexesAndDisplays(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        WarehouseFixtures.controller(helper, RACK_CONTROLLER);
        // The rack joins only through the rail in front of it, which runs on to the controller.
        for (BlockPos rail : List.of(RACK.north(), RACK_CONTROLLER.north())) {
            helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
            WarehouseFixtures.rail(helper, rail);
        }
        TerminalWiring.place(helper, RACK_TERMINAL);
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK, HeavyRackBlockEntity.class);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        rack.items().insertItem(1, new ItemStack(Items.DIRT, 32), false);
        rack.items().insertItem(2, new ItemStack(Items.SAND, 16), false);
        rack.items().insertItem(3, new ItemStack(Items.GRAVEL, 8), false);
        rack.items().insertItem(4, new ItemStack(Items.APPLE, 1), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RACK_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.COBBLESTONE) == 64 && TerminalWiring.stockOf(menu, Items.APPLE) == 1, "terminal lists the rack contents");
            List<ItemStack> sample = rack.displayed();
            helper.assertTrue(sample.size() == 4, "sample shows four types, had " + sample.size());
            helper.assertTrue(sample.get(0).is(Items.COBBLESTONE) && sample.get(3).is(Items.GRAVEL), "sample is ordered by count");

            rack.items().insertItem(5, new ItemStack(Items.APPLE, 64), false);
            rack.items().insertItem(6, new ItemStack(Items.APPLE, 36), false);
            helper.runAfterDelay(10, () -> {
                List<ItemStack> after = rack.displayed();
                helper.assertTrue(after.get(0).is(Items.APPLE), "apples lead the sample after the refill");
                helper.assertTrue(after.stream().noneMatch(stack -> stack.is(Items.GRAVEL)), "gravel dropped out of the sample");
                helper.succeed();
            });
        });
    }

    /** A rail at the side of a rack does not link it; a rail at its front does. */
    @ServusGameTest(timeoutTicks = 200)
    public static void rackLinksRailOnlyFromFront(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, FR_RACK, Direction.NORTH);
        helper.setBlock(FR_SIDE_RAIL.below(), Blocks.STONE.defaultBlockState());
        WarehouseFixtures.rail(helper, FR_SIDE_RAIL);
        WarehouseFixtures.controller(helper, FR_SIDE_CONTROLLER);
        TerminalWiring.place(helper, FR_SIDE_TERMINAL);
        HeavyRackBlockEntity rack = helper.getBlockEntity(FR_RACK, HeavyRackBlockEntity.class);
        rack.items().insertItem(0, new ItemStack(Items.SAND, 20), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity sideTerminal = TerminalWiring.at(helper, FR_SIDE_TERMINAL);
            StorageTerminalMenu sideMenu = new StorageTerminalMenu(1, player.getInventory(), sideTerminal);
            helper.assertTrue(sideMenu.stock().online(), "side warehouse is online");
            helper.assertTrue(TerminalWiring.stockOf(sideMenu, Items.SAND) == 0, "a rail at the rack's side must not link it");
            helper.assertTrue(!helper.getBlockState(FR_SIDE_RAIL).getValue(RailBlock.DOCK_WEST), "no dock onto the rack's side");

            helper.setBlock(FR_FRONT_RAIL.below(), Blocks.STONE.defaultBlockState());
            WarehouseFixtures.rail(helper, FR_FRONT_RAIL);
            WarehouseFixtures.controller(helper, FR_FRONT_CONTROLLER);
            TerminalWiring.place(helper, FR_FRONT_TERMINAL);
            helper.runAfterDelay(10, () -> {
                StorageTerminalBlockEntity frontTerminal = TerminalWiring.at(helper, FR_FRONT_TERMINAL);
                StorageTerminalMenu frontMenu = new StorageTerminalMenu(1, player.getInventory(), frontTerminal);
                helper.assertTrue(frontMenu.stock().online(), "front warehouse is online");
                helper.assertTrue(TerminalWiring.stockOf(frontMenu, Items.SAND) == 20, "a rail at the rack's front links it");
                helper.assertTrue(helper.getBlockState(FR_FRONT_RAIL).getValue(RailBlock.DOCK_SOUTH), "dock onto the rack's front");
                helper.succeed();
            });
        });
    }

    /** Places a rack through its item with the mock player looking south, so the rack faces north. */
    private static void placeRackByHand(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    /** A chest touching the controller is no stock and no put-away target. */
    @ServusGameTest(timeoutTicks = 100)
    public static void chestBesideControllerIsNotStock(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(3, 1, 5), false);
        BlockPos chestPos = warehouse.controllerPos().east();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        helper.getBlockEntity(chestPos, ChestBlockEntity.class).setItem(0, new ItemStack(Items.BREAD, 30));
        helper.runAfterDelay(20, () -> {
            WarehouseNetwork network = warehouse.network();
            helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 0, "bread beside the controller is not stock");
            helper.assertTrue(network.sourceCount() == 1, "only the rack is a source, got " + network.sourceCount());
            helper.assertTrue(network.putAwayTarget(new ItemStack(Items.BREAD, 5), pos -> true).filter(pos -> pos.equals(helper.absolutePos(chestPos))).isEmpty(),
                    "the chest is no put-away target");
            helper.succeed();
        });
    }

    /** A chest touching a rack is no stock either. */
    @ServusGameTest(timeoutTicks = 100)
    public static void chestBesideRackIsNotStock(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(3, 1, 5), false);
        BlockPos chestPos = warehouse.rackPos().west();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        helper.getBlockEntity(chestPos, ChestBlockEntity.class).setItem(0, new ItemStack(Items.BREAD, 30));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(warehouse.network().index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 0, "bread beside the rack is not stock");
            helper.succeed();
        });
    }

    /**
     * A section of three racks facing south at x 2 to 4 (left bay at x 4), a
     * rail only in front of the left bay leading to a terminal, and a garage
     * with one shuttle on that rail.
     */
    private static Player sectionRailSetup(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(180.0F);
        for (int x = 2; x <= 4; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SR_Z));
        }
        List<BlockPos> rails = List.of(new BlockPos(4, 1, 6), new BlockPos(4, 1, 7), new BlockPos(4, 1, 8),
                new BlockPos(5, 1, 8), new BlockPos(6, 1, 8), new BlockPos(7, 1, 8), new BlockPos(8, 1, 8));
        for (BlockPos rail : rails) {
            helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
            helper.setBlock(rail, ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        WarehouseFixtures.controller(helper, SR_CONTROLLER);
        TerminalWiring.place(helper, SR_TERMINAL);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, SR_GARAGE, Direction.NORTH, 1);
        return player;
    }

    /** Bread in the right bay of a section is fetched through the rail in front of the left bay. */
    @ServusGameTest(timeoutTicks = 400)
    public static void rackSectionServedFromOneRail(GameTestHelper helper) {
        Player player = sectionRailSetup(helper);
        helper.assertTrue(HeavyRackBlock.section(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, SR_Z))).size() == 3,
                "the three racks form one section");
        HeavyRackBlockEntity right = helper.getBlockEntity(new BlockPos(2, 1, SR_Z), HeavyRackBlockEntity.class);
        right.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SR_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.BREAD) == 30, "the terminal lists the right bay's bread");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through, got " + menu.stock().message());
            // Shuttles speed up and brake on every leg.
            helper.runAfterDelay(220, () -> {
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 30, "the shuttle should have brought 30 bread, output has " + terminal.outputCount(Items.BREAD));
                helper.assertTrue(right.isEmpty(), "the right bay should be empty");
                helper.succeed();
            });
        });
    }

    /** A lone rack beside a section with a front rail, but without one of its own, is not part of the warehouse. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackBesideSectionNotServed(GameTestHelper helper) {
        Player player = sectionRailSetup(helper);
        BlockPos lonePos = new BlockPos(1, 1, SR_Z);
        placeRackByHand(helper, player, lonePos);
        helper.assertTrue(HeavyRackBlock.section(helper.getLevel(), helper.absolutePos(lonePos)).size() == 1,
                "the fourth rack stands alone");
        HeavyRackBlockEntity lone = helper.getBlockEntity(lonePos, HeavyRackBlockEntity.class);
        lone.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SR_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.BREAD) == 0, "the lone rack's bread is not listed");
            helper.succeed();
        });
    }

    /** A section joins through the rail in front of one bay and leaves when that rail is broken. */
    @ServusGameTest(timeoutTicks = 100)
    public static void frontRailConnectsSection(GameTestHelper helper) {
        Player player = sectionRailSetup(helper);
        HeavyRackBlockEntity left = helper.getBlockEntity(new BlockPos(2, 1, SR_Z), HeavyRackBlockEntity.class);
        left.items().insertItem(0, new ItemStack(Items.BREAD, 12), false);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SR_TERMINAL);
            helper.assertTrue(TerminalWiring.stockOf(new StorageTerminalMenu(1, player.getInventory(), terminal), Items.BREAD) == 12,
                    "the far bay's bread is listed through the one front rail");
            helper.setBlock(new BlockPos(4, 1, 6), Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(TerminalWiring.stockOf(new StorageTerminalMenu(2, player.getInventory(), terminal), Items.BREAD) == 0,
                        "breaking the front rail takes the section out");
                helper.succeed();
            });
        });
    }

    /** A rack touching the controller without a front rail is not part of the warehouse. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackTouchingControllerNotListed(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        WarehouseFixtures.controller(helper, RACK_CONTROLLER);
        TerminalWiring.place(helper, RACK_TERMINAL);
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK, HeavyRackBlockEntity.class);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 9), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), TerminalWiring.at(helper, RACK_TERMINAL));
            helper.assertTrue(menu.stock().online(), "online");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.BREAD) == 0, "a rack without a front rail is not listed");
            helper.succeed();
        });
    }

    /** A chest touching only the back of a connected rack is not listed. */
    @ServusGameTest(timeoutTicks = 100)
    public static void chestBehindRackNotLinked(GameTestHelper helper) {
        Player player = sectionRailSetup(helper);
        BlockPos chestPos = new BlockPos(3, 1, SR_Z - 1);
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        helper.getBlockEntity(chestPos, ChestBlockEntity.class).setItem(0, new ItemStack(Items.APPLE, 7));
        helper.runAfterDelay(10, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), TerminalWiring.at(helper, SR_TERMINAL));
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.APPLE) == 0, "the chest behind the rack is not listed");
            helper.succeed();
        });
    }

    /** A rack touching the controller with a member rail only along its side is not part of the warehouse. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackSideRailDoesNotReach(GameTestHelper helper) {
        for (int x = 2; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 4), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        WarehouseFixtures.controller(helper, SS_CONTROLLER);
        TerminalWiring.place(helper, SS_TERMINAL);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, SS_GARAGE, Direction.NORTH, 1);
        // Faces west onto the controller; the rail at z 4 runs along its south side.
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, SS_RACK, Direction.WEST);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SS_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.BREAD) == 0, "a side rail must not join the rack");
            helper.succeed();
        });
    }

    /** The controller's stock provider counts the racks but gives nothing, and it has no stock sink: items move only by shuttle. */
    @ServusGameTest(timeoutTicks = 100)
    public static void controllerProviderGivesNothing(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(4, 1, 7), false).stock(new ItemStack(Items.COBBLESTONE, 80));
        helper.runAfterDelay(10, () -> {
            BlockPos absolute = helper.absolutePos(warehouse.controllerPos());
            StockProvider provider = helper.getLevel().getCapability(ServusFoundationCapabilities.STOCK_PROVIDER, absolute, Direction.UP);
            helper.assertTrue(provider != null, "the controller should expose its stock provider");
            helper.assertTrue(provider.count(new ItemStack(Items.COBBLESTONE)) == 80, "count should be 80, was " + provider.count(new ItemStack(Items.COBBLESTONE)));
            ItemStack taken = provider.extract(new ItemStack(Items.COBBLESTONE), 20);
            helper.assertTrue(taken.isEmpty() && warehouse.rackCount(Items.COBBLESTONE) == 80, "extract gives nothing and the rack keeps 80");
            helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.STOCK_SINK, absolute, null) == null, "the controller has no stock sink");
            helper.succeed();
        });
    }

    /** The op command takes straight out of the racks. */
    @ServusGameTest(timeoutTicks = 60)
    public static void takeCommandReadsRacks(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(3, 1, 4), false).stock(new ItemStack(Items.COBBLESTONE, 30));
        helper.runAfterDelay(10, () -> {
            BlockPos at = helper.absolutePos(warehouse.controllerPos());
            var source = helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel())
                    .withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER).withSuppressedOutput();
            helper.getLevel().getServer().getCommands().performPrefixedCommand(source,
                    "servus_warehouse take " + at.getX() + " " + at.getY() + " " + at.getZ() + " minecraft:cobblestone 12");
            helper.assertTrue(warehouse.rackCount(Items.COBBLESTONE) == 18, "the command takes 12 from the rack, it holds " + warehouse.rackCount(Items.COBBLESTONE));
            helper.succeed();
        });
    }
}
