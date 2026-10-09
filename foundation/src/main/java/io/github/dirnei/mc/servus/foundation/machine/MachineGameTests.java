package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The machines Foundation owns: Coal Generator, pumps, Sink and Chunk Loader. */
@GameTestHolder(ServusFoundationIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineGameTests {

    static final String EMPTY = "empty_12x6x12";
    static final BlockPos MACHINE = new BlockPos(3, 1, 3);

    private MachineGameTests() {
    }

    /** One coal in the fuel slot fills the generator's buffer at its rate. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void generatorMakesEnergyFromCoal(GameTestHelper helper) {
        helper.setBlock(MACHINE, MachineContent.COAL_GENERATOR.get().defaultBlockState());
        CoalGeneratorBlockEntity generator = helper.getBlockEntity(MACHINE);
        generator.inventory().setStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
        helper.runAfterDelay(20, () -> {
            int stored = generator.energyStorage().getEnergyStored();
            helper.assertTrue(stored > 0, "the generator should have produced energy, has " + stored);
            helper.assertTrue(generator.energyStorage().receiveEnergy(1000, true) == 0, "the generator should never receive energy");
            helper.succeed();
        });
    }
}
