package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerDuctBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerShellBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
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
            if (cell.role() == Role.AIR) {
                continue;
            }
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
        ItemInteractionResult result = state.useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            state.useWithoutItem(helper.getLevel(), player, hit);
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
                case TANK -> helper.assertTrue(state.is(ServusLogisticsBlocks.FLUID_TANK.get()), "the tanks stay tanks");
                case AIR -> {
                }
                case DUCT -> helper.assertTrue(state.getValue(CalcinerDuctBlock.FORMED) == formed, "duct formed should be " + formed);
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
            if (cell.role() == Role.TANK || cell.role() == Role.AIR) {
                // Foundation's tanks keep their own carrot rules; the air cell has nothing to click.
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

    /** Breaking a brick drops Bricks and turns the rest back into Bricks and unformed parts. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void breakingABrickUnforms(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        BlockPos brick = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(3), Facing.NORTH);
        helper.getLevel().destroyBlock(helper.absolutePos(brick), true);
        helper.assertItemEntityPresent(Items.BRICKS, brick, 2.0);
        helper.assertBlockPresent(Blocks.AIR, brick);
        for (Cell cell : CalcinerLayout.cells()) {
            BlockPos pos = CalcinerStructure.pos(CALCINER, cell, Facing.NORTH);
            if (pos.equals(brick)) {
                continue;
            }
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(switch (cell.role()) {
                case CALCINER -> !state.getValue(CalcinerBlock.FORMED);
                case BRICK -> state.is(Blocks.BRICKS);
                case TANK -> state.is(ServusLogisticsBlocks.FLUID_TANK.get());
                case AIR -> true;
                case DUCT -> !state.getValue(CalcinerDuctBlock.FORMED);
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
        helper.assertBlockPresent(Blocks.BRICKS, CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(1), Facing.NORTH));
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

    static IFluidHandler tankAt(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
    }

    /** A hopper feeds the mouth of a formed Calciner, which burns the cobblestone into its tanks. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 300)
    public static void hopperFeedsAFormedCalciner(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));
        BlockPos hopper = CALCINER.north();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.SOUTH));
        HopperBlockEntity hopperEntity = helper.getBlockEntity(hopper);
        hopperEntity.setItem(0, new ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> helper.assertTrue(calciner.tanks().amount(Principle.SALT) >= 200,
                "two cobblestone should give 200 Salt, got " + calciner.tanks().amount(Principle.SALT)));
    }

    /** An unformed Calciner holding matter and fuel makes nothing. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 120)
    public static void unformedCalcinerDoesNothing(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        CalcinerBlockEntity calciner = helper.getBlockEntity(CALCINER);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(calciner.tanks().contents().equals(new Principles(0, 0, 0)), "nothing is calcined while unformed");
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).getCount() == 2, "no fuel is burnt");
            helper.succeed();
        });
    }

    /** Each port gives only its own principle: Sulfur and Mercury into their tanks, the Salt stays as Ash. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 100)
    public static void eachPortGivesItsPrinciple(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        BlockPos sulfurTank = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(12), Facing.NORTH);
        BlockPos mercuryTank = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(15), Facing.NORTH);
        calciner.tanks().insert(new Principles(500, 400, 300));
        IFluidHandler receiver = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(cellPos(CALCINER, Role.RECEIVER, Direction.NORTH)), Direction.NORTH);
        helper.assertTrue(receiver != null && receiver.getTanks() == 1
                && receiver.getFluidInTank(0).getFluid().isSame(PrincipleFluids.fluid(Principle.MERCURY)), "the Receiver offers Mercury only");
        helper.assertTrue(receiver.fill(new FluidStack(PrincipleFluids.fluid(Principle.MERCURY), 100), IFluidHandler.FluidAction.EXECUTE) == 0,
                "a port never takes fluid");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(CALCINER), Direction.NORTH) == null,
                "the mouth has no fluid port");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(cellPos(CALCINER, Role.ASH_PAN, Direction.NORTH)), Direction.NORTH) == null, "the Ash Pan has no fluid port");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(cellPos(CALCINER, Role.HOOD, Direction.NORTH)), Direction.UP) == null, "the Hood has no fluid port");
        helper.succeedWhen(() -> {
            FluidStack sulfur = tankAt(helper, sulfurTank).getFluidInTank(0);
            FluidStack mercury = tankAt(helper, mercuryTank).getFluidInTank(0);
            helper.assertTrue(calciner.tanks().amount(Principle.SALT) == 500, "the Salt stays in the Ash Pan as Ash");
            helper.assertTrue(sulfur.getFluid().isSame(PrincipleFluids.fluid(Principle.SULFUR)) && sulfur.getAmount() == 400,
                    "400 Sulfur in the Sulfur tank, got " + sulfur);
            helper.assertTrue(mercury.getFluid().isSame(PrincipleFluids.fluid(Principle.MERCURY)) && mercury.getAmount() == 300,
                    "300 Mercury in the Mercury tank, got " + mercury);
        });
    }

    /** An empty bucket on the Receiver fills with Mercury. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void bucketFillsAtTheReceiver(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.tanks().insert(new Principles(0, 0, 1200));
        Player player = player(helper, new ItemStack(Items.BUCKET));
        use(helper, player, cellPos(CALCINER, Role.RECEIVER, Direction.NORTH));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(PrincipleFluids.entry(Principle.MERCURY).bucket().get()),
                "the player should hold a Mercury bucket, holds " + player.getItemInHand(InteractionHand.MAIN_HAND));
        helper.assertTrue(calciner.tanks().amount(Principle.MERCURY) == 200, "200 mB Mercury remain");
        helper.succeed();
    }

    /** No block of the Calciner offers energy; the Firebox reports the fuel as upkeep and takes coal, the mouth takes only matter. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void fireboxTakesFuelAndUpkeep(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        for (Cell cell : CalcinerLayout.cells()) {
            BlockPos pos = helper.absolutePos(CalcinerStructure.pos(CALCINER, cell, Facing.NORTH));
            for (Direction side : Direction.values()) {
                helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) == null, "no energy at " + cell.role());
            }
        }
        BlockPos firebox = helper.absolutePos(cellPos(CALCINER, Role.FIREBOX, Direction.NORTH));
        IItemHandler fuel = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, firebox, Direction.SOUTH);
        helper.assertTrue(fuel != null && fuel.insertItem(0, new ItemStack(Items.COAL, 3), false).isEmpty(), "the Firebox takes coal");
        helper.assertTrue(fuel.insertItem(0, new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "the Firebox refuses matter");
        helper.assertTrue(fuel.extractItem(0, 1, false).isEmpty(), "the Firebox gives nothing back");
        helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).getCount() == 3, "the coal lands in the fuel slot");
        UpkeepHolder upkeep = helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, firebox, null);
        helper.assertTrue(upkeep != null && upkeep.upkeepItem().is(Items.COAL) && upkeep.upkeepCount() == 3, "the Firebox reports coal as upkeep");
        IItemHandler mouth = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CALCINER), Direction.NORTH);
        helper.assertTrue(mouth != null, "the mouth takes items");
        helper.assertTrue(mouth.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1, "a diamond is refused");
        helper.assertTrue(mouth.insertItem(0, new ItemStack(Items.OAK_LOG), false).isEmpty(), "a log is taken");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(cellPos(CALCINER, Role.ALEMBIC, Direction.NORTH)), Direction.NORTH) == null, "the Alembic takes no items");
        helper.succeed();
    }

    /** An unformed structure offers no ports. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void unformedHasNoPorts(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CALCINER), Direction.NORTH) == null,
                "no item port while unformed");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(cellPos(CALCINER, Role.ASH_PAN, Direction.NORTH)), Direction.NORTH) == null, "no fluid port while unformed");
        helper.succeed();
    }

    /** The journal's schematic, anchored on the Ash Pan, accepts the structure that forming accepts. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void schematicMatchesTheBuild(GameTestHelper helper) {
        Multiblock schematic = ModonomiconAPI.get().getMultiblock(
                ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner"));
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

    static BlockPos spout(BlockPos controller) {
        return CalcinerStructure.pos(controller, CalcinerLayout.cells(Role.AIR).getFirst(), Facing.NORTH);
    }

    /** A block under the Alembic's spout keeps the Calciner from forming, and placed later it unforms it. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void blockUnderTheSpoutUnforms(GameTestHelper helper) {
        build(helper, CALCINER, Direction.NORTH);
        helper.setBlock(spout(CALCINER), Blocks.STONE);
        use(helper, player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), CALCINER);
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.setBlock(spout(CALCINER), Blocks.AIR);
        formed(helper, CALCINER, Direction.NORTH);
        helper.setBlock(spout(CALCINER), Blocks.STONE);
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.succeed();
    }

    /** Breaking a Fluid Tank of a column unforms the Calciner; the other tanks stay. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void breakingATankUnforms(GameTestHelper helper) {
        formed(helper, CALCINER, Direction.NORTH);
        BlockPos tank = CalcinerStructure.pos(CALCINER, CalcinerLayout.cell(16), Facing.NORTH);
        helper.getLevel().destroyBlock(helper.absolutePos(tank), true);
        assertFormed(helper, CALCINER, Direction.NORTH, false);
        helper.succeed();
    }

    /** A hopper under the Ash Pan takes the Salt as whole Ash; the remainder stays. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 100)
    public static void hopperTakesAshFromTheAshPan(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.tanks().insert(new Principles(350, 0, 0));
        BlockPos hopper = cellPos(CALCINER, Role.ASH_PAN, Direction.NORTH).below();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopperEntity = helper.getBlockEntity(hopper);
        helper.succeedWhen(() -> {
            helper.assertTrue(hopperEntity.countItem(ServusManufacturingItems.ASH.get()) == 3, "the hopper should hold 3 Ash, holds "
                    + hopperEntity.countItem(ServusManufacturingItems.ASH.get()));
            helper.assertTrue(calciner.tanks().amount(Principle.SALT) == 50, "50 mB Salt stay behind");
        });
    }

    /** An empty hand takes all Ash out of the Ash Pan; a bucket takes nothing there. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void handTakesAshFromTheAshPan(GameTestHelper helper) {
        CalcinerBlockEntity calciner = formed(helper, CALCINER, Direction.NORTH);
        calciner.tanks().insert(new Principles(500, 0, 0));
        BlockPos ashPan = cellPos(CALCINER, Role.ASH_PAN, Direction.NORTH);
        Player player = player(helper, new ItemStack(Items.BUCKET));
        use(helper, player, ashPan);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the bucket stays empty");
        helper.assertTrue(calciner.tanks().amount(Principle.SALT) == 500, "a bucket takes no Salt");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        use(helper, player, ashPan);
        helper.assertTrue(player.getInventory().countItem(ServusManufacturingItems.ASH.get()) == 5, "the player gets 5 Ash");
        helper.assertTrue(calciner.tanks().amount(Principle.SALT) == 0, "the Ash Pan is empty");
        helper.succeed();
    }
}
