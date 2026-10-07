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
import io.github.dirnei.mc.servus.lib.side.RelativeSide;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
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
}
