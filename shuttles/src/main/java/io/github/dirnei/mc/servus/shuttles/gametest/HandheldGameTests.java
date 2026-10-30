package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldActions;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBinding;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldInfoPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The handheld: ZONE scans, the rack GUI staying shut, binding, FIND and INFO on the server. */
public final class HandheldGameTests {

    private static final BlockPos RACK = new BlockPos(3, 1, 5);
    private static final BlockPos CONTROLLER = new BlockPos(6, 1, 10);
    private static final BlockPos BREAD_A = new BlockPos(4, 1, 10);
    private static final BlockPos BREAD_B = new BlockPos(5, 1, 10);
    private static final BlockPos COBBLE = new BlockPos(7, 1, 10);

    private HandheldGameTests() {
    }

    private static HeavyRackBlockEntity rack(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, HeavyRackBlockEntity.class);
    }

    /** A scan puts a bay from one zone into the active one, and a scan with No zone clears it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void scanAssignsAndClears(GameTestHelper helper) {
        HeavyRackBlockEntity bay = WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        bay.setZone(ZoneFixtures.zone(helper, "Food", ZoneFilter.EMPTY));
        int ores = ZoneFixtures.zone(helper, "Ores", ZoneFilter.EMPTY);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GameTestSteps.useOn(helper, player, ZoneFixtures.handheld(ores), RACK, Direction.NORTH);
        helper.assertTrue(bay.zoneId() == ores && bay.zone().equals("Ores"), "the bay is in Ores, got '" + bay.zone() + "'");
        GameTestSteps.useOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), ZoneFixtures.handheld(ZoneList.NONE), RACK, Direction.NORTH);
        helper.assertTrue(bay.zoneDefinition() == null, "No zone clears the bay, got '" + bay.zone() + "'");
        helper.succeed();
    }

    /** A scan of a bay already in the active zone changes nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void scanOfZonedBayChangesNothing(GameTestHelper helper) {
        HeavyRackBlockEntity bay = WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        int ores = ZoneFixtures.zone(helper, "Ores", ZoneFilter.EMPTY);
        bay.setZone(ores);
        InteractionResult result = GameTestSteps.useOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), ZoneFixtures.handheld(ores),
                RACK, Direction.NORTH);
        helper.assertTrue(result.consumesAction() && bay.zoneId() == ores, "the bay stays in Ores and the click is used");
        helper.succeed();
    }

    /** A handheld with no zone chosen leaves the bay as it is. */
    @ServusGameTest(timeoutTicks = 20)
    public static void noActiveZoneChangesNothing(GameTestHelper helper) {
        HeavyRackBlockEntity bay = WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        int food = ZoneFixtures.zone(helper, "Food", ZoneFilter.EMPTY);
        bay.setZone(food);
        GameTestSteps.useOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), ZoneFixtures.handheld(HandheldMode.ZONE), RACK, Direction.NORTH);
        helper.assertTrue(bay.zoneId() == food, "the bay stays in Food");
        helper.succeed();
    }

    /** Sneaking takes every bay of the shelf at once. */
    @ServusGameTest(timeoutTicks = 20)
    public static void sneakScanTakesShelf(GameTestHelper helper) {
        BlockPos[] bays = {new BlockPos(2, 1, 5), new BlockPos(3, 1, 5), new BlockPos(4, 1, 5)};
        BlockState base = ServusShuttlesBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH);
        helper.setBlock(bays[0], base.setValue(HeavyRackBlock.RIGHT, true));
        helper.setBlock(bays[1], base.setValue(HeavyRackBlock.LEFT, true).setValue(HeavyRackBlock.RIGHT, true));
        helper.setBlock(bays[2], base.setValue(HeavyRackBlock.LEFT, true));
        helper.assertTrue(HeavyRackBlock.shelf(helper.getLevel(), helper.absolutePos(bays[1])).size() == 3, "the three bays are one shelf");
        int food = ZoneFixtures.zone(helper, "Food", ZoneFilter.EMPTY);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        GameTestSteps.useOn(helper, player, ZoneFixtures.handheld(food), bays[1], Direction.NORTH);
        for (BlockPos pos : bays) {
            helper.assertTrue(rack(helper, pos).zoneId() == food, "every bay is in Food, " + pos + " is not");
        }
        helper.succeed();
    }

    /** The handheld on a rack front never opens the rack's GUI: the rack passes the click to the item. */
    @ServusGameTest(timeoutTicks = 20)
    public static void handheldOpensNoRackMenu(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.INFO);
        player.setItemInHand(InteractionHand.MAIN_HAND, handheld);
        BlockPos absolute = helper.absolutePos(RACK);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false);
        InteractionResult result = helper.getBlockState(RACK).useItemOn(handheld, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(result == InteractionResult.PASS, "the rack passes the handheld on, got " + result);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "no rack menu is open");
        helper.succeed();
    }

    private static void warehouse(GameTestHelper helper) {
        for (int x = 4; x <= 8; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 9));
        }
        WarehouseFixtures.controller(helper, CONTROLLER);
        for (BlockPos pos : List.of(BREAD_A, BREAD_B, COBBLE)) {
            WarehouseFixtures.rack(helper, pos, Direction.NORTH);
        }
    }

    /** Scanning a controller binds the handheld to it; once the controller is broken the handheld reads as not bound. */
    @ServusGameTest(timeoutTicks = 60)
    public static void bindAndLoseController(GameTestHelper helper) {
        warehouse(helper);
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.FIND);
        GameTestSteps.useOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), handheld, CONTROLLER, Direction.NORTH);
        HandheldBinding binding = handheld.get(ServusShuttlesComponents.HANDHELD_BINDING.get());
        helper.assertTrue(binding != null && binding.controller().pos().equals(helper.absolutePos(CONTROLLER)),
                "the handheld is bound to the controller, got " + binding);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(HandheldActions.boundNetwork(helper.getLevel().getServer(), handheld).isPresent(), "the bound warehouse is found");
            helper.setBlock(CONTROLLER, Blocks.AIR.defaultBlockState());
            helper.assertTrue(HandheldActions.boundNetwork(helper.getLevel().getServer(), handheld).isEmpty(), "a broken controller is not bound");
            helper.assertTrue(!handheld.has(ServusShuttlesComponents.HANDHELD_BINDING.get()), "the stale binding is dropped");
            helper.succeed();
        });
    }

    /** FIND lists every loaded bay of the warehouse holding the item and the warehouse's total. */
    @ServusGameTest(timeoutTicks = 100)
    public static void findListsHoldingBays(GameTestHelper helper) {
        warehouse(helper);
        WarehouseFixtures.stock(helper, BREAD_A, new ItemStack(Items.BREAD, 10));
        WarehouseFixtures.stock(helper, BREAD_B, new ItemStack(Items.BREAD, 5));
        WarehouseFixtures.stock(helper, COBBLE, new ItemStack(Items.COBBLESTONE, 64));
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        ItemKey bread = ItemKey.of(new ItemStack(Items.BREAD));
        Waits.when(helper, () -> controller.network().map(network -> network.index().count(bread) == 15).orElse(false), 60,
                () -> "the warehouse indexes the bread", () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            HandheldActions.Found found = HandheldActions.found(network, bread);
            helper.assertTrue(found.total() == 15, "the total is 15, got " + found.total());
            helper.assertTrue(Set.copyOf(found.bays()).equals(Set.of(helper.absolutePos(BREAD_A), helper.absolutePos(BREAD_B))),
                    "both bread bays are listed, got " + found.bays());
            helper.assertTrue(found.codes().size() == 2, "each bay comes with its code");
            helper.succeed();
        });
    }

    /** INFO reports a bay's zone, slots in use and its most held item, and changes nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void infoReportsBay(GameTestHelper helper) {
        HeavyRackBlockEntity bay = WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        int ores = ZoneFixtures.zone(helper, "Ores", ZoneFilter.EMPTY);
        bay.setZone(ores);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.RAW_IRON, 64), new ItemStack(Items.RAW_IRON, 64),
                new ItemStack(Items.RAW_IRON, 64), new ItemStack(Items.RAW_IRON, 64), new ItemStack(Items.RAW_IRON, 64));
        HandheldInfoPayload info = HandheldActions.info(helper.getLevel(), helper.absolutePos(RACK));
        helper.assertTrue(info != null && info.zoneId() == ores, "the answer names the zone");
        helper.assertTrue(info.used() == 5 && info.total() == bay.items().getSlots(), "5 slots in use, got " + info.used() + "/" + info.total());
        helper.assertTrue(info.top().get(0).is(Items.RAW_IRON) && info.counts().get(0) == 320, "raw iron 320 on top, got " + info.counts());
        helper.assertTrue(WarehouseFixtures.count(bay, Items.RAW_IRON) == 320, "the readout changes nothing");
        helper.succeed();
    }
}
