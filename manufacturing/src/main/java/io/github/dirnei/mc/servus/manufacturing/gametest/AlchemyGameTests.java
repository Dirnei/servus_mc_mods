package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
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
}
