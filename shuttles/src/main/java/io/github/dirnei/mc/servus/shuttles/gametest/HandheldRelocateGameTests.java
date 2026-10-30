package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.handheld.Basket;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldActions;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBasketPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldModePayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldRelocation;
import io.github.dirnei.mc.servus.shuttles.handheld.ShelfBasket;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.menu.HandheldMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.Relocation;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.zone.ShelfZones;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

/** The handheld's warehouse and shelf baskets and relocating a shelf basket onto a target shelf by shuttle. */
public final class HandheldRelocateGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(6, 1, 10);
    private static final BlockPos GARAGE = new BlockPos(1, 1, 8);
    private static final BlockPos SOURCE_A = new BlockPos(2, 1, 10);
    private static final BlockPos SOURCE_B = new BlockPos(4, 1, 10);
    private static final BlockPos[] TARGET = {new BlockPos(8, 1, 10), new BlockPos(9, 1, 10)};
    private static final ItemKey COBBLESTONE = key(Items.COBBLESTONE);
    private static final ItemKey BREAD = key(Items.BREAD);

    private HandheldRelocateGameTests() {
    }

    private static ItemKey key(Item item) {
        return ItemKey.of(new ItemStack(item));
    }

    private static HeavyRackBlockEntity rack(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, HeavyRackBlockEntity.class);
    }

    /** A rail along z 9 with a garage of two shuttles at its west end, two single source bays and a target shelf of two. */
    private static WarehouseControllerBlockEntity warehouse(GameTestHelper helper) {
        WarehouseFixtures.railRow(helper, 1, 10, 9);
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, CONTROLLER);
        WarehouseFixtures.garage(helper, GARAGE, Direction.NORTH, 2);
        WarehouseFixtures.rack(helper, SOURCE_A, Direction.NORTH);
        WarehouseFixtures.rack(helper, SOURCE_B, Direction.NORTH);
        WarehouseFixtures.northShelf(helper, TARGET);
        return controller;
    }

    /** Runs {@code then} once the warehouse is online and indexes {@code count} of the key. */
    private static void whenIndexed(GameTestHelper helper, WarehouseControllerBlockEntity controller, ItemKey key, long count,
                                    Runnable then) {
        Waits.when(helper, () -> controller.network().map(network -> network.index().count(key) == count).orElse(false), 100,
                () -> "the warehouse should index " + count + " of " + key.item(), then);
    }

    private static List<BlockPos> targetShelf(GameTestHelper helper) {
        return List.of(helper.absolutePos(TARGET[0]), helper.absolutePos(TARGET[1]));
    }

    private static List<BlockPos> shelfOf(GameTestHelper helper, BlockPos bay) {
        return ShelfZones.shelf(helper.getLevel(), helper.absolutePos(bay));
    }

    private static BlockPos shelfKey(GameTestHelper helper, BlockPos bay) {
        return ShelfZones.key(helper.getLevel(), helper.absolutePos(bay));
    }

    private static int onTarget(GameTestHelper helper, Item item) {
        return WarehouseFixtures.count(rack(helper, TARGET[0]), item) + WarehouseFixtures.count(rack(helper, TARGET[1]), item);
    }

    private static int lockedIn(GameTestHelper helper) {
        int sum = 0;
        for (BlockPos bay : TARGET) {
            sum += rack(helper, bay).items().locked().stream().filter(entry -> entry.lock().kind() == SlotLock.Kind.IN)
                    .mapToInt(entry -> entry.lock().amount()).sum();
        }
        return sum;
    }

    private static boolean targetUnlocked(GameTestHelper helper) {
        return rack(helper, TARGET[0]).items().locked().isEmpty() && rack(helper, TARGET[1]).items().locked().isEmpty();
    }

    /** An INFO handheld bound to the controller. */
    private static ItemStack boundHandheld(Player player, WarehouseControllerBlockEntity controller) {
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.INFO);
        HandheldActions.bind(player, handheld, controller);
        return handheld;
    }

    /** Adds the stacks to the basket of the shelf with that key, or to the warehouse basket for null. */
    private static void fill(Player player, ItemStack handheld, @Nullable BlockPos shelf, ItemStack... stacks) {
        for (ItemStack stack : stacks) {
            HandheldRelocation.edit(player, handheld, shelf, HandheldBasketPayload.add(stack, stack.getCount()));
        }
    }

    /** A bound handheld in pick target mode whose relocation takes the stacks from the shelf of {@code source}. */
    private static ItemStack picking(GameTestHelper helper, Player player, WarehouseControllerBlockEntity controller, BlockPos source,
                                     ItemStack... stacks) {
        ItemStack handheld = boundHandheld(player, controller);
        fill(player, handheld, shelfKey(helper, source), stacks);
        helper.assertTrue(HandheldRelocation.relocate(player, handheld, shelfKey(helper, source)), "Relocate starts pick target mode");
        return handheld;
    }

    /** A handheld GUI of the player's main hand handheld. */
    private static HandheldMenu menu(Player player) {
        return new HandheldMenu(0, player.getInventory(), InteractionHand.MAIN_HAND, player.getUUID());
    }

    /** Puts the handheld into INFO with {@code bay} as the last scanned bay, as an INFO scan does. */
    private static void scanned(GameTestHelper helper, ItemStack handheld, BlockPos bay) {
        handheld.set(ServusShuttlesComponents.HANDHELD_MODE.get(), HandheldMode.INFO);
        handheld.set(ServusShuttlesComponents.HANDHELD_PIN.get(), helper.absolutePos(bay));
    }

    private static Relocation relocate(GameTestHelper helper, WarehouseControllerBlockEntity controller, BlockPos source,
                                       ItemKey key, int amount) {
        Map<ItemKey, Integer> wanted = new LinkedHashMap<>();
        wanted.put(key, amount);
        return controller.network().orElseThrow().relocate(shelfOf(helper, source), targetShelf(helper), wanted);
    }

    private static ItemStack stacks(Item item, int count) {
        return new ItemStack(item, count);
    }

    /** Basket edits add and merge, set, remove and clear; a tenth item is refused. */
    @ServusGameTest(timeoutTicks = 20)
    public static void payloadsBuildABasket(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.STOCK);
        fill(player, handheld, null, stacks(Items.COBBLESTONE, 128), stacks(Items.BREAD, 64), stacks(Items.BREAD, 32));
        Basket<ItemKey> basket = HandheldRelocation.basket(handheld);
        helper.assertTrue(basket.size() == 2 && basket.amount(COBBLESTONE) == 128 && basket.amount(BREAD) == 96,
                "the basket holds 128 cobblestone and 96 bread, got " + basket);
        HandheldRelocation.edit(player, handheld, null, HandheldBasketPayload.set(stacks(Items.BREAD, 1), 10));
        helper.assertTrue(HandheldRelocation.basket(handheld).amount(BREAD) == 10, "set makes it 10 bread");
        HandheldRelocation.edit(player, handheld, null, HandheldBasketPayload.remove(stacks(Items.BREAD, 1)));
        helper.assertTrue(HandheldRelocation.basket(handheld).amount(BREAD) == 0, "remove drops the bread");
        List<Item> more = List.of(Items.STONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.OAK_LOG, Items.APPLE, Items.STICK, Items.COAL);
        for (Item item : more) {
            fill(player, handheld, null, stacks(item, 1));
        }
        fill(player, handheld, null, stacks(Items.FLINT, 1));
        basket = HandheldRelocation.basket(handheld);
        helper.assertTrue(basket.size() == Basket.MAX_ENTRIES && basket.amount(key(Items.FLINT)) == 0,
                "a full basket refuses a tenth item, got " + basket.size() + " entries");
        HandheldRelocation.edit(player, handheld, null, HandheldBasketPayload.clear());
        helper.assertTrue(HandheldRelocation.basket(handheld).isEmpty() && !handheld.has(ServusShuttlesComponents.HANDHELD_BASKET.get()),
                "clear empties the basket");
        helper.succeed();
    }

    /**
     * The GUI edits the basket of its screen: the warehouse basket in STOCK, the scanned shelf's basket in INFO,
     * nothing in ZONE. A scan in STOCK switches to INFO and shows that shelf. The shelf basket is emptied by a scan of
     * another shelf and by a mode switch; the warehouse basket stays over all of it.
     */
    @ServusGameTest(timeoutTicks = 20)
    public static void basketsFollowTheScreen(GameTestHelper helper) {
        WarehouseFixtures.rack(helper, SOURCE_A, Direction.NORTH);
        WarehouseFixtures.rack(helper, SOURCE_B, Direction.NORTH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.STOCK);
        player.setItemInHand(InteractionHand.MAIN_HAND, handheld);
        HandheldMenu menu = menu(player);
        menu.editBasket(player, HandheldBasketPayload.add(stacks(Items.APPLE, 1), 5));
        List<String> failures = new ArrayList<>();
        if (menu.shelfKey() != null) {
            failures.add("STOCK should show no shelf");
        }
        HandheldActions.scan(player, handheld, rack(helper, SOURCE_A));
        if (HandheldActions.mode(handheld) != HandheldMode.INFO || !shelfKey(helper, SOURCE_A).equals(menu.shelfKey())) {
            failures.add("a scan in STOCK should switch to INFO on the scanned shelf, got " + HandheldActions.mode(handheld) + " " + menu.shelfKey());
        }
        menu.editBasket(player, HandheldBasketPayload.add(stacks(Items.BREAD, 1), 10));
        if (HandheldRelocation.shelfBasket(handheld).amount(BREAD) != 10 || HandheldRelocation.basket(handheld).amount(BREAD) != 0) {
            failures.add("INFO should add bread to the shelf basket only");
        }
        HandheldRelocation.enterShelf(handheld, shelfKey(helper, SOURCE_A));
        if (HandheldRelocation.shelfBasket(handheld).amount(BREAD) != 10) {
            failures.add("a scan of the same shelf should keep the shelf basket");
        }
        HandheldActions.scan(player, handheld, rack(helper, SOURCE_B));
        if (!HandheldRelocation.shelfBasket(handheld).isEmpty() || !shelfKey(helper, SOURCE_B).equals(menu.shelfKey())) {
            failures.add("a scan of another shelf should show it and empty the shelf basket");
        }
        menu.editBasket(player, HandheldBasketPayload.add(stacks(Items.BREAD, 1), 10));
        HandheldActions.applyMode(player, HandheldModePayload.to(HandheldMode.STOCK));
        if (!HandheldRelocation.shelfBasket(handheld).isEmpty()) {
            failures.add("a switch to STOCK should empty the shelf basket");
        }
        menu.editBasket(player, HandheldBasketPayload.add(stacks(Items.COAL, 1), 3));
        HandheldActions.applyMode(player, HandheldModePayload.to(HandheldMode.ZONE));
        menu.editBasket(player, HandheldBasketPayload.add(stacks(Items.STICK, 1), 1));
        Basket<ItemKey> warehouse = HandheldRelocation.basket(handheld);
        if (warehouse.size() != 2 || warehouse.amount(key(Items.APPLE)) != 5 || warehouse.amount(key(Items.COAL)) != 3) {
            failures.add("STOCK should edit the warehouse basket, which keeps its apples, and ZONE nothing, got " + warehouse);
        }
        helper.assertTrue(failures.isEmpty(), String.join(", ", failures));
        helper.succeed();
    }

    /** A mode switch ends pick target mode, drops the relocation, empties the shelf basket and keeps the warehouse basket. */
    @ServusGameTest(timeoutTicks = 20)
    public static void modeSwitchDropsTheRelocationAndKeepsTheWarehouseBasket(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.INFO);
        player.setItemInHand(InteractionHand.MAIN_HAND, handheld);
        BlockPos shelf = helper.absolutePos(SOURCE_A);
        fill(player, handheld, null, stacks(Items.BREAD, 64));
        fill(player, handheld, shelf, stacks(Items.APPLE, 3));
        handheld.set(ServusShuttlesComponents.HANDHELD_RELOCATION.get(), ShelfBasket.of(shelf, Basket.<ItemKey>empty().add(COBBLESTONE, 9)));
        handheld.set(ServusShuttlesComponents.HANDHELD_PICK_TARGET.get(), true);
        HandheldActions.applyMode(player, HandheldModePayload.step(1));
        helper.assertTrue(HandheldActions.mode(handheld) == HandheldMode.STOCK, "the handheld is in STOCK");
        helper.assertTrue(!HandheldRelocation.pickingTarget(handheld), "pick target mode ended");
        helper.assertTrue(HandheldRelocation.relocation(handheld) == null, "the relocation is dropped");
        helper.assertTrue(HandheldRelocation.shelfBasket(handheld).isEmpty(), "the shelf basket is empty");
        helper.assertTrue(HandheldRelocation.basket(handheld).amount(BREAD) == 64, "the warehouse basket still holds 64 bread");
        helper.succeed();
    }

    /**
     * Relocate needs INFO on a scanned shelf, shelf basket entries and a binding; it stashes the shelf basket as the
     * relocation and empties it. STOCK never relocates.
     */
    @ServusGameTest(timeoutTicks = 120)
    public static void relocateNeedsAShelfBasketAndABinding(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        Waits.when(helper, () -> controller.network().isPresent(), 100, () -> "the warehouse should form", () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos shelf = shelfKey(helper, SOURCE_A);
            ItemStack empty = boundHandheld(player, controller);
            helper.assertTrue(!HandheldRelocation.relocate(player, empty, shelf) && !HandheldRelocation.pickingTarget(empty),
                    "an empty shelf basket does not relocate");
            ItemStack unbound = ZoneFixtures.handheld(HandheldMode.INFO);
            fill(player, unbound, shelf, stacks(Items.BREAD, 64));
            helper.assertTrue(!HandheldRelocation.relocate(player, unbound, shelf) && !HandheldRelocation.pickingTarget(unbound),
                    "an unbound handheld does not relocate");
            ItemStack stock = boundHandheld(player, controller);
            player.setItemInHand(InteractionHand.MAIN_HAND, stock);
            scanned(helper, stock, SOURCE_A);
            fill(player, stock, null, stacks(Items.BREAD, 64));
            fill(player, stock, shelf, stacks(Items.BREAD, 64));
            stock.set(ServusShuttlesComponents.HANDHELD_MODE.get(), HandheldMode.STOCK);
            helper.assertTrue(!menu(player).relocate(player) && !HandheldRelocation.relocate(player, stock, shelf)
                    && !HandheldRelocation.pickingTarget(stock), "STOCK does not relocate");
            ItemStack ready = boundHandheld(player, controller);
            player.setItemInHand(InteractionHand.MAIN_HAND, ready);
            scanned(helper, ready, SOURCE_A);
            fill(player, ready, shelf, stacks(Items.BREAD, 64), stacks(Items.APPLE, 2));
            helper.assertTrue(menu(player).relocate(player) && HandheldRelocation.pickingTarget(ready),
                    "a bound handheld with shelf basket entries waits for the target");
            ShelfBasket relocation = HandheldRelocation.relocation(ready);
            helper.assertTrue(relocation != null && relocation.shelf().equals(shelf) && relocation.entries().size() == 2,
                    "the relocation holds the two entries of the source shelf, got " + relocation);
            helper.assertTrue(HandheldRelocation.shelfBasket(ready).isEmpty(), "Relocate empties the shelf basket");
            helper.succeed();
        });
    }

    /** Scanning an empty shelf in pick target mode ends it and shuttles carry the stacks there from the source shelf only. */
    @ServusGameTest(timeoutTicks = 700)
    public static void scanCarriesFromTheSourceShelfOnly(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        ItemStack stack = stacks(Items.COBBLESTONE, 64);
        WarehouseFixtures.stock(helper, SOURCE_A, stack.copy(), stack.copy(), stack.copy());
        WarehouseFixtures.stock(helper, SOURCE_B, stack.copy(), stacks(Items.COBBLESTONE, 44));
        whenIndexed(helper, controller, COBBLESTONE, 300, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack handheld = picking(helper, player, controller, SOURCE_A, stacks(Items.COBBLESTONE, 128));
            HandheldActions.scan(player, handheld, rack(helper, TARGET[1]));
            helper.assertTrue(!HandheldRelocation.pickingTarget(handheld), "the scan ends pick target mode");
            helper.assertTrue(HandheldRelocation.relocation(handheld) == null, "the scan clears the relocation");
            helper.assertTrue(lockedIn(helper) == 128, "room for 128 is locked on the shelf, got " + lockedIn(helper));
            Waits.when(helper, () -> onTarget(helper, Items.COBBLESTONE) == 128 && targetUnlocked(helper), 600,
                    () -> "128 cobblestone should arrive on the shelf, has " + onTarget(helper, Items.COBBLESTONE), () -> {
                int fromA = WarehouseFixtures.count(rack(helper, SOURCE_A), Items.COBBLESTONE);
                int fromB = WarehouseFixtures.count(rack(helper, SOURCE_B), Items.COBBLESTONE);
                helper.assertTrue(fromA == 64 && fromB == 108, "64 stay on the source and 108 untouched on the other shelf, got "
                        + fromA + " and " + fromB);
                helper.succeed();
            });
        });
    }

    /** A shelf whose zone takes only raw copper moves the raw copper and refuses the bread. */
    @ServusGameTest(timeoutTicks = 500)
    public static void zoneFilterRefusesAnItem(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.RAW_COPPER, 64), stacks(Items.BREAD, 64));
        int ores = ZoneFixtures.zone(helper, "Ores", new ZoneFilter(List.of(new ItemStack(Items.RAW_COPPER)), List.of()));
        ShelfZones.set(helper.getLevel(), helper.absolutePos(TARGET[0]), ores);
        whenIndexed(helper, controller, BREAD, 64, () -> helper.runAfterDelay(2, () -> {
            Map<ItemKey, Integer> wanted = new LinkedHashMap<>();
            wanted.put(key(Items.RAW_COPPER), 64);
            wanted.put(BREAD, 64);
            Relocation relocation = controller.network().orElseThrow().relocate(shelfOf(helper, SOURCE_A), targetShelf(helper), wanted);
            helper.assertTrue(relocation.started(), "the raw copper starts, got " + relocation.outcome());
            Relocation.Item copper = relocation.items().get(0);
            Relocation.Item bread = relocation.items().get(1);
            helper.assertTrue(!copper.refused() && copper.moved() == 64, "64 raw copper move, got " + copper);
            helper.assertTrue(bread.refused() && bread.moved() == 0, "the bread is refused, got " + bread);
            Waits.when(helper, () -> onTarget(helper, Items.RAW_COPPER) == 64, 450,
                    () -> "the raw copper should arrive, has " + onTarget(helper, Items.RAW_COPPER), () -> {
                helper.assertTrue(WarehouseFixtures.count(rack(helper, SOURCE_A), Items.BREAD) == 64, "the bread stays where it is");
                helper.succeed();
            });
        }));
    }

    /** A shelf without room for everything moves nothing, places no lock and keeps pick target mode. */
    @ServusGameTest(timeoutTicks = 200)
    public static void noRoomMovesNothing(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.COBBLESTONE, 64), stacks(Items.COBBLESTONE, 64));
        for (BlockPos bay : TARGET) {
            WarehouseFixtures.fill(rack(helper, bay), Items.DIRT);
        }
        rack(helper, TARGET[0]).items().setStackInSlot(0, ItemStack.EMPTY);
        whenIndexed(helper, controller, COBBLESTONE, 128, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack handheld = picking(helper, player, controller, SOURCE_A, stacks(Items.COBBLESTONE, 128));
            HandheldActions.scan(player, handheld, rack(helper, TARGET[0]));
            helper.assertTrue(HandheldRelocation.pickingTarget(handheld), "the handheld stays in pick target mode");
            helper.assertTrue(HandheldRelocation.relocation(handheld) != null, "a refusal keeps the relocation");
            helper.assertTrue(targetUnlocked(helper), "no lock on the shelf");
            helper.assertTrue(rack(helper, SOURCE_A).items().locked().isEmpty(), "no lock on the source");
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.queuedJobs() == 0 && network.ridingJobs() == 0, "no shuttle job starts");
            helper.succeed();
        });
    }

    /** An amount is an upper bound: 40 of 128 bread move from the source and 88 are short, though another shelf holds bread. */
    @ServusGameTest(timeoutTicks = 500)
    public static void shortfallIsReportedFromTheSource(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.BREAD, 40));
        WarehouseFixtures.stock(helper, SOURCE_B, stacks(Items.BREAD, 64));
        whenIndexed(helper, controller, BREAD, 104, () -> {
            Relocation relocation = relocate(helper, controller, SOURCE_A, BREAD, 128);
            Relocation.Item bread = relocation.items().getFirst();
            helper.assertTrue(relocation.started() && bread.moved() == 40 && bread.missing() == 88,
                    "40 move and 88 are short, got " + relocation);
            Waits.when(helper, () -> onTarget(helper, Items.BREAD) == 40, 450,
                    () -> "40 bread should arrive, has " + onTarget(helper, Items.BREAD), () -> {
                helper.assertTrue(WarehouseFixtures.count(rack(helper, SOURCE_B), Items.BREAD) == 64, "the other shelf keeps its bread");
                helper.succeed();
            });
        });
    }

    /** The source shelf is no target, and an item gone from the source is short even when another shelf holds it. */
    @ServusGameTest(timeoutTicks = 200)
    public static void sourceShelfIsNoTarget(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.BREAD, 64));
        WarehouseFixtures.stock(helper, SOURCE_B, stacks(Items.STONE, 64));
        ItemKey stone = key(Items.STONE);
        whenIndexed(helper, controller, stone, 64, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack handheld = picking(helper, player, controller, SOURCE_A, stacks(Items.BREAD, 32));
            HandheldActions.scan(player, handheld, rack(helper, SOURCE_A));
            helper.assertTrue(HandheldRelocation.pickingTarget(handheld), "the source shelf as target keeps pick target mode");
            Map<ItemKey, Integer> wanted = Map.of(BREAD, 32);
            Relocation self = controller.network().orElseThrow().relocate(shelfOf(helper, SOURCE_A), shelfOf(helper, SOURCE_A), wanted);
            helper.assertTrue(self.outcome() == Relocation.Outcome.NOT_A_TARGET, "the source shelf is no target, got " + self.outcome());
            Relocation gone = relocate(helper, controller, SOURCE_A, stone, 32);
            helper.assertTrue(gone.outcome() == Relocation.Outcome.NOTHING_TO_MOVE && gone.items().getFirst().missing() == 32,
                    "no stone moves and 32 are short, got " + gone);
            helper.assertTrue(targetUnlocked(helper) && rack(helper, SOURCE_A).items().locked().isEmpty(), "nothing is locked");
            helper.succeed();
        });
    }

    /** A pick whose source ran dry before the shuttle came releases its room on the shelf. */
    @ServusGameTest(timeoutTicks = 500)
    public static void shortPickReleasesTheShelf(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.COBBLESTONE, 64));
        whenIndexed(helper, controller, COBBLESTONE, 64, () -> {
            helper.assertTrue(relocate(helper, controller, SOURCE_A, COBBLESTONE, 64).started(), "the relocation starts");
            helper.assertTrue(lockedIn(helper) == 64, "room for 64 is locked, got " + lockedIn(helper));
            HeavyRackBlockEntity source = rack(helper, SOURCE_A);
            for (int slot = 0; slot < source.items().getSlots(); slot++) {
                source.items().setStackInSlot(slot, ItemStack.EMPTY);
            }
            WarehouseNetwork network = controller.network().orElseThrow();
            Waits.when(helper, () -> targetUnlocked(helper) && network.queuedJobs() == 0 && network.ridingJobs() == 0, 450,
                    () -> "the shelf's locks should be released, " + lockedIn(helper) + " still locked", () -> {
                helper.assertTrue(onTarget(helper, Items.COBBLESTONE) == 0, "nothing arrives");
                helper.succeed();
            });
        });
    }

    /** A Fluid Rack or a rack outside the bound warehouse is no target: the handheld stays in pick target mode. */
    @ServusGameTest(timeoutTicks = 200)
    public static void notATargetKeepsPickTargetMode(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        BlockPos[] fluidBays = {new BlockPos(2, 1, 3), new BlockPos(3, 1, 3)};
        FluidRackFixtures.row(helper, Direction.NORTH, fluidBays);
        BlockPos lone = new BlockPos(8, 1, 3);
        HeavyRackBlockEntity loneRack = WarehouseFixtures.rack(helper, lone, Direction.NORTH);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.BREAD, 64));
        whenIndexed(helper, controller, BREAD, 64, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack handheld = picking(helper, player, controller, SOURCE_A, stacks(Items.BREAD, 64));
            List<String> failures = new ArrayList<>();
            HandheldActions.scan(player, handheld, FluidRackFixtures.bay(helper, fluidBays[0]));
            if (!HandheldRelocation.pickingTarget(handheld)) {
                failures.add("the Fluid Rack ended pick target mode");
            }
            HandheldActions.scan(player, handheld, loneRack);
            if (!HandheldRelocation.pickingTarget(handheld)) {
                failures.add("the rack of no warehouse ended pick target mode");
            }
            helper.assertTrue(failures.isEmpty(), String.join(", ", failures));
            helper.assertTrue(loneRack.items().locked().isEmpty() && targetUnlocked(helper), "nothing is locked");
            helper.succeed();
        });
    }
}
