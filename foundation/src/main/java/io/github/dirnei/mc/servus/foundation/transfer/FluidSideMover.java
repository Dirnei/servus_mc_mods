package io.github.dirnei.mc.servus.foundation.transfer;

import io.github.dirnei.mc.servus.foundation.side.FluidSideRules;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Moves fluid between a block with a Fluids layer and its neighbours: Push and Auto sides give
 * from the tanks they reach, Pull sides take into them. Each move runs in its own transaction, so
 * no fluid is made or lost, and there is no rate limit. Server thread only.
 */
public final class FluidSideMover {

    private FluidSideMover() {
    }

    /** Runs {@link #move} when the game time is a multiple of {@code interval}. */
    public static void tick(Level level, BlockPos pos, FluidSideBlock block, int interval) {
        if (interval > 0 && level.getGameTime() % interval == 0) {
            move(level, pos, block);
        }
    }

    /** Pushes and pulls once through every side whose mode asks for it. */
    public static void move(Level level, BlockPos pos, FluidSideBlock block) {
        FluidSides sides = block.fluidSides();
        if (sides == null || level.isClientSide()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            RelativeSide side = block.relativeSide(direction);
            SideMode mode = sides.mode(side);
            if (sides.isLocked(side) || !(FluidSideRules.pushes(mode) || FluidSideRules.pulls(mode))) {
                continue;
            }
            ResourceHandler<FluidResource> neighbour = level.getCapability(Capabilities.Fluid.BLOCK, pos.relative(direction),
                    direction.getOpposite());
            if (neighbour == null) {
                continue;
            }
            if (FluidSideRules.pushes(mode)) {
                push(block, sides, side, neighbour);
            }
            if (FluidSideRules.pulls(mode)) {
                pull(block, sides, side, neighbour);
            }
        }
    }

    private static void push(FluidSideBlock block, FluidSides sides, RelativeSide side, ResourceHandler<FluidResource> neighbour) {
        for (int tank : FluidSideRules.drainOrder(sides, side, block.sideTankStates())) {
            try (Transaction tx = Transfer.open()) {
                ResourceHandlerUtil.move(block.sideSlots().get(tank), neighbour, resource -> true, Integer.MAX_VALUE, tx);
                tx.commit();
            }
        }
    }

    private static void pull(FluidSideBlock block, FluidSides sides, RelativeSide side, ResourceHandler<FluidResource> neighbour) {
        Set<FluidResource> offered = new LinkedHashSet<>();
        for (int index = 0; index < neighbour.size(); index++) {
            FluidResource resource = neighbour.getResource(index);
            if (!resource.isEmpty() && neighbour.getAmountAsLong(index) > 0) {
                offered.add(resource);
            }
        }
        for (FluidResource fluid : offered) {
            for (int tank : FluidSideRules.fillOrder(sides, side, block.sideTankStates(), fluid, block::sideTankAccepts)) {
                try (Transaction tx = Transfer.open()) {
                    ResourceHandlerUtil.move(neighbour, block.sideSlots().get(tank), fluid::equals, Integer.MAX_VALUE, tx);
                    tx.commit();
                }
            }
        }
    }
}
