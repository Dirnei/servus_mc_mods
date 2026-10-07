package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.lib.production.ProductionProvider;
import io.github.dirnei.mc.servus.lib.production.ProductionRecipe;
import io.github.dirnei.mc.servus.lib.production.ServusLibCapabilities;
import io.github.dirnei.mc.servus.logistics.duct.Line;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.smelter.BurnerSmelterBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.pump.PumpMachine;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import io.github.dirnei.mc.servus.lib.side.RelativeSide;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.BurnerExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.lab.ResearchLabBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
        ProductionProvider empty = helper.getLevel().getCapability(ServusLibCapabilities.PRODUCTION, absolute, null);
        helper.assertTrue(empty != null, "the assembler should expose the production capability");
        helper.assertTrue(empty.recipes().isEmpty() && empty.active().isEmpty(), "nothing configured means no recipes");

        assembler.addRecipe(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "assembling/iron_gear"), null);
        ProductionProvider provider = helper.getLevel().getCapability(ServusLibCapabilities.PRODUCTION, absolute, Direction.UP);
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

    /** An Output side offers water to neighbours but does not push it; an Input side gives nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pumpOutputSideDoesNotPush(GameTestHelper helper) {
        pool(helper, 4);
        burnerPump(helper, 4);
        BlockPos cauldron = PUMP.west();
        helper.setBlock(cauldron, Blocks.CAULDRON.defaultBlockState());
        MachineBlockEntity pump = helper.getBlockEntity(PUMP);
        pump.sideConfig().set(RelativeSide.RIGHT, SideMode.OUTPUT);
        pump.sideConfig().set(RelativeSide.LEFT, SideMode.INPUT);
        helper.getLevel().invalidateCapabilities(helper.absolutePos(PUMP));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(helper.getBlockState(cauldron).is(Blocks.CAULDRON), "an Output side must not fill the cauldron");
            IFluidHandler right = pumpFluid(helper, Direction.WEST);
            helper.assertTrue(right != null && right.drain(250, IFluidHandler.FluidAction.SIMULATE).getAmount() == 250,
                    "an Output side can be drained");
            helper.assertTrue(pumpFluid(helper, Direction.EAST) == null, "an Input side gives no water");
            helper.succeed();
        });
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
}
