package io.github.dirnei.mc.servus.alchemy.gametest;

import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import io.github.dirnei.mc.servus.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinationRecipes;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout.Cell;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerShellBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.EnergyView;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** The Calciner as a structure: building, forming with the Journal, breaking and working it by hand. */
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
        // As the game does: the held item first, even an empty hand, then the block's own use if it passes.
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        InteractionResult result = state.useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        if (result == InteractionResult.TRY_WITH_EMPTY_HAND) {
            state.useWithoutItem(helper.getLevel(), player, hit);
        }
    }

    /** Builds and forms a Calciner facing {@code facing}. */
    static CalcinerBlockEntity formed(GameTestHelper helper, BlockPos controller, Direction facing) {
        build(helper, controller, facing);
        use(helper, player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), controller);
        helper.assertBlockProperty(controller, CalcinerBlock.FORMED, true);
        return helper.getBlockEntity(controller, CalcinerBlockEntity.class);
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
                case STONE_BRICKS, DEEPSLATE_TILES -> helper.assertTrue(formed ? state.getBlock() instanceof CalcinerShellBlock
                                : state.is(CalcinerStructure.expected(cell.role())),
                        "stone cell " + cell.index() + " should be " + (formed ? "a shell" : cell.role()) + ", is " + state);
                case TANK -> helper.assertTrue(state.is(ServusLogisticsBlocks.FLUID_TANK.get()), "the tanks stay tanks");
                case DUCT -> helper.assertTrue(state.getValue(CopperDuctBlock.FORMED) == formed, "duct formed should be " + formed);
                default -> helper.assertTrue(state.getValue(CalcinerPartBlock.FORMED) == formed,
                        cell.role() + " formed should be " + formed);
            }
        }
    }

    /** The Journal forms the structure in each of the four facings, whatever way the Calciner was placed. */
    @ServusGameTest(timeoutTicks = 20)
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
    @ServusGameTest(timeoutTicks = 20)
    public static void calcinerWithoutReceiverDoesNotForm(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        helper.setBlock(cellPos(CALCINER, Role.RECEIVER, Direction.NORTH), Blocks.AIR);
        use(helper, player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), CALCINER);
        helper.assertTrue(CalcinerStructure.check(helper.getLevel(), helper.absolutePos(CALCINER)).firstWrong()
                .map(Cell::role).orElse(null) == Role.RECEIVER, "the Receiver should be named as missing");
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.succeed();
    }

    /** The Journal on any other part forms nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void journalOnTheFireboxFormsNothing(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        use(helper, player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())),
                cellPos(CALCINER, Role.FIREBOX, Direction.NORTH));
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.succeed();
    }

    /** The carrot neither turns nor unforms any block of a formed Calciner. */
    @ServusGameTest(timeoutTicks = 20)
    public static void carrotChangesNothing(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        Player player = player(helper, new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get()));
        for (Cell cell : CalcinerLayout.cells()) {
            if (cell.role() == Role.TANK) {
                // Foundation's tanks keep their own carrot rules.
                continue;
            }
            BlockPos pos = helper.absolutePos(CalcinerStructure.pos(CALCINER, cell, Facing.NORTH));
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

    /** Breaking the Stone Bricks drops them and turns the rest back into stone and unformed parts. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingABrickUnforms(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        BlockPos brick = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(1), Facing.NORTH);
        helper.getLevel().destroyBlock(helper.absolutePos(brick), true);
        helper.assertItemEntityPresent(Items.STONE_BRICKS, brick, 2.0);
        helper.assertBlockPresent(Blocks.AIR, brick);
        for (Cell cell : CalcinerLayout.cells()) {
            BlockPos pos = CalcinerStructure.pos(CALCINER, cell, Facing.NORTH);
            if (pos.equals(brick)) {
                continue;
            }
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(switch (cell.role()) {
                case CALCINER -> !state.getValue(CalcinerBlock.FORMED);
                case STONE_BRICKS, DEEPSLATE_TILES -> state.is(CalcinerStructure.expected(cell.role()));
                case TANK -> state.is(ServusLogisticsBlocks.FLUID_TANK.get());
                case DUCT -> !state.getValue(CopperDuctBlock.FORMED);
                default -> !state.getValue(CalcinerPartBlock.FORMED);
            }, "cell " + cell.index() + " should be unformed, is " + state);
        }
        helper.succeed();
    }

    /** Breaking the Deepslate Tiles beside the Ash Pan drops them and unforms the Calciner. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingDeepslateDropsDeepslate(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        BlockPos tiles = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(4), Facing.NORTH);
        helper.getLevel().destroyBlock(helper.absolutePos(tiles), true);
        helper.assertItemEntityPresent(Items.DEEPSLATE_TILES, tiles, 2.0);
        helper.assertBlockPresent(Blocks.DEEPSLATE_TILES, CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(3), Facing.NORTH));
        helper.assertBlockPresent(Blocks.STONE_BRICKS, CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(2), Facing.NORTH));
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.succeed();
    }

    /** Vanilla Bricks in place of the Stone Bricks keep the Calciner from forming, and the Stone Bricks are named as missing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void oldBricksDoNotForm(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        helper.setBlock(CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(2), Facing.NORTH), Blocks.BRICKS);
        use(helper, player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), CALCINER);
        helper.assertTrue(CalcinerStructure.check(helper.getLevel(), helper.absolutePos(CALCINER)).firstWrong()
                .map(cell -> CalcinerStructure.expected(cell.role())).orElse(null) == Blocks.STONE_BRICKS,
                "Stone Bricks should be named as missing");
        helper.assertBlockProperty(CALCINER, CalcinerBlock.FORMED, false);
        helper.succeed();
    }

    /** Breaking the Calciner drops it with its matter and fuel and unforms the rest. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingTheCalcinerDropsItsItems(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 3));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));
        helper.getLevel().destroyBlock(helper.absolutePos(CALCINER), true);
        helper.assertItemEntityPresent(ServusAlchemyItems.CALCINER.get(), CALCINER, 2.0);
        helper.assertItemEntityPresent(Items.COBBLESTONE, CALCINER, 2.0);
        helper.assertItemEntityPresent(Items.COAL, CALCINER, 2.0);
        helper.assertBlockPresent(Blocks.STONE_BRICKS, CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(1), Facing.NORTH));
        helper.assertBlockProperty(cellPos(CALCINER, Role.ALEMBIC, Direction.NORTH), CalcinerPartBlock.FORMED, false);
        helper.succeed();
    }

    /** Matter goes in at the mouth and fuel at the Firebox by hand; sneaking with an empty hand takes the matter back. */
    @ServusGameTest(timeoutTicks = 20)
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
    @ServusGameTest(timeoutTicks = 20)
    public static void formingPlayerOwnsTheCalciner(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        Player player = player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get()));
        use(helper, player, CALCINER);
        CalcinerBlockEntity calciner = helper.getBlockEntity(CALCINER, CalcinerBlockEntity.class);
        helper.assertTrue(calciner.owner().filter(player.getUUID()::equals).isPresent(), "the forming player owns the Calciner");
        helper.succeed();
    }

    static FluidView tankAt(GameTestHelper helper, BlockPos pos) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), Direction.UP));
    }

    /** A hopper feeds the mouth of a formed Calciner, which burns the cobblestone into its tanks. */
    @ServusGameTest(timeoutTicks = 300)
    public static void hopperFeedsAFormedCalciner(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));
        BlockPos hopper = CALCINER.north();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.SOUTH));
        HopperBlockEntity hopperEntity = helper.getBlockEntity(hopper, HopperBlockEntity.class);
        hopperEntity.setItem(0, new ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> helper.assertTrue(calciner.salt() >= 200,
                "two cobblestone should give 200 Salt, got " + calciner.salt()));
    }

    /** An unformed Calciner holding matter and fuel makes nothing. */
    @ServusGameTest(timeoutTicks = 120)
    public static void unformedCalcinerDoesNothing(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        CalcinerBlockEntity calciner = helper.getBlockEntity(CALCINER, CalcinerBlockEntity.class);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(calciner.salt() == 0, "nothing is calcined while unformed");
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).getCount() == 2, "no fuel is burnt");
            helper.succeed();
        });
    }

    static BlockPos sulfurTank(BlockPos controller) {
        return CalcinerStructure.pos(controller, CalcinerLayout.cell(CalcinerLayout.SULFUR_TANK), Facing.NORTH);
    }

    static BlockPos mercuryTank(BlockPos controller) {
        return CalcinerStructure.pos(controller, CalcinerLayout.cell(CalcinerLayout.MERCURY_TANK), Facing.NORTH);
    }

    /** A finished cycle puts the Mercury straight into the Mercury tank and the Salt into the Ash Pan. */
    @ServusGameTest(timeoutTicks = 200)
    public static void tanksFillFromTheCalciner(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> {
            FluidStack mercury = tankAt(helper, mercuryTank(CALCINER)).getFluidInTank(0);
            helper.assertTrue(mercury.getFluid().isSame(PrincipleFluids.fluid(Principle.MERCURY)) && mercury.getAmount() == 10,
                    "10 Mercury in the Mercury tank, got " + mercury);
            helper.assertTrue(tankAt(helper, sulfurTank(CALCINER)).getFluidInTank(0).isEmpty(), "cobblestone gives no Sulfur");
            helper.assertTrue(calciner.salt() == 100, "the Salt stays in the Ash Pan, got " + calciner.salt());
        });
    }

    /** No block of the structure has a fluid port; the tanks are the only place for Sulfur and Mercury. */
    @ServusGameTest(timeoutTicks = 20)
    public static void noFluidPortOnTheReceiverOrDuct(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        BlockPos duct = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(CalcinerLayout.SULFUR_OUTLET), Facing.NORTH);
        for (BlockPos pos : new BlockPos[] {cellPos(CALCINER, Role.RECEIVER, Direction.NORTH), duct, CALCINER,
                cellPos(CALCINER, Role.ASH_PAN, Direction.NORTH), cellPos(CALCINER, Role.HOOD, Direction.NORTH)}) {
            for (Direction side : Direction.values()) {
                helper.assertTrue(FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), side)) == null,
                        "no fluid port at " + pos + " " + side);
            }
        }
        helper.succeed();
    }

    /** With its Mercury tank full the Calciner keeps its input and fuel and makes nothing. */
    @ServusGameTest(timeoutTicks = 160)
    public static void fullMercuryTankStopsTheCalciner(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        FluidView tank = tankAt(helper, mercuryTank(CALCINER));
        int capacity = tank.getTankCapacity(0);
        helper.assertTrue(tank.fill(new FluidStack(PrincipleFluids.fluid(Principle.MERCURY), capacity), false) == capacity, "the tank fills up");
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).is(Items.COBBLESTONE), "the cobblestone stays");
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).getCount() == 2, "no fuel is burnt");
            helper.assertTrue(calciner.salt() == 0, "no Salt is made");
            helper.succeed();
        });
    }

    /** A bucket on the Receiver stays empty; Mercury is bucketed from its tank. */
    @ServusGameTest(timeoutTicks = 20)
    public static void bucketDoesNothingAtTheReceiver(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        FluidView tank = tankAt(helper, mercuryTank(CALCINER));
        tank.fill(new FluidStack(PrincipleFluids.fluid(Principle.MERCURY), 1200), false);
        Player player = player(helper, new ItemStack(Items.BUCKET));
        use(helper, player, cellPos(CALCINER, Role.RECEIVER, Direction.NORTH));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET),
                "the bucket stays empty, holds " + player.getItemInHand(InteractionHand.MAIN_HAND));
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 1200, "the tank keeps its Mercury");
        helper.succeed();
    }

    /** The Ash Pan fills up to a stack of Ash, then the Calciner waits. */
    @ServusGameTest(timeoutTicks = 300)
    public static void ashPanHoldsAStack(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.putSalt(6300);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 3));
        helper.runAfterDelay(260, () -> {
            helper.assertTrue(calciner.ashCount() == 64 && calciner.ashRoom() == 64, "the Ash Pan holds 64 Ash, got "
                    + calciner.ashCount() + " of " + calciner.ashRoom());
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).getCount() == 2,
                    "only one cobblestone fits, the rest waits");
            helper.succeed();
        });
    }

    /** No block of the Calciner offers energy; the Firebox reports the fuel as upkeep and takes coal, the mouth takes only matter. */
    @ServusGameTest(timeoutTicks = 20)
    public static void fireboxTakesFuelAndUpkeep(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        for (Cell cell : CalcinerLayout.cells()) {
            BlockPos pos = helper.absolutePos(CalcinerStructure.pos(CALCINER, cell, Facing.NORTH));
            for (Direction side : Direction.values()) {
                helper.assertTrue(EnergyView.of(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, pos, side)) == null, "no energy at " + cell.role());
            }
        }
        BlockPos firebox = helper.absolutePos(cellPos(CALCINER, Role.FIREBOX, Direction.NORTH));
        ItemView fuel = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, firebox, Direction.SOUTH));
        helper.assertTrue(fuel != null && fuel.insertItem(0, new ItemStack(Items.COAL, 3), false).isEmpty(), "the Firebox takes coal");
        helper.assertTrue(fuel.insertItem(0, new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "the Firebox refuses matter");
        helper.assertTrue(fuel.extractItem(0, 1, false).isEmpty(), "the Firebox gives nothing back");
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).getCount() == 3, "the coal lands in the fuel slot");
        UpkeepHolder upkeep = helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, firebox, null);
        helper.assertTrue(upkeep != null && upkeep.upkeepItem().is(Items.COAL) && upkeep.upkeepCount() == 3, "the Firebox reports coal as upkeep");
        ItemView mouth = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(CALCINER), Direction.NORTH));
        helper.assertTrue(mouth != null, "the mouth takes items");
        helper.assertTrue(mouth.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1, "a diamond is refused");
        helper.assertTrue(mouth.insertItem(0, new ItemStack(Items.OAK_LOG), false).isEmpty(), "a log is taken");
        helper.assertTrue(ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK,
                helper.absolutePos(cellPos(CALCINER, Role.ALEMBIC, Direction.NORTH)), Direction.NORTH)) == null, "the Alembic takes no items");
        helper.succeed();
    }

    /** An unformed structure offers no ports. */
    @ServusGameTest(timeoutTicks = 20)
    public static void unformedHasNoPorts(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        helper.assertTrue(ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(CALCINER), Direction.NORTH)) == null,
                "no item port while unformed");
        helper.assertTrue(FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK,
                helper.absolutePos(cellPos(CALCINER, Role.ASH_PAN, Direction.NORTH)), Direction.NORTH)) == null, "no fluid port while unformed");
        helper.succeed();
    }

    /** The journal's schematic, anchored on the Ash Pan, accepts the structure that forming accepts. */
    @ServusGameTest(timeoutTicks = 20)
    public static void schematicMatchesTheBuild(GameTestHelper helper) {
        Multiblock schematic = ModonomiconAPI.get().getMultiblock(
                Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "calciner"));
        helper.assertTrue(schematic != null, "the Calciner schematic should be loaded on the server");
        build(helper, CALCINER, Direction.NORTH);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(CALCINER.below(2))) != null,
                "the schematic should accept a complete build");
        helper.assertTrue(CalcinerStructure.check(helper.getLevel(), helper.absolutePos(CALCINER)).complete(), "and so does forming");
        helper.setBlock(cellPos(CALCINER, Role.HOOD, Direction.NORTH), Blocks.AIR);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(CALCINER.below(2))) == null,
                "the schematic should refuse a build without the Hood");
        helper.succeed();
    }

    /** Breaking the Mercury tank unforms the Calciner. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breakingATankUnforms(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        BlockPos tank = mercuryTank(CALCINER);
        helper.getLevel().destroyBlock(helper.absolutePos(tank), true);
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.succeed();
    }

    /** A hopper under the Ash Pan takes the Salt as whole Ash; the remainder stays. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hopperTakesAshFromTheAshPan(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.putSalt(350);
        BlockPos hopper = cellPos(CALCINER, Role.ASH_PAN, Direction.NORTH).below();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopperEntity = helper.getBlockEntity(hopper, HopperBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(hopperEntity.countItem(ServusAlchemyItems.ASH.get()) == 3, "the hopper should hold 3 Ash, holds "
                    + hopperEntity.countItem(ServusAlchemyItems.ASH.get()));
            helper.assertTrue(calciner.salt() == 50, "50 mB Salt stay behind");
        });
    }

    /** An empty hand takes all Ash out of the Ash Pan; a bucket takes nothing there. */
    @ServusGameTest(timeoutTicks = 20)
    public static void handTakesAshFromTheAshPan(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.putSalt(500);
        BlockPos ashPan = cellPos(CALCINER, Role.ASH_PAN, Direction.NORTH);
        Player player = player(helper, new ItemStack(Items.BUCKET));
        use(helper, player, ashPan);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the bucket stays empty");
        helper.assertTrue(calciner.salt() == 500, "a bucket takes no Salt");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        use(helper, player, ashPan);
        helper.assertTrue(player.getInventory().countItem(ServusAlchemyItems.ASH.get()) == 5, "the player gets 5 Ash");
        helper.assertTrue(calciner.salt() == 0, "the Ash Pan is empty");
        helper.succeed();
    }

    static ItemStack ignis(int count) {
        return new ItemStack(ServusAlchemyItems.IGNIS_PERPETUUS.get(), count);
    }

    static BlockPos firebox(BlockPos controller) {
        return cellPos(controller, Role.FIREBOX, Direction.NORTH);
    }

    /** A formed Calciner facing north with Ignis Perpetuus set into its Firebox by hand. */
    static CalcinerBlockEntity withIgnis(GameTestHelper helper, BlockPos controller) {
        CalcinerBlockEntity calciner = formed(helper, controller, Direction.NORTH);
        use(helper, player(helper, ignis(1)), firebox(controller));
        helper.assertTrue(calciner.hasIgnis(), "the Calciner should hold the Ignis Perpetuus");
        return calciner;
    }

    /** One of two Ignis Perpetuus in hand goes into the Firebox. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ignisSetByHand(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        Player player = player(helper, ignis(2));
        use(helper, player, firebox(CALCINER));
        helper.assertTrue(calciner.hasIgnis(), "the Calciner holds the Ignis Perpetuus");
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_IGNIS).getCount() == 1, "it holds one");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "the player keeps one");
        helper.succeed();
    }

    /** Setting the Ignis Perpetuus gives the coal in the fuel slot back to the player. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ignisReturnsFuel(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 5));
        Player player = player(helper, ignis(1));
        use(helper, player, firebox(CALCINER));
        helper.assertTrue(player.getInventory().countItem(Items.COAL) == 5, "the player gets the 5 coal");
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).isEmpty(), "the fuel slot is empty");
        helper.succeed();
    }

    /** A Calciner holds one Ignis Perpetuus; a second stays in the player's hand. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ignisOnlyOne(GameTestHelper helper) {
        CalcinerBlockEntity calciner = withIgnis(helper, CALCINER);
        Player player = player(helper, ignis(1));
        use(helper, player, firebox(CALCINER));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "the player keeps the second");
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_IGNIS).getCount() == 1, "the Calciner holds one");
        helper.succeed();
    }

    /** Ignis Perpetuus used on the mouth is neither set nor taken as matter. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ignisNotAtTheMouth(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        Player player = player(helper, ignis(1));
        use(helper, player, CALCINER);
        helper.assertTrue(!calciner.hasIgnis(), "nothing is set at the mouth");
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).isEmpty(), "the input stays empty");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "the player keeps it");
        helper.succeed();
    }

    /** No port takes Ignis Perpetuus, neither the Firebox nor the mouth. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ignisNotByPipe(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        for (BlockPos pos : new BlockPos[] {firebox(CALCINER), CALCINER}) {
            ItemView port = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), Direction.SOUTH));
            helper.assertTrue(port != null && port.insertItem(0, ignis(1), false).getCount() == 1, "the port at " + pos + " takes no Ignis");
        }
        helper.assertTrue(!calciner.hasIgnis(), "nothing is set");
        helper.succeed();
    }

    /** With the eternal flame the Calciner calcines without fuel until the Ash Pan is full and keeps the Ignis. */
    @ServusGameTest(timeoutTicks = 200)
    public static void ignisBurnsWithoutFuel(GameTestHelper helper) {
        CalcinerBlockEntity calciner = withIgnis(helper, CALCINER);
        calciner.putSalt(6200);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(calciner.ashCount() == 64, "the Ash Pan should fill up, holds " + calciner.ashCount());
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).getCount() == 1, "one cobblestone waits");
            helper.assertTrue(calciner.hasIgnis(), "the Ignis Perpetuus is still there");
        });
    }

    /** With the eternal flame the Firebox takes coal neither by hand nor through its port. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ignisRefusesCoal(GameTestHelper helper) {
        CalcinerBlockEntity calciner = withIgnis(helper, CALCINER);
        Player player = player(helper, new ItemStack(Items.COAL, 4));
        use(helper, player, firebox(CALCINER));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 4, "the player keeps the coal");
        ItemView port = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(firebox(CALCINER)), Direction.SOUTH));
        helper.assertTrue(port != null && port.insertItem(0, new ItemStack(Items.COAL, 3), false).getCount() == 3, "the port takes no coal");
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).isEmpty(), "the fuel slot stays empty");
        helper.succeed();
    }

    /** With the eternal flame the Firebox reports no upkeep, also to a cache made before. */
    @ServusGameTest(timeoutTicks = 20)
    public static void ignisNoUpkeep(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        BlockCapabilityCache<UpkeepHolder, Direction> cache = BlockCapabilityCache.create(ServusFoundationCapabilities.UPKEEP,
                helper.getLevel(), helper.absolutePos(firebox(CALCINER)), null);
        helper.assertTrue(cache.getCapability() != null, "the Firebox reports upkeep while it burns coal");
        use(helper, player(helper, ignis(1)), firebox(CALCINER));
        helper.assertTrue(cache.getCapability() == null, "the Firebox reports no upkeep with the eternal flame");
        helper.succeed();
    }

    /** With the shipped data the eternal flame calcines a cobblestone in 40 ticks, coal in 80. */
    @ServusGameTest(timeoutTicks = 200)
    public static void ignisTwiceAsFast(GameTestHelper helper) {
        BlockPos eternalAt = new BlockPos(2, 3, 5);
        BlockPos coalAt = new BlockPos(6, 3, 5);
        CalcinerBlockEntity eternal = withIgnis(helper, eternalAt);
        CalcinerBlockEntity coal = formed(helper, coalAt, Direction.NORTH);
        coal.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
        eternal.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 2));
        coal.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 2));
        helper.startSequence()
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(eternal.salt() == 100, "the eternal flame has calcined one cobblestone, got " + eternal.salt());
                    helper.assertTrue(coal.salt() == 0, "coal has calcined none yet, got " + coal.salt());
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(eternal.salt() == 200, "the eternal flame has calcined two, got " + eternal.salt());
                    helper.assertTrue(coal.salt() == 100, "coal has calcined one, got " + coal.salt());
                })
                .thenSucceed();
    }

    /** Sneaking with an empty hand takes the Ignis Perpetuus back; without fuel the Calciner then stops. */
    @ServusGameTest(timeoutTicks = 100)
    public static void ignisTakenBack(GameTestHelper helper) {
        CalcinerBlockEntity calciner = withIgnis(helper, CALCINER);
        Player player = player(helper, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        use(helper, player, firebox(CALCINER));
        helper.assertTrue(!calciner.hasIgnis(), "the Calciner holds no Ignis Perpetuus");
        helper.assertTrue(player.getInventory().countItem(ServusAlchemyItems.IGNIS_PERPETUUS.get()) == 1, "the player gets it back");
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(calciner.salt() == 0, "without fuel nothing is calcined");
            helper.succeed();
        });
    }

    /** Unformed and formed again, the Calciner keeps its Ignis Perpetuus and burns without fuel again. */
    @ServusGameTest(timeoutTicks = 200)
    public static void ignisKeptWhileUnformed(GameTestHelper helper) {
        CalcinerBlockEntity calciner = withIgnis(helper, CALCINER);
        BlockPos brick = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(1), Facing.NORTH);
        helper.getLevel().destroyBlock(helper.absolutePos(brick), false);
        helper.assertBlockProperty(CALCINER, CalcinerBlock.FORMED, false);
        helper.assertTrue(calciner.hasIgnis(), "the unformed Calciner keeps the Ignis Perpetuus");
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.startSequence()
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(calciner.salt() == 0, "the unformed Calciner does not work");
                    helper.setBlock(brick, Blocks.STONE_BRICKS);
                    use(helper, player(helper, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get())), CALCINER);
                    helper.assertBlockProperty(CALCINER, CalcinerBlock.FORMED, true);
                    helper.assertTrue(calciner.hasIgnis(), "formed again it still holds the Ignis Perpetuus");
                })
                .thenWaitUntil(() -> helper.assertTrue(calciner.salt() == 100, "it calcines without fuel again, got " + calciner.salt()))
                .thenSucceed();
    }

    /** A broken Firebox drops itself and the Calciner's Ignis Perpetuus. */
    @ServusGameTest(timeoutTicks = 20)
    public static void fireboxBrokenDropsIgnis(GameTestHelper helper) {
        CalcinerBlockEntity calciner = withIgnis(helper, CALCINER);
        helper.getLevel().destroyBlock(helper.absolutePos(firebox(CALCINER)), true);
        helper.assertItemEntityPresent(ServusAlchemyItems.FIREBOX.get(), firebox(CALCINER), 2.0);
        helper.assertItemEntityPresent(ServusAlchemyItems.IGNIS_PERPETUUS.get(), firebox(CALCINER), 2.0);
        helper.assertTrue(!calciner.hasIgnis(), "the Calciner holds no Ignis Perpetuus");
        helper.succeed();
    }

    /** A broken Calciner drops its Ignis Perpetuus with itself. */
    @ServusGameTest(timeoutTicks = 20)
    public static void calcinerBrokenDropsIgnis(GameTestHelper helper) {
        withIgnis(helper, CALCINER);
        helper.getLevel().destroyBlock(helper.absolutePos(CALCINER), true);
        helper.assertItemEntityPresent(ServusAlchemyItems.CALCINER.get(), CALCINER, 2.0);
        helper.assertItemEntityPresent(ServusAlchemyItems.IGNIS_PERPETUUS.get(), CALCINER, 2.0);
        helper.succeed();
    }

    /** The Firebox, Ash Pan and Calciner craft from the stone they are drawn in. */
    @ServusGameTest(timeoutTicks = 20)
    public static void calcinerPartRecipes(GameTestHelper helper) {
        CraftingGrids.assertCrafts(helper, crafted(helper, Items.POLISHED_DEEPSLATE, Items.IRON_BARS, Items.FURNACE, "BIB", "BFB"), ServusAlchemyItems.FIREBOX.get());
        CraftingGrids.assertCrafts(helper, crafted(helper, Items.DEEPSLATE_TILES, Items.IRON_INGOT, Items.AIR, "B B", "BIB"), ServusAlchemyItems.ASH_PAN.get());
        CraftingGrids.assertCrafts(helper, crafted(helper, Items.STONE_BRICKS, Items.IRON_BARS, Items.CAULDRON, "BIB", "BFB"), ServusAlchemyItems.CALCINER.get());
        helper.succeed();
    }

    /** Bricks in the part patterns craft nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void brickGridsCraftNothing(GameTestHelper helper) {
        helper.assertTrue(crafted(helper, Items.BRICKS, Items.IRON_BARS, Items.FURNACE, "BIB", "BFB").isEmpty(), "bricks make no Firebox");
        helper.assertTrue(crafted(helper, Items.BRICKS, Items.IRON_INGOT, Items.AIR, "B B", "BIB").isEmpty(), "bricks make no Ash Pan");
        helper.assertTrue(crafted(helper, Items.BRICKS, Items.IRON_BARS, Items.CAULDRON, "BIB", "BFB").isEmpty(), "bricks make no Calciner");
        helper.succeed();
    }

    /** Burns one {@code item} in a formed Calciner and expects exactly these principles. */
    private static void burnsInto(GameTestHelper helper, Item item, int salt, int sulfur, int mercury) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(item));
        helper.succeedWhen(() -> {
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).isEmpty(), item + " should be burned");
            int madeSulfur = tankAt(helper, sulfurTank(CALCINER)).getFluidInTank(0).getAmount();
            int madeMercury = tankAt(helper, mercuryTank(CALCINER)).getFluidInTank(0).getAmount();
            helper.assertTrue(calciner.salt() == salt && madeSulfur == sulfur && madeMercury == mercury,
                    item + " should give " + salt + "/" + sulfur + "/" + mercury + " Salt/Sulfur/Mercury, gave "
                            + calciner.salt() + "/" + madeSulfur + "/" + madeMercury);
        });
    }

    /** Nether Wart is in the crops tag too, but burns with its own recipe. */
    @ServusGameTest(timeoutTicks = 200)
    public static void netherWartBurnsWithItsOwnRecipe(GameTestHelper helper) {
        burnsInto(helper, Items.NETHER_WART, 0, 60, 20);
    }

    /** A Nether Wart Block burns into the principles of the nine Nether Wart it is crafted from. */
    @ServusGameTest(timeoutTicks = 200)
    public static void netherWartBlockBurnsIntoNineWarts(GameTestHelper helper) {
        burnsInto(helper, Items.NETHER_WART_BLOCK, 0, 540, 180);
    }

    /** A Crimson Stem is a log too, but burns as a stem. */
    @ServusGameTest(timeoutTicks = 200)
    public static void crimsonStemBurnsAsAStem(GameTestHelper helper) {
        burnsInto(helper, Items.CRIMSON_STEM, 0, 150, 0);
    }

    /** Dragon's Breath is kept for the Prelum and never goes into the Calciner. */
    @ServusGameTest(timeoutTicks = 20)
    public static void dragonsBreathIsNotCalcinable(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        ItemStack breath = new ItemStack(Items.DRAGON_BREATH);
        helper.assertTrue(!calciner.calcinable(breath), "Dragon's Breath should not be calcinable");
        helper.assertTrue(calciner.insertInput(breath).getCount() == 1, "the Calciner should refuse Dragon's Breath");
        helper.succeed();
    }

    /** The Crops recipe shows and burns wheat, but leaves Nether Wart to its own recipe. */
    @ServusGameTest(timeoutTicks = 20)
    @SuppressWarnings("unchecked")
    public static void cropsRecipeDoesNotBurnNetherWart(GameTestHelper helper) {
        RecipeHolder<CalcinationRecipe> crops = (RecipeHolder<CalcinationRecipe>) ServusRecipes.byId(helper.getLevel(),
                Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "calcination/crops")).orElseThrow();
        List<ItemStack> burns = CalcinationRecipes.burns(helper.getLevel(), crops);
        helper.assertTrue(burns.stream().anyMatch(stack -> stack.is(Items.WHEAT)), "the Crops recipe should burn wheat");
        helper.assertTrue(burns.stream().noneMatch(stack -> stack.is(Items.NETHER_WART)), "the Crops recipe should not burn Nether Wart");
        helper.assertTrue(CalcinationRecipes.forItem(helper.getLevel(), new ItemStack(Items.NETHER_WART))
                        .map(holder -> holder.id().identifier().getPath()).orElse("").equals("calcination/nether_wart"),
                "Nether Wart should pick the Nether Wart recipe");
        helper.succeed();
    }

    /** What a grid of {@code rows} crafts, with B {@code stone}, I {@code iron}, F {@code middle} and blanks empty. */
    private static ItemStack crafted(GameTestHelper helper, Item stone, Item iron, Item middle, String... rows) {
        return CraftingGrids.crafted(helper, Map.of('B', stone, 'I', iron, 'F', middle), rows);
    }
}
