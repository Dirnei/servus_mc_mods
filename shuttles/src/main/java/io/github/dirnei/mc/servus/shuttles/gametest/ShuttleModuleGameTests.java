package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialHolder;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleWorkshopBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The Shuttle Workshop, shuttle tiers and upgrades, and how the fleet uses them. */
public final class ShuttleModuleGameTests {

    private static final BlockPos WS_POS = new BlockPos(5, 1, 5);
    /** Menu index of hotbar slot 0. */
    private static final int HOTBAR = ShuttleWorkshopMenu.INVENTORY_START + 27;

    private ShuttleModuleGameTests() {
    }

    /** A workshop at the position and its menu as the server opens it. */
    static ShuttleWorkshopMenu menu(GameTestHelper helper, Player player, BlockPos pos) {
        helper.setBlock(pos, ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get().defaultBlockState());
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(pos, ShuttleWorkshopBlockEntity.class);
        return new ShuttleWorkshopMenu(1, player.getInventory(), workshop, ShuttlesStatsManager.INSTANCE.shuttleTiers());
    }

    private static ShuttleWorkshopMenu workshop(GameTestHelper helper, Player player) {
        return menu(helper, player, WS_POS);
    }

    private static ItemStack speedUpgrades(int count) {
        return new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get(), count);
    }

    private static ItemStack shuttle(Item tier, int speed, int handling, Optional<Item> tool) {
        ItemStack shuttle = new ItemStack(tier);
        ShuttleModules.set(shuttle, new ShuttleModules(tool, speed, handling));
        return shuttle;
    }

    private static int count(Player player, Item item) {
        return player.getInventory().countItem(item);
    }

    /** A stack of five Speed upgrades on a Mk1 shuttle: two go in, three stay with the player. */
    @ServusGameTest
    public static void installStackUpToMax(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        player.getInventory().setItem(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        player.getInventory().setItem(1, speedUpgrades(5));
        menu.quickMoveStack(player, HOTBAR);
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).speed() == 2, "Mk1 takes two Speed upgrades");
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 3, "three stay with the player");

        ItemStack handling = new ItemStack(ServusShuttlesItems.HANDLING_UPGRADE.get(), 4);
        ItemStack rest = menu.getSlot(ShuttleWorkshopMenu.INSTALL_SLOT).safeInsert(handling);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).handling() == 2, "a click installs up to the max too");
        helper.assertTrue(rest.getCount() == 2, "the rest stays on the cursor, got " + rest.getCount());
        helper.assertTrue(menu.getSlot(ShuttleWorkshopMenu.INSTALL_SLOT).getItem().isEmpty(), "the install slot stays empty");
        helper.succeed();
    }

    /** Nothing installs without a shuttle, and only upgrades and tools install at all. */
    @ServusGameTest
    public static void installRefusesWithoutShuttleOrUpgrade(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        helper.assertFalse(menu.getSlot(ShuttleWorkshopMenu.INSTALL_SLOT).mayPlace(speedUpgrades(1)), "no install without a shuttle");
        player.getInventory().setItem(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get(), 5));
        menu.quickMoveStack(player, HOTBAR);
        helper.assertTrue(menu.shuttle().getCount() == 1, "one shuttle goes in");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 4, "four stay in the inventory");
        helper.assertFalse(menu.getSlot(ShuttleWorkshopMenu.INSTALL_SLOT).mayPlace(new ItemStack(Items.COBBLESTONE)), "cobblestone is no upgrade");
        player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 10));
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).isEmpty(), "the shuttle got nothing");
        helper.assertTrue(menu.supplied("minecraft:cobblestone") == 10, "other items go to the supplies");
        helper.succeed();
    }

    /** A tool goes in once; the same tool again does not, and the remove button gives it back. */
    @ServusGameTest
    public static void toolInstallsOnce(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        player.getInventory().setItem(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        player.getInventory().setItem(1, new ItemStack(ServusShuttlesItems.HARVESTER.get(), 2));
        menu.quickMoveStack(player, HOTBAR);
        menu.quickMoveStack(player, HOTBAR + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).tool().equals(Optional.of(ServusShuttlesItems.HARVESTER.get())), "the Harvester is in");
        helper.assertTrue(count(player, ServusShuttlesItems.HARVESTER.get()) == 1, "one Harvester went in");
        helper.assertFalse(menu.getSlot(ShuttleWorkshopMenu.INSTALL_SLOT).mayPlace(new ItemStack(ServusShuttlesItems.HARVESTER.get())),
                "the same tool does not go in twice");
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_REMOVE_TOOL);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).tool().isEmpty(), "the tool is out");
        helper.assertTrue(count(player, ServusShuttlesItems.HARVESTER.get()) == 2, "the Harvester is back");
        helper.succeed();
    }

    /** The remove buttons take one count at a time and hand the item back; a bare shuttle stacks with a new one. */
    @ServusGameTest
    public static void removeButtonsGiveItemsBack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        player.getInventory().setItem(0, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 2, 1, Optional.empty()));
        menu.quickMoveStack(player, HOTBAR);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_REMOVE_SPEED);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).speed() == 1, "one Speed upgrade left");
        helper.assertTrue(count(player, ServusFoundationItems.SPEED_UPGRADE.get()) == 1, "one Speed Upgrade item back");
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_REMOVE_SPEED);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_REMOVE_HANDLING);
        helper.assertFalse(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_REMOVE_HANDLING), "nothing left to take");
        helper.assertTrue(count(player, ServusShuttlesItems.HANDLING_UPGRADE.get()) == 1, "the Handling Upgrade is back");
        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.shuttle(), new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get())),
                "a shuttle without upgrades is a plain Mk1 again");
        helper.succeed();
    }

    /** Build waits for every supply; Skip then turns Mk1 into Mk2 with its upgrades and tool and leaves extras. */
    @ServusGameTest
    public static void buildAndSkipNeedSuppliesAndKeepUpgrades(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 2, 1, Optional.of(ServusShuttlesItems.HARVESTER.get())));
        ShuttleTiers.Tier mk2 = ShuttlesStatsManager.INSTANCE.shuttleTiers().tier(2);
        int slot = 0;
        for (ShuttleTiers.Supply supply : mk2.build()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(supply.id()));
            int amount = slot == 0 ? supply.count() - 1 : supply.count();
            workshop.supplies().setStackInSlot(slot++, new ItemStack(item, amount));
        }
        helper.assertFalse(menu.canBuild(), "one supply is short");
        helper.assertFalse(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD), "Build is off while short");
        workshop.supplies().getStackInSlot(0).grow(5);
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD), "Build starts once complete");
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_SKIP), "Skip finishes the build");
        ItemStack built = menu.shuttle();
        helper.assertTrue(built.is(ServusShuttlesItems.SHUTTLE_MK2.get()), "the shuttle is a Mk2 now");
        ShuttleModules modules = ShuttleModules.of(built);
        helper.assertTrue(modules.speed() == 2 && modules.handling() == 1 && modules.tool().isPresent(), "upgrades and tool stay");
        helper.assertTrue(workshop.supplies().getStackInSlot(0).getCount() == 4, "the extra supplies stay, got "
                + workshop.supplies().getStackInSlot(0).getCount());
        helper.assertTrue(workshop.supplies().getStackInSlot(1).isEmpty(), "the used supplies are gone");
        helper.assertTrue(menu.install(speedUpgrades(4)) == 2, "a Mk2 takes up to four Speed upgrades");
        helper.succeed();
    }

    /** The workshop keeps its shuttle and supplies after the GUI closes and drops them when broken. */
    @ServusGameTest
    public static void workshopKeepsAndDropsContents(GameTestHelper helper) {
        Player player = TestPlayers.quiet(helper);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        player.getInventory().setItem(0, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 1, 0, Optional.empty()));
        player.getInventory().setItem(1, new ItemStack(Items.GOLD_INGOT, 7));
        menu.quickMoveStack(player, HOTBAR);
        menu.quickMoveStack(player, HOTBAR + 1);
        menu.removed(player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        helper.assertFalse(workshop.shuttle().getStackInSlot(0).isEmpty(), "the shuttle stays in the workshop");
        helper.assertTrue(ShuttleWorkshopBlockEntity.countSupplies(workshop.supplies()).get("minecraft:gold_ingot") == 7, "the gold stays");
        helper.destroyBlock(WS_POS);
        helper.assertItemEntityPresent(ServusShuttlesItems.SHUTTLE_MK1.get(), WS_POS, 2);
        helper.assertItemEntityPresent(Items.GOLD_INGOT, WS_POS, 2);
        helper.succeed();
    }

    private static final Identifier SPRUCE = BuiltInRegistries.BLOCK.getKey(Blocks.SPRUCE_PLANKS);
    /** Where a dismantled workshop is placed again. */
    private static final BlockPos WS_AGAIN = new BlockPos(9, 1, 9);

    /** Uses the carrot on the block at the position, sneaking or not. */
    private static void carrot(GameTestHelper helper, Player player, BlockPos at, boolean sneak) {
        player.setShiftKeyDown(sneak);
        GameTestSteps.useCarrot(helper, player, at, Direction.NORTH);
        player.setShiftKeyDown(false);
    }

    /** Every item entity around the position. */
    private static List<ItemEntity> itemsAround(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(2));
    }

    /** Places the item on stone at the position like a player does and returns the placed workshop. */
    private static ShuttleWorkshopBlockEntity placeAgain(GameTestHelper helper, Player player, ItemStack item, BlockPos at) {
        helper.setBlock(at.below(), Blocks.STONE.defaultBlockState());
        ItemStack stack = item.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(at.below());
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        helper.assertBlockPresent(ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get(), at);
        return helper.getBlockEntity(at, ShuttleWorkshopBlockEntity.class);
    }

    /** The sneaking carrot drops one spruce workshop item that brings back its shuttle and supplies. */
    @ServusGameTest
    public static void workshopDismantleKeepsContents(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        ((MaterialHolder) workshop).materialSlot().set(SPRUCE);
        workshop.shuttle().setStackInSlot(0, shuttle(ServusShuttlesItems.SHUTTLE_MK2.get(), 1, 0, Optional.empty()));
        workshop.supplies().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
        carrot(helper, player, WS_POS, true);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get(), WS_POS);
        List<ItemEntity> drops = itemsAround(helper, WS_POS);
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ServusShuttlesItems.SHUTTLE_WORKSHOP.get()),
                "exactly one workshop item drops, got " + drops.stream().map(ItemEntity::getItem).toList());
        ItemStack item = drops.get(0).getItem();
        helper.assertTrue(SPRUCE.equals(item.get(MaterialContent.MATERIAL.get())), "the item is spruce, got " + item.get(MaterialContent.MATERIAL.get()));
        drops.get(0).discard();

        ShuttleWorkshopBlockEntity placed = placeAgain(helper, player, item, WS_AGAIN);
        ItemStack held = placed.shuttle().getStackInSlot(0);
        helper.assertTrue(held.is(ServusShuttlesItems.SHUTTLE_MK2.get()) && ShuttleModules.of(held).speed() == 1,
                "the Mk2 with Speed 1 is back, got " + held);
        helper.assertTrue(Integer.valueOf(8).equals(placed.supplyCounts().get("minecraft:iron_ingot")),
                "the 8 iron ingots are back, got " + placed.supplyCounts());
        helper.assertTrue(SPRUCE.equals(placed.materialSlot().material()), "the placed workshop is spruce, got " + placed.materialSlot().material());
        helper.succeed();
    }

    /** Dismantling ends a build by hand; placed again, no build runs and the shuttle keeps its tier. */
    @ServusGameTest
    public static void workshopDismantleCancelsHandBuild(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        supplyFor(workshop, 2);
        helper.assertTrue(workshop.startHandBuild(), "a build by hand starts");
        workshop.reportStep(0);
        workshop.reportStep(1);
        helper.assertTrue(workshop.handBuild() && workshop.buildStep() == Math.min(2, workshop.steps().size()), "the build is at step 2");
        carrot(helper, player, WS_POS, true);
        List<ItemEntity> drops = itemsAround(helper, WS_POS);
        helper.assertTrue(drops.size() == 1, "one item drops, got " + drops.stream().map(ItemEntity::getItem).toList());
        ItemStack item = drops.get(0).getItem();
        drops.get(0).discard();
        ShuttleWorkshopBlockEntity placed = placeAgain(helper, player, item, WS_AGAIN);
        helper.assertFalse(placed.handBuild(), "no build by hand runs");
        helper.assertTrue(placed.shuttle().getStackInSlot(0).is(ServusShuttlesItems.SHUTTLE_MK1.get()), "the shuttle is still a Mk1");
        helper.assertTrue(placed.canBuild(), "the supplies came back too");
        helper.succeed();
    }

    /** The carrot without sneaking leaves the workshop and its contents alone. */
    @ServusGameTest
    public static void workshopPlainCarrotDoesNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        workshop.supplies().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
        carrot(helper, player, WS_POS, false);
        helper.assertBlockPresent(ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get(), WS_POS);
        helper.assertTrue(itemsAround(helper, WS_POS).isEmpty(), "nothing drops");
        helper.assertTrue(workshop.shuttle().getStackInSlot(0).is(ServusShuttlesItems.SHUTTLE_MK1.get())
                && workshop.supplies().getStackInSlot(0).getCount() == 8, "the contents stay");
        helper.succeed();
    }

    /** Fills the supply slots with exactly the sheet that builds the tier. */
    static void supplyFor(ShuttleWorkshopBlockEntity workshop, int tier) {
        int slot = 0;
        for (ShuttleTiers.Supply supply : ShuttlesStatsManager.INSTANCE.shuttleTiers().tier(tier).build()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(supply.id()));
            workshop.supplies().setStackInSlot(slot++, new ItemStack(item, supply.count()));
        }
    }

    /** Reports every step of the running build by hand in order. */
    private static void playAllSteps(ShuttleWorkshopMenu menu, ShuttleWorkshopBlockEntity workshop) {
        for (int step = 0; step < workshop.steps().size(); step++) {
            menu.reportStep(step);
        }
    }

    /** A build by hand locks the shuttle, the supplies and the upgrades; Cancel frees them and uses nothing. */
    @ServusGameTest
    public static void handBuildLocksAndCancels(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 1, 0, Optional.empty()));
        supplyFor(workshop, 2);
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD), "a build by hand starts");
        helper.assertTrue(menu.building(), "the menu sees the build");
        helper.assertTrue(workshop.supplies().extractItem(0, 1, true).isEmpty(), "supplies are locked");
        helper.assertFalse(menu.slots.get(ShuttleWorkshopMenu.SUPPLY_START).mayPickup(player), "the supply slot is locked");
        helper.assertFalse(menu.slots.get(ShuttleWorkshopMenu.SHUTTLE_SLOT).mayPickup(player), "the shuttle slot is locked");
        helper.assertTrue(menu.install(speedUpgrades(1)) == 0, "no upgrade goes in during a build");
        helper.assertFalse(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_REMOVE_SPEED), "no upgrade comes out during a build");
        helper.assertFalse(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD), "Build does not start a second build");
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_CANCEL), "Cancel ends the build");
        helper.assertFalse(workshop.handBuild(), "no build runs after Cancel");
        helper.assertTrue(workshop.shuttle().getStackInSlot(0).is(ServusShuttlesItems.SHUTTLE_MK1.get()), "the shuttle stays a Mk1");
        helper.assertTrue(ShuttleModules.of(workshop.shuttle().getStackInSlot(0)).speed() == 1, "its upgrades stay");
        helper.assertTrue(workshop.canBuild(), "every supply is still there");
        helper.assertTrue(menu.slots.get(ShuttleWorkshopMenu.SUPPLY_START).mayPickup(player), "the slots are free again");
        helper.succeed();
    }

    /** The server only takes the step that is next; an engrave before the last step does nothing. */
    @ServusGameTest
    public static void handBuildStepsInOrder(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        supplyFor(workshop, 2);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        int total = workshop.steps().size();
        helper.assertTrue(total >= 2, "the sheet has steps, got " + total);
        menu.reportStep(2);
        helper.assertTrue(workshop.buildStep() == 0, "a later step is ignored");
        menu.reportStep(0);
        helper.assertTrue(workshop.buildStep() == 1, "the current step counts");
        menu.reportStep(0);
        helper.assertTrue(workshop.buildStep() == 1, "a step reported twice counts once");
        menu.engrave("Berta");
        helper.assertTrue(workshop.handBuild() && workshop.shuttle().getStackInSlot(0).is(ServusShuttlesItems.SHUTTLE_MK1.get()),
                "an engrave before the last step does nothing");
        helper.succeed();
    }

    /** Engrave finishes the build like Skip and puts the cleaned name on the new shuttle. */
    @ServusGameTest
    public static void engraveFinishesWithPlate(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 2, 1, Optional.of(ServusShuttlesItems.HARVESTER.get())));
        supplyFor(workshop, 2);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        playAllSteps(menu, workshop);
        helper.assertTrue(workshop.handDone(), "every step is done");
        menu.engrave("§cBerta");
        ItemStack built = workshop.shuttle().getStackInSlot(0);
        helper.assertTrue(built.is(ServusShuttlesItems.SHUTTLE_MK2.get()), "the shuttle is a Mk2 now");
        helper.assertTrue(ShuttleItem.plate(built).equals(Optional.of("Berta")),
                "the plate reads Berta without the colour code, got " + ShuttleItem.plate(built));
        ShuttleModules modules = ShuttleModules.of(built);
        helper.assertTrue(modules.speed() == 2 && modules.handling() == 1 && modules.tool().isPresent(), "upgrades and tool stay");
        helper.assertFalse(workshop.handBuild(), "the build is over");
        helper.assertTrue(workshop.supplies().getStackInSlot(0).isEmpty(), "the supplies are used");
        helper.succeed();
    }

    /** Skip keeps a plate; a new build by hand replaces it, and an empty name keeps it. */
    @ServusGameTest
    public static void skipKeepsPlate(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        ItemStack named = new ItemStack(ServusShuttlesItems.SHUTTLE_MK2.get());
        ShuttleItem.engrave(named, "Berta");
        workshop.shuttle().setStackInSlot(0, named);
        supplyFor(workshop, 3);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_SKIP), "Skip builds a Mk3");
        ItemStack mk3 = workshop.shuttle().getStackInSlot(0);
        helper.assertTrue(mk3.is(ServusShuttlesItems.SHUTTLE_MK3.get()) && ShuttleItem.plate(mk3).equals(Optional.of("Berta")),
                "the Mk3 keeps Berta");

        ItemStack mk1 = new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get());
        ShuttleItem.engrave(mk1, "Berta");
        workshop.shuttle().setStackInSlot(0, mk1);
        supplyFor(workshop, 2);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        playAllSteps(menu, workshop);
        menu.engrave("Kurt");
        helper.assertTrue(ShuttleItem.plate(workshop.shuttle().getStackInSlot(0)).equals(Optional.of("Kurt")),
                "a new build by hand replaces the plate");

        ItemStack again = new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get());
        ShuttleItem.engrave(again, "Berta");
        workshop.shuttle().setStackInSlot(0, again);
        supplyFor(workshop, 2);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        playAllSteps(menu, workshop);
        menu.engrave("   ");
        ItemStack kept = workshop.shuttle().getStackInSlot(0);
        helper.assertTrue(kept.is(ServusShuttlesItems.SHUTTLE_MK2.get()) && ShuttleItem.plate(kept).equals(Optional.of("Berta")),
                "an empty name finishes the build and keeps the old plate");
        helper.succeed();
    }

    /** Skip on the first step makes the Mk2 at once with the sheet's supplies used and no name to type. */
    @ServusGameTest
    public static void skipOnFirstStepFinishes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), 1, 2, Optional.of(ServusShuttlesItems.HARVESTER.get())));
        supplyFor(workshop, 2);
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD), "Build starts the build");
        helper.assertTrue(workshop.buildStep() == 0, "on step 1");
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_SKIP), "Skip is taken on step 1");
        ItemStack built = workshop.shuttle().getStackInSlot(0);
        helper.assertTrue(built.is(ServusShuttlesItems.SHUTTLE_MK2.get()), "the shuttle is a Mk2 now");
        ShuttleModules modules = ShuttleModules.of(built);
        helper.assertTrue(modules.speed() == 1 && modules.handling() == 2 && modules.tool().isPresent(), "counts and tool stay");
        helper.assertFalse(workshop.handBuild() || workshop.handDone(), "the build is over and no name is asked for");
        for (int i = 0; i < workshop.supplies().getSlots(); i++) {
            helper.assertTrue(workshop.supplies().getStackInSlot(i).isEmpty(), "supply slot " + i + " is used up");
        }
        helper.succeed();
    }

    /** Skip at the name prompt finishes the build and keeps the shuttle's old plate. */
    @ServusGameTest
    public static void skipAtNamePromptKeepsPlate(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        ItemStack named = new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get());
        ShuttleItem.engrave(named, "Berta");
        workshop.shuttle().setStackInSlot(0, named);
        supplyFor(workshop, 2);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        playAllSteps(menu, workshop);
        helper.assertTrue(workshop.handDone(), "the name is asked for");
        helper.assertTrue(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_SKIP), "Skip is taken at the name prompt");
        ItemStack built = workshop.shuttle().getStackInSlot(0);
        helper.assertTrue(built.is(ServusShuttlesItems.SHUTTLE_MK2.get()) && ShuttleItem.plate(built).equals(Optional.of("Berta")),
                "the Mk2 keeps Berta, got " + ShuttleItem.plate(built));
        helper.assertFalse(workshop.handBuild(), "the build is over");
        helper.succeed();
    }

    /** Skip without a running build changes nothing, even with every supply there. */
    @ServusGameTest
    public static void skipWithoutBuildDoesNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        supplyFor(workshop, 2);
        helper.assertFalse(menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_SKIP), "Skip is refused without a build");
        helper.assertTrue(workshop.shuttle().getStackInSlot(0).is(ServusShuttlesItems.SHUTTLE_MK1.get()), "the shuttle stays a Mk1");
        helper.assertTrue(workshop.canBuild(), "every supply is still there");
        helper.assertFalse(workshop.handBuild(), "no build was started");
        helper.succeed();
    }

    /** A build by hand and its step are saved with the workshop and come back on load. */
    @ServusGameTest
    public static void handBuildSurvivesSaveAndLoad(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        supplyFor(workshop, 2);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        menu.reportStep(0);
        menu.reportStep(1);
        CompoundTag saved = workshop.saveWithoutMetadata(helper.getLevel().registryAccess());
        workshop.cancelHandBuild();
        workshop.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(workshop.handBuild(), "the build by hand is back");
        helper.assertTrue(workshop.buildStep() == 2, "on the third step, got step index " + workshop.buildStep());
        helper.assertTrue(workshop.supplies().extractItem(0, 1, true).isEmpty(), "and the supplies are locked again");
        helper.succeed();
    }

    /** Editing the shuttle stack in place keeps the step of a build. */
    @ServusGameTest
    public static void installKeepsBuildStep(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = workshop(helper, player);
        ShuttleWorkshopBlockEntity workshop = helper.getBlockEntity(WS_POS, ShuttleWorkshopBlockEntity.class);
        workshop.shuttle().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        supplyFor(workshop, 2);
        menu.clickMenuButton(player, ShuttleWorkshopMenu.BUTTON_BUILD);
        menu.reportStep(0);
        ItemStack edited = workshop.shuttle().getStackInSlot(0).copy();
        ShuttleModules.set(edited, ShuttleModules.of(edited).withCount(ShuttleTiers.Kind.SPEED, 1));
        workshop.shuttle().setStackInSlot(0, edited);
        helper.assertTrue(workshop.buildStep() == 1, "the step stays after the shuttle stack changes, got " + workshop.buildStep());
        helper.succeed();
    }

    private static final BlockPos FL_CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos FL_TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos FL_GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos FL_RACK = new BlockPos(9, 1, 5);

    /**
     * Controller north of x 1 at z 3, terminal at x 1 z 4, rail along z 4 from x 2 to 9, a garage behind the
     * rail at x 2 and a rack facing the rail at its far end with the given stacks of cobblestone. The shuttle
     * goes into the garage's given slot.
     */
    private static ShuttleGarageBlockEntity fleetSetup(GameTestHelper helper, ItemStack shuttle, int slot, int... stacks) {
        WarehouseFixtures.controller(helper, FL_CONTROLLER);
        TerminalWiring.place(helper, FL_TERMINAL);
        WarehouseFixtures.railRow(helper, 2, 9, 4);
        WarehouseFixtures.garage(helper, FL_GARAGE, Direction.SOUTH, 0);
        WarehouseFixtures.rack(helper, FL_RACK, Direction.NORTH);
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(FL_GARAGE, ShuttleGarageBlockEntity.class);
        garage.shuttles().setStackInSlot(slot, shuttle);
        HeavyRackBlockEntity rack = helper.getBlockEntity(FL_RACK, HeavyRackBlockEntity.class);
        int[] amounts = stacks.length == 0 ? new int[] {64, 36} : stacks;
        for (int i = 0; i < amounts.length; i++) {
            rack.items().insertItem(i, new ItemStack(Items.COBBLESTONE, amounts[i]), false);
        }
        return garage;
    }

    private static WarehouseNetwork requestCobblestone(GameTestHelper helper, int amount) {
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(FL_CONTROLLER, WarehouseControllerBlockEntity.class);
        StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, FL_TERMINAL);
        new StorageTerminalMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), terminal)
                .request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), amount);
        return controller.network().orElseThrow();
    }

    /** A Mk1 shuttle carries two stacks: both pick jobs of a request for 100 ride on one trip. */
    @ServusGameTest(timeoutTicks = 800)
    public static void mk1PicksTwoStacks(GameTestHelper helper) {
        fleetSetup(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()), 0);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = requestCobblestone(helper, 100);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(network.garageOut(helper.absolutePos(FL_GARAGE)) == 1, "the shuttle is out on the picks");
                helper.assertTrue(network.ridingJobs() == 2 && network.queuedJobs() == 0,
                        "both jobs ride the one shuttle, riding " + network.ridingJobs() + ", queued " + network.queuedJobs());
                StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, FL_TERMINAL);
                int[] taken = {0};
                Waits.when(helper, () -> (taken[0] += TerminalWiring.takeOutput(terminal, Items.COBBLESTONE)) == 100, 700,
                        () -> "all 100 should arrive, got " + taken[0], helper::succeed);
            });
        });
    }

    /** More than the tier holds: a Mk1 shuttle takes two of four jobs, the other two wait. */
    @ServusGameTest(timeoutTicks = 200)
    public static void mk1LeavesTheRestQueued(GameTestHelper helper) {
        fleetSetup(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()), 0, 64, 64, 64, 8);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = requestCobblestone(helper, 200);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(network.ridingJobs() == 2 && network.queuedJobs() == 2,
                        "two jobs ride, two wait, riding " + network.ridingJobs() + ", queued " + network.queuedJobs());
                helper.succeed();
            });
        });
    }

    /**
     * The door time, the doorway and seven rails out and back with a stop at
     * each end. The fast shuttle with two Speed upgrades is done by the check,
     * the plain one is not.
     */
    private static void timedTrip(GameTestHelper helper, int speed, boolean doneByCheck) {
        fleetSetup(helper, shuttle(ServusShuttlesItems.SHUTTLE_MK1.get(), speed, 0, Optional.empty()), 0, 30);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = requestCobblestone(helper, 30);
            // Shuttles speed up and brake on every leg, so the check comes a little later than travel alone.
            helper.runAfterDelay(148, () -> {
                int riding = network.ridingJobs();
                helper.assertTrue(doneByCheck ? riding == 0 : riding == 1,
                        (doneByCheck ? "the fast shuttle should have delivered" : "the slow shuttle should still carry the pick") + ", riding " + riding);
                Waits.when(helper, () -> network.ridingJobs() == 0, 200, () -> "the pick should be delivered", helper::succeed);
            });
        });
    }

    /** A plain shuttle takes its stops: travel alone would be done by the check, with both stops it is not. */
    @ServusGameTest(timeoutTicks = 400)
    public static void stopsDelayTheDelivery(GameTestHelper helper) {
        timedTrip(helper, 0, false);
    }

    /** Two Speed upgrades bring the same pick back well before a plain shuttle. */
    @ServusGameTest(timeoutTicks = 400)
    public static void speedShortensTheTrip(GameTestHelper helper) {
        timedTrip(helper, 2, true);
    }

    /** The garage locks the slot of the shuttle that is out, not the first filled slot. */
    @ServusGameTest(timeoutTicks = 200)
    public static void garageLocksSlotOfShuttleOut(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = fleetSetup(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()), 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            requestCobblestone(helper, 30);
            helper.runAfterDelay(8, () -> {
                garage.shuttles().setStackInSlot(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
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

    private static final BlockPos PW_CONTROLLER = new BlockPos(1, 1, 1);
    private static final BlockPos PW_STATION = new BlockPos(3, 1, 2);
    private static final BlockPos PW_RACK = new BlockPos(6, 1, 2);
    private static final BlockPos PW_GARAGE = new BlockPos(10, 1, 2);

    /**
     * A Mk2 shuttle loads three stacks of the station at once and puts them away on one trip. The garage stands
     * far away at x 10, so the station's stacks are all queued before the shuttle arrives.
     */
    @ServusGameTest(timeoutTicks = 500)
    public static void mk2PutAwayLoadsThree(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, PW_CONTROLLER);
        WarehouseFixtures.railRow(helper, 2, 10, 1);
        WarehouseFixtures.inbound(helper, PW_STATION, Direction.NORTH);
        WarehouseFixtures.rack(helper, PW_RACK, Direction.NORTH);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, PW_GARAGE, Direction.SOUTH, 0);
        helper.assertTrue(garage.addShuttle(new ItemStack(ServusShuttlesItems.SHUTTLE_MK2.get())), "the garage takes the Mk2 shuttle");
        InboundStationBlockEntity station = helper.getBlockEntity(PW_STATION, InboundStationBlockEntity.class);
        HeavyRackBlockEntity rack = helper.getBlockEntity(PW_RACK, HeavyRackBlockEntity.class);
        helper.runAfterDelay(10, () -> {
            for (int i = 0; i < 3; i++) {
                Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 64), false);
            }
            Waits.when(helper, station::isEmpty, 250, () -> "the shuttle should load the station", () -> {
                helper.assertTrue(rack.isEmpty(), "all three stacks left on one shuttle before any reached the rack");
                Waits.when(helper, () -> WarehouseFixtures.count(rack, Items.COBBLESTONE) == 192, 200,
                        () -> "all three stacks should reach the rack, it has " + WarehouseFixtures.count(rack, Items.COBBLESTONE), helper::succeed);
            });
        });
    }
}
