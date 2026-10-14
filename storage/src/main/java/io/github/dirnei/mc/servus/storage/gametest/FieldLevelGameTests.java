package io.github.dirnei.mc.servus.storage.gametest;

import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.HEAD;
import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.RAIL_Z;
import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.farm;
import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.network;
import static io.github.dirnei.mc.servus.storage.gametest.FarmFixtures.out;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.storage.field.FertilizeMode;
import io.github.dirnei.mc.servus.storage.field.FertilizePlan;
import io.github.dirnei.mc.servus.storage.field.FertilizeStatus;
import io.github.dirnei.mc.servus.storage.field.FieldAmount;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlock;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldThreshold;
import io.github.dirnei.mc.servus.storage.field.FieldWork;
import io.github.dirnei.mc.servus.storage.field.StockLevels;
import io.github.dirnei.mc.servus.storage.field.SupplyBuffer;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** The produce a Field Head learns and the stock levels that stop harvesting and fertilizing, on the farm of {@link FarmFixtures}. */
public final class FieldLevelGameTests {

    private static final BlockPos[] RIPE = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1)};

    private FieldLevelGameTests() {
    }

    private static ItemStack harvester() {
        return FarmFixtures.shuttleWith(ServusStorageItems.HARVESTER.get());
    }

    private static int age(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
    }

    private static void ripeWheat(GameTestHelper helper) {
        for (BlockPos plot : RIPE) {
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        }
    }

    private static boolean allAged(GameTestHelper helper, int age) {
        for (BlockPos plot : RIPE) {
            if (age(helper, plot) != age) {
                return false;
            }
        }
        return true;
    }

    /** A wheat field with water and wheat learned as its produce, as after a first harvest. */
    private static void wheatField(FieldHeadBlockEntity head) {
        FieldFixtures.water(head);
        head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
        head.learnProduce(Map.of(BuiltInRegistries.ITEM.getKey(Items.WHEAT), 1L));
    }

    private static void setLevel(FieldHeadBlockEntity head, StockLevels.Level which, int value) {
        head.setLevel(which, value);
    }

    /** Takes up to count of the item out of the rack, as an order would. */
    private static void takeOut(HeavyRackBlockEntity rack, Item item, int count) {
        int left = count;
        for (int slot = 0; slot < rack.items().getSlots() && left > 0; slot++) {
            if (rack.items().getStackInSlot(slot).is(item)) {
                left -= rack.items().extractItem(slot, left, false).getCount();
            }
        }
    }

    /** A wheat field's first harvest teaches the head wheat, not the seeds that came home too. */
    @ServusGameTest(timeoutTicks = 600)
    public static void produceIsLearned(GameTestHelper helper) {
        ripeWheat(helper);
        FarmFixtures.Farm farm = farm(helper, harvester());
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(head);
            head.setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.assertTrue(head.produce().isEmpty() && !head.produceKnown(), "a new head knows no produce");
            Waits.when(helper, () -> allAged(helper, 0) && out(helper) == 0 && head.produceKnown(), 550,
                    () -> "the harvest should teach the head its produce, produce " + head.produce() + ", out " + out(helper), () -> {
                helper.assertTrue(head.produce().is(Items.WHEAT), "the produce is wheat, got " + head.produce());
                helper.succeed();
            });
        });
    }

    /** A ripe field with more wheat in stock than its limit: no harvester leaves and the head says the harvest is paused. */
    @ServusGameTest(timeoutTicks = 200)
    public static void harvestStopsAtTheLimit(GameTestHelper helper) {
        ripeWheat(helper);
        FarmFixtures.Farm farm = farm(helper, harvester());
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT, 20));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(2, () -> {
            wheatField(head);
            setLevel(head, StockLevels.Level.HARVEST, 10);
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(allAged(helper, 7), "the wheat should stay ripe on the field");
                helper.assertTrue(out(helper) == 0 && network(helper).queuedJobs() == 0, "no harvester should leave");
                helper.assertTrue(head.harvestDue() && head.harvestPaused() && head.produceStock() == 20,
                        "the harvest is due but paused at 20 of 10, stock " + head.produceStock());
                helper.assertTrue(head.work() == FieldWork.HARVEST_PAUSED, "the Field tab says the harvest is paused, got " + head.work());
                helper.assertTrue(!head.problem() && !helper.getBlockState(HEAD).getValue(FieldHeadBlock.PROBLEM),
                        "a paused harvest is no problem");
                helper.succeed();
            });
        });
    }

    /** Once an order takes the stock below the limit, the next field check sends the harvester. */
    @ServusGameTest(timeoutTicks = 700)
    public static void harvestResumesBelowTheLimit(GameTestHelper helper) {
        ripeWheat(helper);
        FarmFixtures.Farm farm = farm(helper, harvester());
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT, 20));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(2, () -> {
            wheatField(head);
            setLevel(head, StockLevels.Level.HARVEST, 10);
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(allAged(helper, 7) && out(helper) == 0, "the harvest waits while 20 wheat are in stock");
                takeOut(farm.rack(), Items.WHEAT, 15);
                Waits.when(helper, () -> allAged(helper, 0) && out(helper) == 0, 550,
                        () -> "the field should be harvested once 5 wheat are left, rack holds " + WarehouseFixtures.count(farm.rack(), Items.WHEAT),
                        () -> {
                    helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 5 + RIPE.length, "the harvest reached the rack");
                    helper.succeed();
                });
            });
        });
    }

    /** At its limit the field still gets planted: the empty farmland gets wheat, the ripe wheat stays standing. */
    @ServusGameTest(timeoutTicks = 600)
    public static void cappedFieldStillPlants(GameTestHelper helper) {
        BlockPos ripe = RIPE[0];
        BlockPos empty = new BlockPos(4, 1, RAIL_Z + 1);
        FieldFixtures.crop(helper, ripe, Blocks.WHEAT, 7);
        helper.setBlock(empty.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        FarmFixtures.Farm farm = farm(helper, harvester());
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT, 20));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(2, () -> {
            wheatField(head);
            setLevel(head, StockLevels.Level.HARVEST, 10);
            head.fillSupply(SupplyBuffer.Part.SEEDS, Items.WHEAT_SEEDS, 10);
            Waits.when(helper, () -> age(helper, empty) == 0 && out(helper) == 0, 550,
                    () -> "the empty farmland should be planted, it holds " + helper.getBlockState(empty) + ", out " + out(helper), () -> {
                helper.assertTrue(age(helper, ripe) == 7, "the ripe wheat stays on the field");
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.WHEAT) == 20, "no wheat came home");
                helper.assertTrue(head.supplyCount(SupplyBuffer.Part.SEEDS) == 9, "one seed left the buffer, it holds "
                        + head.supplyCount(SupplyBuffer.Part.SEEDS));
                helper.assertTrue(head.work() == FieldWork.HARVEST_PAUSED, "the Field tab says the harvest is paused, got " + head.work());
                helper.succeed();
            });
        });
    }

    /** With more wheat in stock than the fertilize level no Spreader comes; once the stock drops below it, one does. */
    @ServusGameTest(timeoutTicks = 600)
    public static void fertilizeOnlyBelowTheLevel(GameTestHelper helper) {
        StorageStatsManager.INSTANCE.setFertilizeInterval(20);
        StorageStatsManager.INSTANCE.setDosesPerPlot(2);
        BlockPos plot = RIPE[0];
        FarmFixtures.Farm farm = farm(helper, FarmFixtures.shuttleWith(ServusStorageItems.SPREADER.get()));
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.WHEAT, 20));
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(2, () -> {
            wheatField(head);
            head.setFertilizer(new ItemStack(Items.BONE_MEAL));
            head.setFertilizeMode(FertilizeMode.AFTER_PLANTING);
            head.fillSupply(SupplyBuffer.Part.FERTILIZER, Items.BONE_MEAL, 20);
            setLevel(head, StockLevels.Level.FERTILIZE, 15);
            FieldFixtures.crop(helper, plot, Blocks.WHEAT, 0);
            head.recheck(helper.absolutePos(plot));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(age(helper, plot) == 0 && out(helper) == 0, "no Spreader comes while 20 wheat are in stock");
                helper.assertTrue(head.fertilizeStatus() == FertilizeStatus.NOT_NEEDED, "the status says not needed, got " + head.fertilizeStatus());
                helper.assertTrue(!head.fertilizeStatus().problem(), "not needed is no problem");
                takeOut(farm.rack(), Items.WHEAT, 10);
                Waits.when(helper, () -> age(helper, plot) > 0 && head.unfedCount() == 0 && out(helper) == 0, 450,
                        () -> "the Spreader should dose once 10 wheat are left, age " + age(helper, plot) + ", status " + head.fertilizeStatus(),
                        helper::succeed);
            });
        });
    }

    /** A mock player standing next to the head with its menu open. */
    private static Player playerAtTheHead(GameTestHelper helper, FieldHeadBlockEntity head) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(HEAD).east()));
        player.containerMenu = new FieldHeadMenu(1, player.getInventory(), head);
        return player;
    }

    /** The ripe bar slider sets the harvest threshold to the step it was let go at. */
    @ServusGameTest(timeoutTicks = 60)
    public static void ripeSliderSetsThreshold(GameTestHelper helper) {
        FieldHeadBlockEntity head = farm(helper, harvester()).head();
        Player player = playerAtTheHead(helper, head);
        player.containerMenu.clickMenuButton(player, FieldHeadMenu.BUTTON_HARVEST_PERCENT + 7);
        helper.assertTrue(head.harvestPercent() == 70, "the slider sets 70 percent, got " + head.harvestPercent());
        player.containerMenu.clickMenuButton(player, FieldHeadMenu.BUTTON_HARVEST_PERCENT);
        helper.assertTrue(head.harvestPercent() == FieldThreshold.MIN, "the slider stops at the minimum, got " + head.harvestPercent());
        helper.succeed();
    }

    /** Typed levels are set as sent, and the fertilize level never stays above the limit. */
    @ServusGameTest(timeoutTicks = 60)
    public static void levelsStayInOrder(GameTestHelper helper) {
        FieldHeadBlockEntity head = farm(helper, harvester()).head();
        Player player = playerAtTheHead(helper, head);
        BlockPos pos = helper.absolutePos(HEAD);
        helper.assertTrue(head.harvestLimit() == 0 && head.fertilizeBelow() == 0, "a new head has no levels");
        helper.assertTrue(FieldHeadMenu.setAmount(player, pos, FieldAmount.HARVEST_LIMIT, 512) && head.harvestLimit() == 512,
                "512 typed makes 512, got " + head.harvestLimit());
        helper.assertTrue(FieldHeadMenu.setAmount(player, pos, FieldAmount.FERTILIZE_BELOW, 640), "the fertilize level takes 640");
        helper.assertTrue(head.fertilizeBelow() == 640 && head.harvestLimit() == 640,
                "raising the fertilize level to 640 raises the limit along, got " + head.harvestLimit() + " / " + head.fertilizeBelow());
        FieldHeadMenu.setAmount(player, pos, FieldAmount.HARVEST_LIMIT, 300);
        helper.assertTrue(head.harvestLimit() == 300 && head.fertilizeBelow() == 300, "lowering the limit lowers the level along");
        FieldHeadMenu.setAmount(player, pos, FieldAmount.HARVEST_LIMIT, 0);
        helper.assertTrue(head.harvestLimit() == 0 && head.fertilizeBelow() == 300, "no limit leaves the level alone");
        helper.assertTrue(!FieldHeadMenu.setAmount(player, pos, null, 5), "an unknown amount is refused");
        helper.assertTrue(!FieldHeadMenu.setAmount(player, pos.north(), FieldAmount.HARVEST_LIMIT, 1), "another head's position is refused");
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!FieldHeadMenu.setAmount(player, pos, FieldAmount.HARVEST_LIMIT, 1) && head.harvestLimit() == 0,
                "without the head's menu open nothing changes");
        helper.succeed();
    }

    /** The server clamps typed amounts: levels to 0..99,999, the reserve to 0..MAX_RESERVE. */
    @ServusGameTest(timeoutTicks = 60)
    public static void typedAmountsClamped(GameTestHelper helper) {
        FieldHeadBlockEntity head = farm(helper, harvester()).head();
        Player player = playerAtTheHead(helper, head);
        BlockPos pos = helper.absolutePos(HEAD);
        FieldHeadMenu.setAmount(player, pos, FieldAmount.FERTILIZE_BELOW, 250_000);
        helper.assertTrue(head.fertilizeBelow() == StockLevels.MAX, "250,000 stops at 99,999, got " + head.fertilizeBelow());
        FieldHeadMenu.setAmount(player, pos, FieldAmount.HARVEST_LIMIT, Long.MAX_VALUE);
        helper.assertTrue(head.harvestLimit() == StockLevels.MAX, "the limit stops at 99,999, got " + head.harvestLimit());
        FieldHeadMenu.setAmount(player, pos, FieldAmount.HARVEST_LIMIT, -5);
        helper.assertTrue(head.harvestLimit() == 0, "a negative limit is 0, got " + head.harvestLimit());
        FieldHeadMenu.setAmount(player, pos, FieldAmount.RESERVE, 99_999);
        helper.assertTrue(head.reserve() == FertilizePlan.MAX_RESERVE, "the reserve stops at its maximum, got " + head.reserve());
        FieldHeadMenu.setAmount(player, pos, FieldAmount.RESERVE, -10);
        helper.assertTrue(head.reserve() == 0, "a negative reserve is 0, got " + head.reserve());
        FieldHeadMenu.setAmount(player, pos, FieldAmount.ANTI_CORROSION_RESERVE, 99_999);
        helper.assertTrue(head.antiCorrosionReserve() == FertilizePlan.MAX_RESERVE,
                "the wax reserve stops at its maximum, got " + head.antiCorrosionReserve());
        FieldHeadMenu.setAmount(player, pos, FieldAmount.ANTI_CORROSION_RESERVE, -10);
        helper.assertTrue(head.antiCorrosionReserve() == 0, "a negative wax reserve is 0, got " + head.antiCorrosionReserve());
        helper.succeed();
    }

    /** A player who walked out of reach of the head changes nothing, even with its menu still open. */
    @ServusGameTest(timeoutTicks = 60)
    public static void typedAmountRefusedFarAway(GameTestHelper helper) {
        FieldHeadBlockEntity head = farm(helper, harvester()).head();
        Player player = playerAtTheHead(helper, head);
        BlockPos pos = helper.absolutePos(HEAD);
        player.snapTo(Vec3.atBottomCenterOf(pos.east(64)));
        helper.assertTrue(!FieldHeadMenu.setAmount(player, pos, FieldAmount.HARVEST_LIMIT, 500) && head.harvestLimit() == 0,
                "a far player cannot set the limit, got " + head.harvestLimit());
        int reserve = head.reserve();
        helper.assertTrue(!FieldHeadMenu.setAmount(player, pos, FieldAmount.RESERVE, 3) && head.reserve() == reserve,
                "a far player cannot set the reserve");
        helper.succeed();
    }

    /** Produce, levels and the wax reserve survive saving; a new crop forgets the produce. */
    @ServusGameTest(timeoutTicks = 60)
    public static void levelsSaved(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, harvester());
        FieldHeadBlockEntity head = farm.head();
        helper.runAfterDelay(3, () -> {
            wheatField(head);
            setLevel(head, StockLevels.Level.HARVEST, 1000);
            setLevel(head, StockLevels.Level.FERTILIZE, 500);
            head.setAntiCorrosionReserve(6);
            CompoundTag tag = head.saveWithFullMetadata(helper.getLevel().registryAccess());
            FieldHeadBlockEntity copy = new FieldHeadBlockEntity(head.getBlockPos(), head.getBlockState());
            copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
            helper.assertTrue(copy.produce().is(Items.WHEAT) && copy.harvestLimit() == 1000 && copy.fertilizeBelow() == 500
                            && copy.antiCorrosionReserve() == 6,
                    "produce, levels and wax reserve should survive saving, got " + copy.produce() + ", " + copy.harvestLimit()
                            + " / " + copy.fertilizeBelow() + ", " + copy.antiCorrosionReserve());
            head.setCrop(new ItemStack(Items.BEETROOT_SEEDS));
            helper.assertTrue(!head.produceKnown() && head.harvestLimit() == 1000, "a new crop forgets the produce but keeps the levels");
            helper.succeed();
        });
    }
}
