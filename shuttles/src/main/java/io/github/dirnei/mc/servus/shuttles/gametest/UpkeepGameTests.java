package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.data.UpkeepRequest;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorGameTests;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Upkeep of a Coal Generator from a warehouse over the data line. A mini
 * warehouse with its terminal holds coal and Coal Blocks; data ducts run east
 * of the terminal's duct to a Coal Generator beside the last one. The
 * generator's buffer is kept full, so it burns nothing while the test watches
 * its fuel slot.
 */
public final class UpkeepGameTests {

    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);
    private static final BlockPos GENERATOR = new BlockPos(6, 2, 3);

    private UpkeepGameTests() {
    }

    private static MiniWarehouse build(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal()
                .stock(new ItemStack(Items.COAL, 32), new ItemStack(Items.COAL_BLOCK, 32));
        for (int x = 4; x <= 5; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 2, 3));
        }
        CoalGeneratorGameTests.place(helper, GENERATOR, Direction.NORTH);
        CoalGeneratorBlockEntity generator = generator(helper);
        generator.energyStorage().setEnergy(generator.energyStorage().getMaxEnergyStored());
        return warehouse;
    }

    private static CoalGeneratorBlockEntity generator(GameTestHelper helper) {
        return helper.getBlockEntity(GENERATOR, CoalGeneratorBlockEntity.class);
    }

    private static ItemStack fuel(GameTestHelper helper) {
        return generator(helper).inventory().getStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL);
    }

    private static void setFuel(GameTestHelper helper, ItemStack stack) {
        generator(helper).inventory().setStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL, stack);
    }

    private static int openShipments(GameTestHelper helper, MiniWarehouse warehouse) {
        return (int) helper.getBlockEntity(warehouse.packingPos(), PackingStationBlockEntity.class).shipments().stream()
                .filter(shipment -> !shipment.ended()).count();
    }

    /** A generator holding Coal Blocks is topped up with Coal Blocks, never with coal. */
    @ServusGameTest(timeoutTicks = 800)
    public static void coalBlocksTopUpWithCoalBlocks(GameTestHelper helper) {
        MiniWarehouse warehouse = build(helper);
        setFuel(helper, new ItemStack(Items.COAL_BLOCK, 4));
        Waits.when(helper, () -> fuel(helper).is(Items.COAL_BLOCK) && fuel(helper).getCount() == 16
                        && generator(helper).upkeep().upkeepRequest() == null, 700,
                () -> "the fuel slot should reach 16 Coal Blocks and the request end at the next check, got " + fuel(helper)
                        + ", request " + generator(helper).upkeep().upkeepRequest(), () -> {
            helper.assertTrue(warehouse.rackCount(Items.COAL) == 32, "no coal left the rack, got " + warehouse.rackCount(Items.COAL));
            helper.assertTrue(warehouse.rackCount(Items.COAL_BLOCK) == 20, "12 Coal Blocks left the rack, got "
                    + warehouse.rackCount(Items.COAL_BLOCK));
            helper.succeed();
        });
    }

    /**
     * Coal asked for an empty slot is refused once a Coal Block sits there: the
     * upkeep request times out, its shipment is cancelled and the coal goes back
     * into storage, and only then are Coal Blocks asked for. Never more than one
     * shipment is open at a time.
     */
    @ServusGameTest(timeoutTicks = 2600)
    public static void refusedUpkeepIsCancelledAfterTimeout(GameTestHelper helper) {
        MiniWarehouse warehouse = build(helper);
        int[] mostOpen = {0};
        helper.onEachTick(() -> mostOpen[0] = Math.max(mostOpen[0], openShipments(helper, warehouse)));
        Waits.when(helper, () -> generator(helper).upkeep().upkeepRequest() != null, 200,
                () -> "the empty generator should ask for coal", () -> {
            UpkeepRequest first = generator(helper).upkeep().upkeepRequest();
            helper.assertTrue(first.remaining() == 16, "16 coal are asked, got " + first.remaining());
            setFuel(helper, new ItemStack(Items.COAL_BLOCK));
            Waits.when(helper, () -> fuel(helper).is(Items.COAL_BLOCK) && fuel(helper).getCount() == 16
                            && warehouse.rackCount(Items.COAL) == 32, 2400,
                    () -> "Coal Blocks should follow once the coal was given up, slot " + fuel(helper)
                            + ", coal in the rack " + warehouse.rackCount(Items.COAL), () -> {
                UpkeepRequest last = generator(helper).upkeep().upkeepRequest();
                helper.assertTrue(last == null || last.ticket() != first.ticket(), "the coal request is gone");
                helper.assertTrue(mostOpen[0] == 1, "one shipment open at a time, got " + mostOpen[0]);
                helper.succeed();
            });
        });
    }
}
