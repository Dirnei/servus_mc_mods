package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldActions;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBinding;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldInfoPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldModePayload;
import io.github.dirnei.mc.servus.shuttles.handheld.SweepTracker;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The handheld: ZONE scans of whole shelves, sweeps, the rack GUI staying shut, binding, FIND and INFO per shelf, softkeys. */
public final class HandheldGameTests {

    private static final BlockPos RACK = new BlockPos(3, 1, 5);
    private static final BlockPos[] SHELF = {new BlockPos(2, 1, 5), new BlockPos(3, 1, 5), new BlockPos(4, 1, 5)};
    private static final BlockPos CONTROLLER = new BlockPos(6, 1, 10);
    private static final BlockPos BREAD_A = new BlockPos(4, 1, 10);
    private static final BlockPos BREAD_B = new BlockPos(5, 1, 10);
    private static final BlockPos BREAD_C = new BlockPos(7, 1, 10);
    private static final BlockPos COBBLE = new BlockPos(8, 1, 10);

    private HandheldGameTests() {
    }

    private static HeavyRackBlockEntity rack(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, HeavyRackBlockEntity.class);
    }

    /** Heavy Duty Racks facing north joined into one shelf, from west to east. */
    private static void joinedShelf(GameTestHelper helper, BlockPos... bays) {
        BlockState base = ServusShuttlesBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH);
        for (int i = 0; i < bays.length; i++) {
            helper.setBlock(bays[i], base.setValue(HeavyRackBlock.LEFT, i > 0).setValue(HeavyRackBlock.RIGHT, i < bays.length - 1));
        }
        helper.assertTrue(HeavyRackBlock.shelf(helper.getLevel(), helper.absolutePos(bays[0])).size() == bays.length,
                "the bays form one shelf of " + bays.length);
    }

    /** A scan puts a shelf from one zone into the active one, and a scan with No zone clears it. */
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

    /** A right click at a shelf beyond the normal reach but within the scan range zones it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void scanReachesBeyondTheNormalReach(GameTestHelper helper) {
        scanFromAfar(helper, "a scan from 8 blocks");
    }

    /** A rail and an elevator shaft between the player and the shelf do not stop the scan. */
    @ServusGameTest(timeoutTicks = 20)
    public static void scanSeesThroughRailsAndElevators(GameTestHelper helper) {
        helper.setBlock(RACK.east(1), ServusShuttlesBlocks.RAIL.get());
        helper.setBlock(RACK.east(3), ServusShuttlesBlocks.ELEVATOR.get());
        scanFromAfar(helper, "a scan through a rail and an elevator");
    }

    /** Zones the rack at {@link #RACK} with a right click into the air from 8 blocks east of it. */
    private static void scanFromAfar(GameTestHelper helper, String what) {
        HeavyRackBlockEntity bay = WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        int ores = ZoneFixtures.zone(helper, "Ores", ZoneFilter.EMPTY);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(RACK.east(8)));
        player.setPos(stand.x, stand.y, stand.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(helper.absolutePos(RACK)));
        player.setItemInHand(InteractionHand.MAIN_HAND, ZoneFixtures.handheld(ores));
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(bay.zoneId() == ores, what + " zones the shelf, got '" + bay.zone() + "'");
        helper.succeed();
    }

    /** A scan of a shelf already in the active zone changes nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void scanOfZonedShelfChangesNothing(GameTestHelper helper) {
        joinedShelf(helper, SHELF);
        int ores = ZoneFixtures.zone(helper, "Ores", ZoneFilter.EMPTY);
        for (BlockPos pos : SHELF) {
            rack(helper, pos).setZone(ores);
        }
        InteractionResult result = GameTestSteps.useOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), ZoneFixtures.handheld(ores),
                SHELF[0], Direction.NORTH);
        helper.assertTrue(result.consumesAction(), "the click is used");
        for (BlockPos pos : SHELF) {
            helper.assertTrue(rack(helper, pos).zoneId() == ores, "every bay stays in Ores, " + pos + " is not");
        }
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

    /** Scanning the right bay of a shelf of three puts all three into the zone. */
    @ServusGameTest(timeoutTicks = 20)
    public static void scanZonesTheWholeShelf(GameTestHelper helper) {
        scanShelf(helper, false);
    }

    /** Sneaking while scanning gives the same result as without. */
    @ServusGameTest(timeoutTicks = 20)
    public static void sneakingScanZonesTheSameShelf(GameTestHelper helper) {
        scanShelf(helper, true);
    }

    private static void scanShelf(GameTestHelper helper, boolean sneaking) {
        joinedShelf(helper, SHELF);
        int food = ZoneFixtures.zone(helper, "Food", ZoneFilter.EMPTY);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(sneaking);
        GameTestSteps.useOn(helper, player, ZoneFixtures.handheld(food), SHELF[2], Direction.NORTH);
        for (BlockPos pos : SHELF) {
            helper.assertTrue(rack(helper, pos).zoneId() == food, "every bay is in Food, " + pos + " is not");
        }
        helper.succeed();
    }

    /** A held sweep over two bays of one shelf and one bay of the next assigns two shelves, the second with index 1. */
    @ServusGameTest(timeoutTicks = 20)
    public static void sweepCountsShelves(GameTestHelper helper) {
        BlockPos[] first = {new BlockPos(2, 1, 5), new BlockPos(3, 1, 5)};
        BlockPos second = new BlockPos(6, 1, 5);
        joinedShelf(helper, first);
        WarehouseFixtures.rack(helper, second, Direction.NORTH);
        int bulk = ZoneFixtures.zone(helper, "Bulk", ZoneFilter.EMPTY);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack handheld = ZoneFixtures.handheld(bulk);
        int a = HandheldActions.scan(player, handheld, rack(helper, first[0]));
        int b = HandheldActions.scan(player, handheld, rack(helper, first[1]));
        int c = HandheldActions.scan(player, handheld, rack(helper, second));
        helper.assertTrue(a == 0 && b == SweepTracker.SKIP && c == 1, "the sweep counts 0, skip, 1, got " + a + ", " + b + ", " + c);
        for (BlockPos pos : List.of(first[0], first[1], second)) {
            helper.assertTrue(rack(helper, pos).zoneId() == bulk, "every bay is in Bulk, " + pos + " is not");
        }
        helper.succeed();
    }

    /** A ZONE scan of one Fluid Rack bay zones its whole shelf. */
    @ServusGameTest(timeoutTicks = 40)
    public static void fluidRackScanZonesItsShelf(GameTestHelper helper) {
        BlockPos[] bays = {new BlockPos(2, 1, 5), new BlockPos(3, 1, 5), new BlockPos(4, 1, 5)};
        FluidRackFixtures.row(helper, Direction.NORTH, bays);
        helper.runAfterDelay(2, () -> {
            int cooling = ZoneFixtures.zone(helper, "Cooling", ZoneFilter.EMPTY);
            GameTestSteps.useOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), ZoneFixtures.handheld(cooling), bays[0], Direction.NORTH);
            for (BlockPos pos : bays) {
                helper.assertTrue(FluidRackFixtures.bay(helper, pos).zoneId() == cooling, "every bay is in Cooling, " + pos + " is not");
            }
            helper.succeed();
        });
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

    /** A softkey's payload switches straight to its mode; a scroll payload steps on from the current one. */
    @ServusGameTest(timeoutTicks = 20)
    public static void modePayloadSetsTheMode(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.ZONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, handheld);
        HandheldActions.applyMode(player, HandheldModePayload.to(HandheldMode.INFO));
        helper.assertTrue(HandheldActions.mode(handheld) == HandheldMode.INFO, "the softkey sets INFO, got " + HandheldActions.mode(handheld));
        HandheldActions.applyMode(player, HandheldModePayload.step(1));
        helper.assertTrue(HandheldActions.mode(handheld) == HandheldMode.ZONE, "a scroll from INFO wraps to ZONE, got " + HandheldActions.mode(handheld));
        helper.succeed();
    }

    private static void warehouse(GameTestHelper helper) {
        for (int x = 4; x <= 8; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 9));
        }
        WarehouseFixtures.controller(helper, CONTROLLER);
        joinedShelf(helper, BREAD_A, BREAD_B);
        for (BlockPos pos : List.of(BREAD_C, COBBLE)) {
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

    /** FIND lists each loaded shelf holding the item once, with its code, and the warehouse's total. */
    @ServusGameTest(timeoutTicks = 100)
    public static void findListsHoldingShelves(GameTestHelper helper) {
        warehouse(helper);
        WarehouseFixtures.stock(helper, BREAD_A, new ItemStack(Items.BREAD, 10));
        WarehouseFixtures.stock(helper, BREAD_B, new ItemStack(Items.BREAD, 5));
        WarehouseFixtures.stock(helper, BREAD_C, new ItemStack(Items.BREAD, 3));
        WarehouseFixtures.stock(helper, COBBLE, new ItemStack(Items.COBBLESTONE, 64));
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        ItemKey bread = ItemKey.of(new ItemStack(Items.BREAD));
        Waits.when(helper, () -> controller.network().map(network -> network.index().count(bread) == 18).orElse(false), 60,
                () -> "the warehouse indexes the bread", () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            HandheldActions.Found found = HandheldActions.found(network, bread);
            helper.assertTrue(found.total() == 18, "the total is 18, got " + found.total());
            helper.assertTrue(found.shelves().size() == 2, "two shelves hold bread, got " + found.shelves());
            helper.assertTrue(found.codes().size() == 2 && !found.codes().get(0).equals(found.codes().get(1)),
                    "each shelf comes with its own code, got " + found.codes());
            helper.succeed();
        });
    }

    /** An INFO scan beeps on the shelf, keeps the bay as the last scanned one and reports the shelf over all its bays. */
    @ServusGameTest(timeoutTicks = 20)
    public static void infoScanReportsTheShelf(GameTestHelper helper) {
        joinedShelf(helper, SHELF);
        int ores = ZoneFixtures.zone(helper, "Ores", ZoneFilter.EMPTY);
        for (BlockPos pos : SHELF) {
            rack(helper, pos).setZone(ores);
        }
        ItemStack raw = new ItemStack(Items.RAW_IRON, 64);
        WarehouseFixtures.stock(helper, SHELF[0], raw.copy(), raw.copy(), raw.copy());
        WarehouseFixtures.stock(helper, SHELF[1], raw.copy(), raw.copy(), new ItemStack(Items.COBBLESTONE, 5));
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.INFO);
        GameTestSteps.useOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), handheld, SHELF[2], Direction.NORTH);
        helper.assertTrue(helper.absolutePos(SHELF[2]).equals(handheld.get(ServusShuttlesComponents.HANDHELD_PIN.get())),
                "the scanned bay is kept as the last scanned one");
        HandheldInfoPayload info = HandheldActions.info(helper.getLevel(), helper.absolutePos(SHELF[2]));
        int slots = rack(helper, SHELF[0]).items().getSlots() * SHELF.length;
        helper.assertTrue(info != null && info.zoneId() == ores, "the answer names the zone");
        helper.assertTrue(info.used() == 6 && info.total() == slots, "6/" + slots + " slots in use, got " + info.used() + "/" + info.total());
        helper.assertTrue(info.top().get(0).is(Items.RAW_IRON) && info.counts().get(0) == 320, "raw iron 320 on top, got " + info.counts());
        helper.assertTrue(info.top().size() == 2 && info.top().get(1).is(Items.COBBLESTONE), "cobblestone follows, got " + info.top());
        helper.assertTrue(WarehouseFixtures.count(rack(helper, SHELF[0]), Items.RAW_IRON) == 192, "the readout changes nothing");
        helper.succeed();
    }

    /** INFO on a Fluid Rack bay reports the fluid of its whole shelf against the shelf's capacity. */
    @ServusGameTest(timeoutTicks = 40)
    public static void infoReportsAFluidRackShelf(GameTestHelper helper) {
        BlockPos[] bays = {new BlockPos(2, 1, 5), new BlockPos(3, 1, 5), new BlockPos(4, 1, 5)};
        FluidRackFixtures.row(helper, Direction.NORTH, bays);
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, bays[1], Fluids.WATER, 30000);
            FluidRackBlockEntity bay = FluidRackFixtures.bay(helper, bays[0]);
            HandheldInfoPayload info = HandheldActions.info(helper.getLevel(), helper.absolutePos(bays[0]));
            long capacity = bay.shelfHandler().content().capacity();
            helper.assertTrue(info != null && info.fluid(), "the answer is a fluid readout");
            helper.assertTrue(info.used() == 30000 && info.total() == capacity, "30000/" + capacity + " mB, got " + info.used() + "/" + info.total());
            helper.assertTrue(info.top().size() == 1 && info.top().get(0).is(Items.WATER_BUCKET), "water is listed, got " + info.top());
            helper.succeed();
        });
    }
}
