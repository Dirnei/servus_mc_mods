package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.TankRole;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidRole;
import io.github.dirnei.mc.servus.shuttles.menu.FluidOutboundMenu;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
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
 * The Fluid Outbound's tank keeps a fluid and an amount that tankers restock
 * it to from the warehouse's Fluid Racks, and takes only that fluid. Sides
 * hand fluid out on Push or Output and take none in; the back pushes by
 * default. A tank whose keep is cleared keeps what it holds until it is drained.
 */
public class FluidOutboundBlockEntity extends FluidStationBlockEntity {

    private static final String KEEP_FLUID = "KeepFluid";
    private static final String KEEP_AMOUNT = "KeepAmount";

    private FluidKeep<Fluid> keep = FluidKeep.none();

    public FluidOutboundBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.FLUID_OUTBOUND.get(), pos, state, SideConfig.OUTBOUND_MODES, SideMode.DISABLED, TankRole.ANY);
        fluidSides().setMode(RelativeSide.BACK, SideMode.PUSH);
    }

    @Override
    public FluidRole role() {
        return FluidRole.EXPORT;
    }

    /** What the tank keeps. */
    public FluidKeep<Fluid> keep() {
        return keep;
    }

    /** Keeps that fluid at the amount kept before, or nothing when the fluid is null or empty. */
    public void setKeepFluid(@Nullable Fluid fluid) {
        setKeep(keep.withFluid(fluid == null || fluid == Fluids.EMPTY ? null : fluid));
    }

    /** Steps the keep amount by {@code delta} mB, limited to 0 and the tank's capacity; nothing without a keep fluid. */
    public void stepKeep(int delta) {
        if (keep.isSet()) {
            setKeep(keep.step(delta, tank().getCapacity()));
        }
    }

    private void setKeep(FluidKeep<Fluid> next) {
        FluidKeep<Fluid> clamped = next.clamp(tank().getCapacity());
        if (!keep.equals(clamped)) {
            keep = clamped;
            sidesChanged();
        }
    }

    @Override
    protected boolean tankTakes(FluidStack stack) {
        return keep.accepts(stack.getFluid());
    }

    /** The fluid the tank holds, else the fluid it keeps, so the side panel shows what the tank is for. */
    @Override
    public Fluid sideTankFluid(int index) {
        Fluid held = super.sideTankFluid(index);
        if (held != Fluids.EMPTY || level != null && level.isClientSide()) {
            return held;
        }
        Fluid kept = keep.fluid();
        return kept == null ? Fluids.EMPTY : kept;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidOutboundMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (keep.isSet()) {
            output.putString(KEEP_FLUID, BuiltInRegistries.FLUID.getKey(keep.fluid()).toString());
            output.putInt(KEEP_AMOUNT, keep.amount());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Fluid fluid = input.getString(KEEP_FLUID).map(Identifier::tryParse).flatMap(BuiltInRegistries.FLUID::getOptional)
                .filter(found -> found != Fluids.EMPTY).orElse(null);
        keep = fluid == null ? FluidKeep.none() : new FluidKeep<>(fluid, input.getIntOr(KEEP_AMOUNT, 0)).clamp(tank().getCapacity());
    }
}
