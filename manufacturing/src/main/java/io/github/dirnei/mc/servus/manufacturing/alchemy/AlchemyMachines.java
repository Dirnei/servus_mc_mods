package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** What the Calciner and Coagulator share: synced data slots of their tanks and the push into neighbours. */
public final class AlchemyMachines {

    /** Container data after the machine's own: Salt, Sulfur, Mercury amounts, then the tank capacity. */
    public static final int DATA_PRINCIPLE_FIRST = MachineBlockEntity.DATA_COUNT;
    public static final int DATA_TANK_CAPACITY = DATA_PRINCIPLE_FIRST + 3;
    public static final int DATA_COUNT = DATA_PRINCIPLE_FIRST + 4;

    private AlchemyMachines() {
    }

    /** The data value at {@code index} if it is one of the tank entries, else -1. */
    public static int tankData(PrincipleTanks tanks, int index) {
        if (index >= DATA_PRINCIPLE_FIRST && index < DATA_TANK_CAPACITY) {
            return tanks.amount(Principle.values()[index - DATA_PRINCIPLE_FIRST]);
        }
        if (index == DATA_TANK_CAPACITY) {
            return tanks.capacity();
        }
        return -1;
    }

    /**
     * What a machine's tanks offer on a world face by its Fluids layer: every tank on All sides,
     * one tank on a side set to it, nothing on Off sides. A null face (no side) sees every tank.
     */
    public static IFluidHandler sideHandler(MachineBlockEntity machine, PrincipleTanks tanks, Direction face) {
        FluidSides sides = machine.fluidSides();
        if (face == null || sides == null) {
            return tanks;
        }
        RelativeSide side = machine.relativeSide(face);
        if (sides.isAll(side)) {
            return tanks;
        }
        int tank = sides.tank(side);
        return tank < 0 ? null : tanks.view(Principle.values()[tank]);
    }

    /** Fills touching fluid handlers with each principle on the sides set to that principle or to All. */
    public static void push(Level level, BlockPos pos, PrincipleTanks tanks, MachineBlockEntity machine) {
        for (Direction direction : Direction.values()) {
            if (sideHandler(machine, tanks, direction) == null) {
                continue;
            }
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(direction), direction.getOpposite());
            if (neighbour == null || neighbour == tanks) {
                continue;
            }
            for (Principle principle : Principle.values()) {
                FluidTank tank = tanks.tank(principle);
                if (tank.isEmpty() || !machine.fluidSideExposes(direction, principle.ordinal())) {
                    continue;
                }
                FluidStack offer = tank.getFluid().copy();
                int filled = neighbour.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }
}
