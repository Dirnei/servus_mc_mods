package io.github.dirnei.mc.servus.foundation.machine.sink;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The Chimney on its own: carrot settings over a column, redstone, copying on stack, its stone, and on a Sink. */
public final class ChimneyGameTests {

    /** The stone block a free standing column stands on. */
    private static final BlockPos BASE = new BlockPos(8, 0, 8);
    /** The back right top block of the standard Sink of {@link SinkGameTests#sink}. */
    private static final BlockPos SINK_TOP = new BlockPos(2, 1, 3);

    private ChimneyGameTests() {
    }

    /** A free standing column of {@code height} Chimney blocks on stone; returns the positions bottom first. */
    private static List<BlockPos> column(GameTestHelper helper, int height) {
        helper.setBlock(BASE, Blocks.STONE.defaultBlockState());
        SinkGameTests.chimneyColumn(helper, BASE, height);
        return IntStream.rangeClosed(1, height).mapToObj(BASE::above).toList();
    }

    private static BlockState at(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos);
    }

    private static void assertColumn(GameTestHelper helper, List<BlockPos> column, ChimneySmoke smoke, ChimneyRedstone mode) {
        for (BlockPos pos : column) {
            BlockState state = at(helper, pos);
            helper.assertTrue(state.getValue(ChimneyBlock.SMOKE) == smoke && state.getValue(ChimneyBlock.REDSTONE) == mode,
                    "every block is " + smoke + " and " + mode + ", got " + state + " at " + pos);
        }
    }

    /** A carrot click on the middle of three blocks steps all three to the next smoke setting, round to off. */
    @ServusGameTest(timeoutTicks = 40)
    public static void carrotStepsSmokeOverColumn(GameTestHelper helper) {
        List<BlockPos> column = column(helper, 3);
        assertColumn(helper, column, ChimneySmoke.OFF, ChimneyRedstone.ALWAYS);
        SinkGameTests.carrot(helper, column.get(1), Direction.NORTH, false);
        assertColumn(helper, column, ChimneySmoke.THIN, ChimneyRedstone.ALWAYS);
        helper.assertTrue(ChimneyBlock.smokes(at(helper, column.get(2))), "thin and always smokes");
        SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, false);
        SinkGameTests.carrot(helper, column.get(2), Direction.NORTH, false);
        assertColumn(helper, column, ChimneySmoke.SIGNAL, ChimneyRedstone.ALWAYS);
        SinkGameTests.carrot(helper, column.get(1), Direction.NORTH, false);
        assertColumn(helper, column, ChimneySmoke.OFF, ChimneyRedstone.ALWAYS);
        helper.assertTrue(!ChimneyBlock.smokes(at(helper, column.get(2))), "off does not smoke");
        helper.succeed();
    }

    /** A sneak carrot click steps the redstone mode of the whole column and keeps the block. */
    @ServusGameTest(timeoutTicks = 40)
    public static void sneakCarrotStepsRedstoneMode(GameTestHelper helper) {
        List<BlockPos> column = column(helper, 2);
        SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, true);
        helper.assertBlockPresent(MachineContent.CHIMNEY.get(), column.get(0));
        assertColumn(helper, column, ChimneySmoke.OFF, ChimneyRedstone.POWERED);
        SinkGameTests.carrot(helper, column.get(1), Direction.NORTH, true);
        assertColumn(helper, column, ChimneySmoke.OFF, ChimneyRedstone.UNPOWERED);
        SinkGameTests.carrot(helper, column.get(1), Direction.NORTH, true);
        assertColumn(helper, column, ChimneySmoke.OFF, ChimneyRedstone.ALWAYS);
        helper.succeed();
    }

    /** A redstone block beside the lowest block powers the column up to its top; removing it clears the power. */
    @ServusGameTest(timeoutTicks = 40)
    public static void redstoneOnAnyBlockPowersColumn(GameTestHelper helper) {
        List<BlockPos> column = column(helper, 3);
        SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, false);
        SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, true);
        BlockPos top = column.get(2);
        helper.assertTrue(!ChimneyBlock.smokes(at(helper, top)), "thin while powered stays dark without a signal");
        BlockPos beside = column.get(0).east();
        helper.setBlock(beside, Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(at(helper, top).getValue(ChimneyBlock.POWERED), "the top is powered by a signal at the bottom");
        helper.assertTrue(ChimneyBlock.smokes(at(helper, top)), "thin while powered smokes with a signal");
        helper.setBlock(beside, Blocks.AIR.defaultBlockState());
        for (BlockPos pos : column) {
            helper.assertTrue(!at(helper, pos).getValue(ChimneyBlock.POWERED), "no block is powered once the signal is gone");
        }
        helper.succeed();
    }

    /** A Chimney placed on a column takes its settings and becomes the new top. */
    @ServusGameTest(timeoutTicks = 40)
    public static void newChimneyCopiesBelow(GameTestHelper helper) {
        List<BlockPos> column = column(helper, 2);
        SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, false);
        SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, false);
        SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SinkGameTests.useOn(helper, player, new ItemStack(MachineContent.CHIMNEY_ITEM.get()), column.get(1), Direction.UP);
        BlockPos placed = column.get(1).above();
        helper.assertBlockPresent(MachineContent.CHIMNEY.get(), placed);
        assertColumn(helper, List.of(column.get(0), column.get(1), placed), ChimneySmoke.COSY, ChimneyRedstone.POWERED);
        helper.assertTrue(at(helper, placed).getValue(ChimneyBlock.TOP) && !at(helper, column.get(1)).getValue(ChimneyBlock.TOP),
                "the cap moves to the new block");
        helper.succeed();
    }

    /** On a formed Sink the carrot changes no setting of the column. */
    @ServusGameTest(timeoutTicks = 40)
    public static void carrotIgnoredOnSink(GameTestHelper helper) {
        SinkBlockEntity sink = SinkGameTests.sink(helper, 2);
        helper.assertTrue(sink.chimneyHeight() == 2, "the Sink counts the column");
        List<BlockPos> column = List.of(SINK_TOP.above(), SINK_TOP.above(2));
        for (BlockPos pos : column) {
            helper.assertTrue(at(helper, pos).getValue(ChimneyBlock.ON_SINK), "the column stands on the Sink at " + pos);
        }
        SinkGameTests.carrot(helper, column.get(1), Direction.NORTH, false);
        SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, true);
        assertColumn(helper, column, ChimneySmoke.OFF, ChimneyRedstone.ALWAYS);
        helper.assertBlockPresent(MachineContent.CHIMNEY.get(), column.get(0));
        helper.succeed();
    }

    /** A column set before the Sink formed keeps its setting; breaking a Sink block hands the smoke back to it. */
    @ServusGameTest(timeoutTicks = 40)
    public static void settingsReturnAfterUnform(GameTestHelper helper) {
        SinkGameTests.sinkBox(helper, new BlockPos(2, 0, 2), null);
        SinkGameTests.chimneyColumn(helper, SINK_TOP, 2);
        List<BlockPos> column = List.of(SINK_TOP.above(), SINK_TOP.above(2));
        for (int i = 0; i < 3; i++) {
            SinkGameTests.carrot(helper, column.get(0), Direction.NORTH, false);
        }
        assertColumn(helper, column, ChimneySmoke.SIGNAL, ChimneyRedstone.ALWAYS);
        SinkGameTests.carrot(helper, new BlockPos(3, 0, 2), Direction.NORTH, false);
        helper.assertTrue(SinkGameTests.formed(helper, SINK_TOP), "the Sink forms");
        for (BlockPos pos : column) {
            helper.assertTrue(at(helper, pos).getValue(ChimneyBlock.ON_SINK), "forming sets the column on the Sink at " + pos);
        }
        helper.assertTrue(!ChimneyBlock.smokes(at(helper, column.get(1))), "the Sink controls the smoke");
        helper.destroyBlock(new BlockPos(3, 0, 3));
        for (BlockPos pos : column) {
            helper.assertTrue(!at(helper, pos).getValue(ChimneyBlock.ON_SINK), "unforming frees the column at " + pos);
        }
        assertColumn(helper, column, ChimneySmoke.SIGNAL, ChimneyRedstone.ALWAYS);
        helper.assertTrue(ChimneyBlock.smokes(at(helper, column.get(1))), "the column smokes by its own setting again");
        helper.succeed();
    }

    /** A Bricks, a Tuff Bricks and a Mud Bricks Chimney on a Sink give height 3. */
    @ServusGameTest(timeoutTicks = 40)
    public static void chimneyOnSinkStillCounts(GameTestHelper helper) {
        SinkBlockEntity sink = SinkGameTests.sink(helper, 3);
        SinkGameTests.stone(helper, SINK_TOP.above(2), Blocks.TUFF_BRICKS);
        SinkGameTests.stone(helper, SINK_TOP.above(3), Blocks.MUD_BRICKS);
        sink.recountChimney();
        helper.assertTrue(sink.chimneyHeight() == 3, "mixed stones count, got " + sink.chimneyHeight());
        helper.succeed();
    }

    /** A mined Tuff Bricks Chimney drops a Chimney that carries Tuff Bricks. */
    @ServusGameTest(timeoutTicks = 40)
    public static void chimneyKeepsStoneOnDrop(GameTestHelper helper) {
        List<BlockPos> column = column(helper, 1);
        SinkGameTests.stone(helper, column.get(0), Blocks.TUFF_BRICKS);
        helper.getLevel().destroyBlock(helper.absolutePos(column.get(0)), true);
        Identifier tuff = BuiltInRegistries.BLOCK.getKey(Blocks.TUFF_BRICKS);
        List<ItemEntity> drops = GameTestSteps.drops(helper, column.get(0), 2);
        helper.assertTrue(drops.stream().anyMatch(entity -> entity.getItem().is(MachineContent.CHIMNEY_ITEM.get())
                        && tuff.equals(entity.getItem().get(MaterialContent.MATERIAL.get()))),
                "the dropped Chimney carries tuff bricks, got " + drops);
        helper.succeed();
    }
}
