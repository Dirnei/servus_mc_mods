package io.github.dirnei.mc.servus.foundation.machine.sink;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineTestSupport;
import io.github.dirnei.mc.servus.foundation.machine.menu.SinkMenu;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.EnergyView;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialHolder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Sink: forming with the carrot, filling the buffer through every outer
 * face, voiding it in batches at the chimney rate, Capacity Upgrades and the GUI.
 */
public final class SinkGameTests {

    /**
     * The controller of the standard cube, which spans x 2..3, y 0..1, z 2..3 and faces north: the front
     * lower east block (east is the left of a player looking at the front), west is right and south is back.
     */
    private static final BlockPos SINK = new BlockPos(3, 0, 2);
    /** Seven different items, one for each slot of a row. */
    private static final List<Item> SEVEN = List.of(Items.STONE, Items.GRANITE, Items.DIORITE, Items.ANDESITE,
            Items.SAND, Items.GRAVEL, Items.COBBLESTONE);
    /** Long enough after filling for the first batch, short of the second. */
    private static final int AFTER_FIRST_BATCH = SinkRates.BATCH_TICKS + 5;

    private SinkGameTests() {
    }

    /** Places eight unformed Sink blocks in the box from {@code min} to {@code min + (1, 1, 1)}, except {@code skip}. */
    static void sinkBox(GameTestHelper helper, BlockPos min, BlockPos skip) {
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(1, 1, 1))) {
            if (!pos.equals(skip)) {
                helper.setBlock(pos, MachineContent.SINK.get().defaultBlockState());
            }
        }
    }

    /** Uses an item on the {@code face} side of the block at relative {@code pos}. */
    static void useOn(GameTestHelper helper, Player player, ItemStack stack, BlockPos pos, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(pos);
        Vec3 hit = Vec3.atCenterOf(absolute).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, absolute, false)));
    }

    static void carrot(GameTestHelper helper, BlockPos pos, Direction face, boolean sneaking) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(sneaking);
        useOn(helper, player, new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get()), pos, face);
    }

    /** Builds the standard cube, forms it from the controller's front and stacks a column on the back right top block. */
    static SinkBlockEntity sink(GameTestHelper helper, int chimneys) {
        sinkBox(helper, new BlockPos(2, 0, 2), null);
        carrot(helper, SINK, Direction.NORTH, false);
        BlockPos column = new BlockPos(2, 1, 3);
        for (int i = 1; i <= chimneys; i++) {
            helper.setBlock(column.above(i), MachineContent.CHIMNEY.get().defaultBlockState());
        }
        return helper.getBlockEntity(SINK, SinkBlockEntity.class);
    }

    static boolean formed(GameTestHelper helper, BlockPos pos) {
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
        List<ItemEntity> drops = GameTestSteps.drops(helper, broken, 2);
        int dropped = MachineTestSupport.count(drops, MachineContent.SINK_ITEM.get());
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
        List<ItemEntity> drops = GameTestSteps.drops(helper, SINK, 2);
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

    /** How many items the buffer holds in all. */
    private static int itemsHeld(SinkBlockEntity sink) {
        int count = 0;
        for (int slot = 0; slot < sink.items().getSlots(); slot++) {
            count += sink.items().getStackInSlot(slot).getCount();
        }
        return count;
    }

    /** Fills the seven slots of the first row with full stacks of seven different items. */
    private static void fillFirstRow(GameTestHelper helper) {
        ItemView side = sinkItems(helper, SINK, Direction.EAST);
        for (Item item : SEVEN) {
            ItemStack left = side.insertItem(new ItemStack(item, 64), false);
            helper.assertTrue(left.isEmpty(), "a full stack of " + item + " should fit, left " + left);
        }
    }

    private static ItemStack capacityUpgrade() {
        return new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get());
    }

    /** Hoppers pointing into the side of the controller and into the back of another part empty; the items appear nowhere. */
    @ServusGameTest(timeoutTicks = 200)
    public static void hopperEmptiesIntoSink(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 5);
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
            helper.assertTrue(!sink.holdsAnything(), "the buffer should be voided");
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

    /**
     * Without a chimney a full stack goes into the buffer at once and the item rate of 4 per second voids 4
     * per batch from the first slot; all faces share the buffer.
     */
    @ServusGameTest(timeoutTicks = 100)
    public static void sinkBaseRateVoidsFourPerBatch(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        ItemView top = sinkItems(helper, new BlockPos(3, 1, 2), Direction.UP);
        helper.assertTrue(top != null, "the top takes items");
        ItemStack simulated = top.insertItem(new ItemStack(Items.COBBLESTONE, 64), true);
        helper.assertTrue(simulated.isEmpty() && itemsHeld(sink) == 0, "a simulated insert fits and changes nothing");
        ItemStack left = top.insertItem(new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(left.isEmpty(), "the Sink should take all 64 into its buffer, left " + left);
        ItemStack more = sinkItems(helper, new BlockPos(2, 0, 3), Direction.WEST).insertItem(new ItemStack(Items.COBBLESTONE, 10), false);
        helper.assertTrue(more.isEmpty() && sink.items().getStackInSlot(1).getCount() == 10,
                "another face fills the same buffer, stacking after the first slot");
        helper.runAfterDelay(AFTER_FIRST_BATCH, () -> {
            helper.assertTrue(sink.items().getStackInSlot(0).getCount() == 60,
                    "one batch voids 4 from the first slot, holds " + sink.items().getStackInSlot(0));
            helper.assertTrue(sink.items().getStackInSlot(1).getCount() == 10, "the second slot waits");
            helper.runAfterDelay(SinkRates.BATCH_TICKS, () -> {
                helper.assertTrue(sink.items().getStackInSlot(0).getCount() == 56, "the next batch voids 4 more");
                helper.succeed();
            });
        });
    }

    /** With all seven open slots full of different items, dirt is refused. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkFullBufferRefuses(GameTestHelper helper) {
        sink(helper, 0);
        fillFirstRow(helper);
        ItemStack dirt = new ItemStack(Items.DIRT, 16);
        ItemStack left = sinkItems(helper, SINK, Direction.EAST).insertItem(dirt, false);
        helper.assertTrue(left.getCount() == 16, "the dirt should be refused, left " + left);
        ItemStack stone = sinkItems(helper, SINK, Direction.EAST).insertItem(new ItemStack(Items.STONE, 1), false);
        helper.assertTrue(stone.getCount() == 1, "a full stack of stone takes no more stone");
        helper.succeed();
    }

    /** Five Chimney blocks void the whole buffer in one batch; energy is accepted at once on the back of another block. */
    @ServusGameTest(timeoutTicks = 100)
    public static void sinkWithFiveChimneysIsUnlimited(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 5);
        helper.assertTrue(sink.chimneyHeight() == 5, "five Chimney blocks, height " + sink.chimneyHeight());
        EnergyView energy = sinkEnergy(helper, new BlockPos(3, 0, 3), Direction.SOUTH);
        helper.assertTrue(energy.receiveEnergy(10_000, false) == 10_000, "all energy should be accepted");
        helper.assertTrue(energy.receiveEnergy(10_000, false) == 10_000, "again in the same tick");
        helper.assertTrue(energy.getEnergyStored() == 0, "the Sink reports no stored energy");
        FluidView fluid = sinkFluid(helper, new BlockPos(3, 1, 2), Direction.UP);
        int filled = fluid.fill(new FluidStack(Fluids.LAVA, 100_000), false);
        helper.assertTrue(filled == 8_000, "one tank of 8,000 mB of lava should be accepted, got " + filled);
        fillFirstRow(helper);
        helper.runAfterDelay(AFTER_FIRST_BATCH, () -> {
            helper.assertTrue(!sink.holdsAnything(), "one batch should void all of it");
            helper.succeed();
        });
    }

    /** Below the unlimited height energy and fluid follow the rate of the height, shared by all blocks. */
    @ServusGameTest(timeoutTicks = 100)
    public static void sinkRateGrowsWithChimney(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 2);
        // Height 2: 400 FE per tick, one second of carry over in an idle Sink; 4,000 mB per batch.
        helper.assertTrue(sinkEnergy(helper, new BlockPos(2, 0, 3), Direction.SOUTH).receiveEnergy(100_000, false) == 8_000,
                "one second of 400 FE per tick");
        helper.assertTrue(sinkEnergy(helper, SINK, Direction.EAST).receiveEnergy(100_000, false) == 0,
                "another block of the same Sink gets nothing more this tick");
        helper.assertTrue(sinkFluid(helper, new BlockPos(2, 1, 3), Direction.UP)
                        .fill(new FluidStack(Fluids.WATER, 100_000), false) == 8_000,
                "the tank takes 8,000 mB");
        helper.runAfterDelay(AFTER_FIRST_BATCH, () -> {
            int held = sink.tank(0).getAmount();
            helper.assertTrue(held == 4_000, "one batch at height 2 voids 4,000 mB, holds " + held);
            helper.succeed();
        });
    }

    /** At height 0 one batch drains 1,000 mB from the first tank. */
    @ServusGameTest(timeoutTicks = 100)
    public static void sinkFluidBatch(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        sinkFluid(helper, SINK, Direction.UP).fill(new FluidStack(Fluids.WATER, 8_000), false);
        helper.runAfterDelay(AFTER_FIRST_BATCH, () -> {
            int held = sink.tank(0).getAmount();
            helper.assertTrue(held == 7_000, "one batch leaves 7,000 mB, holds " + held);
            helper.succeed();
        });
    }

    /** With one upgrade water and lava queue in two tanks; without it the lava is refused. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkTwoFluidsInTwoTanks(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        FluidView fluid = sinkFluid(helper, SINK, Direction.UP);
        helper.assertTrue(fluid.fill(new FluidStack(Fluids.WATER, 2_000), false) == 2_000, "water goes in");
        helper.assertTrue(fluid.fill(new FluidStack(Fluids.LAVA, 1_000), false) == 0, "lava has no free tank");
        sink.upgrades().setStackInSlot(0, capacityUpgrade());
        helper.assertTrue(sink.openRows() == 2, "one upgrade opens a second row and tank");
        helper.assertTrue(fluid.fill(new FluidStack(Fluids.LAVA, 1_000), false) == 1_000, "now the lava goes in");
        helper.assertTrue(fluid.fill(new FluidStack(Fluids.WATER, 500), false) == 500, "more water joins the water");
        helper.assertTrue(sink.tank(0).getFluid() == Fluids.WATER && sink.tank(0).getAmount() == 2_500, "water in the first tank");
        helper.assertTrue(sink.tank(1).getFluid() == Fluids.LAVA && sink.tank(1).getAmount() == 1_000, "lava in the second tank");
        helper.succeed();
    }

    /** Nothing can be taken out of a Sink by automation. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkGivesNothing(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        ItemView items = sinkItems(helper, SINK, Direction.DOWN);
        items.insertItem(new ItemStack(Items.COBBLESTONE, 2), false);
        sinkFluid(helper, SINK, Direction.DOWN).fill(new FluidStack(Fluids.WATER, 1_000), false);
        helper.assertTrue(items.extractItem(0, 64, false).isEmpty(), "nothing can be extracted");
        helper.assertTrue(sinkFluid(helper, SINK, Direction.DOWN).drain(1000, false).isEmpty(), "nothing can be drained");
        helper.assertTrue(sinkEnergy(helper, SINK, Direction.DOWN).extractEnergy(1000, false) == 0, "no energy can be extracted");
        helper.assertTrue(itemsHeld(sink) == 2 && sink.tank(0).getAmount() == 1_000, "the buffer keeps what it holds");
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

    /** Items in the buffer turn the Sink active with their first batch; once empty, no further heartbeat runs. */
    @ServusGameTest(timeoutTicks = 160)
    public static void sinkEmptiesAndStops(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        sinkItems(helper, SINK, Direction.EAST).insertItem(new ItemStack(Items.COBBLESTONE, 4), false);
        BlockPos absolute = helper.absolutePos(SINK);
        helper.assertTrue(helper.getLevel().getBlockTicks().hasScheduledTick(absolute, MachineContent.SINK.get()),
                "an insert schedules the heartbeat");
        helper.runAfterDelay(AFTER_FIRST_BATCH, () -> {
            helper.assertTrue(!sink.holdsAnything(), "the first batch voids the 4 items");
            helper.assertTrue(helper.getBlockState(SINK).getValue(SinkBlock.ACTIVE), "voiding turns the Sink active");
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(!helper.getBlockState(SINK).getValue(SinkBlock.ACTIVE), "idle again");
                helper.assertTrue(!helper.getLevel().getBlockTicks().hasScheduledTick(absolute, MachineContent.SINK.get()),
                        "an empty idle Sink schedules nothing");
                helper.succeed();
            });
        });
    }

    static void chimneyColumn(GameTestHelper helper, BlockPos topPart, int height) {
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
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SinkMenu menu = new SinkMenu(1, player.getInventory(), sink);
        helper.assertTrue(menu.itemRate() == SinkRates.UNLIMITED_DISPLAY, "an unlimited Sink shows Unlimited");
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

    /** Uses {@code bucket} on the top of the block at {@code pos} and returns what the player holds afterwards. */
    private static ItemStack useBucket(GameTestHelper helper, Player player, ItemStack bucket, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
        helper.getBlockState(pos).useItemOn(bucket, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    /** A lava bucket used on any formed block fills a tank; on a casing it stays full. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkEmptiesLavaBucket(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = useBucket(helper, player, new ItemStack(Items.LAVA_BUCKET), new BlockPos(2, 1, 3));
        helper.assertTrue(held.is(Items.BUCKET) && held.getCount() == 1, "the player should hold an empty bucket, holds " + held);
        helper.assertTrue(sink.tank(0).getFluid() == Fluids.LAVA && sink.tank(0).getAmount() == 1_000,
                "the tank should hold 1,000 mB of lava, holds " + sink.tank(0).getAmount());

        BlockPos casing = new BlockPos(8, 0, 8);
        helper.setBlock(casing, MachineContent.SINK.get().defaultBlockState());
        ItemStack kept = useBucket(helper, player, new ItemStack(Items.LAVA_BUCKET), casing);
        helper.assertTrue(kept.is(Items.LAVA_BUCKET), "a casing takes no lava");
        helper.succeed();
    }

    /** A lava bucket stays full when the only open tank holds water. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkRefusesBucketWithoutRoom(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        sinkFluid(helper, SINK, Direction.UP).fill(new FluidStack(Fluids.WATER, 1_000), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = useBucket(helper, player, new ItemStack(Items.LAVA_BUCKET), SINK);
        helper.assertTrue(held.is(Items.LAVA_BUCKET), "the player should keep the full lava bucket, holds " + held);
        helper.assertTrue(sink.tank(0).getFluid() == Fluids.WATER && sink.tank(0).getAmount() == 1_000, "the water is untouched");
        helper.succeed();
    }

    /** An upgrade stays while the second row holds items and comes out once it is empty; the row locks again. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkUpgradeRemovalRefusedThenAllowed(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(SINK.north())));
        SinkMenu menu = new SinkMenu(1, player.getInventory(), sink);
        int upgradeSlot = menu.bufferSlots();
        menu.getSlot(upgradeSlot).set(capacityUpgrade());
        helper.assertTrue(sink.openRows() == 2 && menu.openRows() == 2, "one upgrade opens the second row");
        helper.assertTrue(menu.getSlot(7).isActive() && !menu.getSlot(14).isActive(), "the second row is open, the third closed");
        fillFirstRow(helper);
        ItemStack left = sinkItems(helper, SINK, Direction.EAST).insertItem(new ItemStack(Items.DIRT, 5), false);
        helper.assertTrue(left.isEmpty() && sink.items().getStackInSlot(7).is(Items.DIRT), "the dirt goes into the second row");

        menu.clicked(upgradeSlot, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(upgradeSlot).hasItem(), "the upgrade must stay");
        helper.assertTrue(menu.upgradeJustRefused(), "the GUI should say why");

        menu.clicked(7, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.DIRT), "the player takes the dirt back by hand");
        player.getInventory().add(menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(upgradeSlot, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(ServusFoundationItems.CAPACITY_UPGRADE.get()), "now the upgrade comes out");
        helper.assertTrue(sink.openRows() == 1 && !menu.getSlot(7).isActive(), "the second row locks again");
        helper.succeed();
    }

    /** Shift clicking a stack from the inventory puts it into the buffer, and shift clicking takes it back. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkShiftClickBothWays(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(10, capacityUpgrade());
        SinkMenu menu = new SinkMenu(1, player.getInventory(), sink);
        int inventoryStart = menu.bufferSlots() + menu.upgradeSlots();
        menu.quickMoveStack(player, inventoryStart);
        helper.assertTrue(sink.items().getStackInSlot(0).getCount() == 64 && player.getInventory().getItem(9).isEmpty(),
                "the cobblestone moves into the buffer");
        menu.quickMoveStack(player, inventoryStart + 1);
        helper.assertTrue(sink.upgrades().count() == 1 && itemsHeld(sink) == 64, "the upgrade goes to the upgrade slots only");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(itemsHeld(sink) == 0 && player.getInventory().countItem(Items.COBBLESTONE) == 64,
                "the cobblestone comes back");
        helper.succeed();
    }

    /** Breaking one block of a Sink with two upgrades and 40 cobblestone drops the upgrades and no cobblestone. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkBreakingDropsUpgradesNotItems(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        sink.upgrades().setStackInSlot(0, capacityUpgrade());
        sink.upgrades().setStackInSlot(1, capacityUpgrade());
        sinkItems(helper, SINK, Direction.EAST).insertItem(new ItemStack(Items.COBBLESTONE, 40), false);
        BlockPos broken = new BlockPos(2, 1, 3);
        helper.getLevel().destroyBlock(helper.absolutePos(broken), true);
        List<ItemEntity> drops = GameTestSteps.drops(helper, SINK, 3);
        int upgrades = MachineTestSupport.count(drops, ServusFoundationItems.CAPACITY_UPGRADE.get());
        helper.assertTrue(upgrades == 2, "two Capacity Upgrades should drop, dropped " + upgrades);
        helper.assertTrue(MachineTestSupport.count(drops, Items.COBBLESTONE) == 0, "no cobblestone drops");
        drops.forEach(ItemEntity::discard);
        helper.setBlock(broken, MachineContent.SINK.get().defaultBlockState());
        carrot(helper, SINK, Direction.NORTH, false);
        SinkBlockEntity reformed = helper.getBlockEntity(SINK, SinkBlockEntity.class);
        helper.assertTrue(!reformed.holdsAnything() && reformed.upgrades().count() == 0, "a reformed Sink starts empty");
        helper.succeed();
    }

    /** Breaking the controller itself drops its upgrades too. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkBreakingControllerDropsUpgrades(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        sink.upgrades().setStackInSlot(2, capacityUpgrade());
        sinkFluid(helper, SINK, Direction.UP).fill(new FluidStack(Fluids.WATER, 4_000), false);
        helper.getLevel().destroyBlock(helper.absolutePos(SINK), true);
        List<ItemEntity> drops = GameTestSteps.drops(helper, SINK, 2);
        int upgrades = MachineTestSupport.count(drops, ServusFoundationItems.CAPACITY_UPGRADE.get());
        helper.assertTrue(upgrades == 1, "the Capacity Upgrade should drop, dropped " + upgrades);
        helper.succeed();
    }

    /** The buffer and the upgrades survive a save and load, and the loaded Sink goes on voiding. */
    @ServusGameTest(timeoutTicks = 100)
    public static void sinkKeepsBufferOnReload(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        sink.upgrades().setStackInSlot(0, capacityUpgrade());
        sinkItems(helper, SINK, Direction.EAST).insertItem(new ItemStack(Items.COBBLESTONE, 30), false);
        sinkFluid(helper, SINK, Direction.UP).fill(new FluidStack(Fluids.LAVA, 3_000), false);
        CompoundTag saved = sink.saveWithoutMetadata(helper.getLevel().registryAccess());
        sink.dissolve();
        helper.assertTrue(!sink.holdsAnything(), "cleared before loading");
        drops(helper).forEach(ItemEntity::discard);
        sink.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(itemsHeld(sink) == 30, "the cobblestone is back, holds " + itemsHeld(sink));
        helper.assertTrue(sink.tank(0).getAmount() == 3_000, "the lava is back");
        helper.assertTrue(sink.upgrades().count() == 1, "the upgrade is back");
        sink.onLoad();
        helper.runAfterDelay(AFTER_FIRST_BATCH, () -> {
            helper.assertTrue(itemsHeld(sink) == 26, "the loaded Sink voids in batches, holds " + itemsHeld(sink));
            helper.succeed();
        });
    }

    private static List<ItemEntity> drops(GameTestHelper helper) {
        return GameTestSteps.drops(helper, SINK, 3);
    }

    /** The GUI of a formed Sink is valid until it unforms; a buffer of 4 rows of 7 with only the first open. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkGuiShowsLockedRows(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(SINK.north())));
        SinkMenu menu = new SinkMenu(1, player.getInventory(), sink);
        helper.assertTrue(menu.stillValid(player), "the GUI of a formed Sink is valid");
        helper.assertTrue(menu.bufferSlots() == 28 && menu.maxRows() == 4 && menu.openRows() == 1, "one of four rows open");
        helper.assertTrue(menu.getSlot(6).isActive() && !menu.getSlot(7).isActive(), "only the first row is open");
        helper.assertTrue(menu.chimneyHeight() == 0 && menu.itemRate() == 4 && menu.fluidRate() == 1_000,
                "height 0 shows 4 items and 1,000 mB per second");
        helper.assertTrue(menu.tankCapacity() == 8_000, "tanks hold 8,000 mB");
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

    /** Sets the stone of the material block at {@code pos}. */
    static void stone(GameTestHelper helper, BlockPos pos, Block stone) {
        ((MaterialHolder) helper.getBlockEntity(pos, BlockEntity.class)).materialSlot().set(BuiltInRegistries.BLOCK.getKey(stone));
    }

    /** The eight blocks of the cube at {@code min}, in reading order of {@link BlockPos#betweenClosed}. */
    private static List<BlockPos> cube(BlockPos min) {
        List<BlockPos> cube = new ArrayList<>();
        BlockPos.betweenClosed(min, min.offset(1, 1, 1)).forEach(pos -> cube.add(pos.immutable()));
        return cube;
    }

    /** Six Stone Bricks blocks and two Deepslate Bricks blocks stay unformed; the refusal names both stones. */
    @ServusGameTest(timeoutTicks = 60)
    public static void mixedStonesRefuseForming(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 0, 2);
        sinkBox(helper, min, null);
        List<BlockPos> cube = cube(min);
        for (int i = 0; i < cube.size(); i++) {
            stone(helper, cube.get(i), i < 2 ? Blocks.DEEPSLATE_BRICKS : Blocks.STONE_BRICKS);
        }
        Component refusal = MachineContent.SINK.get().tryForm(helper.getLevel(), helper.absolutePos(SINK), Direction.NORTH);
        for (BlockPos pos : cube) {
            helper.assertTrue(!formed(helper, pos), "a cube of mixed stones does not form");
        }
        helper.assertTrue(refusal != null && refusal.getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals("message.servus_foundation.sink.mixed_stone"),
                "the refusal says the stones differ, got " + refusal);
        String names = refusal.toString();
        helper.assertTrue(names.contains("block.minecraft.stone_bricks") && names.contains("block.minecraft.deepslate_bricks"),
                "the refusal names both stones, got " + names);
        helper.succeed();
    }

    /** Eight Polished Deepslate blocks form and keep their stone. */
    @ServusGameTest(timeoutTicks = 60)
    public static void oneStoneForms(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 0, 2);
        sinkBox(helper, min, null);
        cube(min).forEach(pos -> stone(helper, pos, Blocks.POLISHED_DEEPSLATE));
        carrot(helper, SINK, Direction.NORTH, false);
        assertFormedCube(helper, min, Direction.NORTH, SINK);
        Identifier kept = helper.getBlockEntity(new BlockPos(2, 1, 3), SinkBlockEntity.class).materialSlot().material();
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(Blocks.POLISHED_DEEPSLATE).equals(kept),
                "the formed blocks keep polished deepslate, got " + kept);
        helper.succeed();
    }

    /** Seven Bricks blocks and one block without a stone form: a missing stone counts as Bricks. */
    @ServusGameTest(timeoutTicks = 60)
    public static void missingStoneCountsAsBricks(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 0, 2);
        sinkBox(helper, min, null);
        List<BlockPos> cube = cube(min);
        for (int i = 1; i < cube.size(); i++) {
            stone(helper, cube.get(i), Blocks.BRICKS);
        }
        carrot(helper, SINK, Direction.NORTH, false);
        assertFormedCube(helper, min, Direction.NORTH, SINK);
        helper.succeed();
    }

    /** A Sandstone Sink block dismantled with the sneak carrot drops a Sink block that carries Sandstone. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkDropKeepsStone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 0, 2);
        helper.setBlock(pos, MachineContent.SINK.get().defaultBlockState());
        stone(helper, pos, Blocks.SANDSTONE);
        carrot(helper, pos, Direction.NORTH, true);
        helper.assertBlockNotPresent(MachineContent.SINK.get(), pos);
        List<ItemEntity> drops = GameTestSteps.drops(helper, pos, 2);
        Identifier sandstone = BuiltInRegistries.BLOCK.getKey(Blocks.SANDSTONE);
        helper.assertTrue(drops.stream().anyMatch(entity -> entity.getItem().is(MachineContent.SINK_ITEM.get())
                        && sandstone.equals(entity.getItem().get(MaterialContent.MATERIAL.get()))),
                "the dropped Sink block carries sandstone, got " + drops);
        helper.succeed();
    }

    /** A Bricks Sink and a Quartz Sink with two Chimney blocks each take the same energy rate. */
    @ServusGameTest(timeoutTicks = 60)
    public static void stoneDoesNotChangeRates(GameTestHelper helper) {
        SinkBlockEntity bricks = sink(helper, 2);
        BlockPos min = new BlockPos(6, 0, 2);
        sinkBox(helper, min, null);
        cube(min).forEach(pos -> stone(helper, pos, Blocks.QUARTZ_BLOCK));
        BlockPos quartzController = new BlockPos(7, 0, 2);
        carrot(helper, quartzController, Direction.NORTH, false);
        chimneyColumn(helper, new BlockPos(6, 1, 3), 2);
        SinkBlockEntity quartz = helper.getBlockEntity(quartzController, SinkBlockEntity.class);
        helper.assertTrue(bricks.chimneyHeight() == 2 && quartz.chimneyHeight() == 2,
                "both report height 2, got " + bricks.chimneyHeight() + " and " + quartz.chimneyHeight());
        int bricksEnergy = sinkEnergy(helper, SINK, Direction.EAST).receiveEnergy(100_000, false);
        int quartzEnergy = sinkEnergy(helper, quartzController, Direction.EAST).receiveEnergy(100_000, false);
        helper.assertTrue(bricksEnergy == 8_000 && quartzEnergy == 8_000,
                "both take one second of 400 FE per tick, got " + bricksEnergy + " and " + quartzEnergy);
        helper.succeed();
    }

    /** The next batch marks the slots it takes from: at 4 per second, 2 dirt and 64 gravel mark the first two slots. */
    @ServusGameTest(timeoutTicks = 60)
    public static void sinkMarksNextBatch(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 0);
        sink.items().setStackInSlot(0, new ItemStack(Items.DIRT, 2));
        sink.items().setStackInSlot(1, new ItemStack(Items.GRAVEL, 64));
        sink.items().setStackInSlot(2, new ItemStack(Items.SAND, 64));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SinkMenu menu = new SinkMenu(1, player.getInventory(), sink);
        boolean[] marked = menu.nextBatchSlots();
        helper.assertTrue(marked[0] && marked[1] && !marked[2], "the first two slots are marked");
        helper.assertTrue(menu.ticksToBatch() >= 0, "a batch waits");
        helper.succeed();
    }
}
