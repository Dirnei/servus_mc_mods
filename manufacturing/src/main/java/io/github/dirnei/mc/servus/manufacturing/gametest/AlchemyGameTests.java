package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The principles as fluids. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlchemyGameTests {

    static final String EMPTY = "empty_12x6x12";

    private AlchemyGameTests() {
    }

    /** A Fluid Tank stores Mercury, and an empty bucket takes it out as a Mercury bucket. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void bucketFillsFromATankOfMercury(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(tank != null, "the tank should expose a fluid handler");
        int filled = tank.fill(new FluidStack(PrincipleFluids.fluid(Principle.MERCURY), 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000, "the tank should take 1000 mB of Mercury, took " + filled);
        FluidActionResult result = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, 1000, null, true);
        helper.assertTrue(result.isSuccess() && result.getResult().is(PrincipleFluids.entry(Principle.MERCURY).bucket().get()),
                "the bucket should become a Mercury bucket, got " + result.getResult());
        helper.assertTrue(tank.getFluidInTank(0).isEmpty(), "the tank should be empty");
        helper.succeed();
    }

    static final BlockPos MACHINE = new BlockPos(3, 1, 3);

    static CalcinerBlockEntity calciner(GameTestHelper helper) {
        helper.setBlock(MACHINE, ServusManufacturingBlocks.CALCINER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CalcinerBlockEntity calciner = helper.getBlockEntity(MACHINE);
        calciner.energyStorage().receiveEnergy(8000, false);
        return calciner;
    }

    /** One cobblestone becomes 100 mB Salt and 10 mB Mercury in the Calciner's tanks. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void calcinerBurnsCobblestoneIntoPrinciples(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> {
            Principles held = calciner.tanks().contents();
            helper.assertTrue(held.equals(new Principles(100, 0, 10)), "expected 100 Salt and 10 Mercury, got " + held);
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).isEmpty(), "the cobblestone is used up");
        });
    }

    /** With no room for the Salt the Calciner waits and spends no energy. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void calcinerWaitsWhenATankIsFull(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        calciner.tanks().insert(new Principles(3950, 0, 0));
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        int energy = calciner.energyStorage().getEnergyStored();
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(calciner.energyStorage().getEnergyStored() == energy, "a blocked Calciner spends no energy");
            helper.assertTrue(calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).is(Items.COBBLESTONE), "the cobblestone stays");
            helper.assertTrue(calciner.tanks().amount(Principle.SALT) == 3950, "no Salt was added");
            helper.succeed();
        });
    }

    /** A Fluid Tank touching an Auto side receives the principles the Calciner makes. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void calcinerPushesIntoATouchingTank(GameTestHelper helper) {
        CalcinerBlockEntity calciner = calciner(helper);
        BlockPos tankPos = MACHINE.east();
        helper.setBlock(tankPos, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
        calciner.inventory().setStackInSlot(CalcinerBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> {
            IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(tankPos), Direction.WEST);
            helper.assertTrue(tank != null && tank.getFluidInTank(0).getFluid().isSame(PrincipleFluids.fluid(Principle.SALT))
                    && tank.getFluidInTank(0).getAmount() >= 100, "the tank should hold Salt, got " + (tank == null ? null : tank.getFluidInTank(0)));
        });
    }

    /** Items without a calcination recipe are refused. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void calcinerRefusesUnknownItems(GameTestHelper helper) {
        calciner(helper);
        IItemHandler side = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MACHINE), Direction.EAST);
        helper.assertTrue(side != null, "the side should take items");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(side, new ItemStack(Items.DIAMOND), false).getCount() == 1, "a diamond is refused");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(side, new ItemStack(Items.OAK_LOG), false).isEmpty(), "a log is taken");
        helper.succeed();
    }
}
