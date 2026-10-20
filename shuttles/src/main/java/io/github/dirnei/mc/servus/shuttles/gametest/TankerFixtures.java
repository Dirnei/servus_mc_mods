package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A warehouse for tanker runs in GameTests: the controller north of a north
 * to south rail on x 4 from z 2 to 11, a garage east of the rail at z 3 and
 * room for gantries over the rail at z 5, 7 and 9. Positions are relative to the test.
 */
final class TankerFixtures {

    static final BlockPos CONTROLLER = new BlockPos(4, 1, 1);
    static final BlockPos GARAGE = new BlockPos(5, 1, 3);
    /** Rails a gantry stands over: the Import one first, then two for Storage. */
    static final BlockPos IMPORT = new BlockPos(4, 1, 5);
    static final BlockPos NEAR = new BlockPos(4, 1, 7);
    static final BlockPos FAR = new BlockPos(4, 1, 9);

    private TankerFixtures() {
    }

    /** The controller, the rail and an empty garage. */
    static ShuttleGarageBlockEntity warehouse(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        GantryFixtures.railAlongZ(helper, CONTROLLER.getX(), 2, 11);
        return WarehouseFixtures.garage(helper, GARAGE, Direction.EAST, 0);
    }

    /** The warehouse with a Mk1 Tanker shuttle in the garage. */
    static ShuttleGarageBlockEntity withTanker(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = warehouse(helper);
        garage.shuttles().setStackInSlot(0, tanker(ServusShuttlesItems.SHUTTLE_MK1.get()));
        return garage;
    }

    /** A shuttle of that tier with a Tanker as its tool. */
    static ItemStack tanker(Item tier) {
        ItemStack shuttle = new ItemStack(tier);
        ShuttleModules.set(shuttle, ShuttleModules.EMPTY.withTool(Optional.of(ServusShuttlesItems.TANKER.get())));
        return shuttle;
    }

    /** A gantry over the rail at {@code rail} in that mode. */
    static FluidGantryBlockEntity gantry(GameTestHelper helper, BlockPos rail, GantryMode mode) {
        FluidGantryBlockEntity gantry = GantryFixtures.place(helper, rail);
        gantry.setMode(mode);
        return gantry;
    }

    /** Fills a tank of the gantry with that much of the fluid, as a pipe or a bucket would. */
    static void fill(FluidGantryBlockEntity gantry, int tank, Fluid fluid, int amount) {
        gantry.tank(tank).fill(new FluidStack(fluid, amount), false);
    }

    static int amount(FluidGantryBlockEntity gantry, int tank) {
        return gantry.tank(tank).getFluidAmount();
    }

    /** What both tanks of the gantry hold together. */
    static int total(FluidGantryBlockEntity gantry) {
        return amount(gantry, 0) + amount(gantry, 1);
    }

    static WarehouseNetwork network(GameTestHelper helper) {
        return helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().orElseThrow();
    }

    /** How many shuttles of the garage are out. */
    static int out(GameTestHelper helper) {
        return network(helper).garageOut(helper.absolutePos(GARAGE));
    }
}
