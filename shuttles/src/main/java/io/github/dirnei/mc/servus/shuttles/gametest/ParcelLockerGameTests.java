package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldActions;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBasketPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldLockersPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldOrders;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldRelocation;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLamp;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.LockerOrder;
import io.github.dirnei.mc.servus.shuttles.locker.LockerOrders;
import io.github.dirnei.mc.servus.shuttles.locker.LockerPart;
import io.github.dirnei.mc.servus.shuttles.locker.LockerReturns;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlock;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity.BoxView;
import io.github.dirnei.mc.servus.shuttles.menu.ParcelLockerMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Parcel Locker: placing it as one piece, breaking it, its number, orders
 * from the Handheld's STOCK basket delivered by shuttle, refusals, collecting
 * only one's own boxes and parcels going back to the racks.
 *
 * <pre>
 *   z 3:  garage(x1)  rack(x3)  rack(x5)            controller(x9)
 *   z 4:  rail from x 1 to x 10
 *   z 5:        locker x 4 to 8, dock at x 6 facing the rail, two high
 * </pre>
 */
public final class ParcelLockerGameTests {

    private static final BlockPos GARAGE = new BlockPos(1, 1, 3);
    private static final BlockPos RACK = new BlockPos(3, 1, 3);
    private static final BlockPos CONTROLLER = new BlockPos(9, 1, 3);
    private static final BlockPos DOCK = new BlockPos(6, 1, 5);
    private static final int TRIP = 400;

    private ParcelLockerGameTests() {
    }

    /** Stone under the locker's five columns. */
    private static void footing(GameTestHelper helper) {
        for (int x = 4; x <= 8; x++) {
            WarehouseFixtures.stone(helper, new BlockPos(x, 0, DOCK.getZ()));
        }
    }

    /** The rail, garage with one shuttle, a rack and the controller; the locker's footing. */
    private static WarehouseControllerBlockEntity warehouse(GameTestHelper helper) {
        WarehouseFixtures.railRow(helper, 1, 10, 4);
        WarehouseFixtures.garage(helper, GARAGE, Direction.NORTH, 1);
        WarehouseFixtures.rack(helper, RACK, Direction.SOUTH);
        footing(helper);
        return WarehouseFixtures.controller(helper, CONTROLLER);
    }

    /** Places the locker the way its item does, its front to the north. */
    private static ParcelLockerBlockEntity place(GameTestHelper helper) {
        BlockState dock = ServusShuttlesBlocks.PARCEL_LOCKER.get().defaultBlockState().setValue(ParcelLockerBlock.FACING, Direction.NORTH);
        helper.setBlock(DOCK, dock);
        dock.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(DOCK), dock, null, new ItemStack(ServusShuttlesItems.PARCEL_LOCKER.get()));
        return locker(helper);
    }

    private static ParcelLockerBlockEntity locker(GameTestHelper helper) {
        return helper.getBlockEntity(DOCK, ParcelLockerBlockEntity.class);
    }

    /** Where a part of the locker stands, relative to the test. */
    private static BlockPos part(LockerPart part) {
        return part.at(DOCK, Direction.NORTH);
    }

    private static HeavyRackBlockEntity rack(GameTestHelper helper) {
        return helper.getBlockEntity(RACK, HeavyRackBlockEntity.class);
    }

    /** Runs {@code then} once the warehouse is online, numbered the locker and indexes {@code count} of the item. */
    private static void whenReady(GameTestHelper helper, WarehouseControllerBlockEntity controller, Item item, long count, Runnable then) {
        Waits.when(helper, () -> controller.network().map(network -> network.index().count(ItemKey.of(new ItemStack(item))) == count
                        && !controller.span().isEmpty() && locker(helper).number() == 1).orElse(false), 100,
                () -> "the warehouse should index " + count + " " + item + " and number the locker", then);
    }

    /** A survival player standing on the rail beside the locker. */
    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(DOCK.north().getCenter()));
        return player;
    }

    /** A STOCK handheld in the player's main hand, bound to the controller, with the stacks in its basket. */
    private static ItemStack handheld(Player player, WarehouseControllerBlockEntity controller, ItemStack... basket) {
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.STOCK);
        player.setItemInHand(InteractionHand.MAIN_HAND, handheld);
        HandheldActions.bind(player, handheld, controller);
        for (ItemStack stack : basket) {
            HandheldRelocation.edit(player, handheld, null, HandheldBasketPayload.add(stack, stack.getCount()));
        }
        return handheld;
    }

    private static int count(ParcelLockerBlockEntity locker, LockerOrder order, Item item) {
        return locker.contents(order).stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** A parcel of the player in the box, as if shuttles had delivered it. */
    private static LockerOrder parcel(ParcelLockerBlockEntity locker, UUID owner, String name, int box, ItemStack stack, long readySince) {
        LockerOrder order = locker.reserve(locker.orders().size() + 1000, owner, name, List.of(box));
        locker.boxes().setStackInSlot(locker.range(box).first(), stack);
        locker.markReady(order, readySince);
        return order;
    }

    /** The item places all ten parts in front of the rail; with one space taken it places nothing and stays in hand. */
    @ServusGameTest(timeoutTicks = 40)
    public static void placementNeedsTenFreeSpaces(GameTestHelper helper) {
        WarehouseFixtures.railRow(helper, 1, 10, 4);
        footing(helper);
        helper.setBlock(part(LockerPart.L_HIGH), Blocks.STONE.defaultBlockState());
        ItemStack item = new ItemStack(ServusShuttlesItems.PARCEL_LOCKER.get());
        Player player = TestPlayers.holding(helper, item);
        TestPlayers.useOnTop(helper, player, item, DOCK.below());
        helper.assertTrue(helper.getBlockState(DOCK).isAir() && item.getCount() == 1, "a taken space places nothing and keeps the item");
        helper.setBlock(part(LockerPart.L_HIGH), Blocks.AIR.defaultBlockState());
        TestPlayers.useOnTop(helper, player, item, DOCK.below());
        for (LockerPart part : LockerPart.values()) {
            BlockState state = helper.getBlockState(part(part));
            helper.assertTrue(state.is(ServusShuttlesBlocks.PARCEL_LOCKER.get()) && state.getValue(ParcelLockerBlock.PART) == part
                    && state.getValue(ParcelLockerBlock.FACING) == Direction.NORTH, "the " + part + " stands facing the rail");
        }
        helper.assertTrue(item.isEmpty(), "the item is used up");
        helper.succeed();
    }

    /** Breaking any part removes all ten and drops the locker item and every box's items. */
    @ServusGameTest(timeoutTicks = 40)
    public static void breakRemovesAllAndDropsContents(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        parcel(locker, UUID.randomUUID(), "Alex", 0, new ItemStack(Items.BREAD, 32), 0);
        parcel(locker, UUID.randomUUID(), "Steve", 9, new ItemStack(Items.IRON_INGOT, 20), 0);
        helper.getLevel().destroyBlock(helper.absolutePos(part(LockerPart.S_COLUMN_HIGH)), true);
        helper.runAfterDelay(2, () -> {
            for (LockerPart part : LockerPart.values()) {
                helper.assertTrue(helper.getBlockState(part(part)).isAir(), "the " + part + " is gone");
            }
            AABB area = new AABB(helper.absolutePos(new BlockPos(2, 0, 3))).expandTowards(8, 4, 4);
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
            int lockers = drops.stream().filter(drop -> drop.getItem().is(ServusShuttlesItems.PARCEL_LOCKER.get())).mapToInt(drop -> drop.getItem().getCount()).sum();
            int bread = drops.stream().filter(drop -> drop.getItem().is(Items.BREAD)).mapToInt(drop -> drop.getItem().getCount()).sum();
            int iron = drops.stream().filter(drop -> drop.getItem().is(Items.IRON_INGOT)).mapToInt(drop -> drop.getItem().getCount()).sum();
            helper.assertTrue(lockers == 1 && bread == 32 && iron == 20,
                    "one locker, 32 bread and 20 iron drop, got " + lockers + ", " + bread + ", " + iron);
            helper.succeed();
        });
    }

    /** The first locker at a warehouse's rail is Parcel Locker #1; an empty one has 6 S, 3 M and 1 L box, all free. */
    @ServusGameTest(timeoutTicks = 120)
    public static void numberedByItsWarehouse(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        ParcelLockerBlockEntity locker = place(helper);
        Waits.when(helper, () -> locker.number() == 1, 100, () -> "the locker should be numbered 1, got " + locker.number(), () -> {
            helper.assertTrue(locker.network().isPresent() && locker.network().equals(controller.network()), "the locker joins by its rail");
            helper.assertTrue(locker.freeCount(LockerLayout.Size.S) == 6 && locker.freeCount(LockerLayout.Size.M) == 3
                    && locker.freeCount(LockerLayout.Size.L) == 1, "an empty locker has 6 S, 3 M and 1 L box free");
            helper.assertTrue(locker.views(UUID.randomUUID()).stream().allMatch(view -> view == BoxView.FREE), "every box shows free");
            helper.succeed();
        });
    }

    /**
     * SEND orders the STOCK basket: 128 cobblestone reserve one S box, the
     * basket empties, a shuttle brings the stack in and the parcel turns ready.
     */
    @ServusGameTest(timeoutTicks = TRIP)
    public static void orderReservesBoxesAndDelivers(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64));
        ParcelLockerBlockEntity locker = place(helper);
        whenReady(helper, controller, Items.COBBLESTONE, 128, () -> {
            Player player = player(helper);
            ItemStack handheld = handheld(player, controller, new ItemStack(Items.COBBLESTONE, 128));
            helper.assertTrue(HandheldOrders.request(player, handheld, locker.getBlockPos()), "the order stands");
            helper.assertTrue(HandheldRelocation.basket(handheld).isEmpty(), "SEND empties the STOCK basket");
            LockerOrder order = locker.orders().getFirst();
            helper.assertTrue(order.boxes().equals(List.of(0)) && order.owner().equals(player.getUUID()),
                    "the order reserves S-01 for the player, got " + order.boxes());
            Waits.when(helper, () -> locker.orders().stream().anyMatch(found -> found.state() == LockerReturns.State.READY), TRIP - 20,
                    () -> "the parcel should turn ready", () -> {
                        LockerOrder ready = locker.orders().getFirst();
                        helper.assertTrue(count(locker, ready, Items.COBBLESTONE) == 128, "the box holds the 128 cobblestone");
                        helper.assertTrue(WarehouseFixtures.count(rack(helper), Items.COBBLESTONE) == 0, "the rack gave them");
                        helper.assertTrue(helper.getBlockState(part(LockerPart.S_COLUMN_HIGH)).getValue(ParcelLockerBlock.LAMP_TOP)
                                == LockerLamp.READY, "the lamp of S-01 is green");
                        helper.succeed();
                    });
        });
    }

    /** An order the free boxes cannot take is refused before anything is picked or locked, and the basket stays. */
    @ServusGameTest(timeoutTicks = 120)
    public static void tooBigIsRefused(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.COBBLESTONE, 64));
        ParcelLockerBlockEntity locker = place(helper);
        whenReady(helper, controller, Items.COBBLESTONE, 64, () -> {
            for (int box = 0; box < LockerLayout.BOXES.size(); box++) {
                locker.reserve(2000 + box, UUID.randomUUID(), "Alex", List.of(box));
            }
            Player player = player(helper);
            ItemStack handheld = handheld(player, controller, new ItemStack(Items.COBBLESTONE, 64));
            helper.assertTrue(!HandheldOrders.request(player, handheld, locker.getBlockPos()), "a full locker refuses the order");
            helper.assertTrue(rack(helper).items().locked().isEmpty(), "nothing is locked in the rack");
            helper.assertTrue(HandheldRelocation.basket(handheld).amount(ItemKey.of(new ItemStack(Items.COBBLESTONE))) == 64,
                    "the basket stays");
            HandheldLockersPayload lockers = HandheldOrders.lockers(player, handheld);
            helper.assertTrue(lockers.problem().isEmpty() && lockers.lockers().size() == 1 && !lockers.lockers().getFirst().fits(),
                    "the picker lists the locker as not fitting");
            helper.succeed();
        });
    }

    /** SEND orders only what the warehouse holds unlocked now and reports the rest as short. */
    @ServusGameTest(timeoutTicks = 120)
    public static void shortfallIsClamped(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.BREAD, 20));
        ParcelLockerBlockEntity locker = place(helper);
        whenReady(helper, controller, Items.BREAD, 20, () -> {
            Player player = player(helper);
            ItemStack basketHolder = handheld(player, controller, new ItemStack(Items.BREAD, 64));
            LockerOrders.Placed placed = LockerOrders.place(controller.network().orElseThrow(), locker, player.getUUID(), "Steve",
                    HandheldRelocation.basket(basketHolder));
            ItemKey bread = ItemKey.of(new ItemStack(Items.BREAD));
            helper.assertTrue(placed.placed() && placed.ordered().get(bread) == 20 && placed.shortBy().get(bread) == 44,
                    "20 bread are ordered and 44 are short, got " + placed);
            helper.succeed();
        });
    }

    /** The GUI shows only the player's own boxes to take from; emptying one frees it, others show as taken. */
    @ServusGameTest(timeoutTicks = 40)
    public static void collectingOnlyOwnBoxes(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        Player steve = helper.makeMockPlayer(GameType.SURVIVAL);
        UUID alex = UUID.randomUUID();
        parcel(locker, alex, "Alex", 0, new ItemStack(Items.BREAD, 32), helper.getLevel().getGameTime());
        LockerOrder mine = parcel(locker, steve.getUUID(), "Steve", 1, new ItemStack(Items.IRON_INGOT, 20), helper.getLevel().getGameTime());
        List<BoxView> views = locker.views(steve.getUUID());
        helper.assertTrue(views.get(0) == BoxView.TAKEN && views.get(1) == BoxView.MINE && views.get(2) == BoxView.FREE,
                "Steve sees Alex's box taken, his own box his, the rest free");
        ParcelLockerMenu menu = new ParcelLockerMenu(0, steve.getInventory(), locker, views);
        helper.assertTrue(menu.boxSlots() == ShuttlesStatsManager.INSTANCE.lockerCapacities().small(), "the GUI shows Steve's box only");
        helper.assertTrue(!menu.slots.getFirst().mayPlace(new ItemStack(Items.DIRT)), "nothing goes into a box by hand");
        menu.quickMoveStack(steve, 0);
        helper.assertTrue(steve.getInventory().countItem(Items.IRON_INGOT) == 20, "Steve takes his iron");
        helper.assertTrue(locker.orders().stream().noneMatch(order -> order.id() == mine.id()) && locker.orderOfBox(1).isEmpty(),
                "his emptied box is free again");
        helper.assertTrue(count(locker, locker.orderOfBox(0).orElseThrow(), Items.BREAD) == 32, "Alex's parcel stays");
        helper.succeed();
    }

    /** A parcel past its time goes back into the racks by shuttle and frees its box. */
    @ServusGameTest(timeoutTicks = TRIP)
    public static void expiredParcelIsPutAway(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        ParcelLockerBlockEntity locker = place(helper);
        whenReady(helper, controller, Items.COBBLESTONE, 0, () -> {
            long past = helper.getLevel().getGameTime() - ShuttlesStatsManager.INSTANCE.lockerReturnTicks();
            parcel(locker, UUID.randomUUID(), "Alex", 9, new ItemStack(Items.COBBLESTONE, 64), past);
            Waits.when(helper, () -> WarehouseFixtures.count(rack(helper), Items.COBBLESTONE) == 64 && locker.orders().isEmpty(), TRIP - 20,
                    () -> "the parcel should be back in the rack and its box free, rack holds "
                            + WarehouseFixtures.count(rack(helper), Items.COBBLESTONE), helper::succeed);
        });
    }

    /** Breaking the locker right after an order ends it and unlocks the stock its picks had locked. */
    @ServusGameTest(timeoutTicks = 120)
    public static void breakDuringOrderUnlocksStock(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.COBBLESTONE, 64));
        ParcelLockerBlockEntity locker = place(helper);
        whenReady(helper, controller, Items.COBBLESTONE, 64, () -> {
            Player player = player(helper);
            ItemStack handheld = handheld(player, controller, new ItemStack(Items.COBBLESTONE, 64));
            helper.assertTrue(HandheldOrders.request(player, handheld, locker.getBlockPos()), "the order stands");
            helper.assertTrue(!rack(helper).items().locked().isEmpty(), "the pick locks the rack's stack");
            helper.getLevel().destroyBlock(helper.absolutePos(part(LockerPart.L_LOW)), false);
            helper.assertTrue(helper.getBlockState(DOCK).isAir(), "the locker is gone");
            helper.assertTrue(rack(helper).items().locked().isEmpty(), "the queued pick is dropped and the stack unlocked");
            helper.succeed();
        });
    }

    /** Outside the site and its margin the picker says out of range and SEND is refused; the basket stays. */
    @ServusGameTest(timeoutTicks = 120)
    public static void outOfRangeIsRefused(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.COBBLESTONE, 64));
        ParcelLockerBlockEntity locker = place(helper);
        whenReady(helper, controller, Items.COBBLESTONE, 64, () -> {
            Player player = player(helper);
            ItemStack handheld = handheld(player, controller, new ItemStack(Items.COBBLESTONE, 64));
            player.setPos(player.position().add(300 + ShuttlesStatsManager.INSTANCE.handheldOrderMargin(), 0, 0));
            helper.assertTrue(HandheldOrders.lockers(player, handheld).problem().equals("out_of_range"), "the picker says out of range");
            helper.assertTrue(!HandheldOrders.request(player, handheld, locker.getBlockPos()), "SEND is refused");
            helper.assertTrue(HandheldRelocation.basket(handheld).amount(ItemKey.of(new ItemStack(Items.COBBLESTONE))) == 64
                    && locker.orders().isEmpty(), "the basket stays and no order is placed");
            helper.succeed();
        });
    }
}
