package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.handheld.Basket;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldActions;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBasketPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldModePayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldRelocation;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
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

/** The handheld's FIND basket and relocating it onto a target shelf by shuttle. */
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

    /** A FIND handheld bound to the controller with the stacks in its basket. */
    private static ItemStack boundHandheld(Player player, WarehouseControllerBlockEntity controller, ItemStack... basket) {
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.FIND);
        HandheldActions.bind(player, handheld, controller);
        for (ItemStack stack : basket) {
            HandheldRelocation.edit(player, handheld, HandheldBasketPayload.add(stack, stack.getCount()));
        }
        return handheld;
    }

    private static Relocation relocate(GameTestHelper helper, WarehouseControllerBlockEntity controller, ItemKey key, int amount) {
        Map<ItemKey, Integer> wanted = new LinkedHashMap<>();
        wanted.put(key, amount);
        return controller.network().orElseThrow().relocate(targetShelf(helper), wanted);
    }

    private static ItemStack stacks(Item item, int count) {
        return new ItemStack(item, count);
    }

    /** Basket edits add and merge, set, remove and clear; a tenth item is refused. */
    @ServusGameTest(timeoutTicks = 20)
    public static void payloadsBuildABasket(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.FIND);
        HandheldRelocation.edit(player, handheld, HandheldBasketPayload.add(stacks(Items.COBBLESTONE, 1), 128));
        HandheldRelocation.edit(player, handheld, HandheldBasketPayload.add(stacks(Items.BREAD, 1), 64));
        HandheldRelocation.edit(player, handheld, HandheldBasketPayload.add(stacks(Items.BREAD, 1), 32));
        Basket<ItemKey> basket = HandheldRelocation.basket(handheld);
        helper.assertTrue(basket.size() == 2 && basket.amount(COBBLESTONE) == 128 && basket.amount(BREAD) == 96,
                "the basket holds 128 cobblestone and 96 bread, got " + basket);
        HandheldRelocation.edit(player, handheld, HandheldBasketPayload.set(stacks(Items.BREAD, 1), 10));
        helper.assertTrue(HandheldRelocation.basket(handheld).amount(BREAD) == 10, "set makes it 10 bread");
        HandheldRelocation.edit(player, handheld, HandheldBasketPayload.remove(stacks(Items.BREAD, 1)));
        helper.assertTrue(HandheldRelocation.basket(handheld).amount(BREAD) == 0, "remove drops the bread");
        List<Item> more = List.of(Items.STONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.OAK_LOG, Items.APPLE, Items.STICK, Items.COAL);
        for (Item item : more) {
            HandheldRelocation.edit(player, handheld, HandheldBasketPayload.add(stacks(item, 1), 1));
        }
        HandheldRelocation.edit(player, handheld, HandheldBasketPayload.add(stacks(Items.FLINT, 1), 1));
        basket = HandheldRelocation.basket(handheld);
        helper.assertTrue(basket.size() == Basket.MAX_ENTRIES && basket.amount(key(Items.FLINT)) == 0,
                "a full basket refuses a tenth item, got " + basket.size() + " entries");
        HandheldRelocation.edit(player, handheld, HandheldBasketPayload.clear());
        helper.assertTrue(HandheldRelocation.basket(handheld).isEmpty() && !handheld.has(ServusShuttlesComponents.HANDHELD_BASKET.get()),
                "clear empties the basket");
        helper.succeed();
    }

    /** A mode switch ends pick target mode and keeps the basket. */
    @ServusGameTest(timeoutTicks = 20)
    public static void modeSwitchLeavesPickTargetAndKeepsTheBasket(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack handheld = ZoneFixtures.handheld(HandheldMode.FIND);
        player.setItemInHand(InteractionHand.MAIN_HAND, handheld);
        HandheldRelocation.edit(player, handheld, HandheldBasketPayload.add(stacks(Items.BREAD, 1), 64));
        handheld.set(ServusShuttlesComponents.HANDHELD_PICK_TARGET.get(), true);
        HandheldActions.applyMode(player, HandheldModePayload.step(1));
        helper.assertTrue(HandheldActions.mode(handheld) == HandheldMode.INFO, "the handheld is in INFO");
        helper.assertTrue(!HandheldRelocation.pickingTarget(handheld), "pick target mode ended");
        helper.assertTrue(HandheldRelocation.basket(handheld).amount(BREAD) == 64, "the basket still holds 64 bread");
        helper.succeed();
    }

    /** Relocate needs entries in the basket and a binding; without either nothing changes. */
    @ServusGameTest(timeoutTicks = 120)
    public static void relocateNeedsEntriesAndABinding(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        Waits.when(helper, () -> controller.network().isPresent(), 100, () -> "the warehouse should form", () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack empty = boundHandheld(player, controller);
            helper.assertTrue(!HandheldRelocation.relocate(player, empty) && !HandheldRelocation.pickingTarget(empty),
                    "an empty basket does not relocate");
            ItemStack unbound = ZoneFixtures.handheld(HandheldMode.FIND);
            HandheldRelocation.edit(player, unbound, HandheldBasketPayload.add(stacks(Items.BREAD, 1), 64));
            helper.assertTrue(!HandheldRelocation.relocate(player, unbound) && !HandheldRelocation.pickingTarget(unbound),
                    "an unbound handheld does not relocate");
            ItemStack ready = boundHandheld(player, controller, stacks(Items.BREAD, 64));
            helper.assertTrue(HandheldRelocation.relocate(player, ready) && HandheldRelocation.pickingTarget(ready),
                    "a bound handheld with entries waits for the target");
            helper.succeed();
        });
    }

    /** Scanning an empty shelf in pick target mode clears the flag and shuttles carry the basket there in stacks. */
    @ServusGameTest(timeoutTicks = 700)
    public static void scanCarriesTheBasketOntoTheShelf(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        ItemStack stack = stacks(Items.COBBLESTONE, 64);
        WarehouseFixtures.stock(helper, SOURCE_A, stack.copy(), stack.copy(), stack.copy());
        WarehouseFixtures.stock(helper, SOURCE_B, stack.copy(), stacks(Items.COBBLESTONE, 44));
        whenIndexed(helper, controller, COBBLESTONE, 300, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack handheld = boundHandheld(player, controller, stacks(Items.COBBLESTONE, 128));
            helper.assertTrue(HandheldRelocation.relocate(player, handheld), "Relocate starts pick target mode");
            HandheldActions.scan(player, handheld, rack(helper, TARGET[1]));
            helper.assertTrue(!HandheldRelocation.pickingTarget(handheld), "the scan ends pick target mode");
            helper.assertTrue(lockedIn(helper) == 128, "room for 128 is locked on the shelf, got " + lockedIn(helper));
            Waits.when(helper, () -> onTarget(helper, Items.COBBLESTONE) == 128 && targetUnlocked(helper), 600,
                    () -> "128 cobblestone should arrive on the shelf, has " + onTarget(helper, Items.COBBLESTONE), () -> {
                int left = WarehouseFixtures.count(rack(helper, SOURCE_A), Items.COBBLESTONE)
                        + WarehouseFixtures.count(rack(helper, SOURCE_B), Items.COBBLESTONE);
                helper.assertTrue(left == 172, "172 stay on the other shelves, got " + left);
                helper.assertTrue(HandheldRelocation.basket(handheld).amount(COBBLESTONE) == 128, "the basket is kept");
                helper.succeed();
            });
        });
    }

    /** A shelf whose zone takes only raw copper moves the raw copper and refuses the bread. */
    @ServusGameTest(timeoutTicks = 500)
    public static void zoneFilterRefusesAnItem(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.RAW_COPPER, 64));
        WarehouseFixtures.stock(helper, SOURCE_B, stacks(Items.BREAD, 64));
        int ores = ZoneFixtures.zone(helper, "Ores", new ZoneFilter(List.of(new ItemStack(Items.RAW_COPPER)), List.of()));
        ShelfZones.set(helper.getLevel(), helper.absolutePos(TARGET[0]), ores);
        whenIndexed(helper, controller, BREAD, 64, () -> helper.runAfterDelay(2, () -> {
            Map<ItemKey, Integer> wanted = new LinkedHashMap<>();
            wanted.put(key(Items.RAW_COPPER), 64);
            wanted.put(BREAD, 64);
            Relocation relocation = controller.network().orElseThrow().relocate(targetShelf(helper), wanted);
            helper.assertTrue(relocation.started(), "the raw copper starts, got " + relocation.outcome());
            Relocation.Item copper = relocation.items().get(0);
            Relocation.Item bread = relocation.items().get(1);
            helper.assertTrue(!copper.refused() && copper.moved() == 64, "64 raw copper move, got " + copper);
            helper.assertTrue(bread.refused() && bread.moved() == 0, "the bread is refused, got " + bread);
            Waits.when(helper, () -> onTarget(helper, Items.RAW_COPPER) == 64, 450,
                    () -> "the raw copper should arrive, has " + onTarget(helper, Items.RAW_COPPER), () -> {
                helper.assertTrue(WarehouseFixtures.count(rack(helper, SOURCE_B), Items.BREAD) == 64, "the bread stays where it is");
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
            ItemStack handheld = boundHandheld(player, controller, stacks(Items.COBBLESTONE, 128));
            HandheldRelocation.relocate(player, handheld);
            HandheldActions.scan(player, handheld, rack(helper, TARGET[0]));
            helper.assertTrue(HandheldRelocation.pickingTarget(handheld), "the handheld stays in pick target mode");
            helper.assertTrue(targetUnlocked(helper), "no lock on the shelf");
            helper.assertTrue(rack(helper, SOURCE_A).items().locked().isEmpty(), "no lock on the source");
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.queuedJobs() == 0 && network.ridingJobs() == 0, "no shuttle job starts");
            helper.succeed();
        });
    }

    /** An amount is an upper bound: 40 of 128 bread move and 88 are reported short. */
    @ServusGameTest(timeoutTicks = 500)
    public static void shortfallIsReported(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.BREAD, 40));
        whenIndexed(helper, controller, BREAD, 40, () -> {
            Relocation relocation = relocate(helper, controller, BREAD, 128);
            Relocation.Item bread = relocation.items().getFirst();
            helper.assertTrue(relocation.started() && bread.moved() == 40 && bread.missing() == 88,
                    "40 move and 88 are short, got " + relocation);
            Waits.when(helper, () -> onTarget(helper, Items.BREAD) == 40, 450,
                    () -> "40 bread should arrive, has " + onTarget(helper, Items.BREAD), helper::succeed);
        });
    }

    /** Stock lying on the target shelf never counts: with all stone there, nothing moves and the stone is short. */
    @ServusGameTest(timeoutTicks = 200)
    public static void stockOnTheTargetIsNotUsed(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, TARGET[0], stacks(Items.STONE, 64));
        ItemKey stone = key(Items.STONE);
        whenIndexed(helper, controller, stone, 64, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack handheld = boundHandheld(player, controller, stacks(Items.STONE, 32));
            HandheldRelocation.relocate(player, handheld);
            HandheldActions.scan(player, handheld, rack(helper, TARGET[1]));
            helper.assertTrue(HandheldRelocation.pickingTarget(handheld), "with nothing to move the handheld stays in pick target mode");
            Relocation relocation = relocate(helper, controller, stone, 32);
            helper.assertTrue(relocation.outcome() == Relocation.Outcome.NOTHING_TO_MOVE && relocation.items().getFirst().missing() == 32,
                    "no stone moves and 32 are short, got " + relocation);
            helper.assertTrue(targetUnlocked(helper), "no lock on the shelf");
            helper.succeed();
        });
    }

    /** A pick whose source ran dry before the shuttle came releases its room on the shelf. */
    @ServusGameTest(timeoutTicks = 500)
    public static void shortPickReleasesTheShelf(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = warehouse(helper);
        WarehouseFixtures.stock(helper, SOURCE_A, stacks(Items.COBBLESTONE, 64));
        whenIndexed(helper, controller, COBBLESTONE, 64, () -> {
            helper.assertTrue(relocate(helper, controller, COBBLESTONE, 64).started(), "the relocation starts");
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
            ItemStack handheld = boundHandheld(player, controller, stacks(Items.BREAD, 64));
            HandheldRelocation.relocate(player, handheld);
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
