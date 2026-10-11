package io.github.dirnei.mc.servus.foundation.transfer;

import java.util.function.Predicate;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A single fluid tank on the NeoForge transfer API with the methods the
 * Servus code used on the old fluid tank. It is a full
 * {@code ResourceHandler<FluidResource>} and can be exposed as a capability.
 *
 * <p>{@code fill} and {@code drain} open their own transaction and must only
 * be called outside of one, like the old tank methods.
 */
public class FluidSlot extends FluidStacksResourceHandler {

    private final Predicate<FluidStack> validator;

    public FluidSlot(int capacity) {
        this(capacity, stack -> true);
    }

    public FluidSlot(int capacity, Predicate<FluidStack> validator) {
        super(1, capacity);
        this.validator = validator;
    }

    /** Called after the contents changed. */
    protected void onContentsChanged() {
    }

    @Override
    protected final void onContentsChanged(int index, FluidStack previousContents) {
        onContentsChanged();
    }

    public boolean isFluidValid(FluidStack stack) {
        return validator.test(stack);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.isEmpty() || isFluidValid(resource.toStack(1));
    }

    /** The live stack in the tank; do not change it without calling {@link #setFluid}. */
    public FluidStack getFluid() {
        return stacks.get(0);
    }

    public int getFluidAmount() {
        return stacks.get(0).getAmount();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public int getSpace() {
        return Math.max(0, capacity - getFluidAmount());
    }

    public boolean isEmpty() {
        return stacks.get(0).isEmpty();
    }

    public void setFluid(FluidStack stack) {
        FluidStack previous = stacks.set(0, stack);
        onContentsChanged(0, previous);
    }

    /** Fills the tank; returns the amount taken. */
    public int fill(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int filled = insert(0, FluidResource.of(resource), resource.getAmount(), tx);
            if (!simulate) {
                tx.commit();
            }
            return filled;
        }
    }

    /** Drains up to {@code maxDrain} of whatever is in the tank. */
    public FluidStack drain(int maxDrain, boolean simulate) {
        FluidStack current = getFluid();
        if (current.isEmpty() || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        return drain(current.copyWithAmount(maxDrain), simulate);
    }

    /** Drains the given fluid; returns what came out. */
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidResource key = FluidResource.of(resource);
        try (Transaction tx = Transaction.openRoot()) {
            int drained = extract(0, key, resource.getAmount(), tx);
            if (!simulate) {
                tx.commit();
            }
            return drained <= 0 ? FluidStack.EMPTY : key.toStack(drained);
        }
    }

    public void save(ValueOutput output, String key) {
        output.store(key, FluidStack.OPTIONAL_CODEC, getFluid());
    }

    /** Loads the contents saved under {@code key}; the capacity stays. */
    public void load(ValueInput input, String key) {
        stacks.set(0, input.read(key, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY));
    }
}
