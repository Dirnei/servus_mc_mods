package io.github.dirnei.mc.servus.shuttles.gametest;

import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.GARAGE;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.HEAD;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.RAIL_Z;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.farm;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.noItemsLying;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.out;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.field.FertilizeMode;
import io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldKind;
import io.github.dirnei.mc.servus.shuttles.field.FieldWork;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleWorkshopMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.FieldJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.FleetSaves;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleProfile;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

/**
 * Lumber shuttles on a Trees field of the farm of {@link FarmFixtures}: dirt
 * under the plots beside the farm rails, trees built by hand from logs and
 * leaves so their size is known, in a structure tall enough for them.
 */
public final class LumberGameTests {

    private static final String TALL = "empty_12x12x12";
    private static final String LIMIT_BATCH = "lumber_limit";
    private static final int OAK_HEIGHT = 4;

    private LumberGameTests() {
    }

    /**
     * The spot north of the rails for tree A: x 4 or 5 and z 4 or 5, whichever
     * is even in world coordinates, since spots follow the world grid.
     */
    private static BlockPos spot(GameTestHelper helper) {
        BlockPos corner = helper.absolutePos(new BlockPos(4, 1, 4));
        return new BlockPos(4 + Math.floorMod(corner.getX(), 2), 1, 4 + Math.floorMod(corner.getZ(), 2));
    }

    /** Tree B, outside the field at z 0 in line with tree A, whose small crown touches tree A's. */
    private static BlockPos neighbourOf(BlockPos spot) {
        return new BlockPos(spot.getX(), 1, 0);
    }

    private static ItemStack lumber() {
        return FarmFixtures.shuttleWith(ServusShuttlesItems.LUMBER.get());
    }

    /** Dirt under the plots beside the farm rails, x 4 to 9 and z 2 to 10, then the farm with that shuttle. */
    private static FarmFixtures.Farm treeFarm(GameTestHelper helper, ItemStack shuttle) {
        for (int x = 4; x <= 9; x++) {
            for (int z = 2; z <= 10; z++) {
                if (z != RAIL_Z) {
                    helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
                }
            }
        }
        return farm(helper, shuttle);
    }

    /** Makes the head a Trees field planting that sapling. */
    private static void trees(FieldHeadBlockEntity head, ItemStack sapling, boolean twoByTwo) {
        head.setKind(FieldKind.TREES);
        head.setSapling(sapling, twoByTwo);
    }

    private static Block oakLeaves() {
        return Blocks.OAK_LEAVES;
    }

    private static void leaf(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockState(pos).isAir()) {
            helper.setBlock(pos, oakLeaves().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
        }
    }

    /** An oak of {@link #OAK_HEIGHT} logs with a crown two wide around its top two logs and one wide above. */
    private static void oak(GameTestHelper helper, BlockPos base) {
        int top = base.getY() + OAK_HEIGHT - 1;
        for (int y = base.getY(); y <= top; y++) {
            helper.setBlock(new BlockPos(base.getX(), y, base.getZ()), Blocks.OAK_LOG.defaultBlockState());
        }
        for (int y = top - 1; y <= top + 1; y++) {
            int radius = y > top ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    leaf(helper, new BlockPos(base.getX() + dx, y, base.getZ() + dz));
                }
            }
        }
    }

    /** Tree B: a trunk at z 0 with a crown one wide on its south side that reaches up to tree A's crown. */
    private static void neighbour(GameTestHelper helper, BlockPos spot) {
        BlockPos base = neighbourOf(spot);
        int top = base.getY() + OAK_HEIGHT - 1;
        for (int y = base.getY(); y <= top; y++) {
            helper.setBlock(new BlockPos(base.getX(), y, base.getZ()), Blocks.OAK_LOG.defaultBlockState());
        }
        for (int y = top - 1; y <= top; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int z = base.getZ() + 1; z <= spot.getZ() - 3; z++) {
                    leaf(helper, new BlockPos(base.getX() + dx, y, z));
                }
            }
        }
    }

    private static boolean trunkStands(GameTestHelper helper, BlockPos base) {
        for (int y = 0; y < OAK_HEIGHT; y++) {
            if (!helper.getBlockState(base.above(y)).is(Blocks.OAK_LOG)) {
                return false;
            }
        }
        return true;
    }

    private static boolean trunkGone(GameTestHelper helper, BlockPos base) {
        for (int y = 0; y < OAK_HEIGHT; y++) {
            if (helper.getBlockState(base.above(y)).is(BlockTags.LOGS)) {
                return false;
            }
        }
        return true;
    }

    /** The Lumber module goes in as the tool; it makes the shuttle field only, too wide for lifts and fit only for tree jobs. */
    @ServusGameTest
    public static void workshopFitsLumber(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ShuttleWorkshopMenu menu = ShuttleModuleGameTests.menu(helper, player, new BlockPos(5, 1, 5));
        player.getInventory().setItem(0, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        player.getInventory().setItem(1, new ItemStack(ServusShuttlesItems.LUMBER.get()));
        int hotbar = ShuttleWorkshopMenu.INVENTORY_START + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        helper.assertTrue(ShuttleModules.of(menu.shuttle()).tool().equals(Optional.of(ServusShuttlesItems.LUMBER.get())),
                "the Lumber module is the shuttle's tool");
        ShuttleProfile profile = ShuttleModules.profile(menu.shuttle(), ShuttlesStatsManager.INSTANCE.shuttleTiers());
        helper.assertTrue(profile.fieldOnly() && profile.liftless(), "a Lumber shuttle is field only and too wide for lifts");
        helper.assertTrue(JobNeeds.LUMBER.fits(profile), "tree jobs fit it");
        helper.assertTrue(!JobNeeds.WAREHOUSE.fits(profile) && !JobNeeds.HARVESTER.fits(profile) && !JobNeeds.SPREADER.fits(profile),
                "warehouse jobs, crop jobs and fertilize runs do not");
        helper.succeed();
    }

    /** Choosing a sapling on a serving Trees field opens its empty spots on grass without any block changing. */
    @ServusGameTest(template = TALL, timeoutTicks = 40)
    public static void choosingSaplingOpensSpots(GameTestHelper helper) {
        FarmFixtures.Farm farm = treeFarm(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        helper.runAfterDelay(2, () -> {
            FieldHeadBlockEntity head = farm.head();
            head.setKind(FieldKind.TREES);
            helper.assertTrue(head.plantableCount() == 0, "no spot is open before a sapling is chosen");
            head.setSapling(new ItemStack(Items.OAK_SAPLING), false);
            helper.assertTrue(head.plantableCount() > 0, "the spots should open once oak is chosen");
            helper.assertTrue(head.work() != FieldWork.NO_PLOTS, "the status should not say there is no spot, it says " + head.work());
            head.setHarvestOnly(true);
            helper.assertTrue(head.plantableCount() == 0, "fell only plants nothing");
            helper.succeed();
        });
    }

    /** An oak sapling on a spot grown by bone meal counts as one ripe plot, learned from the grow event. */
    @ServusGameTest(template = TALL, timeoutTicks = 200)
    public static void boneMealGrowsOakRipe(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        FarmFixtures.Farm farm = treeFarm(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            helper.setBlock(spot, Blocks.OAK_SAPLING.defaultBlockState());
            farm.head().recheck(helper.absolutePos(spot));
            Waits.when(helper, () -> farm.head().plantedCount() == 1, 10, () -> "the sapling should count as planted", () ->
                    Waits.when(helper, () -> {
                        if (!helper.getBlockState(spot).is(BlockTags.LOGS)) {
                            BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), helper.getLevel(), helper.absolutePos(spot), null);
                            return false;
                        }
                        return farm.head().ripeCount() == 1;
                    }, 150, () -> "the oak should grow and count ripe, ripe " + farm.head().ripeCount() + ", block " + helper.getBlockState(spot),
                            helper::succeed));
        });
    }

    /** Trees standing when the field is set up count as ready to fell after the first pass. */
    @ServusGameTest(template = TALL, timeoutTicks = 100)
    public static void oldTreesCountAfterFirstPass(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        oak(helper, spot);
        oak(helper, new BlockPos(6, 1, RAIL_Z + 2));
        FarmFixtures.Farm farm = treeFarm(helper, new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()));
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), ItemStack.EMPTY, false);
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> farm.head().ripeCount() == 2, 20, () -> "two trees should be ready to fell, ripe " + farm.head().ripeCount(),
                    () -> {
                        helper.assertTrue(farm.head().harvestDue(), "fell only with grown trees signals harvest due");
                        helper.succeed();
                    });
        });
    }

    /**
     * A Lumber shuttle fells an oak whose crown touches a neighbour's: the
     * oak's logs and the leaves only it held are gone, the neighbour keeps its
     * trunk and the leaves it holds, logs reach the To storage buffer, nothing
     * drops, and the spot holds a new oak sapling.
     */
    @ServusGameTest(template = TALL, timeoutTicks = 700)
    public static void lumberFellsOak(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        oak(helper, spot);
        neighbour(helper, spot);
        FarmFixtures.Farm farm = treeFarm(helper, lumber());
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            farm.head().fillSupply(SupplyBuffer.Part.SEEDS, Items.OAK_SAPLING, 64);
            Waits.when(helper, () -> trunkGone(helper, spot) && helper.getBlockState(spot).is(Blocks.OAK_SAPLING) && out(helper) == 0, 650,
                    () -> "the oak should be felled and replanted, out " + out(helper) + ", spot " + helper.getBlockState(spot)
                            + ", work " + farm.head().work(), () -> {
                        helper.assertTrue(HarvesterGameTests.toStorage(farm, Items.OAK_LOG) == OAK_HEIGHT,
                                "the four logs wait in the To storage buffer, got " + HarvesterGameTests.toStorage(farm, Items.OAK_LOG));
                        helper.assertTrue(noItemsLying(helper), "nothing drops on the ground");
                        helper.assertTrue(trunkStands(helper, neighbourOf(spot)), "the neighbour keeps its trunk");
                        helper.assertTrue(helper.getBlockState(neighbourOf(spot).offset(0, OAK_HEIGHT - 1, 1)).is(oakLeaves()),
                                "the neighbour keeps the leaves it holds");
                        for (BlockPos far : new BlockPos[] {spot.offset(0, 3, 2), spot.offset(2, 3, 2), spot.offset(-2, 3, 2), spot.offset(-2, 2, 1)}) {
                            helper.assertTrue(!helper.getBlockState(far).is(oakLeaves()), "no leaf only the oak held stays at " + far);
                        }
                        helper.succeed();
                    });
        });
    }

    /**
     * A 2x2 dark oak needs a Range Upgrade, and a square is planted only with
     * four saplings, all four at once.
     */
    @ServusGameTest(template = TALL, timeoutTicks = 600)
    public static void squareNeedsRangeAndFour(GameTestHelper helper) {
        // Squares follow the world grid: the corner is the plot north of the rails whose world x and z divide by 3.
        BlockPos origin = helper.absolutePos(new BlockPos(4, 1, 2));
        int cornerX = 4 + Math.floorMod(-origin.getX(), 3);
        int cornerZ = 2 + Math.floorMod(-origin.getZ(), 3);
        BlockPos[] square = {new BlockPos(cornerX, 1, cornerZ), new BlockPos(cornerX + 1, 1, cornerZ), new BlockPos(cornerX, 1, cornerZ + 1),
                new BlockPos(cornerX + 1, 1, cornerZ + 1)};
        for (int x = cornerX; x <= cornerX + 1; x++) {
            for (int z = cornerZ; z < RAIL_Z; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
            }
        }
        FarmFixtures.Farm farm = farm(helper, lumber());
        FieldHeadBlockEntity head = farm.head();
        ItemStack darkOak = new ItemStack(Items.DARK_OAK_SAPLING);
        helper.runAfterDelay(2, () -> {
            head.setKind(FieldKind.TREES);
            helper.assertTrue(!head.setSapling(darkOak, true), "a 2x2 sapling needs a Range Upgrade");
            helper.assertTrue(!head.setSapling(darkOak, false), "dark oak grows only from squares");
            head.rangeUpgrades().setStackInSlot(0, new ItemStack(ServusShuttlesItems.RANGE_UPGRADE.get(), 2));
            helper.assertTrue(head.setSapling(darkOak, true) && head.twoByTwo(), "with a Range Upgrade dark oak 2x2 is taken");
            helper.runAfterDelay(2, () -> {
                head.fillSupply(SupplyBuffer.Part.SEEDS, Items.DARK_OAK_SAPLING, 3);
                helper.runAfterDelay(80, () -> {
                    for (BlockPos plot : square) {
                        helper.assertTrue(helper.getBlockState(plot).isAir(), "three saplings plant nothing at " + plot);
                    }
                    helper.assertTrue(head.plantableCount() == 4, "the square counts four plots to plant, got " + head.plantableCount());
                    head.fillSupply(SupplyBuffer.Part.SEEDS, Items.DARK_OAK_SAPLING, 1);
                    Waits.when(helper, () -> {
                        for (BlockPos plot : square) {
                            if (!helper.getBlockState(plot).is(Blocks.DARK_OAK_SAPLING)) {
                                return false;
                            }
                        }
                        return out(helper) == 0;
                    }, 450, () -> "the four saplings should fill the square, plantable " + head.plantableCount() + ", out " + out(helper), () -> {
                        helper.assertTrue(head.plantedCount() == 4, "four plots planted, got " + head.plantedCount());
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** A tree over the log limit stays standing and the status says a tree is too large to fell. */
    @ServusGameTest(template = TALL, timeoutTicks = 400, batch = LIMIT_BATCH)
    public static void treeOverLimitStays(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        ShuttlesStatsManager.INSTANCE.setMaxTreeLogs(OAK_HEIGHT - 1);
        oak(helper, spot);
        FarmFixtures.Farm farm = treeFarm(helper, lumber());
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            Waits.when(helper, () -> farm.head().work() == FieldWork.TREE_TOO_LARGE && out(helper) == 0, 350,
                    () -> "the tree should be given up on, work " + farm.head().work() + ", out " + out(helper), () -> {
                        ShuttlesStatsManager.INSTANCE.setMaxTreeLogs(ShuttlesStatsManager.DEFAULT_MAX_TREE_LOGS);
                        helper.assertTrue(trunkStands(helper, spot), "the oak stays standing");
                        helper.assertTrue(farm.head().ripeCount() == 0, "it no longer counts as ready to fell");
                        helper.assertTrue(!farm.head().work().problem(), "a tree too large is a warning, not a problem");
                        helper.succeed();
                    });
        });
    }

    /** Logs a player stacked without leaves are not felled. */
    @ServusGameTest(template = TALL, timeoutTicks = 400)
    public static void logPillarStays(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        for (int y = 0; y < 3; y++) {
            helper.setBlock(spot.above(y), Blocks.OAK_LOG.defaultBlockState());
        }
        FarmFixtures.Farm farm = treeFarm(helper, lumber());
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            Waits.when(helper, () -> farm.head().ripeCount() == 0 && out(helper) == 0 && !FarmFixtures.network(helper).hasFieldJob(farm.head().getBlockPos()),
                    350, () -> "the pillar should be given up on, ripe " + farm.head().ripeCount() + ", out " + out(helper), () -> {
                        for (int y = 0; y < 3; y++) {
                            helper.assertTrue(helper.getBlockState(spot.above(y)).is(Blocks.OAK_LOG), "the pillar stays");
                        }
                        helper.succeed();
                    });
        });
    }

    /** A Harvester takes no tree job: with only a Harvester the grown oak stays and the status asks for a Lumber shuttle. */
    @ServusGameTest(template = TALL, timeoutTicks = 200)
    public static void harvesterTakesNoTreeJob(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        oak(helper, spot);
        FarmFixtures.Farm farm = treeFarm(helper, FarmFixtures.shuttleWith(ServusShuttlesItems.HARVESTER.get()));
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(out(helper) == 0, "the harvester stays in its garage");
                helper.assertTrue(trunkStands(helper, spot), "the oak stands");
                helper.assertTrue(farm.head().work() == FieldWork.NO_LUMBER, "the status asks for a Lumber shuttle, got " + farm.head().work());
                helper.succeed();
            });
        });
    }

    /** A Lumber shuttle takes no crop job: ripe wheat stays and the status asks for a Harvester. */
    @ServusGameTest(timeoutTicks = 200)
    public static void lumberTakesNoCropJob(GameTestHelper helper) {
        BlockPos plot = new BlockPos(5, 1, RAIL_Z - 1);
        FieldFixtures.crop(helper, plot, Blocks.WHEAT, 7);
        FarmFixtures.Farm farm = farm(helper, lumber());
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            farm.head().setCrop(new ItemStack(Items.WHEAT_SEEDS));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(out(helper) == 0, "the Lumber shuttle stays in its garage");
                helper.assertTrue(helper.getBlockState(plot).is(Blocks.WHEAT), "the wheat stands");
                helper.assertTrue(farm.head().work() == FieldWork.NO_HARVESTER, "the status asks for a Harvester, got " + farm.head().work());
                helper.succeed();
            });
        });
    }

    /** The Spreader doses a sapling of a Trees field until it grows into a tree and then stops. */
    @ServusGameTest(template = TALL, timeoutTicks = 1200)
    public static void spreaderGrowsSapling(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        ShuttlesStatsManager.INSTANCE.setFertilizeInterval(20);
        ShuttlesStatsManager.INSTANCE.setDosesPerPlot(2);
        FarmFixtures.Farm farm = treeFarm(helper, FarmFixtures.shuttleWith(ServusShuttlesItems.SPREADER.get()));
        FarmFixtures.carrier(farm);
        WarehouseFixtures.put(farm.rack(), new ItemStack(Items.BONE_MEAL, 64));
        helper.runAfterDelay(2, () -> {
            FieldHeadBlockEntity head = farm.head();
            trees(head, new ItemStack(Items.OAK_SAPLING), false);
            head.setFertilizer(new ItemStack(Items.BONE_MEAL));
            head.setFertilizeMode(FertilizeMode.UNTIL_RIPE);
            head.setReserve(0);
            helper.setBlock(spot, Blocks.OAK_SAPLING.defaultBlockState());
            head.recheck(helper.absolutePos(spot));
            Waits.when(helper, () -> helper.getBlockState(spot).is(BlockTags.LOGS) && head.ripeCount() == 1 && out(helper) == 0, 1100,
                    () -> "the Spreader should grow the oak and go home, block " + helper.getBlockState(spot) + ", out " + out(helper)
                            + ", fertilize " + head.fertilizeStatus(), () -> {
                        helper.assertTrue(head.fertilizeTargetCount() == 0, "a grown tree gets no more doses");
                        helper.succeed();
                    });
        });
    }

    /** A Trees field uses no water, so its water stays and its rails do not age. */
    @ServusGameTest(timeoutTicks = 200)
    public static void treesUseNoWater(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, lumber());
        helper.runAfterDelay(2, () -> {
            FieldFixtures.water(farm.head());
            trees(farm.head(), new ItemStack(Items.BIRCH_SAPLING), false);
            int water = farm.head().water();
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(farm.head().water() == water, "the water stays, " + farm.head().water() + " of " + water);
                helper.assertTrue(!farm.head().hydrated() && farm.head().waterPerMinute() == 0, "trees are never watered");
                helper.assertTrue(farm.head().corrosion() == FieldCorrosion.COPPER && farm.head().agedWater() == 0, "the rails do not age");
                helper.succeed();
            });
        });
    }

    /** A Lumber shuttle whose garage reloads while it carries logs brings them to the head and finishes the run. */
    @ServusGameTest(template = TALL, timeoutTicks = 800)
    public static void runSurvivesReload(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        oak(helper, spot);
        FarmFixtures.Farm farm = treeFarm(helper, lumber());
        BlockPos garage = helper.absolutePos(GARAGE);
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> trunkGone(helper, spot) && FarmFixtures.network(helper).garageOut(garage) > 0,
                    500, () -> "the Lumber shuttle should fell the oak and still be out", () -> {
                        FleetPersistenceGameTests.reload(helper, GARAGE, tag -> { }, () -> { });
                        Waits.when(helper, () -> HarvesterGameTests.toStorage(farm, Items.OAK_LOG) == OAK_HEIGHT && out(helper) == 0, 500,
                                () -> "the logs should reach the head, it holds " + HarvesterGameTests.toStorage(farm, Items.OAK_LOG)
                                        + ", out " + out(helper), () -> {
                                    helper.assertTrue(noItemsLying(helper), "nothing drops");
                                    helper.succeed();
                                });
                    });
        });
    }

    /** A head saved before field kinds existed loads as a Crops field with its crop; a field job saved without a kind is a crop job. */
    @ServusGameTest(timeoutTicks = 60)
    public static void oldSavesLoadAsCrops(GameTestHelper helper) {
        FarmFixtures.Farm farm = farm(helper, lumber());
        helper.runAfterDelay(2, () -> {
            farm.head().setCrop(new ItemStack(Items.CARROT));
            FieldHeadBlockEntity loaded = FleetPersistenceGameTests.reload(helper, HEAD, tag -> tag.remove("Kind"), () -> { });
            helper.assertTrue(loaded.kind() == FieldKind.CROPS && loaded.crop().is(Items.CARROT), "the old head is a Crops field with its carrots");
            trees(loaded, new ItemStack(Items.SPRUCE_SAPLING), false);
            FieldHeadBlockEntity again = FleetPersistenceGameTests.reload(helper, HEAD, tag -> { }, () -> { });
            helper.assertTrue(again.kind() == FieldKind.TREES && again.crop().is(Items.SPRUCE_SAPLING), "a Trees field keeps its kind and sapling");
            BlockPos head = farm.head().getBlockPos();
            CompoundTag job = FleetSaves.saveJob(new FieldJob(7, head, FieldKind.TREES), helper.getLevel().registryAccess());
            helper.assertTrue(FleetSaves.loadJob(job, helper.getLevel().registryAccess()).map(LumberGameTests::kindOf).orElse(null) == FieldKind.TREES,
                    "a tree job keeps its kind");
            job.remove("Kind");
            helper.assertTrue(FleetSaves.loadJob(job, helper.getLevel().registryAccess()).map(LumberGameTests::kindOf).orElse(null) == FieldKind.CROPS,
                    "a job saved without a kind is a crop job");
            helper.assertTrue(helper.getBlockState(HEAD).is(ServusShuttlesBlocks.FIELD_HEAD.get()), "the head is still there");
            helper.succeed();
        });
    }

    private static FieldKind kindOf(ShuttleJob job) {
        return job instanceof FieldJob field ? field.kind() : null;
    }
}
