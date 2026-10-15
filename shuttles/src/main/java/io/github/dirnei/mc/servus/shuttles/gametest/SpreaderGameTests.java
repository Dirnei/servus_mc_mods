package io.github.dirnei.mc.servus.shuttles.gametest;

import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.HEAD;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.RACK;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.RAIL_Z;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.farm;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.network;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.noItemsLying;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.out;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.FertilizeMode;
import io.github.dirnei.mc.servus.shuttles.field.FieldAmount;
import io.github.dirnei.mc.servus.shuttles.field.FertilizeStatus;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleProfile;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Spreader shuttles on the farm of {@link FarmFixtures}. */
public final class SpreaderGameTests {

    private SpreaderGameTests() {
    }

    private static ItemStack spreader() {
        return FarmFixtures.shuttleWith(ServusShuttlesItems.SPREADER.get());
    }

    /**
     * The farm with quick fertilize calls and two doses per plot. GameTests of a
     * batch run at once and share these numbers, so every Spreader test sets the same.
     */
    private static FarmFixtures.Farm spreaderFarm(GameTestHelper helper, ItemStack shuttle) {
        ShuttlesStatsManager.INSTANCE.setFertilizeInterval(20);
        ShuttlesStatsManager.INSTANCE.setDosesPerPlot(2);
        return farm(helper, shuttle);
    }

    /** Water, bone meal as the fertilizer, that mode and that reserve. */
    private static void fertilize(FarmFixtures.Farm farm, FertilizeMode mode, int reserve) {
        FieldFixtures.water(farm.head());
        farm.head().setFertilizer(new ItemStack(Items.BONE_MEAL));
        farm.head().setFertilizeMode(mode);
        farm.head().setReserve(reserve);
    }

    /** A wheat crop of that age on farmland. */
    private static void wheat(GameTestHelper helper, BlockPos pos, int age) {
        FieldFixtures.crop(helper, pos, Blocks.WHEAT, age);
    }

    /** A crop a player just planted: placed, and the head hears of it like from the place event. */
    private static void planted(GameTestHelper helper, FarmFixtures.Farm farm, BlockPos pos) {
        wheat(helper, pos, 0);
        farm.head().recheck(helper.absolutePos(pos));
    }

    private static int buffered(FarmFixtures.Farm farm) {
        return farm.head().supplyCount(SupplyBuffer.Part.FERTILIZER);
    }

    private static int rackMeal(FarmFixtures.Farm farm) {
        return WarehouseFixtures.count(farm.rack(), Items.BONE_MEAL);
    }

    private static int age(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
    }

    /** The Spreader goes in as the shuttle's tool; it makes the shuttle field only and fits only fertilize runs. */
    @ServusGameTest
    public static void workshopFitsSpreader(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = new BlockPos(5, 1, 5);
        helper.setBlock(at, ServusShuttlesBlocks.SHUTTLE_WORKSHOP.get().defaultBlockState());
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, at);
        player.getInventory().setItem(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        player.getInventory().setItem(1, new ItemStack(ServusShuttlesItems.SPREADER.get()));
        int hotbar = ShuttleWorkshopMenu.INVENTORY_START + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).tool().equals(Optional.of(ServusShuttlesItems.SPREADER.get())),
                "the Spreader is the shuttle's tool");
        ShuttleProfile profile = ShuttleModules.profile(menu.shuttle(), ShuttlesStatsManager.INSTANCE.shuttleTiers());
        helper.assertTrue(profile.fieldOnly() && profile.liftless(), "a Spreader shuttle is field only and too wide for lifts");
        helper.assertTrue(JobNeeds.SPREADER.fits(profile), "fertilize runs fit it");
        helper.assertTrue(!JobNeeds.WAREHOUSE.fits(profile) && !JobNeeds.HARVESTER.fits(profile), "warehouse jobs and harvest runs do not");
        helper.succeed();
    }

    /** With only a Spreader, a due harvest stays on the field and the warehouse counts no shuttle for its jobs. */
    @ServusGameTest(timeoutTicks = 200)
    public static void spreaderTakesNoWarehouseOrHarvestJob(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        wheat(helper, plot, 7);
        FarmFixtures.Farm farm = spreaderFarm(helper, spreader());
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.COBBLESTONE, 10));
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(farm.head().harvestDue(), "the harvest should be due");
                helper.assertTrue(age(helper, plot) == 7, "the wheat should still stand");
                helper.assertTrue(!network(helper).canReach(helper.absolutePos(RACK)), "a Spreader must not count as a shuttle for warehouse jobs");
                helper.assertTrue(out(helper) == 0 && network(helper).queuedJobs() == 0, "no shuttle should be out and no job queued");
                helper.succeed();
            });
        });
    }

    /**
     * After planting: a supply run brings the rack's 20 bone meal into the
     * head's buffer, and two wheat planted after the head was set get two
     * doses each from it; an older growing wheat gets none, so the buffer
     * holds exactly four bone meal less.
     */
    @ServusGameTest(timeoutTicks = 600)
    public static void afterPlantingDosesFreshPlots(GameTestHelper helper) {
        BlockPos old = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos[] fresh = {new BlockPos(6, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1)};
        wheat(helper, old, 1);
        FarmFixtures.Farm farm = spreaderFarm(helper, spreader());
        FarmFixtures.carrier(farm);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 20));
        helper.runAfterDelay(3, () -> {
            fertilize(farm, FertilizeMode.AFTER_PLANTING, 0);
            for (BlockPos plot : fresh) {
                planted(helper, farm, plot);
            }
            Waits.when(helper, () -> rackMeal(farm) == 0 && buffered(farm) == 16 && out(helper) == 0
                    && age(helper, fresh[0]) >= 4 && age(helper, fresh[1]) >= 4, 550,
                    () -> "the fresh wheat should have two doses each, rack holds " + rackMeal(farm) + " bone meal, buffer " + buffered(farm)
                            + ", ages " + age(helper, fresh[0]) + " and " + age(helper, fresh[1]) + ", out " + out(helper), () -> {
                helper.assertTrue(farm.head().unfedCount() == 0, "no plot should still be owed a dose");
                helper.assertTrue(farm.head().fertilizeTargetCount() == 0, "no plot needs a dose");
                helper.assertTrue(noItemsLying(helper), "nothing should drop on the ground");
                helper.succeed();
            });
        });
    }

    /** Stock equal to the reserve: no supply run and no Spreader, the rack keeps its bone meal and the status says below reserve. */
    @ServusGameTest(timeoutTicks = 200)
    public static void reserveRespected(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        FarmFixtures.Farm farm = spreaderFarm(helper, spreader());
        FarmFixtures.carrier(farm);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 10));
        helper.runAfterDelay(3, () -> {
            fertilize(farm, FertilizeMode.AFTER_PLANTING, 10);
            planted(helper, farm, plot);
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(farm.head().fertilizeTargetCount() == 1, "the new wheat needs a dose");
                helper.assertTrue(out(helper) == 0 && network(helper).queuedJobs() == 0, "no Spreader and no supply run should drive out");
                helper.assertTrue(buffered(farm) == 0, "the buffer stays empty");
                helper.assertTrue(WarehouseFixtures.count(farm.rack(), Items.BONE_MEAL) == 10, "the rack keeps its 10 bone meal");
                helper.assertTrue(farm.head().fertilizeStatus() == FertilizeStatus.BELOW_RESERVE,
                        "the status should say below reserve, got " + farm.head().fertilizeStatus());
                helper.succeed();
            });
        });
    }

    /** Until ripe share at 50 percent: one ripe of four, doses go out until two are ripe, then they stop. */
    @ServusGameTest(timeoutTicks = 900)
    public static void untilRipeShareStopsAtThreshold(GameTestHelper helper) {
        BlockPos[] plots = {new BlockPos(5, 1, RAIL_Z - 1), new BlockPos(6, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1), new BlockPos(7, 1, RAIL_Z - 1)};
        wheat(helper, plots[0], 7);
        for (int i = 1; i < plots.length; i++) {
            wheat(helper, plots[i], 0);
        }
        FarmFixtures.Farm farm = spreaderFarm(helper, spreader());
        FarmFixtures.carrier(farm);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 64));
        helper.runAfterDelay(3, () -> {
            fertilize(farm, FertilizeMode.UNTIL_RIPE, 0);
            farm.head().stepFertilizePercent((50 - farm.head().fertilizePercent()) / 10);
            Waits.when(helper, () -> farm.head().ripeCount() >= 2 && out(helper) == 0 && network(helper).queuedJobs() == 0, 850,
                    () -> "two of four should be ripe and the Spreader home, ripe " + farm.head().ripeCount() + ", out " + out(helper), () -> {
                int stock = rackMeal(farm) + buffered(farm);
                helper.runAfterDelay(60, () -> {
                    helper.assertTrue(farm.head().ripeCount() < 4, "the doses should stop at the threshold, not ripen everything");
                    helper.assertTrue(rackMeal(farm) + buffered(farm) == stock, "no more bone meal should be used once the share is reached");
                    helper.assertTrue(out(helper) == 0, "the Spreader should stay home");
                    helper.assertTrue(farm.head().fertilizeTargetCount() == 0, "no plot needs a dose at the threshold");
                    helper.succeed();
                });
            });
        });
    }

    /**
     * Until ripe share at 100 percent with a ripe plot and a plot one dose
     * from ripe, two doses per plot: the ripe plot gets nothing, the other
     * takes one dose and the bone meal left goes back into the head's buffer.
     */
    @ServusGameTest(timeoutTicks = 600)
    public static void ripePlotsUntouchedAndLeftoverBackInBuffer(GameTestHelper helper) {
        BlockPos ripe = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos almost = new BlockPos(6, 1, RAIL_Z + 1);
        wheat(helper, ripe, 7);
        wheat(helper, almost, 5);
        FarmFixtures.Farm farm = spreaderFarm(helper, spreader());
        FarmFixtures.carrier(farm);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 20));
        helper.runAfterDelay(3, () -> {
            fertilize(farm, FertilizeMode.UNTIL_RIPE, 0);
            farm.head().stepFertilizePercent(10);
            helper.assertTrue(farm.head().fertilizePercent() == 100, "the threshold should be 100 percent");
            Waits.when(helper, () -> rackMeal(farm) == 0 && buffered(farm) == 19 && out(helper) == 0 && age(helper, almost) == 7, 550,
                    () -> "one dose should be used and one put back into the buffer, buffer holds " + buffered(farm) + ", rack "
                            + rackMeal(farm) + ", out " + out(helper) + ", age " + age(helper, almost), () -> {
                helper.assertTrue(age(helper, ripe) == 7, "the ripe wheat stays ripe");
                helper.assertTrue(noItemsLying(helper), "nothing should drop on the ground");
                helper.assertTrue(network(helper).queuedJobs() == 0, "at 100 percent ripe no new run is queued");
                helper.succeed();
            });
        });
    }

    /** The fertilizer is chosen by id from the tag only; mode and threshold buttons and the typed reserve work and the settings are saved. */
    @ServusGameTest(timeoutTicks = 100)
    public static void menuChoosesFertilizerAndSettings(GameTestHelper helper) {
        FarmFixtures.Farm farm = spreaderFarm(helper, spreader());
        FieldHeadBlockEntity head = farm.head();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(HEAD).east()));
        FieldHeadMenu menu = new FieldHeadMenu(1, player.getInventory(), head);
        player.containerMenu = menu;
        helper.assertTrue(head.fertilizer().isEmpty() && head.fertilizeMode() == FertilizeMode.OFF && !head.fertilizing(),
                "a new head does not fertilize");
        helper.assertTrue(head.reserve() == ShuttlesStatsManager.INSTANCE.fertilizerReserve(), "the reserve starts at fertilizer_reserve");
        helper.assertTrue(FieldHeadMenu.chooseFertilizer(player, id(Items.BONE_MEAL)), "bone meal should be taken");
        helper.assertTrue(head.fertilizer().is(Items.BONE_MEAL), "bone meal is the fertilizer");
        helper.assertTrue(!FieldHeadMenu.chooseFertilizer(player, id(Items.COBBLESTONE)), "cobblestone should be refused");
        helper.assertTrue(!FieldHeadMenu.chooseFertilizer(player, Optional.of(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "no_such_item"))),
                "an unknown id should be refused");
        helper.assertTrue(head.fertilizer().is(Items.BONE_MEAL), "refused ids change nothing");
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_FERTILIZE_MODE + FertilizeMode.UNTIL_RIPE.ordinal());
        helper.assertTrue(head.fertilizeMode() == FertilizeMode.UNTIL_RIPE && head.fertilizing(), "the mode button sets Until ripe share");
        int percent = head.fertilizePercent();
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_FERTILIZE_UP);
        helper.assertTrue(head.fertilizePercent() == Math.min(100, percent + 10), "+ raises the fertilize threshold");
        BlockPos pos = helper.absolutePos(HEAD);
        head.setReserve(5);
        FieldHeadMenu.setAmount(player, pos, FieldAmount.RESERVE, -10);
        helper.assertTrue(head.reserve() == 0, "-10 typed stops at 0, got " + head.reserve());
        FieldHeadMenu.setAmount(player, pos, FieldAmount.RESERVE, 21);
        helper.assertTrue(head.reserve() == 21, "21 typed makes 21, got " + head.reserve());
        helper.assertTrue(!menu.clickMenuButton(player, 99), "unknown buttons are refused");
        CompoundTag tag = head.saveWithFullMetadata(helper.getLevel().registryAccess());
        FieldHeadBlockEntity copy = new FieldHeadBlockEntity(head.getBlockPos(), head.getBlockState());
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(copy.fertilizer().is(Items.BONE_MEAL) && copy.fertilizeMode() == FertilizeMode.UNTIL_RIPE
                && copy.fertilizePercent() == head.fertilizePercent() && copy.reserve() == 21, "the settings should survive saving");
        helper.assertTrue(FieldHeadMenu.chooseFertilizer(player, Optional.empty()), "Off should be taken");
        helper.assertTrue(head.fertilizer().isEmpty() && !head.fertilizing(), "Off clears the fertilizer");
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!FieldHeadMenu.chooseFertilizer(player, id(Items.BONE_MEAL)) && head.fertilizer().isEmpty(),
                "without the head's menu open nothing changes");
        helper.succeed();
    }

    private static Optional<Identifier> id(Item item) {
        return Optional.of(BuiltInRegistries.ITEM.getKey(item));
    }
}
