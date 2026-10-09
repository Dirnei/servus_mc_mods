package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorStructure;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** The Coagulator as a build: forming, unforming, the Vats and the casting table's items. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoagulatorGameTests {

    static final BlockPos COAGULATOR = new BlockPos(5, 1, 5);

    private CoagulatorGameTests() {
    }

    /**
     * Builds the unformed structure for a front facing {@code facing}, the Coagulator placed facing
     * that way: the build is the same seen from both sides, so the Coagulator's own facing decides.
     */
    static void build(GameTestHelper helper, BlockPos controller, Direction facing) {
        for (Cell cell : CoagulatorLayout.cells()) {
            BlockState state = CoagulatorStructure.expected(cell.role()).defaultBlockState();
            if (cell.role() == Role.COAGULATOR) {
                state = state.setValue(MachineBlock.FACING, facing);
            }
            helper.setBlock(CoagulatorStructure.pos(controller, cell, Structures.facing(facing)), state);
        }
    }

    /** Builds and forms a Coagulator facing north. */
    static CoagulatorBlockEntity formed(GameTestHelper helper, BlockPos controller) {
        build(helper, controller, Direction.NORTH);
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), controller);
        helper.assertBlockProperty(controller, CoagulatorBlock.FORMED, true);
        return helper.getBlockEntity(controller);
    }

    static BlockPos vat(BlockPos controller, int piece) {
        return CoagulatorStructure.pos(controller, CoagulatorLayout.vat(piece), Facing.NORTH);
    }

    static IFluidHandler vatHandler(GameTestHelper helper, BlockPos controller, int piece) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(vat(controller, piece)), Direction.UP);
    }

    static boolean isFormed(GameTestHelper helper, BlockPos controller) {
        return helper.getBlockState(controller).getValue(CoagulatorBlock.FORMED);
    }

    /** The Journal forms the build in every facing. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void coagulatorFormsInEveryFacing(GameTestHelper helper) {
        BlockPos[] at = {new BlockPos(2, 1, 2), new BlockPos(2, 1, 8), new BlockPos(7, 1, 3), new BlockPos(9, 1, 8)};
        Direction[] facings = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < at.length; i++) {
            build(helper, at[i], facings[i]);
            CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), at[i]);
            helper.assertTrue(isFormed(helper, at[i]), "the Coagulator facing " + facings[i] + " should form");
            helper.assertBlockProperty(at[i], MachineBlock.FACING, facings[i]);
            for (int piece = 0; piece < 3; piece++) {
                helper.assertBlockProperty(CoagulatorStructure.pos(at[i], CoagulatorLayout.vat(piece), Structures.facing(facings[i])),
                        VatBlock.PIECE, piece);
            }
        }
        helper.succeed();
    }

    /** Without the Mercury Vat nothing forms, and the check names it. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void coagulatorWithoutAVatDoesNotForm(GameTestHelper helper) {
        build(helper, COAGULATOR, Direction.NORTH);
        helper.setBlock(vat(COAGULATOR, 2), Blocks.AIR);
        CalcinerGameTests.use(helper, CalcinerGameTests.player(helper, new ItemStack(ServusManufacturingItems.PARACELSUS_JOURNAL.get())), COAGULATOR);
        helper.assertTrue(!isFormed(helper, COAGULATOR), "nothing forms");
        helper.assertTrue(CoagulatorStructure.check(helper.getLevel(), helper.absolutePos(COAGULATOR)).firstWrong()
                .map(Cell::role).orElse(null) == Role.VAT, "the Vat is named");
        helper.succeed();
    }

    /** Breaking a Stone Bricks block unforms the build and stops the work. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void breakingAStoneUnformsTheCoagulator(GameTestHelper helper) {
        formed(helper, COAGULATOR);
        helper.getLevel().destroyBlock(helper.absolutePos(COAGULATOR.east()), true);
        helper.assertTrue(!isFormed(helper, COAGULATOR), "the Coagulator unforms");
        for (int piece = 0; piece < 3; piece++) {
            helper.assertBlockProperty(vat(COAGULATOR, piece), VatBlock.FORMED, false);
        }
        helper.succeed();
    }

    /** Each Vat takes only its own principle and gives nothing back. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void eachVatTakesItsPrinciple(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        Principle[] order = {Principle.SULFUR, Principle.SALT, Principle.MERCURY};
        for (int piece = 0; piece < 3; piece++) {
            IFluidHandler handler = vatHandler(helper, COAGULATOR, piece);
            for (Principle principle : Principle.values()) {
                int filled = handler.fill(new FluidStack(PrincipleFluids.fluid(principle), 100), IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(filled == (principle == order[piece] ? 100 : 0), "Vat " + piece + " and " + principle + " took " + filled);
            }
            helper.assertTrue(handler.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "a Vat gives nothing");
        }
        helper.assertTrue(coagulator.tanks().contents().equals(new Principles(100, 100, 100)), "each tank holds 100 mB");
        helper.succeed();
    }

    /** A bucket of another principle does nothing at a Vat. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void wrongBucketDoesNothing(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        Player player = CalcinerGameTests.player(helper, new ItemStack(PrincipleFluids.entry(Principle.MERCURY).bucket().get()));
        CalcinerGameTests.use(helper, player, vat(COAGULATOR, 0));
        helper.assertTrue(coagulator.tanks().contents().equals(new Principles(0, 0, 0)), "the Sulfur Vat takes no Mercury");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(PrincipleFluids.entry(Principle.MERCURY).bucket().get()),
                "the bucket stays full");
        helper.succeed();
    }

    /** The casting table takes the ingredient and a hopper under it takes the product. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 200)
    public static void itemsAtTheCastingTable(GameTestHelper helper) {
        CoagulatorBlockEntity coagulator = formed(helper, COAGULATOR);
        coagulator.select(AlchemyGameTests.coagulation("salt_crystal"));
        IItemHandler port = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(COAGULATOR), Direction.NORTH);
        helper.assertTrue(port != null && port.getSlots() == 2, "the Coagulator has its item port");
        helper.assertTrue(port.insertItem(1, new ItemStack(Items.STONE), false).getCount() == 1, "nothing goes into the output");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(vat(COAGULATOR, 1)), Direction.NORTH) == null,
                "a Vat takes no items");
        helper.setBlock(COAGULATOR.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(COAGULATOR.below());
        vatHandler(helper, COAGULATOR, 1).fill(new FluidStack(PrincipleFluids.fluid(Principle.SALT), 250), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> helper.assertTrue(hopper.countItem(ServusManufacturingItems.SALT_CRYSTAL.get()) == 1,
                "the hopper should take the Salt Crystal"));
    }

    /** An unformed Coagulator with Salt and a recipe makes nothing and offers no port. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 120)
    public static void unformedCoagulatorDoesNothing(GameTestHelper helper) {
        build(helper, COAGULATOR, Direction.NORTH);
        CoagulatorBlockEntity coagulator = helper.getBlockEntity(COAGULATOR);
        coagulator.select(AlchemyGameTests.coagulation("salt_crystal"));
        coagulator.tanks().insert(new Principles(250, 0, 0));
        helper.assertTrue(vatHandler(helper, COAGULATOR, 1) == null, "an unformed Vat offers nothing");
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(coagulator.inventory().getStackInSlot(CoagulatorBlockEntity.SLOT_OUTPUT).isEmpty(), "nothing is made");
            helper.succeed();
        });
    }

    /** The journal's schematic accepts the build that forming accepts. */
    @GameTest(template = AlchemyGameTests.EMPTY, timeoutTicks = 20)
    public static void coagulatorSchematicMatchesTheBuild(GameTestHelper helper) {
        Multiblock schematic = ModonomiconAPI.get().getMultiblock(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coagulator"));
        helper.assertTrue(schematic != null, "the Coagulator schematic should be loaded");
        build(helper, COAGULATOR, Direction.NORTH);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(COAGULATOR)) != null, "the schematic accepts the build");
        helper.setBlock(vat(COAGULATOR, 0), Blocks.AIR);
        helper.assertTrue(schematic.validate(helper.getLevel(), helper.absolutePos(COAGULATOR)) == null, "and refuses it without a Vat");
        helper.succeed();
    }
}
