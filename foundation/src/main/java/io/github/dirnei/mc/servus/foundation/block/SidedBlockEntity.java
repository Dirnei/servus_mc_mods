package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import java.util.List;
import net.minecraft.core.Direction;

/**
 * A block entity with a side configuration relative to its horizontal facing. Every one has
 * the Items layer; blocks with several tanks add a Fluids layer.
 */
public interface SidedBlockEntity {

    SideConfig sideConfig();

    /** The horizontal facing the relative sides are measured from. */
    Direction facing();

    /**
     * Cycles a relative side, marks the block entity changed, refreshes
     * capability caches and syncs; returns the new mode.
     */
    SideMode cycleSide(RelativeSide side);

    /** The Fluids layer, or null when the block has none. */
    default FluidSides fluidSides() {
        return null;
    }

    /** The tanks the Fluids layer assigns, in tank index order; empty without a Fluids layer. */
    default List<SideTank> sideTanks() {
        return List.of();
    }

    /** Cycles a relative side of the Fluids layer like {@link #cycleSide}; returns the new value. */
    default int cycleFluidSide(RelativeSide side) {
        return FluidSides.ALL;
    }

    default RelativeSide relativeSide(Direction absolute) {
        return RelativeSide.fromSide(Conversions.toSide(facing()), Conversions.toSide(absolute));
    }

    default SideMode sideMode(Direction absolute) {
        return sideConfig().mode(relativeSide(absolute));
    }

    /** Cycles the side a world face belongs to. */
    default SideMode cycleFace(Direction face) {
        return cycleSide(relativeSide(face));
    }
}
