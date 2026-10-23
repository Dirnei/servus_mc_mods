package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.ElevatorMenu;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlock;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestRow;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.ArrayList;
import java.util.List;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlockEntity;
import net.minecraft.resources.Identifier;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Elevator shafts: linking floors, the lift car, its upgrades and placing segments. */
public final class ElevatorGameTests {

    private static final BlockPos LIFT_RACK = new BlockPos(2, 1, 3);
    private static final BlockPos LIFT_RAIL_LOW = new BlockPos(3, 1, 3);
    private static final BlockPos LIFT_BOTTOM = new BlockPos(4, 1, 3);
    private static final BlockPos LIFT_MIDDLE = new BlockPos(4, 2, 3);
    private static final BlockPos LIFT_TOP = new BlockPos(4, 3, 3);
    private static final BlockPos LIFT_RAIL_HIGH = new BlockPos(5, 3, 3);
    private static final BlockPos SHAFT_STATION = new BlockPos(6, 3, 3);
    private static final BlockPos LIFT_CONTROLLER = new BlockPos(6, 3, 3);
    private static final BlockPos LIFT_TERMINAL = new BlockPos(7, 3, 3);
    private static final BlockPos LIFT_CHEST = new BlockPos(4, 2, 4);
    private static final BlockPos LIFT_SIDE_RACK = new BlockPos(4, 1, 4);
    private static final BlockPos NC_SEGMENT = new BlockPos(6, 1, 6);
    private static final BlockPos LC_BOTTOM = new BlockPos(4, 1, 3);
    private static final BlockPos LC_MIDDLE = new BlockPos(4, 2, 3);
    private static final BlockPos LC_TOP = new BlockPos(4, 3, 3);
    private static final BlockPos LC_GARAGE = new BlockPos(3, 1, 2);
    private static final BlockPos LC_CONTROLLER = new BlockPos(1, 1, 2);
    private static final BlockPos LC_TERMINAL = new BlockPos(2, 1, 4);

    private ElevatorGameTests() {
    }

    /** A three segment shaft links a ground floor rack to a controller two floors up; a chest on the shaft stays out. */
    @ServusGameTest(timeoutTicks = 200)
    public static void elevatorLinksFloors(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, LIFT_RACK, Direction.EAST);
        helper.setBlock(LIFT_RAIL_LOW.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_BOTTOM.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_RAIL_LOW, ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(LIFT_BOTTOM, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_MIDDLE, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_TOP, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_RAIL_HIGH.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_CONTROLLER.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_TERMINAL.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_RAIL_HIGH, ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        WarehouseFixtures.controller(helper, LIFT_CONTROLLER);
        // The Packing Station joins only through a rail: two rails south of the controller and the Packing Station.
        for (BlockPos rail : List.of(LIFT_CONTROLLER.south(), LIFT_TERMINAL.south())) {
            WarehouseFixtures.railOnStone(helper, rail);
        }
        TerminalWiring.place(helper, LIFT_TERMINAL);
        helper.setBlock(LIFT_CHEST, Blocks.CHEST.defaultBlockState());
        WarehouseFixtures.rack(helper, LIFT_SIDE_RACK, Direction.NORTH);
        WarehouseFixtures.stock(helper, LIFT_RACK, new ItemStack(Items.COBBLESTONE, 30));
        WarehouseFixtures.stock(helper, LIFT_SIDE_RACK, new ItemStack(Items.APPLE, 7));
        ChestBlockEntity chest = helper.getBlockEntity(LIFT_CHEST, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.BREAD, 10));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LIFT_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online through the shaft");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.COBBLESTONE) == 30, "stock should list 30 cobblestone from the ground floor rack");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.BREAD) == 0, "bread beside the shaft must not be indexed");
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.APPLE) == 0, "a rack facing only a segment must not join through it");

            helper.setBlock(LIFT_MIDDLE, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(10, () -> {
                StorageTerminalMenu after = new StorageTerminalMenu(1, player.getInventory(), terminal);
                helper.assertTrue(after.stock().online(), "controller side stays online");
                helper.assertTrue(TerminalWiring.stockOf(after, Items.COBBLESTONE) == 0, "rack stock should leave the index after the shaft is cut");
                helper.assertTrue(WarehouseFixtures.lit(helper, TerminalWiring.terminalPos(LIFT_TERMINAL)), "terminal lamp stays lit beside the controller");
                helper.succeed();
            });
        });
    }

    /** A controller touching only the side of a shaft brings a station two floors up online. */
    @ServusGameTest(timeoutTicks = 100)
    public static void controllerBesideShaftJoins(GameTestHelper helper) {
        shaftToStation(helper);
        helper.setBlock(LIFT_RAIL_LOW.below(), Blocks.STONE.defaultBlockState());
        WarehouseFixtures.controller(helper, LIFT_RAIL_LOW);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(WarehouseFixtures.lit(helper, SHAFT_STATION), "the station should be online through the shaft beside the controller");
            helper.succeed();
        });
    }

    /** A controller sitting on top of a shaft does not join it. */
    @ServusGameTest(timeoutTicks = 100)
    public static void controllerOnShaftTopStaysApart(GameTestHelper helper) {
        shaftToStation(helper);
        WarehouseFixtures.controller(helper, LIFT_TOP.above());
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(WarehouseFixtures.lit(helper, SHAFT_STATION), "a controller on the shaft top must not bring the station online");
            helper.succeed();
        });
    }

    /** A three segment shaft with a rail and an Inbound Station on its top floor. */
    private static void shaftToStation(GameTestHelper helper) {
        helper.setBlock(LIFT_BOTTOM.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_BOTTOM, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_MIDDLE, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_TOP, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        WarehouseFixtures.railOnStone(helper, LIFT_RAIL_HIGH);
        WarehouseFixtures.stone(helper, SHAFT_STATION.below());
        WarehouseFixtures.inbound(helper, SHAFT_STATION, Direction.EAST);
    }

    /** An entity above a segment stands on its top plate; one that descends or is inside passes. */
    @ServusGameTest(timeoutTicks = 20)
    public static void elevatorStandsFromAbove(GameTestHelper helper) {
        helper.setBlock(NC_SEGMENT.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(NC_SEGMENT, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        BlockState state = helper.getBlockState(NC_SEGMENT);
        BlockPos absolute = helper.absolutePos(NC_SEGMENT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        VoxelShape above = state.getCollisionShape(helper.getLevel(), absolute, CollisionContext.of(player));
        helper.assertTrue(!above.isEmpty() && above.max(Direction.Axis.Y) == 1.0, "an entity above should stand on the top plate");
        player.setShiftKeyDown(true);
        helper.assertTrue(state.getCollisionShape(helper.getLevel(), absolute, CollisionContext.of(player)).isEmpty(),
                "a sneaking player should sink through");
        player.setShiftKeyDown(false);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 0.2, absolute.getZ() + 0.5);
        helper.assertTrue(state.getCollisionShape(helper.getLevel(), absolute, CollisionContext.of(player)).isEmpty(),
                "an entity inside the segment should pass");
        helper.assertTrue(state.isScaffolding(player), "sneaking should not hold the player like a ladder");
        helper.succeed();
    }

    /** A segment has at most its top plate as collision, keeps a full outline to target it, and is climbable. */
    @ServusGameTest(timeoutTicks = 20)
    public static void elevatorHasNoCollision(GameTestHelper helper) {
        helper.setBlock(NC_SEGMENT.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(NC_SEGMENT, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        BlockState state = helper.getBlockState(NC_SEGMENT);
        BlockPos absolute = helper.absolutePos(NC_SEGMENT);
        VoxelShape plain = state.getCollisionShape(helper.getLevel(), absolute);
        helper.assertTrue(plain.min(Direction.Axis.Y) >= 14.0 / 16.0, "without an entity only the top plate collides, like scaffolding");
        helper.assertTrue(!state.getShape(helper.getLevel(), absolute).isEmpty(), "a segment keeps an outline");
        helper.assertTrue(state.is(BlockTags.CLIMBABLE), "a segment is climbable");
        helper.succeed();
    }

    /**
     * A shaft at x 4, z 3 from y 1 to 3, ground floor rails x 1 to 3, the garage with the given shuttles north
     * of x 3, the controller north of x 1 and the terminal south of x 2.
     */
    private static void liftBase(GameTestHelper helper, int shuttles) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(LC_BOTTOM, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LC_MIDDLE, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LC_TOP, ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        WarehouseFixtures.controller(helper, LC_CONTROLLER);
        TerminalWiring.place(helper, LC_TERMINAL);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, LC_GARAGE, Direction.NORTH, 0);
        for (int i = 0; i < shuttles; i++) {
            helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        }
    }

    /** A rail beside a shaft floor with a rack behind it facing the rail, both standing on stone. */
    private static HeavyRackBlockEntity liftRack(GameTestHelper helper, BlockPos rail, Item item, int count) {
        BlockPos rackPos = rail.south();
        helper.setBlock(rail.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(rackPos.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(rail, ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, rackPos, Direction.NORTH);
        rack.items().insertItem(0, new ItemStack(item, count), false);
        return rack;
    }

    private static WarehouseNetwork liftNetwork(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(LC_CONTROLLER, WarehouseControllerBlockEntity.class);
        return controller.network().orElseThrow();
    }

    /** A ground floor shuttle rides the car up to a rack two floors up and back down to the terminal. */
    @ServusGameTest(timeoutTicks = 400)
    public static void shuttleRidesCarToUpperRack(GameTestHelper helper) {
        liftBase(helper, 1);
        HeavyRackBlockEntity rack = liftRack(helper, new BlockPos(5, 3, 3), Items.BREAD, 20);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos bottom = helper.absolutePos(LC_BOTTOM);
        double[] highest = {Double.NEGATIVE_INFINITY};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(LC_CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().map(network -> network.lift(bottom)).ifPresent(car -> highest[0] = Math.max(highest[0], car.y()));
            }
        });
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = liftNetwork(helper);
            helper.assertTrue(network.lift(bottom) != null, "the shaft has a car");
            helper.assertTrue(network.lift(bottom).y() == bottom.getY(), "the car starts at the lowest segment");
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 20);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request goes through, got " + menu.stock().message());
            // Shuttles speed up and brake on every leg.
            helper.runAfterDelay(260, () -> {
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 20, "the shuttle should have brought 20 bread, output has " + terminal.outputCount(Items.BREAD));
                helper.assertTrue(rack.isEmpty(), "the rack should be empty");
                helper.assertTrue(highest[0] == bottom.getY() + 2, "the car went up to the top floor, highest was " + highest[0]);
                helper.assertTrue(network.lift(bottom).y() == bottom.getY(), "the car brought the shuttle back down");
                helper.assertTrue(network.garageOut(helper.absolutePos(LC_GARAGE)) == 0, "the shuttle is home");
                helper.succeed();
            });
        });
    }

    /**
     * A ride is paid per block of height when the car takes
     * the shuttle, together with the block into the shaft. Out: one through
     * the doorway, one into the shaft, two up, one out to the rack; back: one
     * in, two down, one out, one on to the Packing Station; home: one and one
     * through the doorway. Twelve blocks in all.
     */
    @ServusGameTest(timeoutTicks = 500)
    public static void liftRideCostsPerBlockOfHeight(GameTestHelper helper) {
        liftBase(helper, 1);
        liftRack(helper, new BlockPos(5, 3, 3), Items.BREAD, 20);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(LC_CONTROLLER, WarehouseControllerBlockEntity.class);
        long perBlock = (long) Math.ceil(ShuttlesStatsManager.INSTANCE.shuttleFePerBlock());
        long[] last = {-1};
        long[] biggest = {0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(LC_CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity c) {
                if (last[0] >= 0) {
                    biggest[0] = Math.max(biggest[0], last[0] - c.energyStored());
                }
                last[0] = c.energyStored();
            }
        });
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
            new StorageTerminalMenu(1, player.getInventory(), terminal).request(ItemKey.of(new ItemStack(Items.BREAD)), 20);
            Waits.when(helper, () -> terminal.outputCount(Items.BREAD) == 20
                            && liftNetwork(helper).garageOut(helper.absolutePos(LC_GARAGE)) == 0, 450,
                    () -> "the bread arrives and the shuttle parks, output " + terminal.outputCount(Items.BREAD), () -> {
                long paid = controller.energy().capacity() - controller.energyStored();
                helper.assertTrue(paid == 12 * perBlock, "twelve blocks cost " + 12 * perBlock + " FE, paid " + paid);
                helper.assertTrue(biggest[0] == 3 * perBlock, "the block into the shaft and the ride of two are paid at once, biggest tick "
                        + biggest[0]);
                helper.succeed();
            });
        });
    }

    /** Two shuttles call the car at the same floor: the first caller rides first, the second waits for the car. */
    @ServusGameTest(timeoutTicks = 700)
    public static void shuttlesQueueForCar(GameTestHelper helper) {
        liftBase(helper, 2);
        HeavyRackBlockEntity top = liftRack(helper, new BlockPos(3, 3, 3), Items.BREAD, 10);
        HeavyRackBlockEntity middle = liftRack(helper, new BlockPos(5, 2, 3), Items.APPLE, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 10);
            menu.request(ItemKey.of(new ItemStack(Items.APPLE)), 10);
            Waits.when(helper, top::isEmpty, 200, () -> "the first caller rides up and loads the top rack", () -> {
                helper.assertTrue(!middle.isEmpty(), "the second caller is still waiting for the car, although its floor is nearer");
                // The car speeds up and brakes on each of its trips.
                helper.runAfterDelay(420, () -> {
                    helper.assertTrue(terminal.outputCount(Items.BREAD) == 10 && terminal.outputCount(Items.APPLE) == 10,
                            "both loads arrive, output has " + terminal.outputCount(Items.BREAD) + " bread and " + terminal.outputCount(Items.APPLE) + " apples");
                    helper.succeed();
                });
            });
        });
    }

    /** The shaft's upgrade menu opened from the top segment works on the lowest one; only Speed Upgrades go in. */
    @ServusGameTest
    public static void liftUpgradesLiveInTheBottom(GameTestHelper helper) {
        liftBase(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ElevatorBlockEntity bottom = ElevatorBlock.bottomOf(helper.getLevel(), helper.absolutePos(LC_TOP));
        helper.assertTrue(bottom != null && bottom.getBlockPos().equals(helper.absolutePos(LC_BOTTOM)), "the top segment finds the lowest one");
        ElevatorMenu menu = new ElevatorMenu(1, player.getInventory(), bottom);
        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(ServusShuttlesItems.HANDLING_UPGRADE.get())), "a Handling Upgrade does not fit");
        ItemStack rest = menu.getSlot(0).safeInsert(new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get(), 3));
        helper.assertTrue(rest.getCount() == 2, "one Speed Upgrade goes in, two stay, left " + rest.getCount());
        ElevatorBlockEntity top = helper.getBlockEntity(LC_TOP, ElevatorBlockEntity.class);
        helper.assertTrue(bottom.upgradeCount() == 1 && top.upgradeCount() == 0, "the lowest segment keeps the upgrade");
        helper.succeed();
    }

    /** A segment placed under a shaft becomes its lowest one and takes the upgrades over. */
    @ServusGameTest
    public static void liftUpgradesMoveDown(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(2, 2, 2), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(new BlockPos(2, 3, 2), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        ElevatorBlockEntity old = helper.getBlockEntity(new BlockPos(2, 2, 2), ElevatorBlockEntity.class);
        old.upgrades().insertAll(List.of(new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get()), new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get())));
        helper.setBlock(new BlockPos(2, 1, 2), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        ElevatorBlockEntity lowest = helper.getBlockEntity(new BlockPos(2, 1, 2), ElevatorBlockEntity.class);
        helper.assertTrue(lowest.upgradeCount() == 2, "the new lowest segment holds both upgrades, has " + lowest.upgradeCount());
        helper.assertTrue(old.upgradeCount() == 0, "the old lowest segment holds none");
        helper.succeed();
    }

    /** A lowest segment replaced by a block the shaft stands on hands its upgrades to the segment above. */
    @ServusGameTest
    public static void liftUpgradesClimbWhenBottomIsReplaced(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE.defaultBlockState());
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(2, y, 2), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        }
        ElevatorBlockEntity bottom = helper.getBlockEntity(new BlockPos(2, 1, 2), ElevatorBlockEntity.class);
        bottom.upgrades().insertAll(List.of(new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get()), new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get())));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE.defaultBlockState());
        ElevatorBlockEntity next = helper.getBlockEntity(new BlockPos(2, 2, 2), ElevatorBlockEntity.class);
        helper.assertTrue(next.upgradeCount() == 2, "the segment above holds both upgrades, has " + next.upgradeCount());
        helper.assertItemEntityNotPresent(ServusFoundationItems.SPEED_UPGRADE.get(), new BlockPos(2, 1, 2), 4);
        helper.succeed();
    }

    /** Breaking the lowest segment brings the shaft down like scaffolding; the upgrades drop where it was. */
    @ServusGameTest
    public static void liftUpgradesDropWithTheBottom(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE.defaultBlockState());
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(2, y, 2), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        }
        ElevatorBlockEntity bottom = helper.getBlockEntity(new BlockPos(2, 1, 2), ElevatorBlockEntity.class);
        bottom.upgrades().insertAll(List.of(new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get()), new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get())));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(ServusShuttlesBlocks.ELEVATOR.get(), new BlockPos(2, 3, 2));
            helper.assertItemEntityCountIs(ServusFoundationItems.SPEED_UPGRADE.get(), new BlockPos(2, 1, 2), 1.5, 2);
            helper.succeed();
        });
    }

    /** Four Speed Upgrades make the car faster: it reaches the top floor before a plain car could. */
    @ServusGameTest(timeoutTicks = 200)
    public static void upgradedCarIsFaster(GameTestHelper helper) {
        liftBase(helper, 0);
        ElevatorBlockEntity bottomSegment = helper.getBlockEntity(LC_BOTTOM, ElevatorBlockEntity.class);
        List<ItemStack> four = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            four.add(new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get()));
        }
        helper.runAfterDelay(10, () -> {
            bottomSegment.upgrades().insertAll(four);
            BlockPos bottom = helper.absolutePos(LC_BOTTOM);
            helper.runAfterDelay(2, () -> {
                WarehouseNetwork network = liftNetwork(helper);
                helper.assertTrue(network.lift(bottom).upgrades() == 4, "the car knows its four upgrades, has " + network.lift(bottom).upgrades());
                network.lift(bottom).call(-1, bottom.getY() + 2);
                // A plain car (0.1 top, 0.005 accel) needs about 40 ticks for two floors; the upgraded one about 25.
                helper.runAfterDelay(28, () -> {
                    helper.assertTrue(network.lift(bottom).y() == bottom.getY() + 2,
                            "the upgraded car is at the top floor, at " + (network.lift(bottom).y() - bottom.getY()));
                    network.lift(bottom).remove(-1);
                    helper.succeed();
                });
            });
        });
    }

    /** A shuttle crosses the shaft on the ground floor deck while the car stays parked at the top. */
    @ServusGameTest(timeoutTicks = 300)
    public static void shuttleCrossesShaftWithoutCar(GameTestHelper helper) {
        liftBase(helper, 1);
        BlockPos crossRail = new BlockPos(5, 1, 3);
        BlockPos rackPos = new BlockPos(6, 1, 3);
        helper.setBlock(crossRail.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(crossRail, ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        WarehouseFixtures.rack(helper, rackPos, Direction.WEST);
        WarehouseFixtures.stock(helper, rackPos, new ItemStack(Items.COBBLESTONE, 16));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos bottom = helper.absolutePos(LC_BOTTOM);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = liftNetwork(helper);
            // Park the car at the top floor with a call nobody answers, then take the call back.
            network.lift(bottom).call(-1, bottom.getY() + 2);
            helper.runAfterDelay(40, () -> {
                network.lift(bottom).remove(-1);
                helper.assertTrue(network.lift(bottom).y() == bottom.getY() + 2, "the car is parked at the top");
                StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
                StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
                menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 16);
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "the rack behind the shaft is served by the shuttle");
                helper.runAfterDelay(160, () -> {
                    helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 16,
                            "the shuttle crossed both ways without waiting, output has " + terminal.outputCount(Items.COBBLESTONE));
                    helper.assertTrue(network.lift(bottom).y() == bottom.getY() + 2 && !network.lift(bottom).moving(), "the car did not move");
                    helper.succeed();
                });
            });
        });
    }

    /** Breaking the middle segment while the car carries a loaded shuttle drops the load at its garage. */
    @ServusGameTest(timeoutTicks = 400)
    public static void shaftBreakDropsRiderCargo(GameTestHelper helper) {
        liftBase(helper, 1);
        liftRack(helper, new BlockPos(5, 3, 3), Items.BREAD, 20);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos bottom = helper.absolutePos(LC_BOTTOM);
        BlockPos garagePos = helper.absolutePos(LC_GARAGE);
        long[] brokeAt = {-1};
        StorageTerminalMenu[] menu = {null};
        // One top level tick hook: scheduling from inside a scheduled task is not safe.
        helper.onEachTick(() -> {
            if (helper.getTick() == 10) {
                menu[0] = TerminalWiring.menu(helper, player, LC_TERMINAL);
                menu[0].request(ItemKey.of(new ItemStack(Items.BREAD)), 20);
            }
            if (menu[0] == null) {
                return;
            }
            WarehouseNetwork network = liftNetwork(helper);
            if (brokeAt[0] < 0) {
                var car = network.lift(bottom);
                boolean loaded = network.shuttleViews(garagePos).stream().anyMatch(view -> !view.cargo().isEmpty());
                if (car != null && car.carrying() && loaded && car.y() < bottom.getY() + 1.5) {
                    brokeAt[0] = helper.getTick();
                    helper.setBlock(LC_MIDDLE, Blocks.AIR.defaultBlockState());
                }
            } else if (helper.getTick() == brokeAt[0] + 5) {
                helper.assertItemEntityPresent(Items.BREAD, LC_GARAGE, 1.5);
            } else if (helper.getTick() == brokeAt[0] + 40) {
                StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, LC_TERMINAL);
                List<RequestRow> rows = menu[0].stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).missing() == 20, "the request shows the bread missing, got " + rows);
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 0, "no bread arrived");
                helper.assertTrue(network.garageOut(garagePos) == 0, "the shuttle is back in its garage");
                helper.succeed();
            }
        });
    }

    /** Uses an elevator item while sneaking on the east face of a segment. */
    private static ItemStack sneakElevator(GameTestHelper helper, Player player, BlockPos clicked) {
        return useElevator(helper, player, clicked, true);
    }

    /** Uses an elevator item on the east face of a block, sneaking or not. */
    private static ItemStack useElevator(GameTestHelper helper, Player player, BlockPos clicked, boolean sneak) {
        BlockPos absolute = helper.absolutePos(clicked);
        ItemStack elevator = new ItemStack(ServusShuttlesBlocks.ELEVATOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, elevator);
        player.setShiftKeyDown(sneak);
        elevator.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.EAST, 0.5), Direction.EAST, absolute, false)));
        player.setShiftKeyDown(false);
        return elevator;
    }

    /** A plain click on the side of the lowest segment places a new segment on top of the shaft, like scaffolding. */
    @ServusGameTest(timeoutTicks = 40)
    public static void plainElevatorBuildsUp(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        shaft(helper, 3, 3, 4);
        ItemStack held = useElevator(helper, player, new BlockPos(3, 1, 3), false);
        helper.assertBlockPresent(ServusShuttlesBlocks.ELEVATOR.get(), new BlockPos(3, 5, 3));
        helper.assertBlockNotPresent(ServusShuttlesBlocks.ELEVATOR.get(), new BlockPos(4, 1, 3));
        helper.assertTrue(held.isEmpty(), "one elevator should be used");
        helper.succeed();
    }

    /** A plain click on a shaft capped by stone places nothing and keeps the stack. */
    @ServusGameTest(timeoutTicks = 40)
    public static void plainElevatorBlockedTop(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        shaft(helper, 3, 3, 2);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE.defaultBlockState());
        ItemStack held = useElevator(helper, player, new BlockPos(3, 2, 3), false);
        helper.assertTrue(held.getCount() == 1, "the stack should keep its elevator");
        helper.assertBlockNotPresent(ServusShuttlesBlocks.ELEVATOR.get(), new BlockPos(4, 2, 3));
        helper.succeed();
    }

    /** A plain click on the top of a stone block places a segment on it as with any block. */
    @ServusGameTest(timeoutTicks = 40)
    public static void plainElevatorOnGround(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE.defaultBlockState());
        BlockPos absolute = helper.absolutePos(new BlockPos(3, 1, 3));
        ItemStack elevator = new ItemStack(ServusShuttlesBlocks.ELEVATOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, elevator);
        elevator.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false)));
        helper.assertBlockPresent(ServusShuttlesBlocks.ELEVATOR.get(), new BlockPos(3, 2, 3));
        helper.succeed();
    }

    private static void shaft(GameTestHelper helper, int x, int z, int height) {
        helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
        for (int y = 1; y <= height; y++) {
            helper.setBlock(new BlockPos(x, y, z), ServusShuttlesBlocks.ELEVATOR.get().defaultBlockState());
        }
    }

    /** A sneak click on the side of the lowest segment places a new segment on top of the shaft. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakElevatorBuildsUp(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        shaft(helper, 3, 3, 4);
        sneakElevator(helper, player, new BlockPos(3, 1, 3));
        helper.assertBlockPresent(ServusShuttlesBlocks.ELEVATOR.get(), new BlockPos(3, 5, 3));
        helper.assertBlockNotPresent(ServusShuttlesBlocks.ELEVATOR.get(), new BlockPos(4, 1, 3));
        helper.succeed();
    }

    /** A shaft capped by stone takes no new segment and the stack keeps its count. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakElevatorBlockedTop(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        shaft(helper, 3, 3, 2);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE.defaultBlockState());
        ItemStack held = sneakElevator(helper, player, new BlockPos(3, 1, 3));
        helper.assertTrue(held.getCount() == 1, "the stack should keep its elevator");
        helper.assertBlockNotPresent(ServusShuttlesBlocks.ELEVATOR.get(), new BlockPos(4, 1, 3));
        helper.succeed();
    }

    private static final BlockPos ON_RAIL = new BlockPos(4, 1, 3);
    private static final Identifier SPRUCE = Identifier.withDefaultNamespace("spruce_planks");

    /** Stone and a shaped east to west Warehouse Rail from x 2 to 6 along z 3, the middle rail in spruce. */
    private static void railRun(GameTestHelper helper) {
        WarehouseFixtures.railRow(helper, 2, 6, ON_RAIL.getZ());
        for (int x = 2; x <= 6; x++) {
            GantryFixtures.shape(helper, new BlockPos(x, 1, ON_RAIL.getZ()));
        }
        helper.getBlockEntity(ON_RAIL, RailBlockEntity.class).materialSlot().set(SPRUCE);
    }

    private static void useElevatorOnTop(GameTestHelper helper, BlockPos pos) {
        ItemStack stack = new ItemStack(ServusShuttlesItems.ELEVATOR.get());
        TestPlayers.useOnTop(helper, TestPlayers.holding(helper, stack), stack, pos);
    }

    /** Uses an elevator on the north face of the block at {@code pos}, as a click on the side of a shaft does. */
    private static void useElevatorOnSide(GameTestHelper helper, BlockPos pos) {
        ItemStack stack = new ItemStack(ServusShuttlesItems.ELEVATOR.get());
        Player player = TestPlayers.holding(helper, stack);
        BlockPos absolute = helper.absolutePos(pos);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false)));
    }

    private static boolean isSegment(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getBlock() instanceof ElevatorBlock;
    }

    private static boolean isWarehouseRail(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).is(ServusShuttlesBlocks.RAIL.get());
    }

    /** An elevator used on a rail of a run stands in the rail's place, joined to the rails beside it. */
    @ServusGameTest
    public static void segmentPlacedOnRailTakesItsPlace(GameTestHelper helper) {
        railRun(helper);
        useElevatorOnTop(helper, ON_RAIL);
        helper.assertTrue(isSegment(helper, ON_RAIL), "the segment stands where the rail was");
        helper.assertTrue(helper.getBlockEntity(ON_RAIL, ElevatorBlockEntity.class).replacedRail(), "the segment remembers the rail");
        helper.assertTrue(!isSegment(helper, ON_RAIL.above()), "nothing is placed above the rail");
        helper.assertBlockProperty(ON_RAIL.west(), ConnectableBlock.EAST, true);
        helper.assertBlockProperty(ON_RAIL.east(), ConnectableBlock.WEST, true);
        helper.succeed();
    }

    /** Breaking that segment drops it and puts the rail back, joined to its neighbours. */
    @ServusGameTest
    public static void brokenSegmentGivesTheRailBack(GameTestHelper helper) {
        railRun(helper);
        useElevatorOnTop(helper, ON_RAIL);
        helper.getLevel().destroyBlock(helper.absolutePos(ON_RAIL), true);
        helper.succeedWhen(() -> {
            helper.assertTrue(isWarehouseRail(helper, ON_RAIL), "the rail is back");
            helper.assertBlockProperty(ON_RAIL, ConnectableBlock.WEST, true);
            helper.assertBlockProperty(ON_RAIL.west(), ConnectableBlock.EAST, true);
            helper.assertItemEntityPresent(ServusShuttlesItems.ELEVATOR.get(), ON_RAIL, 2);
        });
    }

    /** The rail comes back in the wood it had, also after the segment was saved and loaded. */
    @ServusGameTest
    public static void railComesBackWithItsWood(GameTestHelper helper) {
        railRun(helper);
        useElevatorOnTop(helper, ON_RAIL);
        helper.getLevel().destroyBlock(helper.absolutePos(ON_RAIL), true);
        helper.succeedWhen(() -> {
            helper.assertTrue(isWarehouseRail(helper, ON_RAIL), "the rail is back");
            Identifier wood = helper.getBlockEntity(ON_RAIL, RailBlockEntity.class).materialSlot().material();
            helper.assertTrue(SPRUCE.equals(wood), "the rail keeps its spruce sleepers, got " + wood);
        });
    }

    /** Breaking the lowest segment of a shaft on a rail drops the shaft and leaves the rail. */
    @ServusGameTest
    public static void fallingShaftLeavesTheRail(GameTestHelper helper) {
        railRun(helper);
        useElevatorOnTop(helper, ON_RAIL);
        useElevatorOnSide(helper, ON_RAIL);
        useElevatorOnSide(helper, ON_RAIL);
        helper.assertTrue(isSegment(helper, ON_RAIL.above(2)), "the shaft is three segments high");
        helper.getLevel().destroyBlock(helper.absolutePos(ON_RAIL), true);
        helper.succeedWhen(() -> {
            helper.assertTrue(isWarehouseRail(helper, ON_RAIL), "the rail is back");
            helper.assertTrue(!isSegment(helper, ON_RAIL.above()) && !isSegment(helper, ON_RAIL.above(2)), "the shaft above fell");
        });
    }

    /** A segment placed on bare stone leaves nothing behind. */
    @ServusGameTest
    public static void bareGroundSegmentLeavesNothing(GameTestHelper helper) {
        BlockPos stone = new BlockPos(4, 0, 6);
        WarehouseFixtures.stone(helper, stone);
        useElevatorOnTop(helper, stone);
        BlockPos segment = stone.above();
        helper.assertTrue(isSegment(helper, segment) && !helper.getBlockEntity(segment, ElevatorBlockEntity.class).replacedRail(),
                "a segment on stone remembers no rail");
        helper.getLevel().destroyBlock(helper.absolutePos(segment), true);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(helper.getBlockState(segment).isAir(), "nothing is left in its place");
            helper.succeed();
        });
    }

    /** A click on a segment that replaced a rail builds the shaft up. */
    @ServusGameTest
    public static void clickOnRailSegmentBuildsUp(GameTestHelper helper) {
        railRun(helper);
        useElevatorOnTop(helper, ON_RAIL);
        useElevatorOnTop(helper, ON_RAIL);
        helper.assertTrue(isSegment(helper, ON_RAIL) && isSegment(helper, ON_RAIL.above()), "the second segment goes on top");
        helper.succeed();
    }

    /** A Farm Rail is not replaced. */
    @ServusGameTest
    public static void farmRailIsNotReplaced(GameTestHelper helper) {
        BlockPos farm = new BlockPos(4, 1, 6);
        WarehouseFixtures.stone(helper, farm.below());
        helper.setBlock(farm, ServusShuttlesBlocks.FARM_RAIL.get().defaultBlockState());
        useElevatorOnTop(helper, farm);
        helper.assertTrue(helper.getBlockState(farm).is(ServusShuttlesBlocks.FARM_RAIL.get()), "the farm rail stays");
        helper.succeed();
    }

    /** The remembered rail survives a save and load of the segment. */
    @ServusGameTest
    public static void rememberedRailSurvivesSave(GameTestHelper helper) {
        railRun(helper);
        useElevatorOnTop(helper, ON_RAIL);
        ElevatorBlockEntity loaded = PersistGameTests.reload(helper, ON_RAIL);
        helper.assertTrue(loaded.replacedRail(), "the loaded segment still remembers the rail");
        helper.getLevel().destroyBlock(helper.absolutePos(ON_RAIL), true);
        helper.succeedWhen(() -> {
            helper.assertTrue(isWarehouseRail(helper, ON_RAIL), "the rail is back after a reload");
            Identifier wood = helper.getBlockEntity(ON_RAIL, RailBlockEntity.class).materialSlot().material();
            helper.assertTrue(SPRUCE.equals(wood), "with its spruce sleepers, got " + wood);
        });
    }

    private static final Identifier BIRCH = Identifier.withDefaultNamespace("birch_planks");

    private static Identifier segmentWood(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ElevatorBlockEntity.class).materialSlot().material();
    }

    /** A segment placed on a spruce rail is a spruce segment. */
    @ServusGameTest
    public static void segmentOnRailTakesItsWood(GameTestHelper helper) {
        railRun(helper);
        useElevatorOnTop(helper, ON_RAIL);
        helper.assertTrue(SPRUCE.equals(segmentWood(helper, ON_RAIL)), "the segment took the rail's spruce, got " + segmentWood(helper, ON_RAIL));
        helper.succeed();
    }

    /** Elevators crafted from a birch rail carry birch. */
    @ServusGameTest
    public static void craftedElevatorCarriesTheRailWood(GameTestHelper helper) {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack rail = new ItemStack(ServusShuttlesItems.RAIL.get());
        rail.set(MaterialContent.MATERIAL.get(), BIRCH);
        CraftingInput input = CraftingInput.of(3, 3, List.of(iron, iron, iron, iron, rail, iron, iron, iron, iron));
        RecipeHolder<?> holder = helper.getLevel().recipeAccess()
                .byKey(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "elevator"))).orElseThrow();
        @SuppressWarnings("unchecked")
        CraftingRecipe recipe = ((RecipeHolder<CraftingRecipe>) holder).value();
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "the elevator recipe takes a birch rail");
        ItemStack result = recipe.assemble(input);
        helper.assertTrue(result.is(ServusShuttlesItems.ELEVATOR.get()) && result.getCount() == 4, "four elevators, got " + result);
        helper.assertTrue(BIRCH.equals(result.get(MaterialContent.MATERIAL.get())), "they carry birch, got " + result.get(MaterialContent.MATERIAL.get()));
        helper.succeed();
    }

    /** A broken spruce segment drops a spruce elevator that does not stack with an oak one. */
    @ServusGameTest
    public static void brokenSegmentDropsItsWood(GameTestHelper helper) {
        railRun(helper);
        useElevatorOnTop(helper, ON_RAIL);
        BlockPos absolute = helper.absolutePos(ON_RAIL);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(ON_RAIL), helper.getLevel(), absolute, helper.getLevel().getBlockEntity(absolute));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ServusShuttlesItems.ELEVATOR.get()), "one elevator drops, got " + drops);
        helper.assertTrue(SPRUCE.equals(drops.get(0).get(MaterialContent.MATERIAL.get())), "it keeps spruce");
        helper.assertTrue(!ItemStack.isSameItemSameComponents(drops.get(0), new ItemStack(ServusShuttlesItems.ELEVATOR.get())),
                "a spruce elevator does not stack with an oak one");
        helper.succeed();
    }

    /** Placing a birch elevator item makes a birch segment. */
    @ServusGameTest
    public static void placedItemGivesItsWood(GameTestHelper helper) {
        BlockPos stone = new BlockPos(4, 0, 6);
        WarehouseFixtures.stone(helper, stone);
        ItemStack stack = new ItemStack(ServusShuttlesItems.ELEVATOR.get());
        stack.set(MaterialContent.MATERIAL.get(), BIRCH);
        TestPlayers.useOnTop(helper, TestPlayers.holding(helper, stack), stack, stone);
        helper.assertTrue(BIRCH.equals(segmentWood(helper, stone.above())), "the segment is birch, got " + segmentWood(helper, stone.above()));
        helper.succeed();
    }
}
