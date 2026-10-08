package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.logistics.duct.Line;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.ChimneyBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.menu.SinkMenu;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.BurnerExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.ElectricExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.lab.ResearchLabBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.PumpBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.PumpMachine;
import io.github.dirnei.mc.servus.manufacturing.machine.smelter.BurnerSmelterBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** GameTests of Servus: Manufacturing. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ServusManufacturingGameTests {

    /** The holder's namespace is prefixed by NeoForge; only the path goes here. */
    private static final String EMPTY = "empty_12x6x12";
    private static final int RUN_TICKS = 1200;
    private static final int MIN_PACKS = 7;
    private static final int MAX_PACKS = 13;

    private ServusManufacturingGameTests() {
    }

    /**
     * The reference balanced block from PLAN.md: 2 iron pairs, 1 copper pair,
     * 1 coal extractor, 3 assemblers, 1 lab. Expect one red pack per 5 s once
     * the pipeline is filled.
     */
    @GameTest(template = EMPTY, timeoutTicks = RUN_TICKS + 100)
    public static void referenceBlockProducesOnePackPerFiveSeconds(GameTestHelper helper) {
        // Deposits (y 1) and extractors (y 2) at x 1; smelters at x 2.
        burnerPair(helper, 1, ServusManufacturingBlocks.IRON_DEPOSIT.get(), true);
        burnerPair(helper, 3, ServusManufacturingBlocks.IRON_DEPOSIT.get(), true);
        burnerPair(helper, 5, ServusManufacturingBlocks.COPPER_DEPOSIT.get(), true);
        burnerPair(helper, 7, ServusManufacturingBlocks.COAL_DEPOSIT.get(), false);

        // Duct column at x 3 from z 1 to z 7: Utility Ducts with an Item Pipe on line 1.
        for (int z = 1; z <= 7; z++) {
            duct(helper, new BlockPos(3, 2, z), Line.ITEM);
        }

        // Assemblers at x 4 with their recipes, generators at x 5.
        assembler(helper, 1, "iron_gear");
        assembler(helper, 3, "copper_wire");
        assembler(helper, 5, "red_pack");

        // Lab at x 4 z 7 collecting packs, generator beside it.
        BlockPos labPos = new BlockPos(4, 2, 7);
        helper.setBlock(labPos, facing(ServusManufacturingBlocks.RESEARCH_LAB.get(), Direction.EAST));
        generator(helper, new BlockPos(5, 2, 7));

        helper.runAtTickTime(RUN_TICKS, () -> {
            ResearchLabBlockEntity lab = helper.getBlockEntity(labPos);
            int packs = 0;
            for (int slot = 0; slot < lab.inventory().getSlots(); slot++) {
                ItemStack stack = lab.inventory().getStackInSlot(slot);
                if (stack.is(ServusManufacturingItems.RED_PACK.get())) {
                    packs += stack.getCount();
                }
            }
            if (packs < MIN_PACKS || packs > MAX_PACKS) {
                helper.fail("Expected " + MIN_PACKS + " to " + MAX_PACKS + " red packs after " + RUN_TICKS
                        + " ticks, got " + packs, labPos);
            }
            helper.succeed();
        });
    }

    private static BlockState facing(Block block, Direction direction) {
        return block.defaultBlockState().setValue(MachineBlock.FACING, direction);
    }

    private static void fuel(GameTestHelper helper, BlockPos pos) {
        MachineBlockEntity machine = helper.getBlockEntity(pos);
        machine.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 64));
    }

    /** Deposit, extractor on top and, when wanted, a smelter east of it. */
    private static void burnerPair(GameTestHelper helper, int z, Block deposit, boolean withSmelter) {
        helper.setBlock(new BlockPos(1, 1, z), deposit.defaultBlockState());
        BlockPos extractor = new BlockPos(1, 2, z);
        helper.setBlock(extractor, facing(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), Direction.NORTH));
        fuel(helper, extractor);
        if (withSmelter) {
            BlockPos smelter = new BlockPos(2, 2, z);
            helper.setBlock(smelter, facing(ServusManufacturingBlocks.BURNER_SMELTER.get(), Direction.NORTH));
            fuel(helper, smelter);
        } else {
            // The coal extractor feeds the duct column directly.
            duct(helper, new BlockPos(2, 2, z), Line.ITEM);
        }
    }

    /**
     * Places a Utility Duct carrying the given lines with default modes and
     * line 1. {@code setBlock} skips placement logic, so the connection
     * flags are refreshed from the neighbours afterwards.
     */
    private static void duct(GameTestHelper helper, BlockPos pos, Line... lines) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
        for (Line line : lines) {
            duct.addLine(line);
        }
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
    }

    private static void assembler(GameTestHelper helper, int z, String recipe) {
        BlockPos pos = new BlockPos(4, 2, z);
        // The front is closed to items, so the assemblers face the generators and take the duct at their back.
        helper.setBlock(pos, facing(ServusManufacturingBlocks.ASSEMBLER.get(), Direction.EAST));
        AssemblerBlockEntity assembler = helper.getBlockEntity(pos);
        assembler.addRecipe(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "assembling/" + recipe), null);
        generator(helper, new BlockPos(5, 2, z));
    }

    private static void generator(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, facing(ServusManufacturingBlocks.COAL_GENERATOR.get(), Direction.NORTH));
        fuel(helper, pos);
    }

    /**
     * Two generators and two machines along one Power Cable run: generator A
     * and machine A on channel 1, generator B (no fuel) and machine B on
     * channel 2. Only machine A gets energy, so channels keep energy apart.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void energyStaysOnItsChannel(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            duct(helper, new BlockPos(7, 2, z), Line.ENERGY);
        }
        BlockPos generatorA = new BlockPos(6, 2, 1);
        BlockPos machineA = new BlockPos(8, 2, 1);
        BlockPos generatorB = new BlockPos(6, 2, 3);
        BlockPos machineB = new BlockPos(8, 2, 3);
        generator(helper, generatorA);
        helper.setBlock(generatorB, facing(ServusManufacturingBlocks.COAL_GENERATOR.get(), Direction.NORTH));
        helper.setBlock(machineA, facing(ServusManufacturingBlocks.ASSEMBLER.get(), Direction.NORTH));
        helper.setBlock(machineB, facing(ServusManufacturingBlocks.ASSEMBLER.get(), Direction.NORTH));
        UtilityDuctBlockEntity ductB = helper.getBlockEntity(new BlockPos(7, 2, 3));
        ductB.setChannel(Line.ENERGY, Direction.WEST, 1);
        ductB.setChannel(Line.ENERGY, Direction.EAST, 1);

        helper.runAfterDelay(60, () -> {
            PoweredMachineBlockEntity a = helper.getBlockEntity(machineA);
            PoweredMachineBlockEntity b = helper.getBlockEntity(machineB);
            helper.assertTrue(a.energyStorage().getEnergyStored() > 0, "machine A on channel 1 should receive energy");
            helper.assertTrue(b.energyStorage().getEnergyStored() == 0,
                    "machine B on channel 2 must stay empty, got " + b.energyStorage().getEnergyStored());
            helper.succeed();
        });
    }

    private static final BlockPos PR_ASSEMBLER = new BlockPos(9, 2, 10);

    /** The assembler reports its configured recipes through the Lib production capability. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void assemblerReportsItsRecipes(GameTestHelper helper) {
        helper.setBlock(PR_ASSEMBLER, facing(ServusManufacturingBlocks.ASSEMBLER.get(), Direction.WEST));
        AssemblerBlockEntity assembler = helper.getBlockEntity(PR_ASSEMBLER);
        BlockPos absolute = helper.absolutePos(PR_ASSEMBLER);
        ProductionProvider empty = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, absolute, null);
        helper.assertTrue(empty != null, "the assembler should expose the production capability");
        helper.assertTrue(empty.recipes().isEmpty() && empty.active().isEmpty(), "nothing configured means no recipes");

        assembler.addRecipe(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "assembling/iron_gear"), null);
        ProductionProvider provider = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, absolute, Direction.UP);
        helper.assertTrue(provider != null && provider.recipes().size() == 1, "one recipe should be reported");
        ProductionRecipe gear = provider.recipes().get(0);
        helper.assertTrue(gear.inputs().size() == 1 && gear.inputs().get(0).is(Items.IRON_INGOT) && gear.inputs().get(0).getCount() == 2,
                "the gear recipe takes two iron ingots, got " + gear.inputs());
        helper.assertTrue(gear.outputs().size() == 1 && gear.outputs().get(0).is(ServusManufacturingItems.IRON_GEAR.get()) && gear.outputs().get(0).getCount() == 1,
                "the gear recipe makes one iron gear, got " + gear.outputs());
        helper.assertTrue(gear.ticks() == 20, "the gear recipe takes 20 ticks, got " + gear.ticks());
        helper.assertTrue(provider.active().isEmpty(), "idle: no active recipe");

        assembler.removeRecipe(0);
        helper.assertTrue(provider.recipes().isEmpty(), "no recipes after removal");
        helper.succeed();
    }

    private static final BlockPos SP_SMELTER = new BlockPos(3, 2, 9);

    /** A Pull side takes what the machine accepts from the neighbour and leaves the rest. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void machinePullsFromNeighbour(GameTestHelper helper) {
        helper.setBlock(SP_SMELTER, facing(ServusManufacturingBlocks.BURNER_SMELTER.get(), Direction.NORTH));
        BlockPos chestPos = SP_SMELTER.west();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        chest.setItem(1, new ItemStack(Items.DIAMOND, 3));
        MachineBlockEntity smelter = helper.getBlockEntity(SP_SMELTER);
        helper.assertTrue(smelter.relativeSide(Direction.WEST) == RelativeSide.RIGHT, "west is the right side of a machine facing north, as the player sees it");
        smelter.sideConfig().set(RelativeSide.RIGHT, SideMode.PULL);
        helper.runAfterDelay(30, () -> {
            ItemStack input = smelter.inventory().getStackInSlot(BurnerSmelterBlockEntity.SLOT_INPUT);
            helper.assertTrue(input.is(Items.RAW_IRON) && input.getCount() == 4, "the raw iron should have been pulled in, input holds " + input);
            helper.assertTrue(chest.countItem(Items.RAW_IRON) == 0 && chest.countItem(Items.DIAMOND) == 3, "the diamonds stay in the chest");
            helper.succeed();
        });
    }

    /** An Output side lets neighbours take but pushes nothing; a Push side pushes. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void outputSideDoesNotPush(GameTestHelper helper) {
        helper.setBlock(SP_SMELTER, facing(ServusManufacturingBlocks.BURNER_SMELTER.get(), Direction.NORTH));
        BlockPos chestPos = SP_SMELTER.east();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(chestPos);
        MachineBlockEntity smelter = helper.getBlockEntity(SP_SMELTER);
        smelter.inventory().setStackInSlot(BurnerSmelterBlockEntity.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 5));
        smelter.sideConfig().set(RelativeSide.LEFT, SideMode.OUTPUT);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(chest.countItem(Items.IRON_INGOT) == 0, "an Output side must not push");
            helper.assertTrue(smelter.cycleSide(RelativeSide.LEFT) == SideMode.BOTH, "Output cycles to Both");
            smelter.sideConfig().set(RelativeSide.LEFT, SideMode.PUSH);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(chest.countItem(Items.IRON_INGOT) == 5, "a Push side pushes, chest has " + chest.countItem(Items.IRON_INGOT));
                helper.succeed();
            });
        });
    }

    private static final BlockPos FC_SMELTER = new BlockPos(6, 2, 9);

    /** The front of a machine takes nothing, gives nothing and cannot be set to anything else. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void machineFrontIsClosed(GameTestHelper helper) {
        helper.setBlock(FC_SMELTER, facing(ServusManufacturingBlocks.BURNER_SMELTER.get(), Direction.NORTH));
        MachineBlockEntity smelter = helper.getBlockEntity(FC_SMELTER);
        helper.assertTrue(smelter.sideMode(Direction.NORTH) == SideMode.DISABLED, "the front starts closed");
        helper.assertTrue(smelter.cycleFace(Direction.NORTH) == SideMode.DISABLED, "the front cannot be cycled");
        smelter.sideConfig().set(RelativeSide.FRONT, SideMode.INPUT);
        helper.assertTrue(smelter.sideMode(Direction.NORTH) == SideMode.DISABLED, "the front cannot be set");
        net.neoforged.neoforge.items.IItemHandler front = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(FC_SMELTER), Direction.NORTH);
        helper.assertTrue(front == null || front.insertItem(0, new ItemStack(Items.COAL, 4), false).getCount() == 4, "nothing goes in at the front");
        smelter.inventory().setStackInSlot(BurnerSmelterBlockEntity.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 2));
        helper.assertTrue(front == null || front.extractItem(BurnerSmelterBlockEntity.SLOT_OUTPUT, 2, false).isEmpty(), "nothing comes out at the front");
        helper.succeed();
    }

    private static final BlockPos FR_DEPOSIT = new BlockPos(4, 1, 4);
    private static final BlockPos FR_EXTRACTOR = new BlockPos(4, 2, 4);

    /** Deposit, extractor on top lit with a single coal. */
    private static MachineBlockEntity litExtractor(GameTestHelper helper, Block deposit) {
        helper.setBlock(FR_DEPOSIT, deposit.defaultBlockState());
        helper.setBlock(FR_EXTRACTOR, facing(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), Direction.NORTH));
        MachineBlockEntity extractor = helper.getBlockEntity(FR_EXTRACTOR);
        extractor.inventory().setStackInSlot(BurnerExtractorBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 1));
        return extractor;
    }

    /**
     * On coal the extractor first fills its fuel slot to the reserve target of
     * four (one coal per 80 ticks, the lit coal is consumed at once), then the
     * surplus goes to the output slot from tick 400 on.
     */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void coalExtractorKeepsFuelReserve(GameTestHelper helper) {
        MachineBlockEntity extractor = litExtractor(helper, ServusManufacturingBlocks.COAL_DEPOSIT.get());
        helper.runAtTickTime(500, () -> {
            ItemStack fuel = extractor.inventory().getStackInSlot(BurnerExtractorBlockEntity.SLOT_FUEL);
            ItemStack output = extractor.inventory().getStackInSlot(BurnerExtractorBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(fuel.is(Items.COAL) && fuel.getCount() == 4,
                    "expected 4 coal in the fuel slot, got " + fuel);
            helper.assertTrue(output.is(Items.COAL) && output.getCount() >= 1,
                    "expected surplus coal in the output slot, got " + output);
            helper.succeed();
        });
    }

    /** Raw iron is not a fuel, so it never enters the fuel slot however low the reserve is. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void ironExtractorNeverReservesOre(GameTestHelper helper) {
        MachineBlockEntity extractor = litExtractor(helper, ServusManufacturingBlocks.IRON_DEPOSIT.get());
        helper.runAtTickTime(200, () -> {
            ItemStack fuel = extractor.inventory().getStackInSlot(BurnerExtractorBlockEntity.SLOT_FUEL);
            ItemStack output = extractor.inventory().getStackInSlot(BurnerExtractorBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(!fuel.is(Items.RAW_IRON), "raw iron must not enter the fuel slot, got " + fuel);
            helper.assertTrue(output.is(Items.RAW_IRON) && output.getCount() >= 1,
                    "expected raw iron in the output slot, got " + output);
            helper.succeed();
        });
    }

    private static final BlockPos BC_BURNER = new BlockPos(4, 2, 4);
    private static final BlockPos BC_ELECTRIC = new BlockPos(7, 2, 4);

    /** Asserts that an extractor's bottom is fixed Off and that nothing goes in or comes out there. */
    private static void assertBottomClosed(GameTestHelper helper, BlockPos pos, int outputSlot) {
        MachineBlockEntity extractor = helper.getBlockEntity(pos);
        helper.assertTrue(extractor.sideMode(Direction.DOWN) == SideMode.DISABLED, "the bottom starts Off");
        helper.assertTrue(extractor.cycleFace(Direction.DOWN) == SideMode.DISABLED, "the bottom cannot be cycled");
        extractor.sideConfig().set(RelativeSide.BOTTOM, SideMode.OUTPUT);
        helper.assertTrue(extractor.sideMode(Direction.DOWN) == SideMode.DISABLED, "the bottom cannot be set");
        extractor.inventory().setStackInSlot(outputSlot, new ItemStack(Items.RAW_IRON, 4));
        IItemHandler bottom = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.DOWN);
        helper.assertTrue(bottom == null || bottom.extractItem(outputSlot, 4, false).isEmpty(), "nothing comes out at the bottom");
        helper.assertTrue(bottom == null || bottom.insertItem(0, new ItemStack(Items.COAL, 4), false).getCount() == 4, "nothing goes in at the bottom");
    }

    /** Both extractors stand on their deposit, so the bottom is fixed Off; the Electric Extractor still takes energy there. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void extractorBottomIsClosed(GameTestHelper helper) {
        helper.setBlock(BC_BURNER.below(), ServusManufacturingBlocks.IRON_DEPOSIT.get().defaultBlockState());
        helper.setBlock(BC_BURNER, facing(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), Direction.NORTH));
        helper.setBlock(BC_ELECTRIC.below(), ServusManufacturingBlocks.IRON_DEPOSIT.get().defaultBlockState());
        helper.setBlock(BC_ELECTRIC, facing(ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get(), Direction.NORTH));
        assertBottomClosed(helper, BC_BURNER, BurnerExtractorBlockEntity.SLOT_OUTPUT);
        assertBottomClosed(helper, BC_ELECTRIC, ElectricExtractorBlockEntity.SLOT_OUTPUT);
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(BC_ELECTRIC), Direction.DOWN);
        helper.assertTrue(energy != null && energy.receiveEnergy(100, true) > 0, "the Electric Extractor takes energy at the bottom");
        helper.succeed();
    }
    // Pumps. The Fluid Tank is built in a parallel change, so an empty vanilla cauldron (NeoForge's
    // CauldronWrapper) is the fluid sink, and the pump buffer is read through its capability.

    private static final BlockPos PUMP = new BlockPos(3, 2, 3);

    /** Stone at y 1 around and under the pump with up to four water sources in a 2 by 2 pool below it. */
    private static void pool(GameTestHelper helper, int sources) {
        for (int x = 2; x <= 5; x++) {
            for (int z = 2; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
            }
        }
        for (int i = 0; i < sources; i++) {
            helper.setBlock(poolBlocks()[i], Blocks.WATER.defaultBlockState());
        }
    }

    private static BlockPos[] poolBlocks() {
        BlockPos below = PUMP.below();
        return new BlockPos[] {below, below.east(), below.south(), below.south().east()};
    }

    private static boolean poolIntact(GameTestHelper helper) {
        for (BlockPos pos : poolBlocks()) {
            FluidState fluid = helper.getLevel().getFluidState(helper.absolutePos(pos));
            if (!fluid.isSource() || !fluid.is(FluidTags.WATER)) {
                return false;
            }
        }
        return true;
    }

    private static IFluidHandler pumpFluid(GameTestHelper helper, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(PUMP), side);
    }

    private static int buffered(GameTestHelper helper) {
        PumpMachine pump = helper.getBlockEntity(PUMP);
        return pump.pump().tank().getFluidAmount();
    }

    private static void burnerPump(GameTestHelper helper, int coal) {
        helper.setBlock(PUMP, facing(ServusManufacturingBlocks.BURNER_PUMP.get(), Direction.NORTH));
        MachineBlockEntity pump = helper.getBlockEntity(PUMP);
        pump.inventory().setStackInSlot(BurnerPumpBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, coal));
    }

    private static boolean cauldronFull(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3;
    }

    /** A Burner Pump on a pond fills the cauldron beside it through an Auto side, and the pond stays. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void burnerPumpFillsAdjacentCauldron(GameTestHelper helper) {
        pool(helper, 4);
        burnerPump(helper, 4);
        // Facing north, west is the pump's right side, Auto by default.
        BlockPos cauldron = PUMP.west();
        helper.setBlock(cauldron, Blocks.CAULDRON.defaultBlockState());
        helper.succeedWhen(() -> {
            helper.assertTrue(cauldronFull(helper, cauldron), "the cauldron should fill with water");
            IFluidHandler top = pumpFluid(helper, Direction.UP);
            helper.assertTrue(top != null && top.getFluidInTank(0).is(Fluids.WATER) && top.getFluidInTank(0).getAmount() > 0,
                    "the pump buffer should hold water again after filling the cauldron");
            helper.assertTrue(poolIntact(helper), "the pond must stay unchanged");
        });
    }

    /** A pump on a lone source block does nothing and keeps its fuel. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pumpOnLoneSourceBurnsNoFuel(GameTestHelper helper) {
        pool(helper, 1);
        burnerPump(helper, 1);
        helper.runAfterDelay(60, () -> {
            BurnerPumpBlockEntity pump = helper.getBlockEntity(PUMP);
            helper.assertTrue(pump.inventory().getStackInSlot(BurnerPumpBlockEntity.SLOT_FUEL).getCount() == 1, "the coal must stay");
            helper.assertTrue(!pump.isBurning(), "nothing burns");
            helper.assertTrue(buffered(helper) == 0, "no water from a lone source, got " + buffered(helper));
            helper.assertTrue(!pump.pump().sourceValid(), "a lone source is not infinite");
            helper.succeed();
        });
    }

    /** Removing the neighbouring sources stops the pump at the start of its next cycle. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pumpStopsWhenSourceIsDrained(GameTestHelper helper) {
        pool(helper, 4);
        burnerPump(helper, 4);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(buffered(helper) > 0, "the pump should have pumped on the pond");
            BlockPos[] pool = poolBlocks();
            for (int i = 1; i < pool.length; i++) {
                helper.setBlock(pool[i], Blocks.STONE.defaultBlockState());
            }
            int atRemoval = buffered(helper);
            helper.runAfterDelay(40, () -> {
                int afterNextCycle = buffered(helper);
                helper.assertTrue(afterNextCycle <= atRemoval + 250,
                        "at most the running cycle finishes, " + atRemoval + " then " + afterNextCycle);
                helper.runAfterDelay(40, () -> {
                    helper.assertTrue(buffered(helper) == afterNextCycle, "the pump must have stopped, got " + buffered(helper));
                    helper.succeed();
                });
            });
        });
    }

    /** A pump has no side settings: every side is fixed, the carrot changes nothing, fuel goes in by automation. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void pumpSidesAreFixed(GameTestHelper helper) {
        burnerPump(helper, 0);
        MachineBlockEntity pump = helper.getBlockEntity(PUMP);
        for (RelativeSide side : RelativeSide.values()) {
            helper.assertTrue(pump.sideConfig().isLocked(side), "side " + side + " should be fixed");
        }
        helper.assertTrue(pump.cycleFace(Direction.WEST) == SideMode.AUTO, "a side stays Auto when cycled");
        IItemHandler west = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(PUMP), Direction.WEST);
        helper.assertTrue(west != null, "a side takes fuel by automation");
        ItemStack left = ItemHandlerHelper.insertItem(west, new ItemStack(Items.COAL, 3), false);
        helper.assertTrue(left.isEmpty() && pump.inventory().getStackInSlot(BurnerPumpBlockEntity.SLOT_FUEL).getCount() == 3,
                "the coal should go into the fuel slot");
        helper.succeed();
    }

    private static int stub(GameTestHelper helper, IntegerProperty face) {
        return helper.getBlockState(PUMP).getValue(face);
    }

    /** Stubs show toward ducts and fluid blocks on every face but the front and the bottom, sized like the arm they meet. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void pumpStubsFollowNeighbours(GameTestHelper helper) {
        burnerPump(helper, 0);
        helper.setBlock(PUMP.east(), Blocks.CAULDRON.defaultBlockState());
        helper.setBlock(PUMP.north(), Blocks.CAULDRON.defaultBlockState());
        helper.setBlock(PUMP.west(), Blocks.STONE.defaultBlockState());
        duct(helper, PUMP.south(), Line.ITEM);
        helper.assertTrue(stub(helper, PumpBlock.EAST) == PumpBlock.PLAIN_STUB, "a cauldron to the east gets a medium stub");
        helper.assertTrue(stub(helper, PumpBlock.NORTH) == 0, "the front never gets a stub");
        helper.assertTrue(stub(helper, PumpBlock.WEST) == 0, "stone gets no stub");
        helper.assertTrue(stub(helper, PumpBlock.SOUTH) == 1, "an Item Pipe that brings fuel gets a thin stub");
        UtilityDuctBlockEntity duct = helper.getBlockEntity(PUMP.south());
        duct.addLine(Line.FLUID);
        helper.assertTrue(stub(helper, PumpBlock.SOUTH) == 2, "a second line grows the stub");
        duct.addLine(Line.ENERGY);
        helper.assertTrue(stub(helper, PumpBlock.SOUTH) == 2, "a Burner Pump takes no power, so the stub stays at two lines");
        helper.setBlock(PUMP.east(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(stub(helper, PumpBlock.EAST) == 0, "removing the cauldron removes the stub");
        duct(helper, PUMP.above(), Line.FLUID);
        helper.assertTrue(stub(helper, PumpBlock.UP) == 1, "a Fluid Pipe on top reaches the outlet");
        helper.succeed();
    }

    /** A Power Cable meets an Electric Pump with a stub on its sides, but no cable reaches its front. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void electricPumpStubForPowerCable(GameTestHelper helper) {
        helper.setBlock(PUMP, facing(ServusManufacturingBlocks.ELECTRIC_PUMP.get(), Direction.NORTH));
        duct(helper, PUMP.east(), Line.ENERGY);
        duct(helper, PUMP.north(), Line.ENERGY);
        helper.assertTrue(stub(helper, PumpBlock.EAST) == 1, "a Power Cable on the side gets a thin stub");
        helper.assertTrue(stub(helper, PumpBlock.NORTH) == 0, "the front takes no power and gets no stub");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(PUMP), Direction.NORTH) == null,
                "no energy through the front");
        helper.succeed();
    }

    /** A pump never accepts fluid, and its front and bottom are fixed Off. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void pumpRefusesFluidAndClosesFrontAndBottom(GameTestHelper helper) {
        burnerPump(helper, 0);
        IFluidHandler top = pumpFluid(helper, Direction.UP);
        helper.assertTrue(top != null, "the top is Auto and offers the buffer");
        helper.assertTrue(top.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "a pump accepts nothing");
        helper.assertTrue(buffered(helper) == 0, "the buffer stays empty");
        MachineBlockEntity pump = helper.getBlockEntity(PUMP);
        helper.assertTrue(pump.sideMode(Direction.DOWN) == SideMode.DISABLED, "the bottom starts Off");
        helper.assertTrue(pump.cycleFace(Direction.DOWN) == SideMode.DISABLED, "the bottom cannot be cycled");
        helper.assertTrue(pumpFluid(helper, Direction.DOWN) == null, "no water through the bottom");
        helper.assertTrue(pumpFluid(helper, Direction.NORTH) == null, "no water through the front");
        helper.succeed();
    }

    /** An Electric Pump idles without energy and runs once energy arrives. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void electricPumpIdlesWithoutEnergy(GameTestHelper helper) {
        pool(helper, 4);
        helper.setBlock(PUMP, facing(ServusManufacturingBlocks.ELECTRIC_PUMP.get(), Direction.NORTH));
        helper.runAfterDelay(60, () -> {
            PoweredMachineBlockEntity pump = helper.getBlockEntity(PUMP);
            helper.assertTrue(buffered(helper) == 0, "no water without energy");
            pump.energyStorage().receiveEnergy(8000, false);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(buffered(helper) >= 500, "energy should start the pump, got " + buffered(helper));
                helper.succeed();
            });
        });
    }

    /** A powered pump fills its buffer, then pauses without spending energy, and the pond stays. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void electricPumpFillsBufferThenPauses(GameTestHelper helper) {
        pool(helper, 4);
        helper.setBlock(PUMP, facing(ServusManufacturingBlocks.ELECTRIC_PUMP.get(), Direction.NORTH));
        PoweredMachineBlockEntity pump = helper.getBlockEntity(PUMP);
        pump.energyStorage().receiveEnergy(8000, false);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(buffered(helper) == 4000, "the buffer should be full, got " + buffered(helper));
            int energy = pump.energyStorage().getEnergyStored();
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(pump.energyStorage().getEnergyStored() == energy, "a full pump spends no energy");
                helper.assertTrue(poolIntact(helper), "the pond must stay unchanged");
                helper.succeed();
            });
        });
    }

    // Sink: a 2x2x2 cube of Sink blocks formed with the carrot. The standard cube spans
    // x 2..3, y 0..1, z 2..3 and faces north: its controller is the front lower east block
    // (east is the left of a player looking at the front), west is right and south is back.

    private static final BlockPos SINK = new BlockPos(3, 0, 2);

    /** Places eight unformed Sink blocks in the box from {@code min} to {@code min + (1, 1, 1)}, except {@code skip}. */
    private static void sinkBox(GameTestHelper helper, BlockPos min, BlockPos skip) {
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(1, 1, 1))) {
            if (!pos.equals(skip)) {
                helper.setBlock(pos, ServusManufacturingBlocks.SINK.get().defaultBlockState());
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
            helper.setBlock(column.above(i), ServusManufacturingBlocks.CHIMNEY.get().defaultBlockState());
        }
        return helper.getBlockEntity(SINK);
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkSevenBlocksDoNotForm(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 0, 2);
        sinkBox(helper, min, new BlockPos(2, 1, 3));
        carrot(helper, SINK, Direction.NORTH, false);
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(1, 1, 1))) {
            helper.assertTrue(!formed(helper, pos), "nothing should form from seven blocks");
        }
        helper.setBlock(new BlockPos(2, 1, 3), ServusManufacturingBlocks.SINK.get().defaultBlockState());
        carrot(helper, new BlockPos(3, 1, 2), Direction.UP, false);
        helper.assertTrue(!formed(helper, SINK), "the top face is no front");
        carrot(helper, new BlockPos(3, 1, 2), Direction.NORTH, false);
        assertFormedCube(helper, min, Direction.NORTH, SINK);
        helper.succeed();
    }

    /** Casings next to a formed Sink never take its blocks; they form once their own cube is complete. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkFormingIgnoresFormedNeighbour(GameTestHelper helper) {
        sink(helper, 0);
        // Four casings east of the formed cube: only the cube with the formed blocks would be complete.
        BlockPos column = new BlockPos(4, 0, 2);
        for (BlockPos pos : BlockPos.betweenClosed(column, column.offset(0, 1, 1))) {
            helper.setBlock(pos, ServusManufacturingBlocks.SINK.get().defaultBlockState());
        }
        carrot(helper, column, Direction.NORTH, false);
        helper.assertTrue(!formed(helper, column), "the casings must not form with blocks of a formed Sink");
        assertFormedCube(helper, new BlockPos(2, 0, 2), Direction.NORTH, SINK);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(5, 0, 2), new BlockPos(5, 1, 3))) {
            helper.setBlock(pos, ServusManufacturingBlocks.SINK.get().defaultBlockState());
        }
        carrot(helper, column, Direction.NORTH, false);
        assertFormedCube(helper, new BlockPos(4, 0, 2), Direction.NORTH, new BlockPos(5, 0, 2));
        assertFormedCube(helper, new BlockPos(2, 0, 2), Direction.NORTH, SINK);
        helper.succeed();
    }

    /** Breaking one block unforms the other seven and drops one plain Sink block; repairing and the carrot form it again. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkBreakingUnformsAndReforms(GameTestHelper helper) {
        sink(helper, 0);
        BlockPos broken = new BlockPos(2, 1, 3);
        // With drops, like a player breaking it.
        helper.getLevel().destroyBlock(helper.absolutePos(broken), true);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 0, 2), new BlockPos(3, 1, 3))) {
            if (!pos.equals(broken)) {
                BlockState state = helper.getBlockState(pos);
                helper.assertTrue(state.is(ServusManufacturingBlocks.SINK.get()) && !state.getValue(SinkBlock.FORMED),
                        "the rest should be unformed casings, not " + state);
            }
        }
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(broken)).inflate(2));
        int dropped = drops.stream().filter(entity -> entity.getItem().is(ServusManufacturingItems.SINK.get()))
                .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(dropped == 1, "one Sink block should drop, dropped " + dropped);
        drops.forEach(ItemEntity::discard);
        helper.setBlock(broken, ServusManufacturingBlocks.SINK.get().defaultBlockState());
        carrot(helper, new BlockPos(2, 0, 2), Direction.NORTH, false);
        assertFormedCube(helper, new BlockPos(2, 0, 2), Direction.NORTH, SINK);
        helper.succeed();
    }

    /** The sneak carrot dismantles one block of a formed Sink into a plain Sink item and unforms the rest. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkSneakCarrotDismantlesOneBlock(GameTestHelper helper) {
        sink(helper, 0);
        carrot(helper, SINK, Direction.NORTH, true);
        helper.assertBlockNotPresent(ServusManufacturingBlocks.SINK.get(), SINK);
        helper.assertTrue(!formed(helper, new BlockPos(2, 0, 2)), "the rest should be unformed");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(SINK)).inflate(2));
        helper.assertTrue(drops.stream().anyMatch(entity -> entity.getItem().is(ServusManufacturingItems.SINK.get())),
                "the block should drop as a Sink item");
        helper.succeed();
    }

    private static IItemHandler sinkItems(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side);
    }

    private static IFluidHandler sinkFluid(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), side);
    }

    private static IEnergyStorage sinkEnergy(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), side);
    }

    /** Hoppers pointing into the side of the controller and into the back of another part empty; the items appear nowhere. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void hopperEmptiesIntoSink(GameTestHelper helper) {
        sink(helper, 5);
        BlockPos east = new BlockPos(4, 0, 2);
        helper.setBlock(east, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        HopperBlockEntity intoController = helper.getBlockEntity(east);
        intoController.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        BlockPos back = new BlockPos(2, 0, 4);
        helper.setBlock(back, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        HopperBlockEntity intoBack = helper.getBlockEntity(back);
        intoBack.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        helper.succeedWhen(() -> {
            helper.assertTrue(intoController.isEmpty(), "the hopper at the side should empty into the Sink");
            helper.assertTrue(intoBack.isEmpty(), "the hopper at the back should empty into the Sink");
            helper.assertItemEntityNotPresent(Items.COBBLESTONE);
        });
    }

    /** A hopper pointing into a front face keeps its items: the front offers no capability. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void sinkFrontRefuses(GameTestHelper helper) {
        sink(helper, 5);
        for (BlockPos front : List.of(new BlockPos(2, 0, 2), SINK, new BlockPos(2, 1, 2), new BlockPos(3, 1, 2))) {
            helper.assertTrue(sinkItems(helper, front, Direction.NORTH) == null, "no items through the front");
            helper.assertTrue(sinkFluid(helper, front, Direction.NORTH) == null, "no fluid through the front");
            helper.assertTrue(sinkEnergy(helper, front, Direction.NORTH) == null, "no energy through the front");
        }
        BlockPos hopperPos = new BlockPos(2, 0, 1);
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.SOUTH));
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 5, "the hopper at the front should keep its items");
            helper.succeed();
        });
    }

    /** Without a chimney the item rate of 4 per second takes 4 of a 64 stack; all faces share the budget. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkBaseRateTakesFourItems(GameTestHelper helper) {
        sink(helper, 0);
        IItemHandler top = sinkItems(helper, new BlockPos(3, 1, 2), Direction.UP);
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkWithFiveChimneysIsUnlimited(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 5);
        helper.assertTrue(sink.chimneyHeight() == 5, "five Chimney blocks, height " + sink.chimneyHeight());
        IEnergyStorage energy = sinkEnergy(helper, new BlockPos(3, 0, 3), Direction.SOUTH);
        helper.assertTrue(energy.receiveEnergy(10_000, false) == 10_000, "all energy should be accepted");
        helper.assertTrue(energy.receiveEnergy(10_000, false) == 10_000, "again in the same tick");
        helper.assertTrue(energy.getEnergyStored() == 0, "the Sink reports no stored energy");
        IFluidHandler fluid = sinkFluid(helper, new BlockPos(3, 1, 2), Direction.UP);
        helper.assertTrue(fluid.fill(new FluidStack(Fluids.WATER, 1_000_000), IFluidHandler.FluidAction.EXECUTE) == 1_000_000,
                "all water should be accepted");
        helper.assertTrue(fluid.getFluidInTank(0).isEmpty(), "the Sink reports no fluid");
        ItemStack left = sinkItems(helper, SINK, Direction.EAST).insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(left.isEmpty(), "the full stack should be accepted, left " + left);
        helper.succeed();
    }

    /** Below the unlimited height energy and fluid follow the rate of the height, shared by all blocks. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkRateGrowsWithChimney(GameTestHelper helper) {
        sink(helper, 2);
        // Height 2: 400 FE and 200 mB per tick, one second of carry over in an idle Sink.
        helper.assertTrue(sinkEnergy(helper, new BlockPos(2, 0, 3), Direction.SOUTH).receiveEnergy(100_000, false) == 8_000,
                "one second of 400 FE per tick");
        helper.assertTrue(sinkEnergy(helper, SINK, Direction.EAST).receiveEnergy(100_000, false) == 0,
                "another block of the same Sink gets nothing more this tick");
        helper.assertTrue(sinkFluid(helper, new BlockPos(2, 1, 3), Direction.UP)
                        .fill(new FluidStack(Fluids.WATER, 100_000), IFluidHandler.FluidAction.EXECUTE) == 4_000,
                "one second of 200 mB per tick");
        helper.succeed();
    }

    /** Nothing can be taken out of a Sink. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkGivesNothing(GameTestHelper helper) {
        sink(helper, 0);
        IItemHandler items = sinkItems(helper, SINK, Direction.DOWN);
        items.insertItem(0, new ItemStack(Items.COBBLESTONE, 2), false);
        helper.assertTrue(items.extractItem(0, 64, false).isEmpty(), "nothing can be extracted");
        helper.assertTrue(sinkFluid(helper, SINK, Direction.DOWN).drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "nothing can be drained");
        helper.assertTrue(sinkEnergy(helper, SINK, Direction.DOWN).extractEnergy(1000, false) == 0, "no energy can be extracted");
        helper.succeed();
    }

    /** Unformed casings offer no capability on any face, and a hopper keeps its items. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
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
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 5, "the hopper should keep its items");
            helper.succeed();
        });
    }

    /** Voiding through any block turns the controller active; after the active time without voiding it turns idle. */
    @GameTest(template = EMPTY, timeoutTicks = 120)
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
            helper.setBlock(topPart.above(i), ServusManufacturingBlocks.CHIMNEY.get().defaultBlockState());
        }
    }

    /** Three Chimney blocks on a corner give height 3, only the top one shows the cap, and the cap moves up. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkCountsItsChimney(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 3);
        BlockPos column = new BlockPos(2, 1, 3);
        helper.assertTrue(sink.chimneyHeight() == 3, "expected height 3, got " + sink.chimneyHeight());
        helper.assertTrue(sink.chimneyColumn() == 7, "the column stands on the back right top part, got " + sink.chimneyColumn());
        helper.assertTrue(!helper.getBlockState(column.above(1)).getValue(ChimneyBlock.TOP), "the lowest block has no cap");
        helper.assertTrue(!helper.getBlockState(column.above(2)).getValue(ChimneyBlock.TOP), "the middle block has no cap");
        helper.assertTrue(helper.getBlockState(column.above(3)).getValue(ChimneyBlock.TOP), "the top block has the cap");
        helper.setBlock(column.above(4), ServusManufacturingBlocks.CHIMNEY.get().defaultBlockState());
        helper.assertTrue(!helper.getBlockState(column.above(3)).getValue(ChimneyBlock.TOP), "the cap moves up");
        helper.assertTrue(sink.chimneyHeight() == 4, "expected height 4, got " + sink.chimneyHeight());
        helper.succeed();
    }

    /** Five Chimney blocks on the front left corner make the Sink unlimited. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkChimneyMiddleRemoved(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 4);
        BlockPos column = new BlockPos(2, 1, 3);
        helper.assertTrue(sink.chimneyHeight() == 4, "expected height 4, got " + sink.chimneyHeight());
        helper.destroyBlock(column.above(2));
        helper.assertTrue(sink.chimneyHeight() == 1, "expected height 1, got " + sink.chimneyHeight());
        helper.assertTrue(helper.getBlockState(column.above(1)).getValue(ChimneyBlock.TOP), "the remaining block shows the cap");
        helper.setBlock(column.above(2), ServusManufacturingBlocks.CHIMNEY.get().defaultBlockState());
        helper.assertTrue(sink.chimneyHeight() == 4, "filling the gap joins the column again, got " + sink.chimneyHeight());
        helper.succeed();
    }

    /** Chimney blocks on stone or on unformed casings are decorative; forming the cube counts a column already there. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
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
        SinkBlockEntity casing = helper.getBlockEntity(SINK);
        helper.assertTrue(casing.chimneyHeight() == 0, "a casing counts no chimney, got " + casing.chimneyHeight());
        carrot(helper, SINK, Direction.NORTH, false);
        SinkBlockEntity sink = helper.getBlockEntity(SINK);
        helper.assertTrue(sink.chimneyHeight() == 3, "forming counts the column already standing, got " + sink.chimneyHeight());
        helper.succeed();
    }

    /** A lava bucket used on any formed block is emptied whatever the rate; on a casing it stays full. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkEmptiesLavaBucket(GameTestHelper helper) {
        sink(helper, 0);
        // Use up the fluid budget first: the bucket must not depend on it.
        sinkFluid(helper, SINK, Direction.UP).fill(new FluidStack(Fluids.WATER, 100_000), IFluidHandler.FluidAction.EXECUTE);
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
        helper.setBlock(casing, ServusManufacturingBlocks.SINK.get().defaultBlockState());
        ItemStack full = new ItemStack(Items.LAVA_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, full);
        BlockPos casingAbsolute = helper.absolutePos(casing);
        helper.getBlockState(casing).useItemOn(full, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(casingAbsolute).add(0, 0.5, 0), Direction.UP, casingAbsolute, false));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.LAVA_BUCKET), "a casing takes no lava");
        helper.succeed();
    }

    /** Shift clicking a stack into the trash slot voids all of it at once, even without a chimney; unforming closes the GUI. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void sinkGuiShowsUnlimited(GameTestHelper helper) {
        SinkBlockEntity sink = sink(helper, 5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SinkMenu menu = new SinkMenu(1, player.getInventory(), sink);
        helper.assertTrue(menu.chimneyHeight() == 5, "height 5, got " + menu.chimneyHeight());
        helper.assertTrue(menu.itemRate() == -1 && menu.fluidRate() == -1 && menu.energyRate() == -1, "all three Unlimited");
        helper.succeed();
    }
}
