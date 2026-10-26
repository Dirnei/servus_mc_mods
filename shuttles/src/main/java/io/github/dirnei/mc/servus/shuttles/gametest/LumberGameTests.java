package io.github.dirnei.mc.servus.shuttles.gametest;

import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.GARAGE;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.HEAD;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.RAIL_Z;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.farm;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.noItemsLying;
import static io.github.dirnei.mc.servus.shuttles.gametest.FarmFixtures.out;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
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
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleProfile;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

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
    @ServusGameTest(template = TALL, timeoutTicks = 900)
    public static void lumberFellsOak(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        oak(helper, spot);
        neighbour(helper, spot);
        FarmFixtures.Farm farm = treeFarm(helper, lumber());
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            farm.head().fillSupply(SupplyBuffer.Part.SEEDS, Items.OAK_SAPLING, 64);
            Waits.when(helper, () -> trunkGone(helper, spot) && helper.getBlockState(spot).is(Blocks.OAK_SAPLING) && out(helper) == 0, 850,
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
    @ServusGameTest(template = TALL, timeoutTicks = 1000)
    public static void runSurvivesReload(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        oak(helper, spot);
        FarmFixtures.Farm farm = treeFarm(helper, lumber());
        BlockPos garage = helper.absolutePos(GARAGE);
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            farm.head().setHarvestOnly(true);
            Waits.when(helper, () -> trunkGone(helper, spot) && FarmFixtures.network(helper).garageOut(garage) > 0,
                    600, () -> "the Lumber shuttle should fell the oak and still be out", () -> {
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

    /** The felling the farm's garage shows for a shuttle now, if any. */
    private static Optional<ShuttleView.Felling> felling(GameTestHelper helper) {
        return FarmFixtures.network(helper).shuttleViews(helper.absolutePos(GARAGE)).stream()
                .flatMap(view -> view.felling().stream()).findFirst();
    }

    private static boolean inPhase(GameTestHelper helper, ShuttleView.Felling.Phase phase) {
        return felling(helper).filter(fell -> fell.phase() == phase).isPresent();
    }

    /** An oak on the spot, a Lumber shuttle with oak saplings in the head's buffer. */
    private static FarmFixtures.Farm oakField(GameTestHelper helper, BlockPos spot, ItemStack shuttle) {
        oak(helper, spot);
        FarmFixtures.Farm farm = treeFarm(helper, shuttle);
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            farm.head().fillSupply(SupplyBuffer.Part.SEEDS, Items.OAK_SAPLING, 64);
        });
        return farm;
    }

    /**
     * The tree's blocks are gone as the topple part starts and its drops are
     * aboard; the spot is planted only once the topple part is over.
     */
    @ServusGameTest(template = TALL, timeoutTicks = 900)
    public static void toppleRemovesThenPlants(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        FarmFixtures.Farm farm = oakField(helper, spot, lumber());
        Waits.when(helper, () -> inPhase(helper, ShuttleView.Felling.Phase.TOPPLE), 600, () -> "the oak should start to topple", () -> {
            long toppled = helper.getLevel().getGameTime();
            helper.assertTrue(trunkGone(helper, spot), "the logs are gone when the topple part starts");
            helper.assertTrue(!helper.getBlockState(spot).is(Blocks.OAK_SAPLING), "the spot is not planted yet");
            helper.assertTrue(FarmFixtures.network(helper).shuttleViews(helper.absolutePos(GARAGE)).stream().anyMatch(view -> !view.cargo().isEmpty()),
                    "the drops are aboard");
            Waits.when(helper, () -> helper.getBlockState(spot).is(Blocks.OAK_SAPLING), 200, () -> "the spot should be planted after the topple", () -> {
                long waited = helper.getLevel().getGameTime() - toppled;
                helper.assertTrue(waited >= ShuttlesStatsManager.INSTANCE.toppleTicks() - 1,
                        "the sapling comes after the topple part, waited " + waited);
                helper.assertTrue(noItemsLying(helper), "nothing drops");
                helper.succeed();
            });
        });
    }

    /** Handling shortens the saw part and leaves the topple part as long as it is. */
    @ServusGameTest(template = TALL, timeoutTicks = 900)
    public static void handlingShortensOnlyTheSaw(GameTestHelper helper) {
        ItemStack shuttle = new ItemStack(ServusShuttlesItems.SHUTTLE_MK3.get());
        ShuttleModules.set(shuttle, new ShuttleModules(Optional.of(ServusShuttlesItems.LUMBER.get()), 0, 2));
        ShuttleProfile profile = ShuttleModules.profile(shuttle, ShuttlesStatsManager.INSTANCE.shuttleTiers());
        int saw = ShuttleTiers.workTicks(ShuttlesStatsManager.INSTANCE.fellTicks(), profile.handling());
        helper.assertTrue(saw < ShuttlesStatsManager.INSTANCE.fellTicks(), "two Handling upgrades shorten the saw part, got " + saw);
        oakField(helper, spot(helper), shuttle);
        Waits.when(helper, () -> inPhase(helper, ShuttleView.Felling.Phase.SAW), 500, () -> "the shuttle should start to saw", () -> {
            helper.assertTrue(felling(helper).get().ticks() == saw, "the saw part takes " + saw + " ticks, got " + felling(helper).get().ticks());
            Waits.when(helper, () -> inPhase(helper, ShuttleView.Felling.Phase.TOPPLE), 200, () -> "the oak should topple", () -> {
                helper.assertTrue(felling(helper).get().ticks() == ShuttlesStatsManager.INSTANCE.toppleTicks(),
                        "the topple part keeps its length, got " + felling(helper).get().ticks());
                helper.succeed();
            });
        });
    }

    /** A tree a player breaks while the shuttle saws is not felled: no topple, and the spot is planted. */
    @ServusGameTest(template = TALL, timeoutTicks = 900)
    public static void brokenTreeEndsTheSaw(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        FarmFixtures.Farm farm = oakField(helper, spot, lumber());
        boolean[] toppled = {false};
        Waits.when(helper, () -> inPhase(helper, ShuttleView.Felling.Phase.SAW), 600, () -> "the shuttle should start to saw", () -> {
            for (int y = 0; y < OAK_HEIGHT; y++) {
                helper.setBlock(spot.above(y), Blocks.AIR.defaultBlockState());
            }
            farm.head().recheck(helper.absolutePos(spot));
            Waits.when(helper, () -> {
                toppled[0] |= inPhase(helper, ShuttleView.Felling.Phase.TOPPLE);
                return helper.getBlockState(spot).is(Blocks.OAK_SAPLING) && out(helper) == 0;
            }, 400, () -> "the shuttle should plant the empty spot and go home, out " + out(helper), () -> {
                helper.assertTrue(!toppled[0], "nothing toppled");
                helper.succeed();
            });
        });
    }

    /** A run saved while the tree topples goes on after a reload and brings the logs home. */
    @ServusGameTest(template = TALL, timeoutTicks = 1000)
    public static void reloadDuringTopple(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        FarmFixtures.Farm farm = oakField(helper, spot, lumber());
        Waits.when(helper, () -> inPhase(helper, ShuttleView.Felling.Phase.TOPPLE), 600, () -> "the oak should start to topple", () -> {
            FleetPersistenceGameTests.reload(helper, GARAGE, tag -> { }, () -> { });
            Waits.when(helper, () -> helper.getBlockState(spot).is(Blocks.OAK_SAPLING) && out(helper) == 0
                            && HarvesterGameTests.toStorage(farm, Items.OAK_LOG) == OAK_HEIGHT, 600,
                    () -> "the run should go on, plant the spot and bring the logs home, logs " + HarvesterGameTests.toStorage(farm, Items.OAK_LOG)
                            + ", out " + out(helper), () -> {
                        helper.assertTrue(noItemsLying(helper), "nothing drops");
                        helper.succeed();
                    });
        });
    }

    /**
     * Two oaks on neighbouring spots, a branch of one touching the other's
     * trunk: both come down whole, no stump stays on either spot, and both
     * spots hold a new sapling.
     */
    @ServusGameTest(template = TALL, timeoutTicks = 1200)
    public static void branchIntoNeighbourLeavesNoStump(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        BlockPos east = spot.offset(2, 0, 0);
        BlockPos branch = spot.offset(1, 2, 0);
        oak(helper, east);
        helper.setBlock(branch, Blocks.OAK_LOG.defaultBlockState());
        FarmFixtures.Farm farm = oakField(helper, spot, roomyLumber());
        Waits.when(helper, () -> trunkGone(helper, spot) && trunkGone(helper, east) && helper.getBlockState(spot).is(Blocks.OAK_SAPLING)
                        && helper.getBlockState(east).is(Blocks.OAK_SAPLING) && out(helper) == 0, 1150,
                () -> "both oaks should be felled and replanted, spot " + helper.getBlockState(spot) + ", east " + helper.getBlockState(east)
                        + ", work " + farm.head().work() + ", out " + out(helper), () -> {
                    helper.assertTrue(!helper.getBlockState(branch).is(BlockTags.LOGS), "the branch is gone");
                    helper.assertTrue(HarvesterGameTests.toStorage(farm, Items.OAK_LOG) == 2 * OAK_HEIGHT + 1,
                            "both trunks and the branch are unloaded, got " + HarvesterGameTests.toStorage(farm, Items.OAK_LOG));
                    helper.assertTrue(noItemsLying(helper), "nothing drops");
                    helper.succeed();
                });
    }

    /**
     * A bee nest on the trunk goes with the tree: the nest and the bees in it
     * are gone and, as for a nest broken without Silk Touch, nothing of it
     * reaches the cargo.
     */
    @ServusGameTest(template = TALL, timeoutTicks = 900)
    public static void beeNestGoesWithTheTree(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        BlockPos nest = spot.offset(1, 1, 0);
        FarmFixtures.Farm farm = oakField(helper, spot, lumber());
        helper.setBlock(nest, Blocks.BEE_NEST.defaultBlockState());
        BeehiveBlockEntity hive = helper.getBlockEntity(nest, BeehiveBlockEntity.class);
        hive.storeBee(BeehiveBlockEntity.Occupant.create(0));
        hive.storeBee(BeehiveBlockEntity.Occupant.create(0));
        Waits.when(helper, () -> trunkGone(helper, spot) && helper.getBlockState(spot).is(Blocks.OAK_SAPLING) && out(helper) == 0, 850,
                () -> "the oak should be felled and replanted, out " + out(helper) + ", spot " + helper.getBlockState(spot), () -> {
                    helper.assertTrue(helper.getBlockState(nest).isAir(), "the nest is gone, got " + helper.getBlockState(nest));
                    helper.assertTrue(helper.getEntities(EntityType.BEE).isEmpty(), "no bee is left flying");
                    helper.assertTrue(HarvesterGameTests.toStorage(farm, Items.BEE_NEST) == 0
                            && HarvesterGameTests.toStorage(farm, Items.HONEYCOMB) == 0, "the nest gives nothing");
                    helper.assertTrue(HarvesterGameTests.toStorage(farm, Items.OAK_LOG) == OAK_HEIGHT, "the logs are unloaded");
                    helper.assertTrue(noItemsLying(helper), "nothing drops");
                    helper.succeed();
                });
    }

    /** A Mk3 Lumber shuttle with two Handling upgrades: room for two oaks' drops, and Handling that must not shorten unfolding. */
    private static ItemStack roomyLumber() {
        ItemStack shuttle = new ItemStack(ServusShuttlesItems.SHUTTLE_MK3.get());
        ShuttleModules.set(shuttle, new ShuttleModules(Optional.of(ServusShuttlesItems.LUMBER.get()), 0, 2));
        return shuttle;
    }

    /**
     * What the farm's shuttle did, tick by tick: each felling phase it
     * showed, apart from standing ready, and MOVE while it drove, each
     * written once for as long as it lasts; the ticks each unfold and fold
     * part was given; and whether it ever moved while unfolding or folding.
     */
    private static final class Record {
        final List<String> steps = new ArrayList<>();
        final List<Integer> unfoldTicks = new ArrayList<>();
        boolean movedUnfolded;

        /** Starts recording; called as the test starts, since a running tick may not add tick work. */
        static Record of(GameTestHelper helper) {
            Record record = new Record();
            helper.onEachTick(() -> record.tick(helper));
            return record;
        }

        private void tick(GameTestHelper helper) {
            Optional<ShuttleView> view = helper.getBlockEntity(FarmFixtures.CONTROLLER, WarehouseControllerBlockEntity.class).network()
                    .flatMap(network -> network.shuttleViews(helper.absolutePos(GARAGE)).stream().findFirst());
            if (view.isEmpty()) {
                return;
            }
            Optional<ShuttleView.Felling> felling = view.get().felling();
            String step = felling.map(fell -> fell.phase().name()).orElse(view.get().moving() ? "MOVE" : null);
            if (felling.isPresent() && view.get().moving()) {
                movedUnfolded = true;
            }
            if (step == null || step.equals(ShuttleView.Felling.Phase.READY.name()) || (!steps.isEmpty() && steps.getLast().equals(step))) {
                return;
            }
            steps.add(step);
            felling.filter(fell -> fell.phase() == ShuttleView.Felling.Phase.UNFOLD || fell.phase() == ShuttleView.Felling.Phase.FOLD)
                    .ifPresent(fell -> unfoldTicks.add(fell.ticks()));
        }

        void clear() {
            steps.clear();
            unfoldTicks.clear();
        }

        /** The steps from the first unfold to the last fold. */
        String felling() {
            int from = steps.indexOf(ShuttleView.Felling.Phase.UNFOLD.name());
            int to = steps.lastIndexOf(ShuttleView.Felling.Phase.FOLD.name());
            return from < 0 || to < from ? "" : String.join(" ", steps.subList(from, to + 1));
        }

        int count(ShuttleView.Felling.Phase phase) {
            return (int) steps.stream().filter(phase.name()::equals).count();
        }
    }

    /**
     * Two oaks worked from one rail: the shuttle unfolds once, in
     * {@code unfold_ticks} that Handling does not shorten, fells both without
     * folding in between, and folds once before it drives off to unload,
     * standing still while it unfolds and folds.
     */
    @ServusGameTest(template = TALL, timeoutTicks = 1100)
    public static void twoOaksAtOneRailUnfoldOnce(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        BlockPos south = new BlockPos(spot.getX(), 1, 2 * RAIL_Z - spot.getZ());
        oak(helper, south);
        FarmFixtures.Farm farm = oakField(helper, spot, roomyLumber());
        Record record = Record.of(helper);
        Waits.when(helper, () -> trunkGone(helper, spot) && trunkGone(helper, south) && out(helper) == 0, 1050,
                () -> "both oaks should be felled, steps " + record.steps, () -> {
                    helper.assertTrue(record.felling().equals("UNFOLD SAW TOPPLE SAW TOPPLE FOLD"),
                            "one unfold, two trees, one fold, got " + record.steps);
                    helper.assertTrue(record.unfoldTicks.stream().allMatch(ticks -> ticks == ShuttlesStatsManager.INSTANCE.unfoldTicks()),
                            "unfolding and folding take unfold_ticks, got " + record.unfoldTicks);
                    helper.assertTrue(!record.movedUnfolded, "the shuttle stands still while it is unfolded");
                    helper.assertTrue(HarvesterGameTests.toStorage(farm, Items.OAK_LOG) == 2 * OAK_HEIGHT,
                            "the logs of both oaks are unloaded, got " + HarvesterGameTests.toStorage(farm, Items.OAK_LOG));
                    helper.succeed();
                });
    }

    /** Oaks at two rails: the shuttle folds before it drives to the second rail and unfolds again there. */
    @ServusGameTest(template = TALL, timeoutTicks = 1200)
    public static void foldsBeforeTheNextRail(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        BlockPos east = spot.offset(2, 0, 0);
        oak(helper, east);
        oakField(helper, spot, roomyLumber());
        Record record = Record.of(helper);
        Waits.when(helper, () -> trunkGone(helper, spot) && trunkGone(helper, east) && out(helper) == 0, 1150,
                () -> "both oaks should be felled, steps " + record.steps, () -> {
                    helper.assertTrue(record.felling().equals("UNFOLD SAW TOPPLE FOLD MOVE UNFOLD SAW TOPPLE FOLD"),
                            "fold, drive, unfold again, got " + record.steps);
                    helper.assertTrue(!record.movedUnfolded, "the shuttle stands still while it is unfolded");
                    helper.succeed();
                });
    }

    /** A stop where the shuttle only plants saplings does not unfold. */
    @ServusGameTest(template = TALL, timeoutTicks = 500)
    public static void plantingDoesNotUnfold(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        FarmFixtures.Farm farm = treeFarm(helper, lumber());
        Record record = Record.of(helper);
        helper.runAfterDelay(2, () -> {
            trees(farm.head(), new ItemStack(Items.OAK_SAPLING), false);
            farm.head().fillSupply(SupplyBuffer.Part.SEEDS, Items.OAK_SAPLING, 64);
            Waits.when(helper, () -> helper.getBlockState(spot).is(Blocks.OAK_SAPLING) && out(helper) == 0, 450,
                    () -> "the spot should be planted, out " + out(helper), () -> {
                        helper.assertTrue(record.count(ShuttleView.Felling.Phase.UNFOLD) == 0 && record.count(ShuttleView.Felling.Phase.FOLD) == 0,
                                "planting needs no crane, got " + record.steps);
                        helper.succeed();
                    });
        });
    }

    /** A run saved while the shuttle unfolds restores folded: it unfolds again from the start, then fells the oak. */
    @ServusGameTest(template = TALL, timeoutTicks = 1000)
    public static void reloadDuringUnfoldRestoresFolded(GameTestHelper helper) {
        BlockPos spot = spot(helper);
        FarmFixtures.Farm farm = oakField(helper, spot, lumber());
        Record record = Record.of(helper);
        Waits.when(helper, () -> felling(helper).filter(fell -> fell.phase() == ShuttleView.Felling.Phase.UNFOLD && fell.elapsed() > 5).isPresent(),
                500, () -> "the shuttle should start to unfold", () -> {
                    FleetPersistenceGameTests.reload(helper, GARAGE, tag -> { }, () -> { });
                    record.clear();
                    Waits.when(helper, () -> trunkGone(helper, spot) && helper.getBlockState(spot).is(Blocks.OAK_SAPLING) && out(helper) == 0, 800,
                            () -> "the run should fell the oak after the reload, steps " + record.steps, () -> {
                                helper.assertTrue(record.felling().startsWith("UNFOLD SAW TOPPLE"),
                                        "the restored shuttle unfolds again before it saws, got " + record.steps);
                                helper.assertTrue(HarvesterGameTests.toStorage(farm, Items.OAK_LOG) == OAK_HEIGHT, "the logs reach the head");
                                helper.succeed();
                            });
                });
    }
}
