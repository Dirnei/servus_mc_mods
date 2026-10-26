package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldKind;
import io.github.dirnei.mc.servus.shuttles.field.FieldPos;
import io.github.dirnei.mc.servus.shuttles.field.FieldPositions;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterWork;
import io.github.dirnei.mc.servus.shuttles.field.PlotState;
import io.github.dirnei.mc.servus.shuttles.field.TreeSpots;
import io.github.dirnei.mc.servus.shuttles.field.TreeTags;
import io.github.dirnei.mc.servus.shuttles.field.TreeWalk;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.grower.TreeGrower;

/**
 * Real trees grown by their vanilla features on a Trees field, planted the way
 * a Lumber shuttle plants them: single saplings on every spot, 2x2 saplings
 * on every square, so neighbouring crowns, leaning trunks and branches touch.
 * Every plot that counts as ready is then felled with the walk a Lumber
 * shuttle uses, until none is left: no log may stay behind and every spot can
 * be planted again.
 */
public final class TreeFellingGameTests {

    private static final String FOREST = "empty_28x40x28";
    private static final int SIZE = 28;
    private static final int HEIGHT = 40;
    private static final int RAIL_Z = 14;
    private static final int FIRST_RAIL_X = 3;
    private static final int LAST_RAIL_X = 24;
    private static final int SEEDS_PER_TREE = 8;

    private TreeFellingGameTests() {
    }

    /** Dirt under the whole floor, farm rails along z 14 and the head at their east end. */
    private static FieldHeadBlockEntity forest(GameTestHelper helper) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
            }
        }
        for (int x = FIRST_RAIL_X; x <= LAST_RAIL_X; x++) {
            FieldFixtures.farmRail(helper, new BlockPos(x, 1, RAIL_Z));
        }
        return FieldFixtures.head(helper, new BlockPos(LAST_RAIL_X + 1, 1, RAIL_Z));
    }

    /** The plots beside the middle of the rails, where crowns stay inside the structure, in test coordinates. */
    private static List<BlockPos> plantedPlots(int radius) {
        List<BlockPos> plots = new ArrayList<>();
        for (int x = FIRST_RAIL_X + 2; x <= LAST_RAIL_X - 2; x++) {
            for (int z = RAIL_Z - radius; z <= RAIL_Z + radius; z++) {
                if (z != RAIL_Z) {
                    plots.add(new BlockPos(x, 1, z));
                }
            }
        }
        return plots;
    }

    /** Every plot of the field, in test coordinates. */
    private static List<BlockPos> fieldPlots(int radius) {
        List<BlockPos> plots = new ArrayList<>();
        for (int x = FIRST_RAIL_X - radius; x <= LAST_RAIL_X + radius; x++) {
            for (int z = RAIL_Z - radius; z <= RAIL_Z + radius; z++) {
                if (z != RAIL_Z || x < FIRST_RAIL_X || x > LAST_RAIL_X + 1) {
                    plots.add(new BlockPos(x, 1, z));
                }
            }
        }
        return plots;
    }

    /** The world bases of the spots, or of the complete squares, among the planted plots. */
    private static List<List<BlockPos>> bases(GameTestHelper helper, int radius, boolean twoByTwo) {
        List<BlockPos> plots = plantedPlots(radius).stream().map(helper::absolutePos).toList();
        Set<BlockPos> plotSet = Set.copyOf(plots);
        List<List<BlockPos>> bases = new ArrayList<>();
        Set<BlockPos> taken = new LinkedHashSet<>();
        for (BlockPos plot : plots) {
            List<BlockPos> base = new ArrayList<>();
            for (FieldPos member : TreeSpots.base(FieldPositions.of(plot), twoByTwo)) {
                base.add(FieldPositions.toBlockPos(member));
            }
            boolean planted = twoByTwo ? TreeSpots.square(FieldPositions.of(plot)).isPresent() && plotSet.containsAll(base)
                    : TreeSpots.isSpot(plot.getX(), plot.getZ());
            if (planted && taken.addAll(base)) {
                base.sort((a, b) -> a.getX() != b.getX() ? a.getX() - b.getX() : a.getZ() - b.getZ());
                bases.add(base);
            }
        }
        return bases;
    }

    /** Grows a tree from saplings on the world plots, its smaller corner first, trying several seeds; false when none grew. */
    private static boolean grow(ServerLevel level, TreeGrower grower, Block sapling, List<BlockPos> plots, long seed) {
        BlockPos at = plots.getFirst();
        for (int attempt = 0; attempt < SEEDS_PER_TREE; attempt++) {
            for (BlockPos plot : plots) {
                level.setBlockAndUpdate(plot, sapling.defaultBlockState());
            }
            grower.growTree(level, level.getChunkSource().getGenerator(), at, level.getBlockState(at), RandomSource.create(seed * 31 + attempt));
            if (!level.getBlockState(at).is(sapling)) {
                return true;
            }
        }
        for (BlockPos plot : plots) {
            level.setBlockAndUpdate(plot, Blocks.AIR.defaultBlockState());
        }
        return false;
    }

    private static void fellForest(GameTestHelper helper, Block sapling, TreeGrower grower, boolean twoByTwo) {
        FieldHeadBlockEntity head = forest(helper);
        helper.runAfterDelay(2, () -> {
            head.setKind(FieldKind.TREES);
            if (twoByTwo) {
                head.rangeUpgrades().setStackInSlot(0, new ItemStack(ServusShuttlesItems.RANGE_UPGRADE.get(), 1));
            }
            helper.assertTrue(head.setSapling(new ItemStack(sapling), twoByTwo), "the field takes " + sapling);
            helper.runAfterDelay(2, () -> growAndFell(helper, head, sapling, grower, twoByTwo));
        });
    }

    private static void growAndFell(GameTestHelper helper, FieldHeadBlockEntity head, Block sapling, TreeGrower grower, boolean twoByTwo) {
        ServerLevel level = helper.getLevel();
        int radius = head.radius();
        List<List<BlockPos>> bases = bases(helper, radius, twoByTwo);
        int grown = 0;
        for (int i = 0; i < bases.size(); i++) {
            if (grow(level, grower, sapling, bases.get(i), i)) {
                grown++;
            }
        }
        helper.assertTrue(grown >= 2, "trees should grow, " + grown + " of " + bases.size());
        head.recheckAll();
        List<String> givenUp = new ArrayList<>();
        boolean felled = true;
        while (felled) {
            felled = false;
            for (BlockPos plot : fieldPlots(radius)) {
                BlockPos world = helper.absolutePos(plot);
                if (head.classify(world) != PlotState.RIPE) {
                    continue;
                }
                List<BlockPos> base = head.treeBase(world);
                TreeWalk.Result walked = head.walkTree(base);
                if (walked instanceof TreeWalk.Felled tree) {
                    HarvesterWork.removeTree(level, tree);
                    felled = true;
                } else {
                    givenUp.add(plot.toShortString() + " " + walked.getClass().getSimpleName());
                    head.giveUp(base, walked instanceof TreeWalk.TooLarge);
                }
            }
        }
        List<String> logsLeft = new ArrayList<>();
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 1; y < HEIGHT; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(TreeTags.TREE_LOGS)) {
                        logsLeft.add(x + " " + y + " " + z);
                    }
                }
            }
        }
        helper.assertTrue(givenUp.isEmpty() && logsLeft.isEmpty(),
                grown + " grown trees: given up " + givenUp + ", logs left " + logsLeft);
        for (List<BlockPos> base : bases) {
            for (BlockPos plot : base) {
                PlotState state = head.classify(plot);
                helper.assertTrue(state == PlotState.PLANTABLE, "every spot can be planted again, one is " + state
                        + " holding " + level.getBlockState(plot) + " on " + level.getBlockState(plot.below()));
            }
        }
        helper.succeed();
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsOakForest(GameTestHelper helper) {
        fellForest(helper, Blocks.OAK_SAPLING, TreeGrower.OAK, false);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsBirchForest(GameTestHelper helper) {
        fellForest(helper, Blocks.BIRCH_SAPLING, TreeGrower.BIRCH, false);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsSpruceForest(GameTestHelper helper) {
        fellForest(helper, Blocks.SPRUCE_SAPLING, TreeGrower.SPRUCE, false);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsJungleForest(GameTestHelper helper) {
        fellForest(helper, Blocks.JUNGLE_SAPLING, TreeGrower.JUNGLE, false);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsAcaciaForest(GameTestHelper helper) {
        fellForest(helper, Blocks.ACACIA_SAPLING, TreeGrower.ACACIA, false);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsCherryForest(GameTestHelper helper) {
        fellForest(helper, Blocks.CHERRY_SAPLING, TreeGrower.CHERRY, false);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsAzaleaForest(GameTestHelper helper) {
        fellForest(helper, Blocks.AZALEA, TreeGrower.AZALEA, false);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsDarkOakSquares(GameTestHelper helper) {
        fellForest(helper, Blocks.DARK_OAK_SAPLING, TreeGrower.DARK_OAK, true);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsPaleOakSquares(GameTestHelper helper) {
        fellForest(helper, Blocks.PALE_OAK_SAPLING, TreeGrower.PALE_OAK, true);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsSpruceSquares(GameTestHelper helper) {
        fellForest(helper, Blocks.SPRUCE_SAPLING, TreeGrower.SPRUCE, true);
    }

    @ServusGameTest(template = FOREST, timeoutTicks = 60)
    public static void fellsJungleSquares(GameTestHelper helper) {
        fellForest(helper, Blocks.JUNGLE_SAPLING, TreeGrower.JUNGLE, true);
    }
}
