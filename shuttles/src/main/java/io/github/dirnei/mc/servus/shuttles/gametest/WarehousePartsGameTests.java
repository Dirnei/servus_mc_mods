package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** A rack or a Warehouse Rail in hand places against a rack instead of opening its GUI; other blocks still open theirs. */
public final class WarehousePartsGameTests {

    private static final BlockPos BLOCK = new BlockPos(3, 1, 5);

    private WarehousePartsGameTests() {
    }

    /** A rack in hand on the front of a rack places a rack in front of it, facing the same way, and opens no menu. */
    @ServusGameTest
    public static void rackOnARackFrontPlaces(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        WarehouseFixtures.rack(helper, BLOCK, Direction.NORTH);
        ServerPlayer player = PlayerClicks.player(helper, BLOCK, new ItemStack(ServusShuttlesItems.HEAVY_RACK.get()));
        PlayerClicks.use(helper, player, BLOCK);
        helper.assertBlockProperty(BLOCK.north(), HeavyRackBlock.FACING, Direction.NORTH);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "no menu opens");
        helper.succeed();
    }

    /** A Warehouse Rail in hand on a rack front places the rail against it and opens no menu. */
    @ServusGameTest
    public static void railOnARackFrontPlaces(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        WarehouseFixtures.rack(helper, BLOCK, Direction.NORTH);
        ServerPlayer player = PlayerClicks.player(helper, BLOCK, new ItemStack(ServusShuttlesItems.RAIL.get()));
        PlayerClicks.use(helper, player, BLOCK);
        helper.assertBlockPresent(ServusShuttlesBlocks.RAIL.get(), BLOCK.north());
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "no menu opens");
        helper.succeed();
    }

    /** A Warehouse Rail in hand on an Inbound Station opens the station's menu and places nothing. */
    @ServusGameTest
    public static void railOnAStationOpensIt(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        WarehouseFixtures.inbound(helper, BLOCK, Direction.EAST);
        ServerPlayer player = PlayerClicks.player(helper, BLOCK, new ItemStack(ServusShuttlesItems.RAIL.get()));
        PlayerClicks.use(helper, player, BLOCK);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.RAIL.get(), BLOCK.north());
        helper.assertTrue(player.containerMenu != player.inventoryMenu, "the station menu opens");
        helper.succeed();
    }

    /** Cobblestone on the front of a rack opens the rack's menu as before. */
    @ServusGameTest
    public static void cobblestoneOnARackFrontOpensIt(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        WarehouseFixtures.rack(helper, BLOCK, Direction.NORTH);
        ServerPlayer player = PlayerClicks.player(helper, BLOCK, new ItemStack(Items.COBBLESTONE));
        PlayerClicks.use(helper, player, BLOCK);
        helper.assertTrue(player.containerMenu instanceof HeavyRackMenu, "the rack menu opens, got " + player.containerMenu);
        helper.assertBlockNotPresent(Blocks.COBBLESTONE, BLOCK.north());
        helper.succeed();
    }
}
