package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.TankRole;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidRole;
import io.github.dirnei.mc.servus.shuttles.menu.FluidOutboundMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The Fluid Outbound's tanks: each keeps a fluid and an amount that tankers
 * restock it to from the warehouse's Fluid Racks, and takes only that fluid.
 * Sides hand fluid out on Push or Output and take none in; the back pushes on
 * All by default. A tank whose keep is cleared keeps what it holds until it is drained.
 */
public class FluidOutboundBlockEntity extends FluidStationBlockEntity {

    private static final String KEEP_FLUID = "KeepFluid";
    private static final String KEEP_AMOUNT = "KeepAmount";

    private final List<FluidKeep<Fluid>> keeps;

    public FluidOutboundBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.FLUID_OUTBOUND.get(), pos, state, SideConfig.OUTBOUND_MODES, SideMode.DISABLED, TankRole.ANY);
        keeps = new ArrayList<>(Collections.nCopies(tankCount(), FluidKeep.none()));
        fluidSides().setMode(RelativeSide.BACK, SideMode.PUSH);
    }

    @Override
    public FluidRole role() {
        return FluidRole.EXPORT;
    }

    /** What a tank keeps. */
    public FluidKeep<Fluid> keep(int tank) {
        return tank >= 0 && tank < keeps.size() ? keeps.get(tank) : FluidKeep.none();
    }

    /** Keeps that fluid in a tank at the amount it kept before, or nothing when the fluid is null or empty. */
    public void setKeepFluid(int tank, @Nullable Fluid fluid) {
        setKeep(tank, keep(tank).withFluid(fluid == null || fluid == Fluids.EMPTY ? null : fluid));
    }

    /** Steps a tank's keep amount by {@code delta} mB, limited to 0 and the tank's capacity; nothing without a keep fluid. */
    public void stepKeep(int tank, int delta) {
        if (keep(tank).isSet()) {
            setKeep(tank, keep(tank).step(delta, tank(tank).getCapacity()));
        }
    }

    private void setKeep(int tank, FluidKeep<Fluid> keep) {
        if (tank < 0 || tank >= keeps.size() || keeps.get(tank).equals(keep)) {
            return;
        }
        keeps.set(tank, keep.clamp(tank(tank).getCapacity()));
        sidesChanged();
    }

    /** Whether every tank holds what it keeps. */
    public boolean keepsMet() {
        for (int tank = 0; tank < keeps.size(); tank++) {
            FluidStack held = tank(tank).getFluid();
            int target = keeps.get(tank).target();
            if (target > 0 && (held.getAmount() < target || !held.isEmpty() && !keeps.get(tank).accepts(held.getFluid()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean tankTakes(int tank, FluidStack stack) {
        return keep(tank).accepts(stack.getFluid());
    }

    /** The fluid a tank holds, else the fluid it keeps, so the side panel shows what the tank is for. */
    @Override
    public Fluid sideTankFluid(int tank) {
        Fluid held = super.sideTankFluid(tank);
        if (held != Fluids.EMPTY || level != null && level.isClientSide()) {
            return held;
        }
        Fluid kept = keep(tank).fluid();
        return kept == null ? Fluids.EMPTY : kept;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidOutboundMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int tank = 0; tank < keeps.size(); tank++) {
            FluidKeep<Fluid> keep = keeps.get(tank);
            if (keep.isSet()) {
                output.putString(KEEP_FLUID + tank, BuiltInRegistries.FLUID.getKey(keep.fluid()).toString());
                output.putInt(KEEP_AMOUNT + tank, keep.amount());
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int tank = 0; tank < keeps.size(); tank++) {
            Fluid fluid = input.getString(KEEP_FLUID + tank).map(Identifier::tryParse).flatMap(BuiltInRegistries.FLUID::getOptional)
                    .filter(found -> found != Fluids.EMPTY).orElse(null);
            keeps.set(tank, fluid == null ? FluidKeep.none()
                    : new FluidKeep<>(fluid, input.getIntOr(KEEP_AMOUNT + tank, 0)).clamp(tank(tank).getCapacity()));
        }
    }
}
