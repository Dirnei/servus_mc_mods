package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.shuttles.controller.SiteStatusPayload;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerItem;
import io.github.dirnei.mc.servus.shuttles.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.order.StockRule;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.site.SiteCapacity;
import io.github.dirnei.mc.servus.shuttles.site.SiteChunks;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A controller is a site: name, chunk span, Anchor Upgrades and the over capacity lamp. */
public final class ControllerSiteGameTests {

    /** Wide enough that every row of x positions 2..30 crosses a chunk border. */
    static final String WIDE = "empty_34x6x4";
    private static final int ROW_Z = 1;

    private ControllerSiteGameTests() {
    }

    /** The first relative x in 3..30 whose block starts a new chunk. */
    static int border(GameTestHelper helper) {
        for (int x = 3; x <= 30; x++) {
            if ((helper.absolutePos(new BlockPos(x, 1, ROW_Z)).getX() & 15) == 0) {
                return x;
            }
        }
        throw new IllegalStateException("no chunk border in the wide template");
    }

    /**
     * Controller two blocks before the border, rails on both sides of it, a rack
     * just past it facing the last rail: the site spans the controller's chunk and the next one.
     */
    static WarehouseControllerBlockEntity acrossBorder(GameTestHelper helper, int border) {
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, new BlockPos(border - 2, 1, ROW_Z));
        WarehouseFixtures.railOnStone(helper, new BlockPos(border - 1, 1, ROW_Z));
        WarehouseFixtures.railOnStone(helper, new BlockPos(border, 1, ROW_Z));
        helper.setBlock(new BlockPos(border + 1, 1, ROW_Z), ServusShuttlesBlocks.HEAVY_RACK.get().defaultBlockState()
                .setValue(HeavyRackBlock.FACING, Direction.WEST));
        return controller;
    }

    /** A rail run carries the site into the next chunk; breaking it takes the chunk out again. */
    @ServusGameTest(template = WIDE, timeoutTicks = 200)
    public static void spanFollowsRailIntoNextChunk(GameTestHelper helper) {
        int border = border(helper);
        WarehouseControllerBlockEntity controller = acrossBorder(helper, border);
        Waits.when(helper, () -> controller.span().size() == 2, 60,
                () -> "the site spans two chunks, had " + controller.span().size(), () -> {
            long far = SiteChunks.ofBlock(helper.absolutePos(new BlockPos(border, 1, ROW_Z)).getX(),
                    helper.absolutePos(new BlockPos(border, 1, ROW_Z)).getZ());
            helper.assertTrue(controller.span().equals(Set.of(controller.homeChunk(), far)), "the span is home and the next chunk");
            helper.setBlock(new BlockPos(border - 1, 1, ROW_Z), Blocks.AIR.defaultBlockState());
            Waits.when(helper, () -> controller.span().size() == 1, 60,
                    () -> "breaking the rail leaves one chunk, had " + controller.span().size(), helper::succeed);
        });
    }

    /** A chest across the border touching the controller is no part of the site: its chunk does not count. */
    @ServusGameTest(template = WIDE, timeoutTicks = 200)
    public static void chestAcrossBorderCountsNothing(GameTestHelper helper) {
        int border = border(helper);
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, new BlockPos(border - 1, 1, ROW_Z));
        BlockPos chest = new BlockPos(border, 1, ROW_Z);
        helper.setBlock(chest, Blocks.CHEST.defaultBlockState());
        helper.getBlockEntity(chest, ChestBlockEntity.class).setItem(0, new ItemStack(Items.BREAD, 5));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(controller.span().size() == 1 && controller.span().contains(controller.homeChunk()),
                    "only the controller's chunk counts, had " + controller.span().size());
            helper.assertTrue(controller.network().orElseThrow().index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 0, "the chest is not indexed");
            helper.succeed();
        });
    }

    /** One upgrade of one chunk is too small for two chunks: red lamp; a second upgrade turns it green. */
    @ServusGameTest(template = WIDE, timeoutTicks = 200)
    public static void overCapacityLampAndBackWithUpgrade(GameTestHelper helper) {
        int border = border(helper);
        BlockPos at = new BlockPos(border - 2, 1, ROW_Z);
        WarehouseControllerBlockEntity controller = acrossBorder(helper, border);
        ShuttlesStatsManager.INSTANCE.setChunksPerUpgrade(1);
        controller.upgrades().insertItem(0, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
        Waits.when(helper, () -> controller.span().size() == 2 && WarehouseFixtures.activity(helper, at) == StorageNodeBlocks.Activity.BLOCKED, 60,
                () -> "two chunks over one upgrade turn the lamp red, span " + controller.span().size() + " lamp " + WarehouseFixtures.activity(helper, at), () -> {
            helper.assertTrue(controller.siteState() == SiteCapacity.State.OVER, "the site is over capacity");
            SiteStatusPayload status = WarehouseControllerMenu.status(controller);
            helper.assertTrue(status.capacityState() == SiteCapacity.State.OVER && status.capacity() == 1, "the GUI says over capacity");
            controller.upgrades().insertItem(1, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
            helper.assertTrue(WarehouseFixtures.activity(helper, at) == StorageNodeBlocks.Activity.IDLE, "a second upgrade turns the lamp green");
            helper.assertTrue(controller.siteState() == SiteCapacity.State.COVERED && controller.capacity() == 2, "two of two chunks covered");
            ShuttlesStatsManager.INSTANCE.setChunksPerUpgrade(ShuttlesStatsManager.DEFAULT_CHUNKS_PER_UPGRADE);
            helper.succeed();
        });
    }

    /** Only Anchor Upgrades go in, one per slot; without one the lamp stays green. */
    @ServusGameTest(template = WIDE, timeoutTicks = 40)
    public static void upgradeSlotsTakeOnlyAnchorUpgrades(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, ROW_Z);
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, at);
        ItemStack speed = new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get());
        helper.assertTrue(controller.upgrades().insertItem(0, speed, true).getCount() == 1, "a Speed Upgrade does not fit");
        ItemStack two = new ItemStack(MachineContent.ANCHOR_UPGRADE.get(), 2);
        helper.assertTrue(controller.upgrades().insertItem(0, two, false).getCount() == 1, "one upgrade per slot");
        helper.assertTrue(controller.upgrades().getSlots() == ShuttlesStatsManager.INSTANCE.anchorUpgradeSlots(), "slot count from warehouse.json");
        helper.assertTrue(controller.siteState() == SiteCapacity.State.COVERED, "one chunk under one upgrade is covered");
        controller.upgrades().extractItem(0, 1, false);
        helper.assertTrue(controller.siteState() == SiteCapacity.State.NO_UPGRADE, "without upgrades there is nothing to exceed");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(WarehouseFixtures.activity(helper, at) == StorageNodeBlocks.Activity.IDLE, "no upgrade is no error");
            helper.succeed();
        });
    }

    /** A mined controller drops an item with its name and FE; placed again, both are back. */
    @ServusGameTest(template = WIDE, timeoutTicks = 60)
    public static void minedControllerKeepsNameAndEnergy(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, ROW_Z);
        BlockPos again = new BlockPos(28, 1, ROW_Z);
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, at);
        controller.setSiteName("North Mine");
        controller.setEnergy(12000);
        helper.getLevel().destroyBlock(helper.absolutePos(at), true);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get(), at);
        ItemStack item = controllerDrop(helper, at);
        helper.assertTrue(item.getCount() == 1, "one controller item drops, got " + item.getCount());
        List<String> lines = WarehouseControllerItem.carriedLines(item).stream()
                .map(line -> line.getContents() instanceof TranslatableContents text
                        ? text.getKey() + List.of(text.getArgs()) : line.getString())
                .toList();
        helper.assertTrue(lines.equals(List.of("tooltip.servus_shuttles.warehouse_controller.site[North Mine]",
                "tooltip.servus_shuttles.warehouse_controller.energy[12,000]")), "the tooltip names the site and the FE: " + lines);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        WarehouseControllerBlockEntity placed = WarehouseFixtures.placeController(helper, again, player, item);
        helper.assertTrue(placed.siteName().equals("North Mine"), "the name travels, was '" + placed.siteName() + "'");
        helper.assertTrue(placed.energyStored() == 12000, "the FE travels, has " + placed.energyStored());
        helper.succeed();
    }

    /** An item carrying more FE than the capacity fills the placed controller only up to it; a fresh item brings none. */
    @ServusGameTest(template = WIDE, timeoutTicks = 40)
    public static void placedEnergyIsCappedAtCapacity(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack fresh = new ItemStack(ServusShuttlesItems.WAREHOUSE_CONTROLLER.get());
        helper.assertTrue(WarehouseControllerItem.carriedLines(fresh).isEmpty(), "a fresh item shows no site and no FE");
        WarehouseControllerBlockEntity empty = WarehouseFixtures.placeController(helper, new BlockPos(2, 1, ROW_Z), player, fresh);
        helper.assertTrue(empty.energyStored() == 0, "a fresh controller holds no FE, has " + empty.energyStored());

        ItemStack over = new ItemStack(ServusShuttlesItems.WAREHOUSE_CONTROLLER.get());
        int capacity = ShuttlesStatsManager.INSTANCE.controllerEnergyCapacity();
        over.set(ServusShuttlesComponents.CONTROLLER_ENERGY.get(), capacity + 50000);
        WarehouseControllerBlockEntity full = WarehouseFixtures.placeController(helper, new BlockPos(28, 1, ROW_Z), player, over);
        helper.assertTrue(full.energyStored() == capacity, "capped at " + capacity + ", has " + full.energyStored());
        helper.succeed();
    }

    private static ItemStack controllerDrop(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(1)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(ServusShuttlesItems.WAREHOUSE_CONTROLLER.get()))
                .findFirst().orElseThrow();
    }

    /** Name and rules ride the dismantled item, upgrades drop, the span starts over at the new place. */
    @ServusGameTest(template = WIDE, timeoutTicks = 60)
    public static void nameSavedAndCarriedByCarrot(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, ROW_Z);
        BlockPos again = new BlockPos(28, 1, ROW_Z);
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, at);
        controller.setSiteName("  North Mine ");
        controller.setEnergy(12000);
        controller.rules().set(StockRule.make(ItemKey.of(new ItemStack(Items.STICK)), 8, 16));
        controller.upgrades().insertItem(0, new ItemStack(MachineContent.ANCHOR_UPGRADE.get()), false);
        controller.setSpan(Set.of(controller.homeChunk(), SiteChunks.key(9999, 9999)));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        GameTestSteps.useCarrot(helper, player, at, Direction.NORTH);
        BlockPos pos = helper.absolutePos(at);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get(), at);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        ItemStack item = drops.stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(ServusShuttlesItems.WAREHOUSE_CONTROLLER.get())).findFirst().orElseThrow();
        long upgrades = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(MachineContent.ANCHOR_UPGRADE.get()))
                .mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(upgrades == 1, "the upgrade drops on its own, got " + upgrades);

        WarehouseControllerBlockEntity placed = WarehouseFixtures.placeController(helper, again, player, item);
        helper.assertTrue(placed.siteName().equals("North Mine"), "the name travels, was '" + placed.siteName() + "'");
        helper.assertTrue(placed.energyStored() == 12000, "the FE travels, has " + placed.energyStored());
        helper.assertTrue(placed.displayName().equals("North Mine"), "the display name is the name");
        helper.assertTrue(placed.rules().get(ItemKey.of(new ItemStack(Items.STICK))) != null, "the rules travel");
        helper.assertTrue(placed.upgrades().count() == 0, "the item carries no upgrades");
        helper.assertTrue(placed.span().equals(Set.of(placed.homeChunk())), "the span starts over at the new place, was " + placed.span());
        helper.succeed();
    }

    /** The GUI renames the site and cleans the name; an unnamed site shows its position. */
    @ServusGameTest(template = WIDE, timeoutTicks = 40)
    public static void menuRenamesAndCleans(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, ROW_Z);
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, at);
        BlockPos pos = helper.absolutePos(at);
        helper.assertTrue(controller.displayName().equals("Warehouse at " + pos.getX() + ", " + pos.getZ()), "an unnamed site shows its position");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos).add(1, 0, 0));
        WarehouseControllerMenu menu = new WarehouseControllerMenu(1, player.getInventory(), controller);
        menu.rename("  North\nMine and a name that is far too long to keep  ");
        helper.assertTrue(controller.siteName().equals("NorthMine and a name that is far"), "cleaned and cut, was '" + controller.siteName() + "'");
        helper.runAfterDelay(3, () -> {
            SiteStatusPayload status = WarehouseControllerMenu.status(controller);
            helper.assertTrue(status.title().equals(controller.siteName()) && status.chunks().size() == 1 && status.online() == 0,
                    "the GUI shows the name, one chunk and online: " + status.title() + ", " + status.chunks().size() + ", " + status.online());
            helper.succeed();
        });
    }
}
