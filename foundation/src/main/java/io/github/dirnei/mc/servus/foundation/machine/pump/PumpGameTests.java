package io.github.dirnei.mc.servus.foundation.machine.pump;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpMachine;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** The pumps on a pond: water into a buffer, fixed sides, stubs and energy. */
@GameTestHolder(ServusFoundationIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PumpGameTests {

    private static final String EMPTY = "empty_12x6x12";

    private PumpGameTests() {
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
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
        for (Line line : lines) {
            duct.addLine(line);
        }
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
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
        placeBurnerPump(helper, PUMP);
        MachineBlockEntity pump = helper.getBlockEntity(PUMP);
        pump.inventory().setStackInSlot(BurnerPumpBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, coal));
    }

    /** Both halves of a Burner Pump facing north; {@code setBlock} skips {@code setPlacedBy}, which would set the top. */
    static void placeBurnerPump(GameTestHelper helper, BlockPos pos) {
        BlockState lower = facing(MachineContent.BURNER_PUMP.get(), Direction.NORTH);
        helper.setBlock(pos, lower);
        helper.setBlock(pos.above(), MachineContent.BURNER_PUMP.get().upperOf(lower));
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
            IFluidHandler east = pumpFluid(helper, Direction.EAST);
            helper.assertTrue(east != null && east.getFluidInTank(0).is(Fluids.WATER) && east.getFluidInTank(0).getAmount() > 0,
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
        helper.assertTrue(stub(helper, PumpBlock.UP) == 0, "the Burner Pump's top is its upper half, never a stub");
        helper.succeed();
    }

    /** A Power Cable meets an Electric Pump with a stub on its sides, but no cable reaches its front. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void electricPumpStubForPowerCable(GameTestHelper helper) {
        helper.setBlock(PUMP, facing(MachineContent.ELECTRIC_PUMP.get(), Direction.NORTH));
        duct(helper, PUMP.east(), Line.ENERGY);
        duct(helper, PUMP.north(), Line.ENERGY);
        helper.assertTrue(stub(helper, PumpBlock.EAST) == 1, "a Power Cable on the side gets a thin stub");
        helper.assertTrue(stub(helper, PumpBlock.NORTH) == 0, "the front takes no power and gets no stub");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(PUMP), Direction.NORTH) == null,
                "no energy through the front");
        duct(helper, PUMP.above(), Line.FLUID);
        helper.assertTrue(stub(helper, PumpBlock.UP) == 1, "a Fluid Pipe on top reaches the outlet");
        helper.succeed();
    }

    /** A pump never accepts fluid, and its front and bottom are fixed Off. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void pumpRefusesFluidAndClosesFrontAndBottom(GameTestHelper helper) {
        burnerPump(helper, 0);
        IFluidHandler side = pumpFluid(helper, Direction.EAST);
        helper.assertTrue(side != null, "a side is Auto and offers the buffer");
        helper.assertTrue(side.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "a pump accepts nothing");
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
        helper.setBlock(PUMP, facing(MachineContent.ELECTRIC_PUMP.get(), Direction.NORTH));
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
        helper.setBlock(PUMP, facing(MachineContent.ELECTRIC_PUMP.get(), Direction.NORTH));
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

    /** A Coal Generator beside an Electric Pump fills the pump's buffer without a cable. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void generatorPowersAdjacentPump(GameTestHelper helper) {
        BlockPos pump = new BlockPos(3, 1, 3);
        // Facing north, the pump takes energy on its east side.
        helper.setBlock(pump, facing(MachineContent.ELECTRIC_PUMP.get(), Direction.NORTH));
        BlockPos generator = pump.east();
        helper.setBlock(generator, facing(MachineContent.COAL_GENERATOR.get(), Direction.NORTH));
        MachineBlockEntity burner = helper.getBlockEntity(generator);
        burner.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 4));
        helper.succeedWhen(() -> {
            PoweredMachineBlockEntity powered = helper.getBlockEntity(pump);
            helper.assertTrue(powered.energyStorage().getEnergyStored() > 0, "the pump should receive energy from the generator");
        });
    }

    /**
     * Two generators and two Electric Pumps along one Power Cable run: generator A
     * and pump A on channel 1, generator B (no fuel) and pump B on channel 2. Only
     * pump A gets energy, so channels keep energy apart.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void energyStaysOnItsChannel(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            duct(helper, new BlockPos(7, 2, z), Line.ENERGY);
        }
        BlockPos generatorA = new BlockPos(6, 2, 1);
        BlockPos pumpA = new BlockPos(8, 2, 1);
        BlockPos generatorB = new BlockPos(6, 2, 3);
        BlockPos pumpB = new BlockPos(8, 2, 3);
        helper.setBlock(generatorA, facing(MachineContent.COAL_GENERATOR.get(), Direction.NORTH));
        MachineBlockEntity fuelled = helper.getBlockEntity(generatorA);
        fuelled.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 64));
        helper.setBlock(generatorB, facing(MachineContent.COAL_GENERATOR.get(), Direction.NORTH));
        // Facing north, a pump takes energy on its west side, where the cable runs.
        helper.setBlock(pumpA, facing(MachineContent.ELECTRIC_PUMP.get(), Direction.NORTH));
        helper.setBlock(pumpB, facing(MachineContent.ELECTRIC_PUMP.get(), Direction.NORTH));
        UtilityDuctBlockEntity ductB = helper.getBlockEntity(new BlockPos(7, 2, 3));
        ductB.setChannel(Line.ENERGY, Direction.WEST, 1);
        ductB.setChannel(Line.ENERGY, Direction.EAST, 1);

        helper.runAfterDelay(60, () -> {
            PoweredMachineBlockEntity a = helper.getBlockEntity(pumpA);
            PoweredMachineBlockEntity b = helper.getBlockEntity(pumpB);
            helper.assertTrue(a.energyStorage().getEnergyStored() > 0, "pump A on channel 1 should receive energy");
            helper.assertTrue(b.energyStorage().getEnergyStored() == 0,
                    "pump B on channel 2 must stay empty, got " + b.energyStorage().getEnergyStored());
            helper.succeed();
        });
    }

    /** The Electric Pump is crafted by hand from a Burner Pump, iron, copper and redstone; no assembler, no research. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void electricPumpIsHandCrafted(GameTestHelper helper) {
        ItemStack c = new ItemStack(Items.COPPER_INGOT);
        ItemStack r = new ItemStack(Items.REDSTONE);
        ItemStack i = new ItemStack(Items.IRON_INGOT);
        ItemStack p = new ItemStack(MachineContent.BURNER_PUMP_ITEM.get());
        CraftingInput input = CraftingInput.of(3, 3, List.of(c, r, c, i, p, i, c, r, c));
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "the Electric Pump pattern should match a crafting recipe");
        ItemStack result = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(MachineContent.ELECTRIC_PUMP_ITEM.get()), "the result should be an Electric Pump, got " + result);
        helper.succeed();
    }

    // The Burner Pump as a two block engine.

    private static final BlockPos ENGINE = new BlockPos(6, 2, 6);

    /** Uses a Burner Pump item on the top of the block below {@code pos} as a survival player facing north. */
    private static ItemStack placeWithItem(GameTestHelper helper, BlockPos pos) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(MachineContent.BURNER_PUMP_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos below = helper.absolutePos(pos.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        return stack;
    }

    private static boolean isHalf(GameTestHelper helper, BlockPos pos, DoubleBlockHalf half) {
        BlockState state = helper.getBlockState(pos);
        return state.is(MachineContent.BURNER_PUMP.get()) && state.getValue(BurnerPumpBlock.HALF) == half;
    }

    /** One item fills the clicked spot and the block above, facing the same way. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void burnerPumpPlacesTwoHalves(GameTestHelper helper) {
        helper.setBlock(ENGINE.below(), Blocks.STONE.defaultBlockState());
        ItemStack left = placeWithItem(helper, ENGINE);
        helper.assertTrue(left.isEmpty(), "the item is used up");
        helper.assertTrue(isHalf(helper, ENGINE, DoubleBlockHalf.LOWER), "the lower half is placed");
        helper.assertTrue(isHalf(helper, ENGINE.above(), DoubleBlockHalf.UPPER), "the upper half is placed above");
        helper.assertTrue(helper.getBlockState(ENGINE).getValue(MachineBlock.FACING) == helper.getBlockState(ENGINE.above()).getValue(MachineBlock.FACING),
                "both halves face the same way");
        helper.assertTrue(helper.getBlockEntity(ENGINE) instanceof BurnerPumpBlockEntity, "the lower half holds the machine");
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(ENGINE.above())) == null, "the upper half has no block entity");
        helper.succeed();
    }

    /** Without room above nothing is placed and the item stays. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void burnerPumpNeedsRoomAbove(GameTestHelper helper) {
        helper.setBlock(ENGINE.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(ENGINE.above(), Blocks.STONE.defaultBlockState());
        ItemStack left = placeWithItem(helper, ENGINE);
        helper.assertTrue(left.getCount() == 1, "the item stays in the hand");
        helper.assertTrue(helper.getBlockState(ENGINE).isAir(), "nothing is placed");
        helper.succeed();
    }

    /**
     * A player above a pond looks straight down at the water at {@code pos.below()} and
     * uses the item on the pond floor, as the client reports a click through water.
     */
    private static ItemStack placeOnWater(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        helper.setBlock(pos.below(2), Blocks.STONE.defaultBlockState());
        helper.setBlock(pos.below(), Blocks.WATER.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(pos.above(2)));
        player.moveTo(feet.x, feet.y, feet.z, 0, 90);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(pos.below(2));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        return stack;
    }

    /** Clicking through the water puts the Burner Pump on the surface, not on the pond floor. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void burnerPumpPlacesOnWaterSurface(GameTestHelper helper) {
        ItemStack left = placeOnWater(helper, ENGINE, new ItemStack(MachineContent.BURNER_PUMP_ITEM.get()));
        helper.assertTrue(left.isEmpty(), "the item is used up");
        helper.assertTrue(isHalf(helper, ENGINE, DoubleBlockHalf.LOWER), "the lower half sits on the water");
        helper.assertTrue(isHalf(helper, ENGINE.above(), DoubleBlockHalf.UPPER), "the upper half is placed above");
        helper.assertTrue(helper.getBlockState(ENGINE.below()).is(Blocks.WATER), "the water source stays");
        helper.succeed();
    }

    /** The Electric Pump lands on the water surface the same way. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void electricPumpPlacesOnWaterSurface(GameTestHelper helper) {
        ItemStack left = placeOnWater(helper, ENGINE, new ItemStack(MachineContent.ELECTRIC_PUMP_ITEM.get()));
        helper.assertTrue(left.isEmpty(), "the item is used up");
        helper.assertTrue(helper.getBlockState(ENGINE).is(MachineContent.ELECTRIC_PUMP.get()), "the pump sits on the water");
        helper.assertTrue(helper.getBlockState(ENGINE.below()).is(Blocks.WATER), "the water source stays");
        helper.succeed();
    }

    private static List<ItemEntity> drops(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2));
    }

    private static int count(List<ItemEntity> drops, net.minecraft.world.item.Item item) {
        return drops.stream().filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum();
    }

    /** Breaking either half removes both and drops one pump plus the fuel. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void breakingEitherHalfDropsOnePump(GameTestHelper helper) {
        BlockPos second = ENGINE.east(3);
        for (BlockPos pos : new BlockPos[] {ENGINE, second}) {
            placeBurnerPump(helper, pos);
            MachineBlockEntity pump = helper.getBlockEntity(pos);
            pump.inventory().setStackInSlot(BurnerPumpBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 5));
        }
        helper.getLevel().destroyBlock(helper.absolutePos(ENGINE.above()), true);
        helper.getLevel().destroyBlock(helper.absolutePos(second), true);
        for (BlockPos pos : new BlockPos[] {ENGINE, second}) {
            helper.assertTrue(helper.getBlockState(pos).isAir() && helper.getBlockState(pos.above()).isAir(), "both halves are gone at " + pos);
            List<ItemEntity> drops = drops(helper, pos);
            helper.assertTrue(count(drops, MachineContent.BURNER_PUMP_ITEM.get()) == 1, "exactly one Burner Pump drops at " + pos);
            helper.assertTrue(count(drops, Items.COAL) == 5, "the fuel drops at " + pos);
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    /** The upper half offers nothing, and a duct beside it does not connect. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void burnerPumpUpperHalfHasNoCapabilities(GameTestHelper helper) {
        placeBurnerPump(helper, ENGINE);
        BlockPos top = helper.absolutePos(ENGINE.above());
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, top, side) == null, "no fluid on " + side);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, top, side) == null, "no items on " + side);
        }
        duct(helper, ENGINE.above().east(), Line.FLUID);
        UtilityDuctBlockEntity duct = helper.getBlockEntity(ENGINE.above().east());
        helper.assertFalse(duct.hasArm(Line.FLUID, Direction.WEST), "the duct shows no arm toward the upper half");
        helper.succeed();
    }

    /** The lower half's top faces the upper half: no water, no fuel, no stub. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void burnerPumpTopFaceClosed(GameTestHelper helper) {
        placeBurnerPump(helper, ENGINE);
        BlockPos pos = helper.absolutePos(ENGINE);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP) == null, "no water upward");
        IItemHandler above = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(above == null || ItemHandlerHelper.insertItem(above, new ItemStack(Items.COAL, 2), false).getCount() == 2,
                "no fuel from above");
        helper.assertTrue(helper.getBlockState(ENGINE).getValue(PumpBlock.UP) == 0, "no stub on top");
        helper.succeed();
    }

    /** Right clicking the chimney reaches the machine below. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void burnerPumpUpperHalfLeadsToTheEngine(GameTestHelper helper) {
        placeBurnerPump(helper, ENGINE);
        BlockState upper = helper.getBlockState(ENGINE.above());
        helper.assertTrue(BurnerPumpBlock.lowerPos(upper, helper.absolutePos(ENGINE.above())).equals(helper.absolutePos(ENGINE)),
                "the upper half points at the lower half");
        helper.succeed();
    }
}
