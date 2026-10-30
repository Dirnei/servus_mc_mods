package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidInboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidOutboundBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import java.util.List;
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
 * to south rail on x 4 from z 2 to 11 and a garage east of the rail at z 3.
 * The Fluid Inbound and the Fluid Outbound stand east of the rail facing it,
 * at z 5 and z 7; Fluid Rack shelves of one bay stand west of it facing it,
 * at z 8 and z 10. Positions are relative to the test.
 */
final class TankerFixtures {

    static final BlockPos CONTROLLER = new BlockPos(4, 1, 1);
    static final BlockPos GARAGE = new BlockPos(5, 1, 3);
    static final BlockPos INBOUND = new BlockPos(5, 1, 5);
    static final BlockPos OUTBOUND = new BlockPos(5, 1, 7);
    /** The port bays of the two one bay shelves, the nearer one first. */
    static final BlockPos NEAR = new BlockPos(3, 1, 8);
    static final BlockPos FAR = new BlockPos(3, 1, 10);
    /** The way the stations east of the rail face. */
    static final Direction STATION_FRONT = Direction.WEST;

    private TankerFixtures() {
    }

    /** The controller, the rail and an empty garage. */
    static ShuttleGarageBlockEntity warehouse(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        RailFixtures.railAlongZ(helper, CONTROLLER.getX(), 2, 11);
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

    /** The Fluid Inbound at {@link #INBOUND}. */
    static FluidInboundBlockEntity inbound(GameTestHelper helper) {
        return FluidStationFixtures.place(helper, INBOUND, STATION_FRONT);
    }

    /** A Fluid Inbound east of the rail at {@code pos}, facing it. */
    static FluidInboundBlockEntity inbound(GameTestHelper helper, BlockPos pos) {
        return FluidStationFixtures.place(helper, pos, STATION_FRONT);
    }

    /** The Fluid Outbound at {@link #OUTBOUND}, keeping {@code amount} mB of the fluid in its first tank. */
    static FluidOutboundBlockEntity outbound(GameTestHelper helper, Fluid fluid, int amount) {
        return outbound(helper, OUTBOUND, fluid, amount);
    }

    /** A Fluid Outbound east of the rail at {@code pos}, keeping {@code amount} mB of the fluid in its first tank. */
    static FluidOutboundBlockEntity outbound(GameTestHelper helper, BlockPos pos, Fluid fluid, int amount) {
        FluidOutboundBlockEntity station = FluidStationFixtures.outbound(helper, pos, STATION_FRONT);
        station.setKeepFluid(fluid);
        station.stepKeep(amount);
        return station;
    }

    /** A Fluid Rack shelf of one bay at {@code bay}, west of the rail and facing it; it settles on the next tick. */
    static void rack(GameTestHelper helper, BlockPos bay) {
        FluidRackFixtures.place(helper, bay, Direction.EAST, FluidRackFixtures.item());
    }

    /** Zones the shelf of the bay at {@code bay} for that fluid's bucket. */
    static void zone(GameTestHelper helper, BlockPos bay, Item bucket) {
        FluidRackBlock.stampShelf(helper.getLevel(), helper.absolutePos(bay),
                ZoneFixtures.zone(helper, "Zone", new ZoneFilter(List.of(new ItemStack(bucket)), List.of())));
    }

    /** Fills the station's tank with that much of the fluid, as a pipe or a bucket would. */
    static void fill(FluidStationBlockEntity station, Fluid fluid, int amount) {
        station.tank().fill(new FluidStack(fluid, amount), false);
    }

    /** What the station's tank holds. */
    static int amount(FluidStationBlockEntity station) {
        return station.tank().getFluidAmount();
    }

    /** Fills the shelf of the bay at {@code bay} as a pipe would. */
    static void fill(GameTestHelper helper, BlockPos bay, Fluid fluid, int amount) {
        FluidRackFixtures.fill(helper, bay, fluid, amount);
    }

    /** What the shelf of the bay at {@code bay} holds. */
    static int amount(GameTestHelper helper, BlockPos bay) {
        return FluidRackFixtures.amount(helper, bay);
    }

    static WarehouseNetwork network(GameTestHelper helper) {
        return helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().orElseThrow();
    }

    /** How many shuttles of the garage are out. */
    static int out(GameTestHelper helper) {
        return network(helper).garageOut(helper.absolutePos(GARAGE));
    }
}
