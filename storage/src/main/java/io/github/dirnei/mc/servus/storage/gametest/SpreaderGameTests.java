package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.field.FertilizeMode;
import io.github.dirnei.mc.servus.storage.field.FertilizeStatus;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleProfile;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Spreader shuttles on the small farm of the harvester tests: a controller,
 * Warehouse Rails x 1 to 3 and farm rails x 4 to 7 along z 6, a garage and a
 * Heavy Duty Rack on the Warehouse Rails and the Field Head east of the farm rails.
 */
public final class SpreaderGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final int RAIL_Z = 6;
    private static final BlockPos CONTROLLER = new BlockPos(0, 1, RAIL_Z);
    private static final BlockPos GARAGE = new BlockPos(1, 1, RAIL_Z + 1);
    private static final BlockPos RACK = new BlockPos(2, 1, RAIL_Z - 1);
    private static final BlockPos HEAD = new BlockPos(8, 1, RAIL_Z);

    private SpreaderGameTests() {
    }

    // Helpers

    private record Farm(FieldHeadBlockEntity head, HeavyRackBlockEntity rack) {
    }

    /** A Mk1 shuttle with a Spreader in its tool slot. */
    private static ItemStack spreader() {
        ItemStack shuttle = new ItemStack(ServusStorageItems.SHUTTLE_MK1.get());
        ShuttleModules.set(shuttle, ShuttleModules.EMPTY.withTool(Optional.of(ServusStorageItems.SPREADER.get())));
        return shuttle;
    }

    private static void rail(GameTestHelper helper, BlockPos pos, Block rail) {
        helper.setBlock(pos.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(pos, Block.updateFromNeighbourShapes(rail.defaultBlockState(), helper.getLevel(), helper.absolutePos(pos)));
    }

    /**
     * Builds the farm with that shuttle in the garage, quick work and checks,
     * two doses per plot and a short call interval. GameTests of a batch run
     * at once and share these numbers, so every Spreader test uses the same.
     */
    private static Farm farm(GameTestHelper helper, ItemStack shuttle) {
        StorageStatsManager.INSTANCE.setFieldWorkTicks(2);
        StorageStatsManager.INSTANCE.setFieldCheckInterval(5);
        StorageStatsManager.INSTANCE.setFertilizeInterval(20);
        StorageStatsManager.INSTANCE.setDosesPerPlot(2);
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, CONTROLLER);
        for (int x = 1; x <= 7; x++) {
            rail(helper, new BlockPos(x, 1, RAIL_Z), x <= 3 ? ServusStorageBlocks.RAIL.get() : ServusStorageBlocks.FARM_RAIL.get());
        }
        helper.setBlock(GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.SOUTH));
        helper.setBlock(HEAD, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(GARAGE, ShuttleGarageBlockEntity.class);
        garage.shuttles().setStackInSlot(0, shuttle);
        return new Farm(helper.getBlockEntity(HEAD, FieldHeadBlockEntity.class), helper.getBlockEntity(RACK, HeavyRackBlockEntity.class));
    }

    /** Water, bone meal as the fertilizer, that mode and that reserve. */
    private static void fertilize(Farm farm, FertilizeMode mode, int reserve) {
        Transfer.fill(farm.head().fluidHandler(), new FluidStack(Fluids.WATER, 4000), false);
        farm.head().setFertilizer(new ItemStack(Items.BONE_MEAL));
        farm.head().setFertilizeMode(mode);
        farm.head().stepReserve(reserve - farm.head().reserve());
    }

    /** A crop of that age on farmland. */
    private static void crop(GameTestHelper helper, BlockPos pos, int age) {
        helper.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(pos, ((CropBlock) Blocks.WHEAT).getStateForAge(age));
    }

    /** A crop a player just planted: placed, and the head hears of it like from the place event. */
    private static void planted(GameTestHelper helper, Farm farm, BlockPos pos) {
        crop(helper, pos, 0);
        farm.head().recheck(helper.absolutePos(pos));
    }

    private static int age(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
    }

    private static int held(HeavyRackBlockEntity rack, Item item) {
        return MiniWarehouse.count(rack, item);
    }

    private static WarehouseNetwork network(GameTestHelper helper) {
        return helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().orElseThrow();
    }

    private static int out(GameTestHelper helper) {
        return network(helper).garageOut(helper.absolutePos(GARAGE));
    }

    private static boolean noItemsLying(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).isEmpty();
    }

    // Module

    /** The Spreader goes in as the shuttle's tool; it makes the shuttle field only and fits only fertilize runs. */
    @ServusGameTest
    public static void workshopFitsSpreader(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = new BlockPos(5, 1, 5);
        helper.setBlock(at, ServusStorageBlocks.SHUTTLE_WORKSHOP.get().defaultBlockState());
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, at);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE_MK1.get()));
        player.getInventory().setItem(1, new ItemStack(ServusStorageItems.SPREADER.get()));
        int hotbar = ShuttleWorkshopMenu.INVENTORY_START + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).tool().equals(Optional.of(ServusStorageItems.SPREADER.get())),
                "the Spreader is the shuttle's tool");
        ShuttleProfile profile = ShuttleModules.profile(menu.shuttle(), StorageStatsManager.INSTANCE.shuttleTiers());
        helper.assertTrue(profile.fieldOnly() && profile.liftless(), "a Spreader shuttle is field only and too wide for lifts");
        helper.assertTrue(JobNeeds.SPREADER.fits(profile), "fertilize runs fit it");
        helper.assertTrue(!JobNeeds.WAREHOUSE.fits(profile) && !JobNeeds.HARVESTER.fits(profile), "warehouse jobs and harvest runs do not");
        helper.succeed();
    }

    /** With only a Spreader, a due harvest stays on the field and the warehouse counts no shuttle for its jobs. */
    @ServusGameTest(timeoutTicks = 200)
    public static void spreaderTakesNoWarehouseOrHarvestJob(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        crop(helper, plot, 7);
        Farm farm = farm(helper, spreader());
        MiniWarehouse.put(farm.rack(), new ItemStack(Items.COBBLESTONE, 10));
        helper.runAfterDelay(2, () -> {
            Transfer.fill(farm.head().fluidHandler(), new FluidStack(Fluids.WATER, 4000), false);
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

    // Runs

    /**
     * After planting: two wheat planted after the head was set get two doses
     * each from the rack; an older growing wheat gets none, so the rack
     * holds exactly four bone meal less.
     */
    @ServusGameTest(timeoutTicks = 600)
    public static void afterPlantingDosesFreshPlots(GameTestHelper helper) {
        BlockPos old = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos[] fresh = {new BlockPos(6, 1, RAIL_Z + 1), new BlockPos(4, 1, RAIL_Z + 1)};
        crop(helper, old, 1);
        Farm farm = farm(helper, spreader());
        MiniWarehouse.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 20));
        helper.runAfterDelay(3, () -> {
            fertilize(farm, FertilizeMode.AFTER_PLANTING, 0);
            for (BlockPos plot : fresh) {
                planted(helper, farm, plot);
            }
            TerminalWiring.when(helper, () -> held(farm.rack(), Items.BONE_MEAL) == 16 && out(helper) == 0
                    && age(helper, fresh[0]) >= 4 && age(helper, fresh[1]) >= 4, 550,
                    () -> "the fresh wheat should have two doses each, rack holds " + held(farm.rack(), Items.BONE_MEAL)
                            + " bone meal, ages " + age(helper, fresh[0]) + " and " + age(helper, fresh[1]) + ", out " + out(helper), () -> {
                helper.assertTrue(farm.head().unfedCount() == 0, "no plot should still be owed a dose");
                helper.assertTrue(farm.head().fertilizeTargetCount() == 0, "no plot needs a dose");
                helper.assertTrue(noItemsLying(helper), "nothing should drop on the ground");
                helper.succeed();
            });
        });
    }

    /** Stock equal to the reserve: no run, the rack keeps its bone meal and the status says below reserve. */
    @ServusGameTest(timeoutTicks = 200)
    public static void reserveRespected(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        Farm farm = farm(helper, spreader());
        MiniWarehouse.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 10));
        helper.runAfterDelay(3, () -> {
            fertilize(farm, FertilizeMode.AFTER_PLANTING, 10);
            planted(helper, farm, plot);
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(farm.head().fertilizeTargetCount() == 1, "the new wheat needs a dose");
                helper.assertTrue(out(helper) == 0 && network(helper).queuedJobs() == 0, "no Spreader should drive out");
                helper.assertTrue(held(farm.rack(), Items.BONE_MEAL) == 10, "the rack keeps its 10 bone meal");
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
        crop(helper, plots[0], 7);
        for (int i = 1; i < plots.length; i++) {
            crop(helper, plots[i], 0);
        }
        Farm farm = farm(helper, spreader());
        MiniWarehouse.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 64));
        helper.runAfterDelay(3, () -> {
            fertilize(farm, FertilizeMode.UNTIL_RIPE, 0);
            farm.head().stepFertilizePercent((50 - farm.head().fertilizePercent()) / 10);
            TerminalWiring.when(helper, () -> farm.head().ripeCount() >= 2 && out(helper) == 0 && network(helper).queuedJobs() == 0, 850,
                    () -> "two of four should be ripe and the Spreader home, ripe " + farm.head().ripeCount() + ", out " + out(helper), () -> {
                int stock = held(farm.rack(), Items.BONE_MEAL);
                helper.runAfterDelay(60, () -> {
                    helper.assertTrue(farm.head().ripeCount() < 4, "the doses should stop at the threshold, not ripen everything");
                    helper.assertTrue(held(farm.rack(), Items.BONE_MEAL) == stock, "no more bone meal should be taken once the share is reached");
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
     * takes one dose and the bone meal left goes back into the rack.
     */
    @ServusGameTest(timeoutTicks = 600)
    public static void ripePlotsUntouchedAndLeftoverPutAway(GameTestHelper helper) {
        BlockPos ripe = new BlockPos(5, 1, RAIL_Z - 1);
        BlockPos almost = new BlockPos(6, 1, RAIL_Z + 1);
        crop(helper, ripe, 7);
        crop(helper, almost, 5);
        Farm farm = farm(helper, spreader());
        MiniWarehouse.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 20));
        helper.runAfterDelay(3, () -> {
            fertilize(farm, FertilizeMode.UNTIL_RIPE, 0);
            farm.head().stepFertilizePercent(10);
            helper.assertTrue(farm.head().fertilizePercent() == 100, "the threshold should be 100 percent");
            TerminalWiring.when(helper, () -> held(farm.rack(), Items.BONE_MEAL) == 19 && out(helper) == 0 && age(helper, almost) == 7, 550,
                    () -> "one dose should be used and one put back, rack holds " + held(farm.rack(), Items.BONE_MEAL)
                            + ", out " + out(helper) + ", age " + age(helper, almost), () -> {
                helper.assertTrue(age(helper, ripe) == 7, "the ripe wheat stays ripe");
                helper.assertTrue(noItemsLying(helper), "nothing should drop on the ground");
                helper.assertTrue(network(helper).queuedJobs() == 0, "at 100 percent ripe no new run is queued");
                helper.succeed();
            });
        });
    }

    // Menu

    /** The fertilizer is chosen by id from the tag only; mode, threshold and reserve buttons work and the settings are saved. */
    @ServusGameTest(timeoutTicks = 100)
    public static void menuChoosesFertilizerAndSettings(GameTestHelper helper) {
        Farm farm = farm(helper, spreader());
        FieldHeadBlockEntity head = farm.head();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(HEAD).east()));
        FieldHeadMenu menu = new FieldHeadMenu(1, player.getInventory(), head);
        player.containerMenu = menu;
        helper.assertTrue(head.fertilizer().isEmpty() && head.fertilizeMode() == FertilizeMode.OFF && !head.fertilizing(),
                "a new head does not fertilize");
        helper.assertTrue(head.reserve() == StorageStatsManager.INSTANCE.fertilizerReserve(), "the reserve starts at fertilizer_reserve");
        helper.assertTrue(FieldHeadMenu.chooseFertilizer(player, id(Items.BONE_MEAL)), "bone meal should be taken");
        helper.assertTrue(head.fertilizer().is(Items.BONE_MEAL), "bone meal is the fertilizer");
        helper.assertTrue(!FieldHeadMenu.chooseFertilizer(player, id(Items.COBBLESTONE)), "cobblestone should be refused");
        helper.assertTrue(!FieldHeadMenu.chooseFertilizer(player, Optional.of(Identifier.fromNamespaceAndPath("servus_storage", "no_such_item"))),
                "an unknown id should be refused");
        helper.assertTrue(head.fertilizer().is(Items.BONE_MEAL), "refused ids change nothing");
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_FERTILIZE_MODE + FertilizeMode.UNTIL_RIPE.ordinal());
        helper.assertTrue(head.fertilizeMode() == FertilizeMode.UNTIL_RIPE && head.fertilizing(), "the mode button sets Until ripe share");
        int percent = head.fertilizePercent();
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_FERTILIZE_UP);
        helper.assertTrue(head.fertilizePercent() == Math.min(100, percent + 10), "+ raises the fertilize threshold");
        head.stepReserve(5 - head.reserve());
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_RESERVE);
        helper.assertTrue(head.reserve() == 0, "-10 from 5 stops at 0, got " + head.reserve());
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_RESERVE + 3);
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_RESERVE + 3);
        menu.clickMenuButton(player, FieldHeadMenu.BUTTON_RESERVE + 2);
        helper.assertTrue(head.reserve() == 21, "+10, +10 and +1 make 21, got " + head.reserve());
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
