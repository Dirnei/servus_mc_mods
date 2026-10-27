package io.github.dirnei.mc.servus.shuttles.fluidrack;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The bays of a Fluid Rack section behind one tank, reached from any bay. A
 * fill or drain moves the amount and then splits the section's new total
 * into the bays' shares again. It takes only one fluid at a time, and only
 * the fluids its zone lists when the zone lists any. Inside a transaction it
 * only simulates; the move runs when the transaction commits.
 */
public final class FluidRackSectionHandler extends LegacyFluidHandler {

    private final FluidRackBlockEntity bay;

    FluidRackSectionHandler(FluidRackBlockEntity bay) {
        this.bay = bay;
    }

    /** What the section holds in all and how much it may hold. */
    public record Content(FluidStack fluid, int capacity) {

        public int room() {
            return Math.max(0, capacity - fluid.getAmount());
        }
    }

    private List<FluidRackBlockEntity> bays() {
        Level level = bay.getLevel();
        return level == null ? List.of(bay) : FluidRackBlock.bays(level, bay.getBlockPos());
    }

    /** The section's fluid summed over its bays, and its capacity. */
    public Content content() {
        return content(bays());
    }

    private static Content content(List<FluidRackBlockEntity> bays) {
        FluidStack fluid = FluidStack.EMPTY;
        int total = 0;
        for (FluidRackBlockEntity each : bays) {
            FluidStack held = each.share();
            if (held.isEmpty()) {
                continue;
            }
            if (fluid.isEmpty()) {
                fluid = held;
            }
            if (FluidStack.isSameFluidSameComponents(fluid, held)) {
                total += held.getAmount();
            }
        }
        int capacity = bays.size() * ShuttlesStatsManager.INSTANCE.fluidRackCapacityPerBay();
        return new Content(fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(total), capacity);
    }

    /** Whether the section's zone lets this fluid in: it lists it, or lists no fluids. */
    public boolean allows(Fluid fluid) {
        Set<Fluid> listed = bay.zoneFluids();
        return listed.isEmpty() || listed.contains(fluid);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return content().fluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return content().capacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return allows(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (resource.isEmpty() || !allows(resource.getFluid())) {
            return 0;
        }
        List<FluidRackBlockEntity> bays = bays();
        Content content = content(bays);
        if (!content.fluid().isEmpty() && !FluidStack.isSameFluidSameComponents(content.fluid(), resource)) {
            return 0;
        }
        int filled = Math.min(resource.getAmount(), content.room());
        if (filled > 0 && !simulate) {
            spread(bays, resource.copyWithAmount(content.fluid().getAmount() + filled));
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        List<FluidRackBlockEntity> bays = bays();
        Content content = content(bays);
        if (content.fluid().isEmpty() || !FluidStack.isSameFluidSameComponents(content.fluid(), resource)) {
            return FluidStack.EMPTY;
        }
        int drained = Math.min(resource.getAmount(), content.fluid().getAmount());
        if (drained > 0 && !simulate) {
            spread(bays, content.fluid().copyWithAmount(content.fluid().getAmount() - drained));
        }
        return drained <= 0 ? FluidStack.EMPTY : content.fluid().copyWithAmount(drained);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        FluidStack held = content().fluid();
        return held.isEmpty() || maxDrain <= 0 ? FluidStack.EMPTY : drain(held.copyWithAmount(maxDrain), simulate);
    }

    /** Writes the section's new total as even shares into its bays and tells every bay. */
    private static void spread(List<FluidRackBlockEntity> bays, FluidStack total) {
        int[] shares = FluidRackSection.shares(total.getAmount(), bays.size());
        for (int i = 0; i < bays.size(); i++) {
            bays.get(i).setShare(shares[i] > 0 ? total.copyWithAmount(shares[i]) : FluidStack.EMPTY);
        }
        for (FluidRackBlockEntity each : bays) {
            each.sectionChanged();
        }
    }
}
