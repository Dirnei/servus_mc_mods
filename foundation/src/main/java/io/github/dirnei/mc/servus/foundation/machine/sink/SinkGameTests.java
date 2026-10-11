package io.github.dirnei.mc.servus.foundation.machine.sink;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.menu.SinkMenu;
import io.github.dirnei.mc.servus.foundation.machine.sink.ChimneyBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import io.github.dirnei.mc.servus.foundation.transfer.EnergyView;
import net.neoforged.neoforge.fluids.FluidStack;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;

/** The Sink: forming with the carrot, voiding through every outer face, chimney rates and the trash GUI. */
public final class SinkGameTests {

    private static final String EMPTY = "empty_12x6x12";

    private SinkGameTests() {
    }

    private static BlockState facing(Block block, Direction direction) {
        return block.defaultBlockState().setValue(MachineBlock.FACING, direction);
    }

    /**
     * Places a Utility Duct carrying the given lines with default modes and
     * line 1. {@code setBlock} skips placement logic, so the connection
     * flags are refreshed from the neighbours afterwards.
     */
    private static void duct(GameTestHelper helper, BlockPos pos, Line... lines) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        for (Line line : lines) {
            duct.addLine(line);
        }
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
    }

    // Sink: a 2x2x2 cube of Sink blocks formed with the carrot. The standard cube spans
    // x 2..3, y 0..1, z 2..3 and faces north: its controller is the front lower east block
    // (east is the left of a player looking at the front), west is right and south is back.

    private static final BlockPos SINK = new BlockPos(3, 0, 2);

    /** Places eight unformed Sink blocks in the box from {@code min} to {@code min + (1, 1, 1)}, except {@code skip}. */
    private static void sinkBox(GameTestHelper helper, BlockPos min, BlockPos skip) {
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(1, 1, 1))) {
            if (!pos.equals(skip)) {
                helper.setBlock(pos, MachineContent.SINK.get().defaultBlockState());
            }
        }
    }

    /** Uses an item on the {@code face} side of the block at relative {@code pos}. */
    private static void useOn(GameTestHelper helper, Player player, ItemStack stack, BlockPos pos, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(pos);
        Vec3 hit = Vec3.atCenterOf(absolute).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, absolute, false)));
    }

    private static void carrot(GameTestHelper helper, BlockPos pos, Direction face, boolean sneaking) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(sneaking);
        useOn(helper, player, new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get()), pos, face);
    }

    /** Builds the standard cube, forms it from the controller's front and stacks a column on the back right top block. */
    private static SinkBlockEntity sink(GameTestHelper helper, int chimneys) {
        sinkBox(helper, new BlockPos(2, 0, 2), null);
        carrot(helper, SINK, Direction.NORTH, false);
        BlockPos column = new BlockPos(2, 1, 3);
        for (int i = 1; i <= chimneys; i++) {
            helper.setBlock(column.above(i), MachineContent.CHIMNEY.get().defaultBlockState());
        }
        return helper.getBlockEntity(SINK, SinkBlockEntity.class);
    }

    private static boolean formed(GameTestHelper helper, BlockPos pos) {
        return SinkBlock.isFormed(helper.getBlockState(pos));
    }

    /** Asserts that the box at {@code min} is one Sink facing {@code facing} with its controller at {@code controller}. */
    private static void assertFormedCube(GameTestHelper helper, BlockPos min, Direction facing, BlockPos controller) {
        Set<Integer> parts = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(1, 1, 1))) {
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(SinkBlock.isFormed(state), "every block should be formed, not " + pos.immutable());
            helper.assertTrue(state.getValue(SinkBlock.FACING) == facing, "every block should face " + facing);
            parts.add(state.getValue(SinkBlock.PART));
        }
        helper.assertTrue(parts.size() == 8, "eight different parts, got " + parts);
        helper.assertTrue(helper.getBlockState(controller).getValue(SinkBlock.PART) == 0,
                "the front lower left block should be the controller for " + facing);
    }

    /** The carrot forms a cube from each of the four front blocks, for all four facings. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkFormsFromEachFrontPart(GameTestHelper helper) {
        // North, clicked on the controller itself (front lower east).
        sinkBox(helper, new BlockPos(1, 0, 1), null);
        carrot(helper, new BlockPos(2, 0, 1), Direction.NORTH, false);
        assertFormedCube(helper, new BlockPos(1, 0, 1), Direction.NORTH, new BlockPos(2, 0, 1));
        // East, clicked on the front lower right block (north end); the controller is the south end.
        sinkBox(helper, new BlockPos(5, 0, 1), null);
        carrot(helper, new BlockPos(6, 0, 1), Direction.EAST, false);
        assertFormedCube(helper, new BlockPos(5, 0, 1), Direction.EAST, new BlockPos(6, 0, 2));
        // South, clicked on the front upper left block (west end).
        sinkBox(helper, new BlockPos(1, 0, 5), null);
        carrot(helper, new BlockPos(1, 1, 6), Direction.SOUTH, false);
        assertFormedCube(helper, new BlockPos(1, 0, 5), Direction.SOUTH, new BlockPos(1, 0, 6));
        // West, clicked on the front upper right block (south end); the controller is the north end.
        sinkBox(helper, new BlockPos(5, 0, 5), null);
        carrot(helper, new BlockPos(5, 1, 6), Direction.WEST, false);
        assertFormedCube(helper, new BlockPos(5, 0, 5), Direction.WEST, new BlockPos(5, 0, 5));
        helper.succeed();
    }

    /** Seven blocks in a 2x2x2 shape do not form; the top face does not form either. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkSevenBlocksDoNotForm(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 0, 2);
        sinkBox(helper, min, new BlockPos(2, 1, 3));
        carrot(helper, SINK, Direction.NORTH, false);
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(1, 1, 1))) {
            helper.assertTrue(!formed(helper, pos), "nothing should form from seven blocks");
        }
        helper.setBlock(new BlockPos(2, 1, 3), MachineContent.SINK.get().defaultBlockState());
        carrot(helper, new BlockPos(3, 1, 2), Direction.UP, false);
        helper.assertTrue(!formed(helper, SINK), "the top face is no front");
        carrot(helper, new BlockPos(3, 1, 2), Direction.NORTH, false);
        assertFormedCube(helper, min, Direction.NORTH, SINK);
        helper.succeed();
    }

    /** Casings next to a formed Sink never take its blocks; they form once their own cube is complete. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkFormingIgnoresFormedNeighbour(GameTestHelper helper) {
        sink(helper, 0);
        // Four casings east of the formed cube: only the cube with the formed blocks would be complete.
        BlockPos column = new BlockPos(4, 0, 2);
        for (BlockPos pos : BlockPos.betweenClosed(column, column.offset(0, 1, 1))) {
            helper.setBlock(pos, MachineContent.SINK.get().defaultBlockState());
        }
        carrot(helper, column, Direction.NORTH, false);
        helper.assertTrue(!formed(helper, column), "the casings must not form with blocks of a formed Sink");
        assertFormedCube(helper, new BlockPos(2, 0, 2), Direction.NORTH, SINK);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(5, 0, 2), new BlockPos(5, 1, 3))) {
            helper.setBlock(pos, MachineContent.SINK.get().defaultBlockState());
        }
        carrot(helper, column, Direction.NORTH, false);
        assertFormedCube(helper, new BlockPos(4, 0, 2), Direction.NORTH, new BlockPos(5, 0, 2));
        assertFormedCube(helper, new BlockPos(2, 0, 2), Direction.NORTH, SINK);
        helper.succeed();
    }

    /** Breaking one block unforms the other seven and drops one plain Sink block; repairing and the carrot form it again. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkBreakingUnformsAndReforms(GameTestHelper helper) {
        sink(helper, 0);
        BlockPos broken = new BlockPos(2, 1, 3);
        // With drops, like a player breaking it.
        helper.getLevel().destroyBlock(helper.absolutePos(broken), true);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 0, 2), new BlockPos(3, 1, 3))) {
            if (!pos.equals(broken)) {
                BlockState state = helper.getBlockState(pos);
                helper.assertTrue(state.is(MachineContent.SINK.get()) && !state.getValue(SinkBlock.FORMED),
                        "the rest should be unformed casings, not " + state);
            }
        }
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(broken)).inflate(2));
        int dropped = drops.stream().filter(entity -> entity.getItem().is(MachineContent.SINK_ITEM.get()))
                .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(dropped == 1, "one Sink block should drop, dropped " + dropped);
        drops.forEach(ItemEntity::discard);
        helper.setBlock(broken, MachineContent.SINK.get().defaultBlockState());
        carrot(helper, new BlockPos(2, 0, 2), Direction.NORTH, false);
        assertFormedCube(helper, new BlockPos(2, 0, 2), Direction.NORTH, SINK);
        helper.succeed();
    }

    /** The sneak carrot dismantles one block of a formed Sink into a plain Sink item and unforms the rest. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkSneakCarrotDismantlesOneBlock(GameTestHelper helper) {
        sink(helper, 0);
        carrot(helper, SINK, Direction.NORTH, true);
        helper.assertBlockNotPresent(MachineContent.SINK.get(), SINK);
        helper.assertTrue(!formed(helper, new BlockPos(2, 0, 2)), "the rest should be unformed");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(SINK)).inflate(2));
        helper.assertTrue(drops.stream().anyMatch(entity -> entity.getItem().is(MachineContent.SINK_ITEM.get())),
                "the block should drop as a Sink item");
        helper.succeed();
    }

    private static ItemView sinkItems(GameTestHelper helper, BlockPos pos, Direction side) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), side));
    }

    private static FluidView sinkFluid(GameTestHelper helper, BlockPos pos, Direction side) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), side));
    }

    private static EnergyView sinkEnergy(GameTestHelper helper, BlockPos pos, Direction side) {
        return EnergyView.of(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), side));
    }

    /** Hoppers pointing into the side of the controller and into the back of another part empty; the items appear nowhere. */
    @ServusGameTest(timeoutTicks = 200)
    public static void hopperEmptiesIntoSink(GameTestHelper helper) {
        sink(helper, 5);
        BlockPos east = new BlockPos(4, 0, 2);
        helper.setBlock(east, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        HopperBlockEntity intoController = helper.getBlockEntity(east, HopperBlockEntity.class);
        intoController.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        BlockPos back = new BlockPos(2, 0, 4);
        helper.setBlock(back, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        HopperBlockEntity intoBack = helper.getBlockEntity(back, HopperBlockEntity.class);
        intoBack.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        helper.succeedWhen(() -> {
            helper.assertTrue(intoController.isEmpty(), "the hopper at the side should empty into the Sink");
            helper.assertTrue(intoBack.isEmpty(), "the hopper at the back should empty into the Sink");
            helper.assertItemEntityNotPresent(Items.COBBLESTONE);
        });
    }

    /** A hopper pointing into a front face keeps its items: the front offers no capability. */
    @ServusGameTest(timeoutTicks = 100)
    public static void sinkFrontRefuses(GameTestHelper helper) {
        sink(helper, 5);
        for (BlockPos front : List.of(new BlockPos(2, 0, 2), SINK, new BlockPos(2, 1, 2), new BlockPos(3, 1, 2))) {
            helper.assertTrue(sinkItems(helper, front, Direction.NORTH) == null, "no items through the front");
            helper.assertTrue(sinkFluid(helper, front, Direction.NORTH) == null, "no fluid through the front");
            helper.assertTrue(sinkEnergy(helper, front, Direction.NORTH) == null, "no energy through the front");
        }
        BlockPos hopperPos = new BlockPos(2, 0, 1);
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.SOUTH));
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 5, "the hopper at the front should keep its items");
            helper.succeed();
        });
    }

    /** Without a chimney the item rate of 4 per second takes 4 of a 64 stack; all faces share the budget. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkBaseRateTakesFourItems(GameTestHelper helper) {
        sink(helper, 0);
        ItemView top = sinkItems(helper, new BlockPos(3, 1, 2), Direction.UP);
        helper.assertTrue(top != null, "the top takes items");
        ItemStack simulated = top.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), true);
        helper.assertTrue(simulated.getCount() == 60, "a simulated insert should leave 60, left " + simulated);
        ItemStack left = top.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(left.getCount() == 60, "the Sink should take 4 of 64, left " + left);
        ItemStack again = sinkItems(helper, new BlockPos(2, 0, 3), Direction.WEST).insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(again.getCount() == 64, "all blocks share one budget, the same tick takes nothing more, left " + again);
        helper.assertTrue(top.getStackInSlot(0).isEmpty(), "the Sink holds nothing");
        helper.succeed();
    }

    /** Five Chimney blocks void everything at once: energy on the back of one block, fluid on top of another. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkWithFiveChimneysIsUnlimited(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 5);
        helper.assertTrue(sink.chimneyHeight() == 5, "five Chimney blocks, height " + sink.chimneyHeight());
        EnergyView energy = sinkEnergy(helper, new BlockPos(3, 0, 3), Direction.SOUTH);
        helper.assertTrue(energy.receiveEnergy(10_000, false) == 10_000, "all energy should be accepted");
        helper.assertTrue(energy.receiveEnergy(10_000, false) == 10_000, "again in the same tick");
        helper.assertTrue(energy.getEnergyStored() == 0, "the Sink reports no stored energy");
        FluidView fluid = sinkFluid(helper, new BlockPos(3, 1, 2), Direction.UP);
        helper.assertTrue(fluid.fill(new FluidStack(Fluids.WATER, 1_000_000), false) == 1_000_000,
                "all water should be accepted");
        helper.assertTrue(fluid.getFluidInTank(0).isEmpty(), "the Sink reports no fluid");
        ItemStack left = sinkItems(helper, SINK, Direction.EAST).insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(left.isEmpty(), "the full stack should be accepted, left " + left);
        helper.succeed();
    }

    /** Below the unlimited height energy and fluid follow the rate of the height, shared by all blocks. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkRateGrowsWithChimney(GameTestHelper helper) {
        sink(helper, 2);
        // Height 2: 400 FE and 200 mB per tick, one second of carry over in an idle Sink.
        helper.assertTrue(sinkEnergy(helper, new BlockPos(2, 0, 3), Direction.SOUTH).receiveEnergy(100_000, false) == 8_000,
                "one second of 400 FE per tick");
        helper.assertTrue(sinkEnergy(helper, SINK, Direction.EAST).receiveEnergy(100_000, false) == 0,
                "another block of the same Sink gets nothing more this tick");
        helper.assertTrue(sinkFluid(helper, new BlockPos(2, 1, 3), Direction.UP)
                        .fill(new FluidStack(Fluids.WATER, 100_000), false) == 4_000,
                "one second of 200 mB per tick");
        helper.succeed();
    }

    /** Nothing can be taken out of a Sink. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkGivesNothing(GameTestHelper helper) {
        sink(helper, 0);
        ItemView items = sinkItems(helper, SINK, Direction.DOWN);
        items.insertItem(0, new ItemStack(Items.COBBLESTONE, 2), false);
        helper.assertTrue(items.extractItem(0, 64, false).isEmpty(), "nothing can be extracted");
        helper.assertTrue(sinkFluid(helper, SINK, Direction.DOWN).drain(1000, false).isEmpty(),
                "nothing can be drained");
        helper.assertTrue(sinkEnergy(helper, SINK, Direction.DOWN).extractEnergy(1000, false) == 0, "no energy can be extracted");
        helper.succeed();
    }

    /** Unformed casings offer no capability on any face, and a hopper keeps its items. */
    @ServusGameTest(timeoutTicks = 100)
    public static void sinkCasingsExposeNothing(GameTestHelper helper) {
        sinkBox(helper, new BlockPos(2, 0, 2), null);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 0, 2), new BlockPos(3, 1, 3))) {
            for (Direction side : Direction.values()) {
                helper.assertTrue(sinkItems(helper, pos, side) == null, "a casing takes no items");
                helper.assertTrue(sinkFluid(helper, pos, side) == null, "a casing takes no fluid");
                helper.assertTrue(sinkEnergy(helper, pos, side) == null, "a casing takes no energy");
            }
        }
        BlockPos hopperPos = new BlockPos(4, 0, 2);
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 5, "the hopper should keep its items");
            helper.succeed();
        });
    }

    /** Voiding through any block turns the controller active; after the active time without voiding it turns idle. */
    @ServusGameTest(timeoutTicks = 120)
    public static void sinkActiveWhileVoiding(GameTestHelper helper) {
        sink(helper, 0);
        BlockPos back = new BlockPos(2, 1, 3);
        helper.assertTrue(!helper.getBlockState(SINK).getValue(SinkBlock.ACTIVE), "a new Sink is idle");
        sinkEnergy(helper, back, Direction.UP).receiveEnergy(50, true);
        helper.assertTrue(!helper.getBlockState(SINK).getValue(SinkBlock.ACTIVE), "a simulated insert does not count");
        sinkEnergy(helper, back, Direction.UP).receiveEnergy(50, false);
        helper.assertTrue(helper.getBlockState(SINK).getValue(SinkBlock.ACTIVE), "voiding turns the controller active");
        helper.assertTrue(!helper.getBlockState(back).getValue(SinkBlock.ACTIVE), "only the controller carries the active state");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(helper.getBlockState(SINK).getValue(SinkBlock.ACTIVE), "still active within the active time");
            helper.runAfterDelay(25, () -> {
                helper.assertTrue(!helper.getBlockState(SINK).getValue(SinkBlock.ACTIVE), "idle after the active time");
                helper.succeed();
            });
        });
    }

    private static void chimneyColumn(GameTestHelper helper, BlockPos topPart, int height) {
        for (int i = 1; i <= height; i++) {
            helper.setBlock(topPart.above(i), MachineContent.CHIMNEY.get().defaultBlockState());
        }
    }

    /** Three Chimney blocks on a corner give height 3, only the top one shows the cap, and the cap moves up. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkCountsItsChimney(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 3);
        BlockPos column = new BlockPos(2, 1, 3);
        helper.assertTrue(sink.chimneyHeight() == 3, "expected height 3, got " + sink.chimneyHeight());
        helper.assertTrue(sink.chimneyColumn() == 7, "the column stands on the back right top part, got " + sink.chimneyColumn());
        helper.assertTrue(!helper.getBlockState(column.above(1)).getValue(ChimneyBlock.TOP), "the lowest block has no cap");
        helper.assertTrue(!helper.getBlockState(column.above(2)).getValue(ChimneyBlock.TOP), "the middle block has no cap");
        helper.assertTrue(helper.getBlockState(column.above(3)).getValue(ChimneyBlock.TOP), "the top block has the cap");
        helper.setBlock(column.above(4), MachineContent.CHIMNEY.get().defaultBlockState());
        helper.assertTrue(!helper.getBlockState(column.above(3)).getValue(ChimneyBlock.TOP), "the cap moves up");
        helper.assertTrue(sink.chimneyHeight() == 4, "expected height 4, got " + sink.chimneyHeight());
        helper.succeed();
    }

    /** Five Chimney blocks on the front left corner make the Sink unlimited. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkFiveOnACornerIsUnlimited(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        chimneyColumn(helper, new BlockPos(3, 1, 2), 5);
        helper.assertTrue(sink.chimneyHeight() == 5, "expected height 5, got " + sink.chimneyHeight());
        helper.assertTrue(sink.chimneyColumn() == 2, "the column stands on the front left top part, got " + sink.chimneyColumn());
        ItemStack left = sinkItems(helper, SINK, Direction.EAST).insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(left.isEmpty(), "an unlimited Sink takes the full stack, left " + left);
        helper.succeed();
    }

    /** With two columns the tallest counts; shortening it falls back to the other one. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkTallestColumnCounts(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        chimneyColumn(helper, new BlockPos(3, 1, 3), 2);
        chimneyColumn(helper, new BlockPos(2, 1, 2), 4);
        helper.assertTrue(sink.chimneyHeight() == 4, "the taller column counts, got " + sink.chimneyHeight());
        helper.assertTrue(sink.chimneyColumn() == 3, "the front right top part carries it, got " + sink.chimneyColumn());
        helper.destroyBlock(new BlockPos(2, 1, 2).above(4));
        helper.destroyBlock(new BlockPos(2, 1, 2).above(3));
        helper.destroyBlock(new BlockPos(2, 1, 2).above(2));
        helper.assertTrue(sink.chimneyHeight() == 2, "the other column is now the tallest, got " + sink.chimneyHeight());
        helper.assertTrue(sink.chimneyColumn() == 6, "the back left top part carries it, got " + sink.chimneyColumn());
        helper.succeed();
    }

    /** Breaking the second of four Chimney blocks leaves a height of 1; filling the gap joins the column again. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkChimneyMiddleRemoved(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 4);
        BlockPos column = new BlockPos(2, 1, 3);
        helper.assertTrue(sink.chimneyHeight() == 4, "expected height 4, got " + sink.chimneyHeight());
        helper.destroyBlock(column.above(2));
        helper.assertTrue(sink.chimneyHeight() == 1, "expected height 1, got " + sink.chimneyHeight());
        helper.assertTrue(helper.getBlockState(column.above(1)).getValue(ChimneyBlock.TOP), "the remaining block shows the cap");
        helper.setBlock(column.above(2), MachineContent.CHIMNEY.get().defaultBlockState());
        helper.assertTrue(sink.chimneyHeight() == 4, "filling the gap joins the column again, got " + sink.chimneyHeight());
        helper.succeed();
    }

    /** Chimney blocks on stone or on unformed casings are decorative; forming the cube counts a column already there. */
    @ServusGameTest(timeoutTicks = 60)
    public static void chimneyWithoutSinkIsDecorative(GameTestHelper helper) {
        BlockPos base = new BlockPos(8, 0, 8);
        helper.setBlock(base, Blocks.STONE.defaultBlockState());
        chimneyColumn(helper, base, 2);
        helper.assertTrue(!helper.getBlockState(base.above(1)).getValue(ChimneyBlock.TOP), "lower block without cap");
        helper.assertTrue(helper.getBlockState(base.above(2)).getValue(ChimneyBlock.TOP), "top block with cap");
        helper.destroyBlock(base.above(2));
        helper.assertTrue(helper.getBlockState(base.above(1)).getValue(ChimneyBlock.TOP), "the cap moves down");

        sinkBox(helper, new BlockPos(2, 0, 2), null);
        chimneyColumn(helper, new BlockPos(2, 1, 3), 3);
        SinkBlockEntity casing = helper.getBlockEntity(SINK, SinkBlockEntity.class);
        helper.assertTrue(casing.chimneyHeight() == 0, "a casing counts no chimney, got " + casing.chimneyHeight());
        carrot(helper, SINK, Direction.NORTH, false);
        SinkBlockEntity sink = helper.getBlockEntity(SINK, SinkBlockEntity.class);
        helper.assertTrue(sink.chimneyHeight() == 3, "forming counts the column already standing, got " + sink.chimneyHeight());
        helper.succeed();
    }

    /** A lava bucket used on any formed block is emptied whatever the rate; on a casing it stays full. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkEmptiesLavaBucket(GameTestHelper helper) {
        sink(helper, 0);
        // Use up the fluid budget first: the bucket must not depend on it.
        sinkFluid(helper, SINK, Direction.UP).fill(new FluidStack(Fluids.WATER, 100_000), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bucket = new ItemStack(Items.LAVA_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        BlockPos part = new BlockPos(2, 1, 3);
        BlockPos absolute = helper.absolutePos(part);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
        helper.getBlockState(part).useItemOn(bucket, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(held.is(Items.BUCKET) && held.getCount() == 1, "the player should hold an empty bucket, holds " + held);

        BlockPos casing = new BlockPos(8, 0, 8);
        helper.setBlock(casing, MachineContent.SINK.get().defaultBlockState());
        ItemStack full = new ItemStack(Items.LAVA_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, full);
        BlockPos casingAbsolute = helper.absolutePos(casing);
        helper.getBlockState(casing).useItemOn(full, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(casingAbsolute).add(0, 0.5, 0), Direction.UP, casingAbsolute, false));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.LAVA_BUCKET), "a casing takes no lava");
        helper.succeed();
    }

    /** Shift clicking a stack into the trash slot voids all of it at once, even without a chimney; unforming closes the GUI. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkTrashSlotVoidsStack(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(SINK.north())));
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 64));
        SinkMenu menu = new SinkMenu(1, player.getInventory(), sink);
        helper.assertTrue(menu.stillValid(player), "the GUI of a formed Sink is valid");
        // Slot 0 is the trash slot, slot 1 the first inventory slot (player inventory index 9).
        menu.quickMoveStack(player, 1);
        helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "the stack should be gone");
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "the trash slot stays empty");
        helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "no cobblestone anywhere in the inventory");
        helper.assertTrue(helper.getBlockState(SINK).getValue(SinkBlock.ACTIVE), "the trash slot turns the Sink active");
        helper.assertTrue(menu.chimneyHeight() == 0 && menu.itemRate() == 4, "height 0 shows 4 items per second");
        helper.destroyBlock(new BlockPos(2, 1, 3));
        helper.assertTrue(!menu.stillValid(player), "the GUI closes once the Sink is unformed");
        helper.succeed();
    }

    /** The GUI data shows height 5 and Unlimited (-1) for all three rates with five Chimney blocks. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkGuiShowsUnlimited(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SinkMenu menu = new SinkMenu(1, player.getInventory(), sink);
        helper.assertTrue(menu.chimneyHeight() == 5, "height 5, got " + menu.chimneyHeight());
        helper.assertTrue(menu.itemRate() == -1 && menu.fluidRate() == -1 && menu.energyRate() == -1, "all three Unlimited");
        helper.succeed();
    }
}
