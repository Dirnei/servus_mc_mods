package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldActions;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBasketPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldLockersPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldOrders;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldRelocation;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLamp;
import io.github.dirnei.mc.servus.shuttles.locker.BoxInfo;
import io.github.dirnei.mc.servus.shuttles.locker.LockerGlance;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.LockerOrder;
import io.github.dirnei.mc.servus.shuttles.locker.LockerOrders;
import io.github.dirnei.mc.servus.shuttles.locker.LockerPart;
import io.github.dirnei.mc.servus.shuttles.locker.LockerReturns;
import io.github.dirnei.mc.servus.shuttles.locker.LockerScreen;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.Vec3;

/**
 * The Parcel Locker: placing it as one piece, breaking it, its number, orders
 * from the Handheld's STOCK basket delivered by shuttle, refusals, collecting
 * only one's own boxes, scanning the screen to open the doors, taking from
 * open boxes and parcels going back to the racks.
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
                        helper.assertTrue(locker.lamp(0, player.getUUID()) == LockerLamp.READY, "the lamp of S-01 is green");
                        helper.assertTrue(locker.lamp(0, UUID.randomUUID()) == LockerLamp.TAKEN, "others see it ochre");
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

    /** The GUI lists every box with who ordered it, read only: nothing can be taken through it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void guiListsBoxesReadOnly(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        Player steve = helper.makeMockPlayer(GameType.SURVIVAL);
        UUID alex = UUID.randomUUID();
        parcel(locker, alex, "Alex", 0, new ItemStack(Items.BREAD, 32), helper.getLevel().getGameTime());
        parcel(locker, steve.getUUID(), "Steve", 1, new ItemStack(Items.IRON_INGOT, 20), helper.getLevel().getGameTime());
        List<BoxInfo> boxes = locker.infos(steve.getUUID());
        helper.assertTrue(boxes.get(0).view() == BoxView.TAKEN && boxes.get(1).view() == BoxView.MINE && boxes.get(2).view() == BoxView.FREE,
                "Steve sees Alex's box taken, his own box his, the rest free");
        helper.assertTrue(boxes.get(0).owner().equals("Alex") && boxes.get(0).state() == LockerReturns.State.READY,
                "Alex's box names him and says his parcel is ready");
        ParcelLockerMenu menu = new ParcelLockerMenu(0, locker, boxes);
        helper.assertTrue(menu.slots.isEmpty(), "the GUI has no slots to take from");
        helper.assertTrue(menu.quickMoveStack(steve, 0).isEmpty() && steve.getInventory().countItem(Items.IRON_INGOT) == 0,
                "nothing comes out through the GUI");
        helper.assertTrue(locker.orderOfBox(1).isPresent(), "Steve's parcel stays in its box");
        helper.succeed();
    }

    /** An open box holding more of one item than a stack takes still syncs to clients, with its whole total. */
    @ServusGameTest(timeoutTicks = 40)
    public static void openBoxSyncsTotalsPastStackLimit(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        Player steve = helper.makeMockPlayer(GameType.SURVIVAL);
        parcel(locker, steve.getUUID(), "Steve", 1, new ItemStack(Items.BREAD, 64), helper.getLevel().getGameTime());
        locker.boxes().setStackInSlot(locker.range(1).first() + 1, new ItemStack(Items.BREAD, 49));
        locker.scan(steve);
        CompoundTag box = locker.getUpdateTag(helper.getLevel().registryAccess()).getCompoundOrEmpty("View")
                .getListOrEmpty("Boxes").getCompoundOrEmpty(1);
        helper.assertTrue(box.getListOrEmpty("Stacks").getCompoundOrEmpty(0).getIntOr("Count", 0) == 113,
                "the open box sends its 113 bread as one total");
        helper.succeed();
    }

    /** What a look at the locker sends: the viewer's own box with its items, someone else's box only with its orderer. */
    @ServusGameTest(timeoutTicks = 40)
    public static void glanceShowsItemsOfOwnBoxOnly(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        UUID steve = UUID.randomUUID();
        long now = helper.getLevel().getGameTime();
        parcel(locker, UUID.randomUUID(), "Alex", 0, new ItemStack(Items.IRON_INGOT, 20), now);
        parcel(locker, steve, "Steve", 1, new ItemStack(Items.BREAD, 64), now);
        locker.boxes().setStackInSlot(locker.range(1).first() + 1, new ItemStack(Items.BREAD, 49));
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        LockerGlance.Line own = sent(locker, LockerPart.S_COLUMN_HIGH, 4, steve, registries).getFirst();
        helper.assertTrue(own.box().id().equals("S-02") && own.owner().equals("Steve") && own.state() == LockerReturns.State.READY,
                "the look at S-02 names Steve and says his parcel is ready");
        helper.assertTrue(own.items().size() == 1 && own.items().getFirst().is(Items.BREAD) && own.items().getFirst().getCount() == 113,
                "Steve's own box sends its 113 bread as one total");

        LockerGlance.Line other = sent(locker, LockerPart.S_COLUMN_HIGH, 12, steve, registries).getFirst();
        helper.assertTrue(other.box().id().equals("S-01") && other.owner().equals("Alex") && other.items().isEmpty(),
                "Alex's box names Alex but sends none of his items to Steve");

        LockerGlance.Line free = sent(locker, LockerPart.S_COLUMN_LOW, 4, steve, registries).getFirst();
        helper.assertTrue(free.box().id().equals("S-04") && free.free(), "an empty box is free");

        List<LockerGlance.Line> head = sent(locker, LockerPart.HEAD_SCREEN, 8, steve, registries);
        helper.assertTrue(head.size() == 2 && head.get(0).owner().equals("Alex") && head.get(0).items().isEmpty()
                        && head.get(1).owner().equals("Steve") && head.get(1).items().getFirst().getCount() == 113,
                "the screen lists both taken boxes, items only in Steve's");
        helper.succeed();
    }

    /** The lines a look at the part sends to the viewer, after the trip to the client. */
    private static List<LockerGlance.Line> sent(ParcelLockerBlockEntity locker, LockerPart part, double hitY, UUID viewer,
            HolderLookup.Provider registries) {
        return LockerGlance.read(LockerGlance.write(LockerGlance.lines(locker, part, hitY, viewer), registries), registries);
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

    /** A survival player in front of the screen with a Handheld in hand. */
    private static ServerPlayer scanner(GameTestHelper helper) {
        return PlayerClicks.player(helper, part(LockerPart.HEAD_SCREEN), ZoneFixtures.handheld(HandheldMode.STOCK));
    }

    private static void scan(GameTestHelper helper, ServerPlayer player) {
        PlayerClicks.use(helper, player, part(LockerPart.HEAD_SCREEN));
    }

    /** Right clicks the front of a part {@code hitY} pixels above its bottom. */
    private static void click(GameTestHelper helper, ServerPlayer player, LockerPart part, double hitY) {
        BlockPos absolute = helper.absolutePos(part(part));
        PlayerClicks.use(helper, player, part(part), new Vec3(absolute.getX() + 0.5, absolute.getY() + hitY / 16, absolute.getZ()));
    }

    /** A Handheld scan opens only the scanner's ready boxes and greets them; reserved and others' boxes stay shut. */
    @ServusGameTest(timeoutTicks = 40)
    public static void scanOpensOnlyOwnReadyBoxes(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        ServerPlayer steve = scanner(helper);
        long now = helper.getLevel().getGameTime();
        parcel(locker, UUID.randomUUID(), "Alex", 4, new ItemStack(Items.APPLE, 16), now);
        parcel(locker, steve.getUUID(), "Steve", 1, new ItemStack(Items.BREAD, 32), now);
        parcel(locker, steve.getUUID(), "Steve", 7, new ItemStack(Items.IRON_INGOT, 64), now);
        locker.reserve(3000, steve.getUUID(), "Steve", List.of(9));
        scan(helper, steve);
        helper.assertTrue(locker.isOpen(1) && locker.isOpen(7), "S-02 and M-02 open");
        helper.assertTrue(!locker.isOpen(4) && !locker.isOpen(9), "Alex's S-05 and the reserved L-01 stay shut");
        helper.assertTrue(locker.screen() == LockerScreen.HELLO && locker.greeted() == 2, "the screen greets Steve with 2 boxes");
        helper.succeed();
    }

    /** A scan with nothing waiting says so on the screen and opens no door. */
    @ServusGameTest(timeoutTicks = 40)
    public static void nothingWaitingSaysNone(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        parcel(locker, UUID.randomUUID(), "Alex", 4, new ItemStack(Items.APPLE, 16), helper.getLevel().getGameTime());
        scan(helper, scanner(helper));
        helper.assertTrue(locker.screen() == LockerScreen.NONE, "the screen says there is no parcel");
        for (int box = 0; box < LockerLayout.BOXES.size(); box++) {
            helper.assertTrue(!locker.isOpen(box), "no door opens");
        }
        helper.succeed();
    }

    /** A click on an open own box moves its items into the inventory; the emptied box closes and is free. */
    @ServusGameTest(timeoutTicks = 40)
    public static void takingFromOpenBoxFreesIt(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        ServerPlayer steve = scanner(helper);
        parcel(locker, steve.getUUID(), "Steve", 1, new ItemStack(Items.BREAD, 32), helper.getLevel().getGameTime());
        scan(helper, steve);
        click(helper, steve, LockerPart.S_COLUMN_HIGH, 4);
        helper.assertTrue(steve.getInventory().countItem(Items.BREAD) == 32, "the bread is in Steve's inventory");
        helper.assertTrue(locker.orderOfBox(1).isEmpty() && !locker.isOpen(1), "S-02 closes and is free");
        helper.succeed();
    }

    /** What does not fit into a full inventory stays in the box, which stays held and open. */
    @ServusGameTest(timeoutTicks = 40)
    public static void fullInventoryLeavesRest(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        ServerPlayer steve = scanner(helper);
        for (int slot = 1; slot < 35; slot++) {
            steve.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        LockerOrder order = parcel(locker, steve.getUUID(), "Steve", 1, new ItemStack(Items.BREAD, 64), helper.getLevel().getGameTime());
        locker.boxes().setStackInSlot(locker.range(1).first() + 1, new ItemStack(Items.BREAD, 64));
        scan(helper, steve);
        click(helper, steve, LockerPart.S_COLUMN_HIGH, 4);
        helper.assertTrue(steve.getInventory().countItem(Items.BREAD) == 64, "one stack fits");
        helper.assertTrue(count(locker, order, Items.BREAD) == 64 && locker.orderOfBox(1).isPresent() && locker.isOpen(1),
                "the other stack stays in the open box");
        helper.succeed();
    }

    /** A click on someone else's box opens nothing and the screen says it is not theirs. */
    @ServusGameTest(timeoutTicks = 40)
    public static void othersBoxIsDenied(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        ServerPlayer steve = scanner(helper);
        LockerOrder alex = parcel(locker, UUID.randomUUID(), "Alex", 4, new ItemStack(Items.APPLE, 16), helper.getLevel().getGameTime());
        click(helper, steve, LockerPart.MIXED_HIGH, 12);
        helper.assertTrue(locker.screen() == LockerScreen.DENY, "the screen says the box is not Steve's");
        helper.assertTrue(!locker.isOpen(4) && count(locker, alex, Items.APPLE) == 16 && steve.getInventory().countItem(Items.APPLE) == 0,
                "Alex's box stays shut and full");
        helper.succeed();
    }

    /** Open doors close by themselves after the open time; the screen falls back and the box keeps its items. */
    @ServusGameTest(timeoutTicks = 300)
    public static void doorsCloseAfterOpenTime(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        ServerPlayer steve = scanner(helper);
        LockerOrder order = parcel(locker, steve.getUUID(), "Steve", 1, new ItemStack(Items.BREAD, 32), helper.getLevel().getGameTime());
        scan(helper, steve);
        helper.runAfterDelay(ShuttlesStatsManager.INSTANCE.lockerDoorOpenTicks() + 1, () -> {
            helper.assertTrue(!locker.isOpen(1), "S-02 closes");
            helper.assertTrue(locker.screen() == LockerScreen.IDLE, "the screen shows the scan code again");
            click(helper, steve, LockerPart.S_COLUMN_HIGH, 4);
            helper.assertTrue(count(locker, order, Items.BREAD) == 32 && steve.getInventory().countItem(Items.BREAD) == 0,
                    "a closed box gives nothing");
            helper.succeed();
        });
    }

    /** An empty hand on the screen opens the locker GUI, whose title renames the locker. */
    @ServusGameTest(timeoutTicks = 40)
    public static void emptyHandOpensGuiAndRenames(GameTestHelper helper) {
        footing(helper);
        ParcelLockerBlockEntity locker = place(helper);
        ServerPlayer player = PlayerClicks.player(helper, part(LockerPart.HEAD_SCREEN), ItemStack.EMPTY);
        scan(helper, player);
        helper.assertTrue(player.containerMenu instanceof ParcelLockerMenu, "the locker GUI opens");
        ((ParcelLockerMenu) player.containerMenu).renameMachine(player, "Front Gate");
        helper.assertTrue(locker.name().equals("Front Gate"), "the locker is called Front Gate, got " + locker.name());
        helper.assertTrue(locker.screen() == LockerScreen.IDLE, "an empty hand scans nothing");
        helper.succeed();
    }
}
