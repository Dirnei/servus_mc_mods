package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerShellBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Calciner as a structure: building, forming with the Journal, breaking and working it by hand. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CalcinerGameTests {

    /** A Calciner whose Ash Pan stands on the floor of the empty template. */
    static final BlockPos CALCINER = new BlockPos(5, 3, 5);

    private CalcinerGameTests() {
    }

    /** Builds the unformed structure around {@code controller} for a front facing {@code facing}; the Calciner itself faces north. */
    static void build(GameTestHelper helper, BlockPos controller, Direction facing) {
        for (Cell cell : CalcinerLayout.cells()) {
            BlockPos pos = CalcinerStructure.pos(controller, cell, CalcinerStructure.facing(facing));
            BlockState state = CalcinerStructure.expected(cell.role()).defaultBlockState();
            if (cell.role() == Role.CALCINER) {
                state = state.setValue(MachineBlock.FACING, Direction.NORTH);
            }
            helper.setBlock(pos, state);
        }
    }

    static BlockPos cellPos(BlockPos controller, Role role, Direction facing) {
        return CalcinerStructure.pos(controller, CalcinerLayout.part(role), CalcinerStructure.facing(facing));
    }

    static Player player(GameTestHelper helper, ItemStack held) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    /** Uses the held item of {@code player} on the block at {@code pos}, like a right click on its north face. */
    static void use(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false);
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.isEmpty()) {
            state.useWithoutItem(helper.getLevel(), player, hit);
        } else {
            state.useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        }
    }

    /** Builds and forms a Calciner facing {@code facing}. */
    static CalcinerBlockEntity formed(GameTestHelper helper, BlockPos controller, Direction facing) {
        build(helper, controller, facing);
        use(helper, player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), controller);
        helper.assertBlockProperty(controller, CalcinerBlock.FORMED, true);
        return helper.getBlockEntity(controller);
    }

    static void assertFormed(GameTestHelper helper, BlockPos controller, Direction facing, boolean formed) {
        for (Cell cell : CalcinerLayout.cells()) {
            BlockPos pos = CalcinerStructure.pos(controller, cell, CalcinerStructure.facing(facing));
            BlockState state = helper.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            switch (cell.role()) {
                case CALCINER -> {
                    helper.assertTrue(state.getValue(CalcinerBlock.FORMED) == formed, "Calciner formed should be " + formed);
                    if (formed) {
                        helper.assertTrue(state.getValue(MachineBlock.FACING) == facing, "the Calciner should face " + facing);
                    }
                }
                case BRICK -> helper.assertTrue(formed ? state.getBlock() instanceof CalcinerShellBlock : state.is(Blocks.BRICKS),
                        "brick cell " + cell.index() + " should be " + (formed ? "a shell" : "Bricks") + ", is " + state);
                default -> helper.assertTrue(state.getValue(CalcinerPartBlock.FORMED) == formed,
                        cell.role() + " formed should be " + formed);
            }
        }
    }

    /** The Journal forms the structure in each of the four facings, whatever way the Calciner was placed. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void calcinerFormsInEveryFacing(GameTestHelper helper) {
        BlockPos north = new BlockPos(2, 3, 2);
        BlockPos south = new BlockPos(2, 3, 8);
        BlockPos east = new BlockPos(6, 3, 3);
        BlockPos west = new BlockPos(9, 3, 8);
        formed(helper, north, Direction.NORTH);
        formed(helper, south, Direction.SOUTH);
        formed(helper, east, Direction.EAST);
        formed(helper, west, Direction.WEST);
        assertFormed(helper, north, Direction.NORTH, true);
        assertFormed(helper, south, Direction.SOUTH, true);
        assertFormed(helper, east, Direction.EAST, true);
        assertFormed(helper, west, Direction.WEST, true);
        helper.succeed();
    }

    /** Without the Receiver nothing forms. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void calcinerWithoutReceiverDoesNotForm(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        helper.setBlock(cellPos(CALCINER, Role.RECEIVER, Direction.NORTH), Blocks.AIR);
        use(helper, player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), CALCINER);
        helper.assertTrue(CalcinerStructure.check(helper.getLevel(), helper.absolutePos(CALCINER)).firstWrong()
                .map(Cell::role).orElse(null) == Role.RECEIVER, "the Receiver should be named as missing");
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.succeed();
    }

    /** The Journal on any other part forms nothing. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void journalOnTheFireboxFormsNothing(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        use(helper, player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())),
                cellPos(CALCINER, Role.FIREBOX, Direction.NORTH));
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.succeed();
    }

    /** The carrot neither turns nor unforms any block of a formed Calciner. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void carrotChangesNothing(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        Player player = player(helper, new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get()));
        for (Cell cell : CalcinerLayout.cells()) {
            BlockPos pos = helper.absolutePos(CalcinerStructure.pos(CALCINER, cell, CalcinerLayout.Facing.NORTH));
            for (boolean sneaking : new boolean[] {false, true}) {
                player.setShiftKeyDown(sneaking);
                ItemStack carrot = player.getItemInHand(InteractionHand.MAIN_HAND);
                carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)));
            }
        }
        assertFormed(helper, CALCINER, Direction.NORTH, true);
        helper.succeed();
    }

    /** Breaking a brick drops Bricks and turns the rest back into Bricks and unformed parts. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void breakingABrickUnforms(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        BlockPos brick = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(3), CalcinerLayout.Facing.NORTH);
        helper.getLevel().destroyBlock(helper.absolutePos(brick), true);
        helper.assertItemEntityPresent(Items.BRICKS, brick, 2.0);
        helper.assertBlockPresent(Blocks.AIR, brick);
        for (Cell cell : CalcinerLayout.cells()) {
            BlockPos pos = CalcinerStructure.pos(CALCINER, cell, CalcinerLayout.Facing.NORTH);
            if (pos.equals(brick)) {
                continue;
            }
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(switch (cell.role()) {
                case CALCINER -> !state.getValue(CalcinerBlock.FORMED);
                case BRICK -> state.is(Blocks.BRICKS);
                default -> !state.getValue(CalcinerPartBlock.FORMED);
            }, "cell " + cell.index() + " should be unformed, is " + state);
        }
        helper.succeed();
    }

    /** Breaking the Calciner drops it with its matter and fuel and unforms the rest. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void breakingTheCalcinerDropsItsItems(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 3));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));
        helper.getLevel().destroyBlock(helper.absolutePos(CALCINER), true);
        helper.assertItemEntityPresent(ServusManufacturingItems.CALCINER.get(), CALCINER, 2.0);
        helper.assertItemEntityPresent(Items.COBBLESTONE, CALCINER, 2.0);
        helper.assertItemEntityPresent(Items.COAL, CALCINER, 2.0);
        helper.assertBlockPresent(Blocks.BRICKS, CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(1), CalcinerLayout.Facing.NORTH));
        helper.assertBlockProperty(cellPos(CALCINER, Role.ALEMBIC, Direction.NORTH), CalcinerPartBlock.FORMED, false);
        helper.succeed();
    }

    /** Matter goes in at the mouth and fuel at the Firebox by hand; sneaking with an empty hand takes the matter back. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void handFeedsAFormedCalciner(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        Player player = player(helper, new ItemStack(Items.COBBLESTONE, 16));
        use(helper, player, CALCINER);
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).getCount() == 16, "16 cobblestone go in");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 5));
        use(helper, player, cellPos(CALCINER, Role.FIREBOX, Direction.NORTH));
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).getCount() == 5, "5 coal go into the Firebox");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        use(helper, player, CALCINER);
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).isEmpty(), "the input is taken back");
        helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 16, "the player gets the cobblestone");
        helper.succeed();
    }

    /** The player who forms the Calciner becomes its owner. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void formingPlayerOwnsTheCalciner(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        Player player = player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get()));
        use(helper, player, CALCINER);
        CalcinerBlockEntity calciner = helper.getBlockEntity(CALCINER);
        helper.assertTrue(calciner.owner().filter(player.getUUID()::equals).isPresent(), "the forming player owns the Calciner");
        helper.succeed();
    }
}
