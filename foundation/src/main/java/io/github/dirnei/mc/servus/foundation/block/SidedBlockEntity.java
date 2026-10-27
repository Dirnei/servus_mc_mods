package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;

/**
 * A block entity with a side configuration relative to its horizontal facing. Blocks that hold
 * items have the Items layer, blocks with fluid tanks a Fluids layer, many both.
 */
public interface SidedBlockEntity {

    /** The update tag key of the fluids behind the tank labels. */
    String TANK_LABELS = "TankLabels";

    /** The Items layer; a block without one returns a config whose sides are all locked Off. */
    SideConfig sideConfig();

    /** The horizontal facing the relative sides are measured from. */
    Direction facing();

    /**
     * Cycles a relative side, marks the block entity changed, refreshes
     * capability caches and syncs; returns the new mode.
     */
    SideMode cycleSide(RelativeSide side);

    /** Whether the block has an Items layer; false for a block that holds only fluid tanks. */
    default boolean hasItemsLayer() {
        return true;
    }

    /** The Fluids layer, or null when the block has none. */
    default FluidSides fluidSides() {
        return null;
    }

    /** The tanks the Fluids layer reaches, in tank index order; empty without a Fluids layer. */
    default List<SideTank> sideTanks() {
        return List.of();
    }

    /**
     * The fluid behind a tank's label: the fluid it holds, else the fluid it keeps or is set to
     * take, else empty. On the client it answers from the last update tag.
     */
    default Fluid sideTankFluid(int tank) {
        return Fluids.EMPTY;
    }

    /** The name and colour of a tank; blocks with fixed tank names override. */
    default TankLabel sideTankLabel(int tank) {
        return TankLabel.of(sideTankFluid(tank), tank);
    }

    /** The labels of every tank in index order. */
    default List<TankLabel> sideTankLabels() {
        List<TankLabel> labels = new ArrayList<>();
        for (int tank = 0; tank < sideTanks().size(); tank++) {
            labels.add(sideTankLabel(tank));
        }
        return labels;
    }

    /** Cycles the mode of a Fluids side like {@link #cycleSide}; returns the new mode. */
    default SideMode cycleFluidMode(RelativeSide side) {
        return SideMode.DISABLED;
    }

    /** Cycles the tank choice of a Fluids side like {@link #cycleSide}; returns the new choice. */
    default int cycleFluidTank(RelativeSide side) {
        return FluidSides.ALL;
    }

    /** The layer a carrot set to {@code wanted} works on here: the wanted one, or the only one the block has. */
    default SideLayer layerFor(SideLayer wanted) {
        if (fluidSides() == null) {
            return SideLayer.ITEMS;
        }
        return hasItemsLayer() ? wanted : SideLayer.FLUIDS;
    }

    /** Writes the fluids behind the tank labels into an update tag; nothing without a Fluids layer. */
    default void writeTankLabels(CompoundTag tag) {
        if (sideTanks().isEmpty()) {
            return;
        }
        List<Fluid> fluids = new ArrayList<>();
        for (int tank = 0; tank < sideTanks().size(); tank++) {
            fluids.add(sideTankFluid(tank));
        }
        tag.store(TANK_LABELS, BuiltInRegistries.FLUID.byNameCodec().listOf(), fluids);
    }

    /** Reads what {@link #writeTankLabels} wrote, empty when the tag has none. */
    static List<Fluid> readTankLabels(ValueInput input) {
        return input.read(TANK_LABELS, BuiltInRegistries.FLUID.byNameCodec().listOf()).orElse(List.of());
    }

    /** The fluid at a tank index of labels read by {@link #readTankLabels}, empty when missing. */
    static Fluid labelFluid(List<Fluid> labels, int tank) {
        return tank >= 0 && tank < labels.size() ? labels.get(tank) : Fluids.EMPTY;
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
